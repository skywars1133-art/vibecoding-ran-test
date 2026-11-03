package com.chesslock.domain.stats

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StatisticsTest {
    private val config = AppConfig.DEFAULT
    private var sequence = 0L

    private fun attempt(
        day: LocalDate,
        solved: Boolean = true,
        skipped: Boolean = false,
        themes: List<String> = emptyList(),
        eloBefore: Int = 1000,
        eloAfter: Int = 1000,
        rewardSeconds: Long = 0,
    ) = AttemptRecord(
        puzzleId = "p${sequence}",
        puzzleRating = 1000,
        themes = themes,
        solved = solved,
        skipped = skipped,
        eloBefore = eloBefore,
        eloAfter = eloAfter,
        rewardSeconds = rewardSeconds,
        day = day,
        playedAtEpochMillis = sequence++,
    )

    // --- success rate ---

    @Test
    fun successRateCountsOnlyRatedAttempts() {
        val summary = Statistics.summarize(
            attempts = listOf(
                attempt(LocalDate.of(2026, 1, 1), solved = true),
                attempt(LocalDate.of(2026, 1, 1), solved = false),
                attempt(LocalDate.of(2026, 1, 1), skipped = true),
            ),
            currentElo = 1000,
            today = LocalDate.of(2026, 1, 1),
            config = config,
        )

        assertEquals(2, summary.ratedAttempts)
        assertEquals(1, summary.solvedAttempts)
        assertEquals(50, summary.successPercent)
    }

    @Test
    fun onlySkipsMeansNoSuccessRate() {
        val summary = Statistics.summarize(
            attempts = listOf(attempt(LocalDate.of(2026, 1, 1), skipped = true)),
            currentElo = 1000,
            today = LocalDate.of(2026, 1, 1),
            config = config,
        )

        assertEquals(0, summary.ratedAttempts)
        assertNull(summary.successPercent)
    }

    // --- streaks ---

    @Test
    fun currentStreakCountsConsecutiveSolvedDaysIncludingToday() {
        val days = setOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 2),
            LocalDate.of(2026, 1, 3),
        )

        assertEquals(3, Statistics.currentStreak(days, LocalDate.of(2026, 1, 3)))
    }

    @Test
    fun currentStreakSurvivesAnEmptyToday() {
        val days = setOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2))

        // The player has not solved anything yet today, but yesterday's streak is still alive.
        assertEquals(2, Statistics.currentStreak(days, LocalDate.of(2026, 1, 3)))
    }

    @Test
    fun currentStreakBreaksAfterAFullMissedDay() {
        val days = setOf(LocalDate.of(2026, 1, 1))

        assertEquals(0, Statistics.currentStreak(days, LocalDate.of(2026, 1, 3)))
    }

    @Test
    fun bestStreakFindsLongestRun() {
        val days = setOf(
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 1, 2),
            LocalDate.of(2026, 1, 4),
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 1, 6),
        )

        assertEquals(3, Statistics.bestStreak(days))
    }

    @Test
    fun bestStreakIsZeroWithoutSolves() {
        assertEquals(0, Statistics.bestStreak(emptySet()))
    }

    // --- themes ---

    @Test
    fun themesIgnoreGenericTagsAndAreSortedByAttempts() {
        val summary = Statistics.summarize(
            attempts = listOf(
                attempt(
                    LocalDate.of(2026, 1, 1),
                    solved = true,
                    themes = listOf("fork", "mateIn2", "short", "master"),
                ),
                attempt(
                    LocalDate.of(2026, 1, 1),
                    solved = false,
                    themes = listOf("fork", "endgame", "blitz"),
                ),
            ),
            currentElo = 1000,
            today = LocalDate.of(2026, 1, 1),
            config = config,
        )

        val themes = summary.themeStats.map { it.theme }
        assertEquals(listOf("fork", "mateIn2", "endgame"), themes)
        assertEquals(2, summary.themeStats.first().attempts)
        assertEquals(1, summary.themeStats.first().solved)
        assertEquals(50, summary.themeStats.first().successPercent)
        assertTrue("generic tags must be filtered", "short" !in themes)
        assertTrue("generic tags must be filtered", "master" !in themes)
        assertTrue("generic tags must be filtered", "blitz" !in themes)
    }

    @Test
    fun topThemesRespectsConfigLimit() {
        val summary = Statistics.summarize(
            attempts = listOf(
                attempt(
                    LocalDate.of(2026, 1, 1),
                    themes = listOf("fork", "pin", "skewer", "endgame"),
                ),
            ),
            currentElo = 1000,
            today = LocalDate.of(2026, 1, 1),
            config = config.copy(statsTopThemes = 2),
        )

        assertEquals(2, summary.themeStats.size)
    }

    // --- ELO history ---

    @Test
    fun eloHistoryStartsAtTheFirstOldEloAndKeepsTheLatestLimit() {
        val summary = Statistics.summarize(
            attempts = listOf(
                attempt(LocalDate.of(2026, 1, 1), eloBefore = 1000, eloAfter = 1010),
                attempt(LocalDate.of(2026, 1, 2), eloBefore = 1010, eloAfter = 1020),
                attempt(LocalDate.of(2026, 1, 3), eloBefore = 1020, eloAfter = 1030),
            ),
            currentElo = 1030,
            today = LocalDate.of(2026, 1, 3),
            config = config.copy(statsEloHistoryLimit = 2),
        )

        assertEquals(3, summary.eloHistory.size)
        assertEquals(1010, summary.eloHistory.first().elo)
        assertEquals(1030, summary.eloHistory.last().elo)
        assertEquals(10, summary.eloHistory.last().delta)
    }

    @Test
    fun eloHistoryWithoutAttemptsShowsCurrentElo() {
        val summary = Statistics.summarize(
            attempts = emptyList(),
            currentElo = 1200,
            today = LocalDate.of(2026, 1, 1),
            config = config,
        )

        assertEquals(1, summary.eloHistory.size)
        assertEquals(1200, summary.eloHistory.single().elo)
        assertEquals(0, summary.eloHistory.single().delta)
    }

    // --- total time ---

    @Test
    fun totalRewardSumsEveryAttemptAndIgnoresSkips() {
        val summary = Statistics.summarize(
            attempts = listOf(
                attempt(LocalDate.of(2026, 1, 1), rewardSeconds = 180),
                attempt(LocalDate.of(2026, 1, 1), solved = false, rewardSeconds = 90),
                attempt(LocalDate.of(2026, 1, 1), skipped = true, rewardSeconds = 0),
            ),
            currentElo = 1000,
            today = LocalDate.of(2026, 1, 1),
            config = config,
        )

        assertEquals(270, summary.totalRewardSeconds)
    }
}
