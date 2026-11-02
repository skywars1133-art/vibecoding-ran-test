package com.chesslock.domain.puzzle

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.elo.EloResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleSessionTest {
    private val config = AppConfig.DEFAULT

    private fun session(rating: Int = 900) = PuzzleSession(rating, config)

    // --- lives ---

    @Test
    fun startsWithThreeLives() {
        assertEquals(3, session().livesLeft)
    }

    @Test
    fun wrongMoveCostsOneLife() {
        val s = session()
        val outcome = s.wrongMove() as WrongMoveOutcome.LifeLost
        assertEquals(2, outcome.livesLeft)
        assertFalse(outcome.eloLossApplied)
        assertNull(s.eloResult)
    }

    @Test
    fun thirdLostLifeAppliesEloLossImmediately() {
        val s = session()
        s.wrongMove()
        s.wrongMove()
        val third = s.wrongMove() as WrongMoveOutcome.LifeLost
        assertEquals(0, third.livesLeft)
        assertTrue(third.eloLossApplied)
        assertEquals(EloResult.LOSS, s.eloResult)
        assertFalse(s.finished)
    }

    @Test
    fun wrongMovesAfterLossRemoveRewardTime() {
        val s = session(900)
        repeat(3) { s.wrongMove() }
        val penalty = s.wrongMove() as WrongMoveOutcome.RewardPenalty
        assertEquals(1, penalty.totalPenaltyMoves)
        assertEquals(160L, penalty.rewardSecondsLeft)
        val second = s.wrongMove() as WrongMoveOutcome.RewardPenalty
        assertEquals(140L, second.rewardSecondsLeft)
        assertEquals(0, s.livesLeft)
    }

    @Test
    fun penaltiesDoNotAddFurtherEloLoss() {
        val s = session()
        repeat(3) { s.wrongMove() }
        repeat(5) { s.wrongMove() }
        assertEquals(EloResult.LOSS, s.eloResult)
    }

    // --- solving ---

    @Test
    fun solvingWithLivesLeftIsAWinWithFullReward() {
        val s = session(900)
        s.wrongMove()
        val out = s.solved()
        assertEquals(EloResult.WIN, out.eloResult)
        assertFalse(out.eloAlreadyApplied)
        assertEquals(180L, out.rewardSeconds)
        assertTrue(s.finished)
    }

    @Test
    fun solvingAfterLossStillPaysReducedRewardButEloIsNotAppliedTwice() {
        val s = session(900)
        repeat(3) { s.wrongMove() }
        s.wrongMove() // one penalty move
        val out = s.solved()
        assertEquals(EloResult.LOSS, out.eloResult)
        assertTrue(out.eloAlreadyApplied)
        assertEquals(160L, out.rewardSeconds)
    }

    // --- skipping ---

    @Test
    fun skipBeforeLosingAllLivesHasNoEloChangeAndNoReward() {
        val s = session()
        s.wrongMove()
        s.skip()
        assertNull(s.eloResult)
        assertTrue(s.finished)
    }

    @Test
    fun skipAfterLossKeepsTheAlreadyAppliedLoss() {
        val s = session()
        repeat(3) { s.wrongMove() }
        s.skip()
        assertEquals(EloResult.LOSS, s.eloResult)
    }

    @Test(expected = IllegalStateException::class)
    fun cannotActAfterFinish() {
        val s = session()
        s.skip()
        s.wrongMove()
    }

    // --- hints ---

    @Test
    fun hintLevelsGoOneThenTwoThenStop() {
        val s = session()
        assertTrue(s.canRequestHint(0))
        assertEquals(1, s.grantHint())
        assertTrue(s.canRequestHint(1))
        assertEquals(2, s.grantHint())
        assertFalse(s.canRequestHint(2))
    }

    @Test(expected = IllegalStateException::class)
    fun thirdHintOnSameMoveIsRejected() {
        val s = session()
        s.grantHint()
        s.grantHint()
        s.grantHint()
    }

    @Test
    fun hintLevelResetsAfterCorrectMove() {
        val s = session()
        s.grantHint()
        s.grantHint()
        s.correctMove()
        assertEquals(0, s.currentHintLevel)
        assertTrue(s.canRequestHint(2))
        assertEquals(1, s.grantHint())
        assertEquals(3, s.hintsUsed)
    }

    @Test
    fun hintDoesNotCostALife() {
        val s = session()
        s.grantHint()
        s.grantHint()
        assertEquals(3, s.livesLeft)
    }

    @Test
    fun winWithHintHalvesRewardAndGivesNoEloGain() {
        val s = session(1000)
        s.grantHint()
        val out = s.solved()
        assertEquals(EloResult.WIN_WITH_HINT, out.eloResult)
        assertEquals(120L, out.rewardSeconds)
    }

    @Test
    fun lossWithHintStillDeductsElo() {
        val s = session(1000)
        s.grantHint()
        repeat(3) { s.wrongMove() }
        assertEquals(EloResult.LOSS, s.eloResult)
        val out = s.solved()
        assertEquals(EloResult.LOSS, out.eloResult)
        assertTrue(out.eloAlreadyApplied)
        assertEquals(120L, out.rewardSeconds)
    }

    @Test
    fun hintAndPenaltiesCombine() {
        val s = session(1000)
        s.grantHint()
        repeat(3) { s.wrongMove() }
        s.wrongMove()
        s.wrongMove()
        assertEquals(80L, s.currentRewardSeconds())
    }

    @Test
    fun dailyHintLimitBlocksFurtherRequests() {
        val limited = PuzzleSession(900, config.copy(dailyHintLimit = 2))
        assertTrue(limited.canRequestHint(1))
        assertFalse(limited.canRequestHint(2))
    }

    @Test
    fun hintsAreUnlimitedByDefault() {
        assertTrue(session().canRequestHint(10_000))
    }

    @Test
    fun noHintAfterFinish() {
        val s = session()
        s.solved()
        assertFalse(s.canRequestHint(0))
    }

    @Test
    fun hintAvailabilityReportsTheBlockingReason() {
        val s = session()
        assertEquals(HintAvailability.AVAILABLE, s.hintAvailability(0))
        s.grantHint()
        s.grantHint()
        assertEquals(HintAvailability.LEVEL_REACHED, s.hintAvailability(0))
        assertEquals(HintAvailability.LEVEL_REACHED, s.hintAvailability(10))

        val limited = PuzzleSession(900, config.copy(dailyHintLimit = 2))
        assertEquals(HintAvailability.DAILY_LIMIT, limited.hintAvailability(2))

        s.skip()
        assertEquals(HintAvailability.FINISHED, s.hintAvailability(0))
    }
}
