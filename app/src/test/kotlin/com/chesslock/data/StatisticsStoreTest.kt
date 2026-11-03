package com.chesslock.data

import com.chesslock.domain.puzzle.Puzzle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests the recording rules that the statistics screen relies on: one row per attempt, a LOSS
 * completed in place when the puzzle is later solved, and skips excluded from the success rate.
 */
class StatisticsStoreTest {
    private val dao = FakeStatsDao()
    private val store = StatisticsStore(dao)

    private fun puzzle(id: String, rating: Int = 1500, themes: List<String> = listOf("fork")) = Puzzle(
        id = id,
        fen = "8/8/8/8/8/8/8/K6k w - - 0 1",
        moves = listOf("a1a2", "h1h2"),
        rating = rating,
        ratingDeviation = 50,
        popularity = 80,
        nbPlays = 100,
        themes = themes,
        gameUrl = "",
        openingTags = emptyList(),
    )

    @Test
    fun recordSolvedStoresAWinWithEloAndReward() = runBlocking {
        store.recordSolved(
            puzzle = puzzle("win", rating = 1400, themes = listOf("fork", "endgame")),
            hintsUsed = 1,
            eloBefore = 1500,
            eloAfter = 1512,
            rewardSeconds = 240,
        )

        val attempt = dao.observeAttempts().first().single()
        assertTrue(attempt.solved)
        assertFalse(attempt.skipped)
        assertEquals("win", attempt.puzzleId)
        assertEquals(1400, attempt.puzzleRating)
        assertEquals("fork endgame", attempt.themes)
        assertEquals(1, attempt.hintsUsed)
        assertEquals(1500, attempt.eloBefore)
        assertEquals(1512, attempt.eloAfter)
        assertEquals(240, attempt.rewardSeconds)
        assertEquals(LocalDate.now().toString(), attempt.day)
    }

    @Test
    fun recordLossStoresAnOpenLossWithoutReward() = runBlocking {
        store.recordLoss(
            puzzle = puzzle("loss"),
            hintsUsed = 2,
            eloBefore = 1500,
            eloAfter = 1488,
        )

        val attempt = dao.observeAttempts().first().single()
        assertFalse(attempt.solved)
        assertFalse(attempt.skipped)
        assertEquals(2, attempt.hintsUsed)
        assertEquals(1500, attempt.eloBefore)
        assertEquals(1488, attempt.eloAfter)
        assertEquals(0, attempt.rewardSeconds)
    }

    @Test
    fun solvedLossCompletesTheSameRowInsteadOfAddingASecondAttempt() = runBlocking {
        store.recordLoss(puzzle("retry"), hintsUsed = 0, eloBefore = 1500, eloAfter = 1488)

        assertTrue(store.completeLoss(puzzleId = "retry", rewardSeconds = 240, hintsUsed = 2))

        val attempt = dao.observeAttempts().first().single()
        assertTrue("the completed attempt counts as a win", attempt.solved)
        assertEquals(240, attempt.rewardSeconds)
        assertEquals(2, attempt.hintsUsed)
        // The ELO result stays the loss that was already applied.
        assertEquals(1500, attempt.eloBefore)
        assertEquals(1488, attempt.eloAfter)
    }

    @Test
    fun completeLossReturnsFalseWhenThereIsNoOpenLoss() = runBlocking {
        store.recordSolved(puzzle("first-try"), hintsUsed = 0, eloBefore = 1500, eloAfter = 1512, rewardSeconds = 180)

        assertFalse(store.completeLoss(puzzleId = "first-try", rewardSeconds = 240, hintsUsed = 0))

        val attempt = dao.observeAttempts().first().single()
        assertEquals(180, attempt.rewardSeconds)
    }

    @Test
    fun completeLossOnlyCompletesTheLatestOpenLossForThatPuzzle() = runBlocking {
        store.recordSkip(puzzle("other"), currentElo = 1500)
        store.recordLoss(puzzle("target"), hintsUsed = 0, eloBefore = 1500, eloAfter = 1488)

        assertTrue(store.completeLoss(puzzleId = "target", rewardSeconds = 120, hintsUsed = 0))

        val attempts = dao.observeAttempts().first()
        assertEquals(2, attempts.size)
        assertFalse("the skip must stay a skip", attempts.first { it.puzzleId == "other" }.solved)
        assertTrue(attempts.first { it.puzzleId == "target" }.solved)
    }

    @Test
    fun recordSkipKeepsTheEloUnchangedAndIsMarkedAsSkip() = runBlocking {
        store.recordSkip(puzzle("skipped"), currentElo = 1573)

        val attempt = dao.observeAttempts().first().single()
        assertFalse(attempt.solved)
        assertTrue(attempt.skipped)
        assertEquals(0, attempt.rewardSeconds)
        assertEquals(1573, attempt.eloBefore)
        assertEquals(1573, attempt.eloAfter)
    }

    @Test
    fun observeAttemptsMapsThemesBackToAList() = runBlocking {
        store.recordSolved(
            puzzle = puzzle("themes", themes = listOf("mateIn2", "backRankMate")),
            hintsUsed = 0,
            eloBefore = 1500,
            eloAfter = 1500,
            rewardSeconds = 60,
        )

        val record = store.observeAttempts().first().single()
        assertEquals(listOf("mateIn2", "backRankMate"), record.themes)
    }

    /** In-memory stand-in for the Room DAO so these rules run as plain JVM unit tests. */
    private class FakeStatsDao : StatsDao {
        private val attempts = MutableStateFlow<List<AttemptEntity>>(emptyList())
        private val usage = MutableStateFlow<List<UsageDayEntity>>(emptyList())
        private var nextId = 1L

        override suspend fun insertAttempt(attempt: AttemptEntity): Long {
            val id = nextId++
            attempts.value = attempts.value + attempt.copy(id = id)
            return id
        }

        override fun observeAttempts(): Flow<List<AttemptEntity>> = attempts

        override suspend fun latestOpenLoss(puzzleId: String): AttemptEntity? =
            attempts.value.lastOrNull { it.puzzleId == puzzleId && !it.solved && !it.skipped }

        override suspend fun completeLoss(id: Long, rewardSeconds: Long, hintsUsed: Int) {
            attempts.value = attempts.value.map {
                if (it.id == id) {
                    it.copy(solved = true, rewardSeconds = rewardSeconds, hintsUsed = hintsUsed)
                } else {
                    it
                }
            }
        }

        override fun observeUsageSince(fromDate: String): Flow<List<UsageDayEntity>> = usage

        override suspend fun addUsageToExisting(date: String, seconds: Long): Int = 0

        override suspend fun addEarnedToExisting(date: String, seconds: Long): Int = 0

        override suspend fun insertUsageDay(day: UsageDayEntity): Long = -1L
    }
}
