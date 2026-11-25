package com.chesslock.domain.elo

import com.chesslock.domain.config.AppConfig
import kotlin.math.roundToInt

/**
 * Placement test (SPEC 4): about 10 adaptive puzzles to find the starting ELO. Every answer updates
 * the estimate with a puzzle-vs-player ELO calculation using the actual puzzle rating, so solving a
 * 1400-rated puzzle moves the estimate much more than solving a 600-rated one. The estimate is also
 * the target rating for the next puzzle. Hints and skips are not offered during placement (a skipped
 * puzzle simply counts as a failure; see README "Decisions").
 */
class PlacementTest(private val config: AppConfig = AppConfig.DEFAULT) {

    private var estimate: Double = config.eloStart.toDouble()

    /** Rating of the puzzle to show next. */
    val targetRating: Double get() = estimate

    var answered: Int = 0
        private set

    val isComplete: Boolean get() = answered >= config.placementPuzzleCount

    /** [solved] true if the player solved [puzzleRating] with at least one life left. */
    fun record(solved: Boolean, puzzleRating: Int) {
        check(!isComplete) { "Placement test already complete" }
        val score = if (solved) 1.0 else 0.0
        val expected = EloCalculator.expectedScore(estimate.roundToInt(), puzzleRating, config)
        estimate = (estimate + config.placementKFactor * (score - expected))
            .coerceIn(config.eloFloor.toDouble(), config.eloCeiling.toDouble())
        answered += 1
    }

    /** Starting ELO. Meaningful once [isComplete], but available at any time for previews. */
    fun estimatedElo(): Int =
        estimate.roundToInt().coerceIn(config.eloFloor, config.eloCeiling)
}
