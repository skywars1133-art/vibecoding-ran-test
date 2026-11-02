package com.chesslock.domain.puzzle

import com.chesslock.domain.config.AppConfig
import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToInt

/** Adaptive next-puzzle choice (SPEC 2): near the player's level with mild random variation. */
object PuzzleSelector {

    /**
     * Picks an unsolved puzzle. The desired rating is [targetRating] plus a Gaussian offset
     * (sigma = `selectionSigma`); then one of the `selectionPoolSize` closest unsolved puzzles is chosen at random,
     * so the same ELO does not always produce the same puzzle. Returns null when no unsolved puzzle is left
     * (the UI then shows the "stock is empty" message). Pure given [random], so it is deterministic in tests.
     */
    fun pick(
        candidates: List<Puzzle>,
        targetRating: Int,
        solvedIds: Set<String>,
        random: Random,
        config: AppConfig = AppConfig.DEFAULT,
    ): Puzzle? {
        val unsolved = candidates.filter { it.id !in solvedIds }
        if (unsolved.isEmpty()) return null
        val desired = targetRating + (random.nextGaussian() * config.selectionSigma).roundToInt()
        val pool = unsolved
            .sortedWith(compareBy({ abs(it.rating - desired) }, { it.id }))
            .take(config.selectionPoolSize.coerceAtLeast(1))
        return pool[random.nextInt(pool.size)]
    }
}
