package com.chesslock.domain.reward

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardCalculatorTest {
    private val config = AppConfig.DEFAULT

    @Test
    fun bonusBandsMatchSpec() {
        assertEquals(0, RewardCalculator.bonusMinutes(0, config))
        assertEquals(0, RewardCalculator.bonusMinutes(999, config))
        assertEquals(1, RewardCalculator.bonusMinutes(1000, config))
        assertEquals(1, RewardCalculator.bonusMinutes(1399, config))
        assertEquals(2, RewardCalculator.bonusMinutes(1400, config))
        assertEquals(2, RewardCalculator.bonusMinutes(1799, config))
        assertEquals(3, RewardCalculator.bonusMinutes(1800, config))
        assertEquals(3, RewardCalculator.bonusMinutes(2199, config))
        assertEquals(4, RewardCalculator.bonusMinutes(2200, config))
        assertEquals(4, RewardCalculator.bonusMinutes(3200, config))
    }

    @Test
    fun grossRewardIsBasePlusBonusInSeconds() {
        assertEquals(180L, RewardCalculator.grossSeconds(900, config))
        assertEquals(240L, RewardCalculator.grossSeconds(1000, config))
        assertEquals(300L, RewardCalculator.grossSeconds(1400, config))
        assertEquals(360L, RewardCalculator.grossSeconds(1800, config))
        assertEquals(420L, RewardCalculator.grossSeconds(2200, config))
    }

    @Test
    fun hintHalvesTheReward() {
        assertEquals(120L, RewardCalculator.rewardSeconds(1000, hintUsed = true, penaltyMoves = 0, config = config))
        assertEquals(210L, RewardCalculator.rewardSeconds(2200, hintUsed = true, penaltyMoves = 0, config = config))
    }

    @Test
    fun hintMultiplierComesFromConfig() {
        val custom = config.copy(hintRewardMultiplier = 0.25)
        assertEquals(60L, RewardCalculator.rewardSeconds(1000, hintUsed = true, penaltyMoves = 0, config = custom))
    }

    @Test
    fun eachPenaltyMoveRemoves20Seconds() {
        assertEquals(160L, RewardCalculator.rewardSeconds(900, hintUsed = false, penaltyMoves = 1, config = config))
        assertEquals(120L, RewardCalculator.rewardSeconds(900, hintUsed = false, penaltyMoves = 3, config = config))
    }

    @Test
    fun penaltyIsAppliedAfterHintReduction() {
        // 240 * 0.5 = 120, minus 2 * 20 = 80
        assertEquals(80L, RewardCalculator.rewardSeconds(1000, hintUsed = true, penaltyMoves = 2, config = config))
    }

    @Test
    fun rewardNeverGoesBelowZero() {
        assertEquals(0L, RewardCalculator.rewardSeconds(900, hintUsed = false, penaltyMoves = 9, config = config))
        assertEquals(0L, RewardCalculator.rewardSeconds(900, hintUsed = false, penaltyMoves = 500, config = config))
        assertEquals(0L, RewardCalculator.rewardSeconds(900, hintUsed = true, penaltyMoves = 500, config = config))
    }
}
