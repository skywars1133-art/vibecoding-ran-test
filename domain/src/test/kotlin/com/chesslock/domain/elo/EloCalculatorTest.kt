package com.chesslock.domain.elo

import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class EloCalculatorTest {
    private val config = AppConfig.DEFAULT

    @Test
    fun expectedScoreIsHalfWhenRatingsEqual() {
        assertEquals(0.5, EloCalculator.expectedScore(1000, 1000, config), 1e-9)
    }

    @Test
    fun expectedScoreIsOneEleventhWhenPuzzleIs400Higher() {
        assertEquals(1.0 / 11.0, EloCalculator.expectedScore(1000, 1400, config), 1e-9)
    }

    @Test
    fun kFactorIs40ForFirst20PuzzlesThen20() {
        assertEquals(40, EloCalculator.kFactor(0, config))
        assertEquals(40, EloCalculator.kFactor(19, config))
        assertEquals(20, EloCalculator.kFactor(20, config))
        assertEquals(20, EloCalculator.kFactor(500, config))
    }

    @Test
    fun evenWinAndLossWithEarlyK() {
        assertEquals(20, EloCalculator.delta(1000, 1000, 0, EloResult.WIN, config))
        assertEquals(-20, EloCalculator.delta(1000, 1000, 0, EloResult.LOSS, config))
    }

    @Test
    fun evenWinAndLossWithLaterK() {
        assertEquals(10, EloCalculator.delta(1000, 1000, 20, EloResult.WIN, config))
        assertEquals(-10, EloCalculator.delta(1000, 1000, 20, EloResult.LOSS, config))
    }

    @Test
    fun beatingMuchHarderPuzzleGivesBigGain() {
        // expected = 1/11, K = 40: 40 * (1 - 1/11) = 36.36 -> 1036
        assertEquals(1036, EloCalculator.newElo(1000, 1400, 0, EloResult.WIN, config))
    }

    @Test
    fun losingToMuchHarderPuzzleCostsLittle() {
        // 40 * (0 - 1/11) = -3.63 -> 996.36 -> 996
        assertEquals(996, EloCalculator.newElo(1000, 1400, 0, EloResult.LOSS, config))
    }

    @Test
    fun losingToMuchEasierPuzzleCostsALot() {
        // expected = 10/11, K = 40: 40 * (0 - 10/11) = -36.36 -> 963.63 -> 964
        assertEquals(964, EloCalculator.newElo(1000, 600, 0, EloResult.LOSS, config))
    }

    @Test
    fun winWithHintNeverChangesElo() {
        assertEquals(0, EloCalculator.delta(1000, 1400, 0, EloResult.WIN_WITH_HINT, config))
        assertEquals(0, EloCalculator.delta(1500, 800, 50, EloResult.WIN_WITH_HINT, config))
    }

    @Test
    fun lossWithHintStillDeductsElo() {
        // A hint does not protect against a loss: the LOSS result is deducted normally.
        assertEquals(-20, EloCalculator.delta(1000, 1000, 0, EloResult.LOSS, config))
    }

    @Test
    fun eloNeverDropsBelowFloor() {
        assertEquals(400, EloCalculator.newElo(400, 400, 0, EloResult.LOSS, config))
        assertEquals(400, EloCalculator.newElo(410, 400, 0, EloResult.LOSS, config))
    }

    @Test
    fun floorIsReadFromConfig() {
        val custom = config.copy(eloFloor = 500)
        assertEquals(500, EloCalculator.newElo(505, 500, 0, EloResult.LOSS, custom))
    }
}
