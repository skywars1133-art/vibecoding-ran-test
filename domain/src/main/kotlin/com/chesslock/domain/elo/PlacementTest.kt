package com.chesslock.domain.elo

import com.chesslock.domain.config.AppConfig
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Placement test (SPEC 4): about 10 adaptive puzzles to find the starting ELO. Difficulty is a staircase:
 * a solved puzzle raises the next target rating, a failed one lowers it, with steps that shrink over time.
 * The estimate is the target the next puzzle would have had. Hints and skips are not offered during placement
 * (a skipped puzzle simply counts as a failure; see README "Decisions").
 */
class PlacementTest(private val config: AppConfig = AppConfig.DEFAULT) {

    /** Rating of the puzzle to show next. */
    var targetRating: Double = config.eloStart.toDouble()
        private set

    var answered: Int = 0
        private set

    val isComplete: Boolean get() = answered >= config.placementPuzzleCount

    /** [solved] true if the player solved it with at least one life left. */
    fun record(solved: Boolean) {
        check(!isComplete) { "Placement test already complete" }
        val step = (config.placementStepInitial * config.placementStepDecay.pow(answered))
            .coerceAtLeast(config.placementStepMin.toDouble())
        targetRating = (targetRating + if (solved) step else -step)
            .coerceIn(config.eloFloor.toDouble(), config.placementMaxRating.toDouble())
        answered += 1
    }

    /** Starting ELO. Meaningful once [isComplete], but available at any time for previews. */
    fun estimatedElo(): Int =
        targetRating.roundToInt().coerceIn(config.eloFloor, config.placementMaxRating)
}
