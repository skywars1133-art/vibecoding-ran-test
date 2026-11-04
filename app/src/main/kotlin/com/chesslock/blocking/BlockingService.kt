package com.chesslock.blocking

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import com.chesslock.data.AppSettings
import com.chesslock.data.BudgetStore
import com.chesslock.data.SettingsStore
import com.chesslock.domain.blocking.BlockPolicy
import com.chesslock.domain.blocking.BlockRules
import com.chesslock.domain.budget.BudgetState
import com.chesslock.domain.budget.TimeBudget
import com.chesslock.domain.budget.UsageMeter
import com.chesslock.domain.config.AppConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import javax.inject.Inject

/**
 * Foreground service that polls the foreground app, charges the shared daily budget and shows
 * the block screen when the budget is empty (SPEC 1). It is not strict: the user can stop it.
 */
@AndroidEntryPoint
class BlockingService : Service() {

    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var budgetStore: BudgetStore
    @Inject lateinit var detector: ForegroundAppDetector
    @Inject lateinit var appInfo: AppInfoProvider
    @Inject lateinit var notifications: BlockingNotifications
    @Inject lateinit var overlay: BlockOverlayController
    @Inject lateinit var controller: BlockingServiceController
    @Inject lateinit var config: AppConfig

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private var loopJob: Job? = null
    private var currentSettings: AppSettings? = null
    private var budgetState: BudgetState? = null
    private var currentDate: LocalDate? = null
    private val meter = UsageMeter()
    private var lastTickElapsed = 0L
    private var lastFlushedElapsed = 0L
    private var previousForegroundBlocked = false
    private var lastNotificationKey: String? = null
    private val whitelistedPackages by lazy { appInfo.whitelistedPackages() }

    override fun onCreate() {
        super.onCreate()
        controller.setRunning(true)
        notifications.createChannels()
        scope.launch { settingsStore.settings.collect { currentSettings = it } }
        scope.launch { budgetStore.state.collect { budgetState = it } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground()
        if (loopJob == null) {
            loopJob = scope.launch { runLoop() }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        controller.setRunning(false)
        loopJob?.cancel()
        overlay.hide()
        if (meter.unflushed > 0L) {
            runBlocking { flushUsage() }
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun startForeground() {
        val notification = notifications.buildProtection(
            remainingSeconds = null,
            usageAccessGranted = detector.hasUsageAccess(),
            overlayGranted = overlay.canShow(),
        )
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, config.blockingNotificationId, notification, type)
    }

    private suspend fun runLoop() {
        while (true) {
            try {
                poll()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.w(TAG, "Blocking poll failed", t)
            }
            delay(config.blockingPollIntervalSeconds * 1000L)
        }
    }

    private suspend fun poll() {
        val settings = currentSettings
            ?: settingsStore.settings.first().also { currentSettings = it }
        if (!settings.blockingEnabled) {
            overlay.hide()
            stopSelf()
            return
        }

        val today = LocalDate.now()
        if (currentDate != today) {
            currentDate = today
            val fresh = budgetStore.currentState(today)
            budgetState = fresh
            meter.reset(fresh.usedSeconds)
        }
        val persisted = budgetState ?: budgetStore.currentState(today).also { budgetState = it }

        val now = SystemClock.elapsedRealtime()
        // Advance the tick before any I/O, flush or notification work. If that work throws, the next
        // poll must not charge the same interval a second time.
        val elapsedMillis = if (lastTickElapsed > 0L) {
            (now - lastTickElapsed).coerceAtLeast(0L)
        } else {
            0L
        }
        lastTickElapsed = now

        val usageAccess = detector.hasUsageAccess()
        val overlayGranted = overlay.canShow()
        // A screen that is off has no foreground app from the user's point of view. This also stops
        // a block screen from lingering over the lock screen.
        val interactive = powerManager?.isInteractive ?: true
        val foreground = if (usageAccess && interactive) detector.foregroundPackage() else null
        val blocked = foreground != null && BlockPolicy.isBlocked(
            packageName = foreground,
            platformCategory = appInfo.platformCategory(foreground),
            rules = BlockRules(
                blockedCategories = settings.blockedCategories,
                alwaysBlockedPackages = settings.alwaysBlockedPackages,
                neverBlockedPackages = settings.neverBlockedPackages,
            ),
            ownPackageName = packageName,
            whitelistedPackages = whitelistedPackages,
        )

        var remaining = remainingSeconds(persisted, settings)
        if (blocked && previousForegroundBlocked && elapsedMillis > 0L) {
            val counted = meter.count(
                elapsedMillis = elapsedMillis,
                maxCatchUpMillis = config.blockingMaxCatchUpSeconds * 1_000L,
                availableSeconds = remaining,
            )
            if (counted > 0L) {
                val newRemaining = remainingSeconds(persisted, settings)
                TimeBudget.crossedWarnings(remaining, newRemaining, config.warnAtSeconds)
                    .minOrNull()
                    ?.let { notifications.warn(newRemaining) }
                remaining = newRemaining
            }
        } else {
            meter.endBlockedRun()
        }

        val flushDue = now - lastFlushedElapsed >= config.budgetFlushIntervalSeconds * 1000L
        if (meter.unflushed > 0L && (flushDue || !blocked)) {
            flushUsage()
        }

        if (blocked && remaining == 0L) {
            overlay.show()
        } else {
            overlay.hide()
        }
        updateNotification(remaining, usageAccess, overlayGranted)

        previousForegroundBlocked = blocked
    }

    private fun remainingSeconds(persisted: BudgetState, settings: AppSettings): Long =
        TimeBudget.remainingSeconds(
            state = persisted.copy(usedSeconds = meter.usedSeconds),
            limitSeconds = settings.dailyLimitSeconds,
            capSeconds = settings.dailyCapSeconds,
        )

    private suspend fun flushUsage() {
        val seconds = meter.takeUnflushed()
        if (seconds <= 0L) return
        try {
            budgetStore.addUsage(seconds)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Throwable) {
            // Retry on the next poll. The meter keeps the total unchanged, so nothing is lost and
            // nothing is charged twice.
            meter.restore(seconds)
            Log.w(TAG, "Could not flush usage", t)
        } finally {
            lastFlushedElapsed = SystemClock.elapsedRealtime()
        }
    }

    private fun updateNotification(remainingSeconds: Long, usageAccess: Boolean, overlayGranted: Boolean) {
        val key = "${remainingSeconds / 60}/$usageAccess/$overlayGranted"
        if (key == lastNotificationKey) return
        lastNotificationKey = key
        notifications.updateProtection(remainingSeconds, usageAccess, overlayGranted)
    }

    companion object {
        const val ACTION_STOP = "com.chesslock.action.STOP_BLOCKING"
        private const val TAG = "BlockingService"

        fun startIntent(context: Context): Intent =
            Intent(context, BlockingService::class.java)
    }
}
