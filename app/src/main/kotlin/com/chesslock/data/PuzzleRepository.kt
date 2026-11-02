package com.chesslock.data

import android.content.Context
import android.util.Log
import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.puzzle.LichessPuzzleCsv
import com.chesslock.domain.puzzle.Puzzle
import com.chesslock.domain.puzzle.PuzzleSelector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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

    suspend fun nextPuzzle(targetRating: Int): Puzzle? = withContext(Dispatchers.IO) {
        ensureBuiltInsImported()
        val candidates = dao.getAll().map { it.toDomain() }
        PuzzleSelector.pick(
            candidates = candidates,
            targetRating = targetRating,
            solvedIds = dao.getSolvedIds().toSet(),
            random = Random(),
            config = config,
        )
    }

    suspend fun markSolved(puzzleId: String) = withContext(Dispatchers.IO) {
        dao.markSolved(puzzleId)
    }

    private suspend fun ensureBuiltInsImported() {
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
