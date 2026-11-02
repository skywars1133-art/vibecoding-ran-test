package com.chesslock.blocking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.chesslock.MainActivity
import com.chesslock.R
import com.chesslock.domain.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/** Ongoing protection notification plus the 5-minute and 1-minute warnings (SPEC 1). */
@Singleton
class BlockingNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: AppConfig,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val protection = NotificationChannel(
            CHANNEL_PROTECTION,
            context.getString(R.string.blocking_channel_protection),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.blocking_channel_protection_description)
            setShowBadge(false)
        }
        val warnings = NotificationChannel(
            CHANNEL_WARNINGS,
            context.getString(R.string.blocking_channel_warnings),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.blocking_channel_warnings_description)
        }
        manager.createNotificationChannel(protection)
        manager.createNotificationChannel(warnings)
    }

    fun buildProtection(
        remainingSeconds: Long?,
        usageAccessGranted: Boolean,
        overlayGranted: Boolean,
    ): Notification {
        val title = when {
            !usageAccessGranted -> context.getString(R.string.blocking_notification_no_usage_access)
            !overlayGranted -> context.getString(R.string.blocking_notification_no_overlay)
            remainingSeconds == null -> context.getString(R.string.blocking_notification_starting)
            else -> context.getString(
                R.string.blocking_notification_active,
                formatRemaining(remainingSeconds),
            )
        }
        return NotificationCompat.Builder(context, CHANNEL_PROTECTION)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.blocking_notification_body))
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateProtection(remainingSeconds: Long, usageAccessGranted: Boolean, overlayGranted: Boolean) {
        runCatching {
            manager.notify(
                config.blockingNotificationId,
                buildProtection(remainingSeconds, usageAccessGranted, overlayGranted),
            )
        }
    }

    fun warn(remainingSeconds: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_WARNINGS)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(context.getString(R.string.blocking_warning_title))
            .setContentText(
                context.getString(
                    R.string.blocking_warning_body,
                    formatRemaining(remainingSeconds),
                ),
            )
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        runCatching { manager.notify(config.blockingWarningNotificationId, notification) }
    }

    fun cancelWarning() {
        runCatching { manager.cancel(config.blockingWarningNotificationId) }
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun formatRemaining(seconds: Long): String {
        val minutes = ceil(seconds.coerceAtLeast(0L) / 60.0).toInt()
        return context.getString(R.string.blocking_minutes, minutes)
    }

    private companion object {
        const val CHANNEL_PROTECTION = "blocking_protection"
        const val CHANNEL_WARNINGS = "blocking_warnings"
    }
}
