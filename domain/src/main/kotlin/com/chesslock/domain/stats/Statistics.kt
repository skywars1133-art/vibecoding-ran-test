package com.chesslock.domain.stats

import com.chesslock.domain.config.AppConfig
import java.time.LocalDate

/** One completed puzzle attempt, reduced to what the statistics screen needs. */
data class AttemptRecord(
    val puzzleId: String,
    val puzzleRating: Int,
    val themes: List<String>,
    val solved: Boolean,
    val skipped: Boolean,
    val eloBefore: Int,
    val eloAfter: Int,
    val rewardSeconds: Long,
    val day: LocalDate,
    val playedAtEpochMillis: Long,
)

/** One point of the ELO chart. [delta] is 0 for the synthetic starting point. */
data class EloPoint(
    val day: LocalDate,
    val elo: Int,
    val delta: Int,
)

data class ThemeStat(
    val theme: String,
    val attempts: Int,
    val solved: Int,
) {
    val successPercent: Int get() = if (attempts == 0) 0 else solved * 100 / attempts
}

/**
 * Local-only summary of the player's puzzles. Skips are kept in the log (they are useful when a
 * later sync adds cloud history) but never count as rated attempts, so they cannot lower the
 * success rate (SPEC 5).
 */
data class StatsSummary(
    val currentElo: Int,
    val ratedAttempts: Int,
    val solvedAttempts: Int,
    val successPercent: Int?,
    val eloHistory: List<EloPoint>,
    val currentStreakDays: Int,
    val bestStreakDays: Int,
    val totalRewardSeconds: Long,
    val themeStats: List<ThemeStat>,
)

object Statistics {

    fun summarize(
        attempts: List<AttemptRecord>,
        currentElo: Int,
        today: LocalDate = LocalDate.now(),
        config: AppConfig = AppConfig.DEFAULT,
    ): StatsSummary {
        val rated = attempts.filter { !it.skipped }
        val solvedCount = rated.count { it.solved }
        val success = if (rated.isEmpty()) null else solvedCount * 100 / rated.size
        val solvedDays = rated.asSequence().filter { it.solved }.map { it.day }.toSet()

        return StatsSummary(
            currentElo = currentElo,
            ratedAttempts = rated.size,
            solvedAttempts = solvedCount,
            successPercent = success,
            eloHistory = eloHistory(rated, currentElo, today, config),
            currentStreakDays = currentStreak(solvedDays, today),
            bestStreakDays = bestStreak(solvedDays),
            totalRewardSeconds = attempts.sumOf { it.rewardSeconds },
            themeStats = themeStats(rated, config),
        )
    }

    /**
     * A synthetic point at the first rated attempt's old ELO makes the chart start where the
     * player started instead of jumping in mid-air. Only the most recent attempts are charted.
     */
    private fun eloHistory(
        rated: List<AttemptRecord>,
        currentElo: Int,
        today: LocalDate,
        config: AppConfig,
    ): List<EloPoint> {
        val recent = rated.sortedBy { it.playedAtEpochMillis }.takeLast(config.statsEloHistoryLimit)
        if (recent.isEmpty()) return listOf(EloPoint(today, currentElo, 0))
        return buildList(recent.size + 1) {
            add(EloPoint(recent.first().day, recent.first().eloBefore, 0))
            recent.forEach { attempt ->
                add(EloPoint(attempt.day, attempt.eloAfter, attempt.eloAfter - attempt.eloBefore))
            }
        }
    }

    /** A streak survives an empty today: it is only broken once a full day passes unsolved. */
    fun currentStreak(solvedDays: Set<LocalDate>, today: LocalDate): Int {
        var cursor = if (today in solvedDays) today else today.minusDays(1)
        var streak = 0
        while (cursor in solvedDays) {
            streak += 1
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    fun bestStreak(solvedDays: Set<LocalDate>): Int {
        if (solvedDays.isEmpty()) return 0
        val sorted = solvedDays.sorted()
        var best = 1
        var run = 1
        for (index in 1 until sorted.size) {
            run = if (sorted[index] == sorted[index - 1].plusDays(1)) run + 1 else 1
            if (run > best) best = run
        }
        return best
    }

    /**
     * Themes are stored comma-free as Lichess tags. Generic length/format tags are ignored so the
     * list shows actual motifs and game phases instead of "short", "blitz" or "master".
     */
    private fun themeStats(rated: List<AttemptRecord>, config: AppConfig): List<ThemeStat> {
        val attempts = mutableMapOf<String, Int>()
        val solved = mutableMapOf<String, Int>()
        rated.forEach { attempt ->
            attempt.themes.asSequence()
                .filter { it.isNotBlank() && it !in config.statsIgnoredThemes }
                .forEach { theme ->
                    attempts[theme] = (attempts[theme] ?: 0) + 1
                    if (attempt.solved) solved[theme] = (solved[theme] ?: 0) + 1
                }
        }
        return attempts.entries
            .map { (theme, count) -> ThemeStat(theme, count, solved[theme] ?: 0) }
            .sortedWith(compareByDescending<ThemeStat> { it.attempts }.thenByDescending { it.solved })
            .take(config.statsTopThemes)
    }
}
