package com.chesslock.blocking

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.chesslock.domain.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds the app currently in the foreground with `UsageStatsManager` events (SPEC 1).
 * No AccessibilityService is needed. Returns null when usage access was not granted.
 */
@Singleton
class ForegroundAppDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AppConfig,
) {
    fun hasUsageAccess(): Boolean = BlockingPermissions.hasUsageAccess(context)

    fun foregroundPackage(nowMillis: Long = System.currentTimeMillis()): String? {
        if (!hasUsageAccess()) return null
        val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val lookback = config.blockingUsageLookbackSeconds * 1000L
        val events = usageStats.queryEvents(nowMillis - lookback, nowMillis)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTimestamp = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val foregroundEvent = event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            if (foregroundEvent && event.timeStamp >= latestTimestamp) {
                latestTimestamp = event.timeStamp
                latestPackage = event.packageName
            }
        }
        return latestPackage
    }
}
