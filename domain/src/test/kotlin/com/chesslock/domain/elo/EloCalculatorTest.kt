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
        assertEquals(1036, EloCalculator.newElo(1000, 1400, 0, EloResult.WIN, config))
    }

    @Test
    fun losingToMuchHarderPuzzleCostsLittle() {
        assertEquals(996, EloCalculator.newElo(1000, 1400, 0, EloResult.LOSS, config))
    }

    @Test
    fun losingToMuchEasierPuzzleCostsALot() {
        assertEquals(964, EloCalculator.newElo(1000, 600, 0, EloResult.LOSS, config))
    }

    @Test
    fun ratingDeviationPullsUnreliablePuzzleRatingTowardThePlayer() {
        // Deviation 200 of scale 200 halves the 400-point gap: 1000 vs 1200 -> 0.24025.
        assertEquals(0.240253073352042, EloCalculator.expectedScore(1000, 1400, 200, config), 1e-9)
        assertEquals(1030, EloCalculator.newElo(1000, 1400, 0, EloResult.WIN, 200, config))
        assertEquals(990, EloCalculator.newElo(1000, 1400, 0, EloResult.LOSS, 200, config))
        assertEquals(970, EloCalculator.newElo(1000, 600, 0, EloResult.LOSS, 200, config))
    }

    @Test
    fun zeroDeviationIsTreatedAsAnExactPuzzleRating() {
        assertEquals(1036, EloCalculator.newElo(1000, 1400, 0, EloResult.WIN, 0, config))
        assertEquals(1036, EloCalculator.newElo(1000, 1400, 0, EloResult.WIN, config))
    }

    @Test
    fun uncertaintyWeightCanBeTurnedOffFromConfig() {
        val exact = config.copy(puzzleRatingUncertaintyWeight = 0.0)
        assertEquals(1036, EloCalculator.newElo(1000, 1400, 0, EloResult.WIN, 200, exact))
    }

    @Test
    fun hintWinOnEvenPuzzleStaysUnchangedButHarderPuzzlesGivePartialCredit() {
        assertEquals(0, EloCalculator.delta(1000, 1000, 0, EloResult.WIN_WITH_HINT, config))
        assertEquals(0, EloCalculator.delta(1500, 1500, 50, EloResult.WIN_WITH_HINT, config))
        // Half K * (0.5 - 1/11) = +8.18; a normal win would gain 36.
        assertEquals(1008, EloCalculator.newElo(1000, 1400, 0, EloResult.WIN_WITH_HINT, config))
        assertEquals(992, EloCalculator.newElo(1000, 600, 0, EloResult.WIN_WITH_HINT, config))
    }

    @Test
    fun lossStillDeductsNormally() {
        assertEquals(-20, EloCalculator.delta(1000, 1000, 0, EloResult.LOSS, config))
    }

    @Test
    fun eloStaysBetweenFloorAndCeiling() {
        assertEquals(400, EloCalculator.newElo(400, 400, 0, EloResult.LOSS, config))
        assertEquals(400, EloCalculator.newElo(410, 400, 0, EloResult.LOSS, config))
        assertEquals(2800, EloCalculator.newElo(2790, 3000, 0, EloResult.WIN, config))
    }

    @Test
    fun floorAndCeilingAreReadFromConfig() {
        val custom = config.copy(eloFloor = 500, eloCeiling = 1500)
        assertEquals(500, EloCalculator.newElo(505, 500, 0, EloResult.LOSS, custom))
        assertEquals(1500, EloCalculator.newElo(1490, 1800, 0, EloResult.WIN, custom))
    }
}
