package com.chesslock.domain.elo

import com.chesslock.domain.config.AppConfig
import kotlin.math.pow
import kotlin.math.roundToInt

/** Outcome of a puzzle as far as ELO is concerned. */
enum class EloResult {
    /** Solved with at least one life left and no hint. */
    WIN,

    /**
     * Solved with at least one life left after a hint. Scored as a draw with a reduced K-factor:
     * the helper still counts, but unaided solutions are worth more.
     */
    WIN_WITH_HINT,

    /** All lives were lost. Applied the moment the last life is lost. */
    LOSS,
}

/** Player-vs-puzzle ELO (SPEC 4). */
object EloCalculator {

    /** Expectation against a puzzle whose rating is treated as exact. */
    fun expectedScore(
        userElo: Int,
        puzzleRating: Int,
        config: AppConfig = AppConfig.DEFAULT,
    ): Double = expectedScore(userElo, puzzleRating, null, config)

    /**
     * Expectation with Lichess rating deviation: unreliable puzzle ratings are pulled toward the
     * player's rating, so a misrated puzzle cannot swing the result as far.
     */
    fun expectedScore(
        userElo: Int,
        puzzleRating: Int,
        puzzleRatingDeviation: Int?,
        config: AppConfig = AppConfig.DEFAULT,
    ): Double {
        val effective = effectivePuzzleRating(userElo, puzzleRating, puzzleRatingDeviation, config)
        return 1.0 / (1.0 + 10.0.pow((effective - userElo) / config.eloScale))
    }

    /** [ratedPuzzlesPlayed] = puzzles that already had an ELO result before this one. */
    fun kFactor(ratedPuzzlesPlayed: Int, config: AppConfig = AppConfig.DEFAULT): Int =
        if (ratedPuzzlesPlayed < config.kFactorEarlyPuzzleCount) config.kFactorEarly else config.kFactorLater

    fun newElo(
        oldElo: Int,
        puzzleRating: Int,
        ratedPuzzlesPlayed: Int,
        result: EloResult,
        config: AppConfig = AppConfig.DEFAULT,
    ): Int = newElo(oldElo, puzzleRating, ratedPuzzlesPlayed, result, null, config)

    fun newElo(
        oldElo: Int,
        puzzleRating: Int,
        ratedPuzzlesPlayed: Int,
        result: EloResult,
        puzzleRatingDeviation: Int?,
        config: AppConfig = AppConfig.DEFAULT,
    ): Int {
        val (score, kMultiplier) = when (result) {
            EloResult.WIN -> 1.0 to 1.0
            EloResult.WIN_WITH_HINT -> config.hintWinScore to config.hintWinKMultiplier
            EloResult.LOSS -> 0.0 to 1.0
        }
        val expected = expectedScore(oldElo, puzzleRating, puzzleRatingDeviation, config)
        val k = kFactor(ratedPuzzlesPlayed, config) * kMultiplier
        val raw = oldElo + k * (score - expected)
        return raw.roundToInt().coerceIn(config.eloFloor, config.eloCeiling)
    }

    fun delta(
        oldElo: Int,
        puzzleRating: Int,
        ratedPuzzlesPlayed: Int,
        result: EloResult,
        config: AppConfig = AppConfig.DEFAULT,
    ): Int = delta(oldElo, puzzleRating, ratedPuzzlesPlayed, result, null, config)

    fun delta(
        oldElo: Int,
        puzzleRating: Int,
        ratedPuzzlesPlayed: Int,
        result: EloResult,
        puzzleRatingDeviation: Int?,
        config: AppConfig = AppConfig.DEFAULT,
    ): Int = newElo(
        oldElo,
        puzzleRating,
        ratedPuzzlesPlayed,
        result,
        puzzleRatingDeviation,
        config,
    ) - oldElo

    private fun effectivePuzzleRating(
        userElo: Int,
        puzzleRating: Int,
        puzzleRatingDeviation: Int?,
        config: AppConfig,
    ): Double {
        if (puzzleRatingDeviation == null) return puzzleRating.toDouble()
        val scale = config.puzzleRatingDeviationScale.coerceAtLeast(1).toDouble()
        val uncertainty = (puzzleRatingDeviation.toDouble() / scale).coerceIn(0.0, 1.0)
        val weight = config.puzzleRatingUncertaintyWeight.coerceIn(0.0, 1.0)
        val confidence = 1.0 - uncertainty * weight
        return userElo + (puzzleRating - userElo) * confidence
    }
}
