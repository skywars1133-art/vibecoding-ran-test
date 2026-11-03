package com.chesslock.data

import android.content.Context
import android.util.Log
import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.puzzle.LichessPuzzleCsv
import com.chesslock.domain.puzzle.Puzzle
import com.chesslock.domain.puzzle.PuzzleSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PuzzleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PuzzleDao,
    private val config: AppConfig,
) {
    private val importMutex = Mutex()
    private var builtInsImported = false

    /** Unsolved puzzles currently available offline, for the Settings stock counter. */
    val unsolvedCount: Flow<Int> = dao.unsolvedCount()

    /** Unique puzzles ever solved; used by the local statistics screen (SPEC 5). */
    val solvedCount: Flow<Int> = dao.solvedCount()

    /**
     * Picks an unsolved puzzle near [targetRating]. [excludeIds] lets the placement test keep every
     * puzzle unique within one run without marking them solved (they stay available for normal play).
     */
    suspend fun nextPuzzle(
        targetRating: Int,
        excludeIds: Set<String> = emptySet(),
    ): Puzzle? = withContext(Dispatchers.IO) {
        importBuiltInsIfNeeded()
        val candidates = dao.getAll().map { it.toDomain() }
        PuzzleSelector.pick(
            candidates = candidates,
            targetRating = targetRating,
            solvedIds = dao.getSolvedIds().toSet() + excludeIds,
            random = Random(),
            config = config,
        )
    }

    suspend fun markSolved(puzzleId: String) = withContext(Dispatchers.IO) {
        dao.markSolved(puzzleId)
    }

    /**
     * Makes sure the built-in offline set is in Room (first launch, placement test). Safe to call
     * repeatedly; parsed rows are inserted with IGNORE and never reset an already-solved puzzle.
     */
    suspend fun prepareStock() = withContext(Dispatchers.IO) {
        importBuiltInsIfNeeded()
    }

    /**
     * Adds a downloaded pack. Existing rows win (IGNORE), so solved puzzles stay solved and a pack
     * that overlaps an earlier one never produces duplicates. Returns how many rows were new.
     */
    suspend fun addPuzzles(puzzles: List<Puzzle>): Int = withContext(Dispatchers.IO) {
        importBuiltInsIfNeeded()
        if (puzzles.isEmpty()) return@withContext 0
        val rowIds = dao.insertAll(puzzles.map { it.toEntity() })
        rowIds.count { it != -1L }
    }

    private suspend fun importBuiltInsIfNeeded() {
        if (builtInsImported) return
        importMutex.withLock {
            if (builtInsImported) return@withLock
            context.assets.open("builtin_puzzles.csv").bufferedReader().useLines { lines ->
                val puzzles = LichessPuzzleCsv.parseAll(lines) { lineNumber, message ->
                    Log.w("PuzzleRepository", "Skipped built-in puzzle line $lineNumber: $message")
                }
                dao.insertAll(puzzles.map { it.toEntity() })
            }
            builtInsImported = true
        }
    }
}
