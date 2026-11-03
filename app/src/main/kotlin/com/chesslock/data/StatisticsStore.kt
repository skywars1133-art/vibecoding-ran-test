package com.chesslock.data

import com.chesslock.domain.puzzle.Puzzle
import com.chesslock.domain.stats.AttemptRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only attempt log used by the statistics screen (SPEC 5). Accounts and cloud sync are
 * deferred, so nothing in this class touches the network.
 */
@Singleton
class StatisticsStore @Inject constructor(
    private val dao: StatsDao,
) {
    fun observeAttempts(): Flow<List<AttemptRecord>> =
        dao.observeAttempts().map { attempts -> attempts.map { it.toRecord() } }

    fun observeUsageSince(from: LocalDate): Flow<List<UsageDayEntity>> =
        dao.observeUsageSince(from.toString())

    /** A win (including a hint win, which changes ELO by 0). */
    suspend fun recordSolved(
        puzzle: Puzzle,
        hintsUsed: Int,
        eloBefore: Int,
        eloAfter: Int,
        rewardSeconds: Long,
    ) {
        insert(
            puzzle = puzzle,
            solved = true,
            skipped = false,
            hintsUsed = hintsUsed,
            eloBefore = eloBefore,
            eloAfter = eloAfter,
            rewardSeconds = rewardSeconds,
        )
    }

    /** Written as soon as the third life is lost, because that is when the ELO loss is applied. */
    suspend fun recordLoss(
        puzzle: Puzzle,
        hintsUsed: Int,
        eloBefore: Int,
        eloAfter: Int,
    ) {
        insert(
            puzzle = puzzle,
            solved = false,
            skipped = false,
            hintsUsed = hintsUsed,
            eloBefore = eloBefore,
            eloAfter = eloAfter,
            rewardSeconds = 0L,
        )
    }

    /**
     * The player lost all lives but kept going and solved the puzzle. Complete that same row so
     * the history shows one attempt whose ELO result stays a loss, as SPEC 4 requires.
     */
    suspend fun completeLoss(puzzleId: String, rewardSeconds: Long, hintsUsed: Int): Boolean {
        val open = dao.latestOpenLoss(puzzleId) ?: return false
        dao.completeLoss(open.id, rewardSeconds, hintsUsed)
        return true
    }

    /** A skip gives no reward and no ELO change; it is excluded from the success rate. */
    suspend fun recordSkip(puzzle: Puzzle, currentElo: Int) {
        insert(
            puzzle = puzzle,
            solved = false,
            skipped = true,
            hintsUsed = 0,
            eloBefore = currentElo,
            eloAfter = currentElo,
            rewardSeconds = 0L,
        )
    }

    private suspend fun insert(
        puzzle: Puzzle,
        solved: Boolean,
        skipped: Boolean,
        hintsUsed: Int,
        eloBefore: Int,
        eloAfter: Int,
        rewardSeconds: Long,
    ) {
        val now = System.currentTimeMillis()
        dao.insertAttempt(
            AttemptEntity(
                puzzleId = puzzle.id,
                puzzleRating = puzzle.rating,
                themes = puzzle.themes.joinToString(" "),
                solved = solved,
                skipped = skipped,
                hintsUsed = hintsUsed,
                eloBefore = eloBefore,
                eloAfter = eloAfter,
                rewardSeconds = rewardSeconds,
                playedAtEpochMillis = now,
                day = LocalDate.now().toString(),
            ),
        )
    }
}

private fun AttemptEntity.toRecord(): AttemptRecord = AttemptRecord(
    puzzleId = puzzleId,
    puzzleRating = puzzleRating,
    themes = themes.split(' ').filter { it.isNotBlank() },
    solved = solved,
    skipped = skipped,
    eloBefore = eloBefore,
    eloAfter = eloAfter,
    rewardSeconds = rewardSeconds,
    day = runCatching { LocalDate.parse(day) }.getOrDefault(LocalDate.now()),
    playedAtEpochMillis = playedAtEpochMillis,
)
