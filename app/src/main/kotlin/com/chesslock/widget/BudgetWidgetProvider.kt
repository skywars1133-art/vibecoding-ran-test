package com.chesslock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.chesslock.MainActivity
import com.chesslock.R
import com.chesslock.data.BudgetStore
import com.chesslock.data.PlayerStore
import com.chesslock.data.SettingsStore
import com.chesslock.domain.budget.BudgetUrgency
import com.chesslock.domain.config.AppConfig
import com.chesslock.ui.common.formatClock
import com.chesslock.ui.common.withAppLanguage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Home-screen widget with today's remaining budget (SPEC 1). It refreshes on every budget, ELO or
 * limit write and on the platform's own 30-minute tick, so it also rolls over at midnight without
 * running a permanent timer. Tapping it opens Home.
 */
class BudgetWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        BudgetWidgetUpdater.scope.launch {
            try {
                BudgetWidgetUpdater.update(context, appWidgetManager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    override fun onEnabled(context: Context) {
        BudgetWidgetUpdater.requestUpdate(context)
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
        views.setTextViewText(R.id.widget_time, formatClock(snapshot.remainingSeconds))
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
}
