package com.chesslock.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.chesslock.MainActivity
import com.chesslock.R
import com.chesslock.data.BudgetStore
import com.chesslock.data.PlayerStore
import com.chesslock.data.SettingsStore
import com.chesslock.domain.budget.BudgetUrgency
import com.chesslock.domain.config.AppConfig
import com.chesslock.ui.common.withAppLanguage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget with today's remaining budget (SPEC 1). It refreshes on every budget, ELO or
 * limit write and once a minute on the wall clock, so it rolls over at midnight and never shows a
 * stale minute. Tapping it opens Home.
 */
class BudgetWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        BudgetWidgetTicker.ensureScheduled(context)
        render(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == BudgetWidgetTicker.ACTION_TICK) {
            // Re-arm before rendering so a render failure cannot stop the chain.
            BudgetWidgetTicker.ensureScheduled(context)
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, BudgetWidgetProvider::class.java))
            if (ids.isEmpty()) {
                BudgetWidgetTicker.cancel(context)
            } else {
                render(context, manager, ids)
            }
        }
    }

    override fun onEnabled(context: Context) {
        BudgetWidgetTicker.ensureScheduled(context)
        BudgetWidgetUpdater.requestUpdate(context)
    }

    override fun onDisabled(context: Context) {
        BudgetWidgetTicker.cancel(context)
    }

    /** Renders asynchronously; `goAsync` keeps the broadcast alive until the last write lands. */
    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val pending = goAsync()
        BudgetWidgetUpdater.scope.launch {
            try {
                BudgetWidgetUpdater.update(context, manager, ids)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Widget render failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BudgetWidget"
    }
}

/** Widget code runs outside an Activity, so Hilt hands it the same singletons through this point. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BudgetWidgetEntryPoint {
    fun budgetStore(): BudgetStore
    fun playerStore(): PlayerStore
    fun settingsStore(): SettingsStore
    fun config(): AppConfig
}

object BudgetWidgetUpdater {
    /** One process-wide scope; widget renders are short and never outlive it. */
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Cheap call for stores: does nothing when no widget is placed, so writes stay fast. */
    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext) ?: return
        val ids = manager.getAppWidgetIds(ComponentName(appContext, BudgetWidgetProvider::class.java))
        if (ids.isEmpty()) return
        scope.launch { update(appContext, manager, ids) }
    }

    /**
     * Re-arms the minute tick and repaints once after a boot, an app update or a launch. Alarms do
     * not survive a reboot or a force-stop, so this is what restarts the chain afterwards.
     */
    suspend fun refreshIfPlaced(context: Context) {
        val appContext = context.applicationContext
        try {
            val manager = AppWidgetManager.getInstance(appContext) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(appContext, BudgetWidgetProvider::class.java))
            if (ids.isEmpty()) return
            BudgetWidgetTicker.ensureScheduled(appContext)
            update(appContext, manager, ids)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Widget refresh failed", e)
        }
    }

    internal suspend fun update(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray,
    ) {
        if (ids.isEmpty()) return
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            BudgetWidgetEntryPoint::class.java,
        )
        val settings = entry.settingsStore().settings.first()
        val snapshot = buildWidgetSnapshot(
            budget = entry.budgetStore().currentState(),
            limitSeconds = settings.dailyLimitSeconds,
            capSeconds = settings.dailyCapSeconds,
            elo = entry.playerStore().currentState().elo,
            warnAtSeconds = entry.config().warnAtSeconds,
        )
        val views = buildRemoteViews(context.withAppLanguage(settings.language), snapshot)
        ids.forEach { id -> manager.updateAppWidget(id, views) }
    }

    private fun buildRemoteViews(context: Context, snapshot: WidgetSnapshot): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_budget)
        views.setTextViewText(R.id.widget_time, remainingText(context, snapshot.remainingSeconds))
        views.setTextViewText(R.id.widget_elo, context.getString(R.string.widget_elo_value, snapshot.elo))

        val captionRes = when (snapshot.urgency) {
            BudgetUrgency.COMFORTABLE -> R.string.home_time_left
            BudgetUrgency.WARNING -> R.string.home_urgency_warning
            BudgetUrgency.CRITICAL -> R.string.home_urgency_critical
            BudgetUrgency.EMPTY -> R.string.home_urgency_empty
        }
        val captionColor = when (snapshot.urgency) {
            BudgetUrgency.COMFORTABLE -> R.color.widget_muted
            BudgetUrgency.WARNING -> R.color.widget_warning
            BudgetUrgency.CRITICAL,
            BudgetUrgency.EMPTY,
            -> R.color.widget_critical
        }
        views.setTextViewText(R.id.widget_caption, context.getString(captionRes))
        views.setTextColor(R.id.widget_caption, context.getColor(captionColor))
        views.setProgressBar(R.id.widget_progress, 100, snapshot.progressPercent, false)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)
        return views
    }

    /** Minute-granularity clock: `29 min` below an hour, `1 h 12 min` above it. */
    private fun remainingText(context: Context, remainingSeconds: Long): String =
        when (val time = widgetTimeOf(remainingSeconds)) {
            is WidgetTime.Minutes -> context.getString(R.string.widget_time_minutes, time.minutes)
            is WidgetTime.Hours ->
                if (time.minutes == 0) {
                    context.getString(R.string.widget_time_hours, time.hours)
                } else {
                    context.getString(R.string.widget_time_hours_minutes, time.hours, time.minutes)
                }
        }

    private const val TAG = "BudgetWidget"
}

/**
 * Minute-aligned refresh. The platform floors `updatePeriodMillis` at 30 minutes, so a chained
 * one-shot RTC alarm provides the finer cadence. RTC (not RTC_WAKEUP) never wakes the device; in
 * Doze the tick is batched and delivered on the next wake, then the chain re-arms itself. A
 * one-shot alarm is used instead of `setInexactRepeating` because repeating alarms get a batching
 * window of up to 45 s even while the screen is on.
 */
internal object BudgetWidgetTicker {
    const val ACTION_TICK = "com.chesslock.widget.action.TICK"

    /** Schedules the next tick. Called from updates and again from the tick itself. */
    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        val alarms = appContext.getSystemService(AlarmManager::class.java) ?: return
        val intervalMs = widgetIntervalMillis(appContext)
        alarms.set(
            AlarmManager.RTC,
            nextWidgetTickAt(System.currentTimeMillis(), intervalMs),
            tickIntent(appContext),
        )
    }

    /** Called when the last widget is removed so no alarm is left behind. */
    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarms = appContext.getSystemService(AlarmManager::class.java) ?: return
        alarms.cancel(tickIntent(appContext))
    }

    private fun widgetIntervalMillis(context: Context): Long {
        val config = EntryPointAccessors
            .fromApplication(context, BudgetWidgetEntryPoint::class.java)
            .config()
        return config.widgetRefreshIntervalSeconds * 1_000L
    }

    private fun tickIntent(context: Context): PendingIntent {
        val intent = Intent(context, BudgetWidgetProvider::class.java).setAction(ACTION_TICK)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
