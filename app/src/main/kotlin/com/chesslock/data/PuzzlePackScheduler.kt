package com.chesslock.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chesslock.domain.config.AppConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the WorkManager schedule for puzzle packs (SPEC 2). One periodic request refreshes the
 * pack every 24 h; the app also asks for a one-time sync on launch and from "Download now".
 * The network constraint is UNMETERED when the user keeps the default Wi-Fi-only setting.
 */
@Singleton
class PuzzlePackScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val config: AppConfig,
) {
    /** Creates or updates the 24-hour periodic request, including its network constraint. */
    suspend fun ensureScheduled() {
        val wifiOnly = settingsStore.settings.first().wifiOnlyDownloads
        val request = PeriodicWorkRequestBuilder<PuzzlePackWorker>(
            config.puzzlePackRefreshHours,
            TimeUnit.HOURS,
        )
            .setConstraints(networkConstraints(wifiOnly))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                config.puzzlePackRetryBackoffMinutes,
                TimeUnit.MINUTES,
            )
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    /**
     * Asks for a sync now. [force] bypasses the 24-hour check (the "Download now" button); without
     * it the worker only runs when the last successful download is old enough.
     */
    suspend fun requestImmediateSync(force: Boolean, replaceExisting: Boolean = force) {
        val wifiOnly = settingsStore.settings.first().wifiOnlyDownloads
        val request = OneTimeWorkRequestBuilder<PuzzlePackWorker>()
            .setConstraints(networkConstraints(wifiOnly))
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                config.puzzlePackRetryBackoffMinutes,
                TimeUnit.MINUTES,
            )
            .setInputData(workDataOf(PuzzlePackWorker.KEY_FORCE to force))
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private fun networkConstraints(wifiOnly: Boolean): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(
                if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED,
            )
            .build()

    private companion object {
        const val TAG = "puzzle_packs"
        const val PERIODIC_WORK_NAME = "puzzle_pack_periodic"
        const val IMMEDIATE_WORK_NAME = "puzzle_pack_immediate"
    }
}
