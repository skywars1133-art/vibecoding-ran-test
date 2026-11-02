package com.chesslock.domain.elo

import com.chesslock.domain.config.AppConfig
import kotlin.math.pow
import kotlin.math.roundToInt

/** Outcome of a puzzle as far as ELO is concerned. */
enum class EloResult {
    /** Solved with at least one life left and no hint. */
    WIN,

    /** Solved with at least one life left, but a hint was used: ELO change is 0. */
    WIN_WITH_HINT,

    /** All lives were lost. Applied the moment the last life is lost. */
    LOSS,
}

/** Player-vs-puzzle ELO (SPEC 4). */
object EloCalculator {

    fun expectedScore(userElo: Int, puzzleRating: Int, config: AppConfig = AppConfig.DEFAULT): Double =
        1.0 / (1.0 + 10.0.pow((puzzleRating - userElo) / config.eloScale))

    /** [ratedPuzzlesPlayed] = puzzles that already had an ELO result before this one. */
    fun kFactor(ratedPuzzlesPlayed: Int, config: AppConfig = AppConfig.DEFAULT): Int =
        if (ratedPuzzlesPlayed < config.kFactorEarlyPuzzleCount) config.kFactorEarly else config.kFactorLater

    fun newElo(
        oldElo: Int,
        puzzleRating: Int,
        ratedPuzzlesPlayed: Int,
        result: EloResult,
        config: AppConfig = AppConfig.DEFAULT,
    ): Int {
        if (result == EloResult.WIN_WITH_HINT) return oldElo
        val score = if (result == EloResult.WIN) 1.0 else 0.0
        val expected = expectedScore(oldElo, puzzleRating, config)
        val k = kFactor(ratedPuzzlesPlayed, config)
        val raw = oldElo + k * (score - expected)
        return raw.roundToInt().coerceAtLeast(config.eloFloor)
    }

    fun delta(
        oldElo: Int,
        puzzleRating: Int,
        ratedPuzzlesPlayed: Int,
        result: EloResult,
        config: AppConfig = AppConfig.DEFAULT,
    ): Int = newElo(oldElo, puzzleRating, ratedPuzzlesPlayed, result, config) - oldElo
}
