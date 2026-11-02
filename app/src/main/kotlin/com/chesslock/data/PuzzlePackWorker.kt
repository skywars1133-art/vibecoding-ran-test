package com.chesslock.data

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chesslock.domain.config.AppConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager entry point for the daily pack (SPEC 2). It runs with a network constraint from
 * [PuzzlePackScheduler]; the constraint respects the user's Wi-Fi-only setting.
 */
@HiltWorker
class PuzzlePackWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sync: PuzzlePackSync,
    private val config: AppConfig,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val force = inputData.getBoolean(KEY_FORCE, false)
        return when (sync.sync(force)) {
            PuzzlePackSyncResult.NotDue,
            is PuzzlePackSyncResult.Success,
            -> Result.success()

            is PuzzlePackSyncResult.Failure ->
                if (runAttemptCount + 1 < config.puzzlePackMaxAttempts) {
                    Result.retry()
                } else {
                    // Do not fail a periodic request permanently: the next daily run (or the next
                    // app launch / "Download now") gets another chance. The error is in the store.
                    Result.success()
                }
        }
    }

    companion object {
        const val KEY_FORCE = "force"
    }
}
