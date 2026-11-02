package com.chesslock.data

import android.util.Log
import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.puzzle.PuzzlePackPlanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PuzzlePackSyncResult {
    data object NotDue : PuzzlePackSyncResult
    data class Success(
        val bandCenter: Int,
        val packSize: Int,
        val addedCount: Int,
    ) : PuzzlePackSyncResult

    data class Failure(val message: String) : PuzzlePackSyncResult
}

/**
 * Orchestrates one daily pack download (SPEC 2): read the current ELO, pick the nearest rating
 * band, download and deduplicate the CSV, persist it in Room, and record the sync metadata.
 *
 * The download is idempotent: a repeated pack inserts nothing new and never resets solved flags.
 */
@Singleton
class PuzzlePackSync @Inject constructor(
    private val playerStore: PlayerStore,
    private val repository: PuzzleRepository,
    private val client: PuzzlePackClient,
    private val syncStore: PuzzleSyncStore,
    private val config: AppConfig,
) {
    private val syncMutex = Mutex()

    suspend fun sync(force: Boolean = false): PuzzlePackSyncResult = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val now = System.currentTimeMillis()
            val lastSuccess = syncStore.state.first().lastSuccessEpochMs
            if (!force && !PuzzlePackPlanner.isDue(lastSuccess, now, config.puzzlePackRefreshIntervalMs)) {
                return@withLock PuzzlePackSyncResult.NotDue
            }

            try {
                repository.prepareStock()
                val elo = playerStore.currentState().elo
                val bandCenter = PuzzlePackPlanner.bandCenter(elo, config)
                val pack = client.fetch(bandCenter)
                if (pack.isEmpty()) {
                    throw IOException("The downloaded puzzle pack is empty")
                }
                val addedCount = repository.addPuzzles(pack)
                syncStore.markSuccess(
                    epochMs = now,
                    bandCenter = bandCenter,
                    packSize = pack.size,
                    addedCount = addedCount,
                )
                Log.i(
                    TAG,
                    "Puzzle pack $bandCenter: ${pack.size} puzzles, $addedCount new",
                )
                PuzzlePackSyncResult.Success(bandCenter, pack.size, addedCount)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: e.javaClass.simpleName
                syncStore.markFailure(message)
                Log.w(TAG, "Puzzle pack sync failed", e)
                PuzzlePackSyncResult.Failure(message)
            }
        }
    }

    private companion object {
        const val TAG = "PuzzlePackSync"
    }
}
