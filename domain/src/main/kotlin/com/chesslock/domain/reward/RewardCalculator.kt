package com.chesslock.domain.reward

import com.chesslock.domain.config.AppConfig
import kotlin.math.roundToLong

/** Time reward for a solved puzzle (SPEC 3 and 3b). All results are in seconds. */
object RewardCalculator {

    fun bonusMinutes(puzzleRating: Int, config: AppConfig = AppConfig.DEFAULT): Int =
        config.difficultyBonuses
            .filter { it.minRating <= puzzleRating }
            .maxByOrNull { it.minRating }
            ?.bonusMinutes
            ?: 0

    /** Base time plus difficulty bonus, before hint reduction and penalties. */
    fun grossSeconds(puzzleRating: Int, config: AppConfig = AppConfig.DEFAULT): Long =
        (config.baseRewardMinutes + bonusMinutes(puzzleRating, config)) * 60L

    /**
     * Order of reductions: gross reward, then the hint multiplier (if any hint was used),
     * then a flat penalty per wrong move made after all lives were lost. Never below 0.
     */
    fun rewardSeconds(
        puzzleRating: Int,
        hintUsed: Boolean,
        penaltyMoves: Int,
        config: AppConfig = AppConfig.DEFAULT,
    ): Long {
        var reward = grossSeconds(puzzleRating, config).toDouble()
        if (hintUsed) reward *= config.hintRewardMultiplier
        reward -= penaltyMoves.toLong() * config.penaltySecondsPerWrongMoveAfterLoss
        return reward.roundToLong().coerceAtLeast(0L)
    }
}
