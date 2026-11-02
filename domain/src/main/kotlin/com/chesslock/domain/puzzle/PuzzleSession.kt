package com.chesslock.domain.puzzle

import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.elo.EloResult
import com.chesslock.domain.reward.RewardCalculator

sealed interface WrongMoveOutcome {
    /** A life was lost. [eloLossApplied] is true when this was the last life. */
    data class LifeLost(val livesLeft: Int, val eloLossApplied: Boolean) : WrongMoveOutcome

    /** No lives left: the reward shrinks. [totalPenaltyMoves] counts all such moves so far. */
    data class RewardPenalty(val totalPenaltyMoves: Int, val rewardSecondsLeft: Long) : WrongMoveOutcome
}

data class SolveOutcome(
    val rewardSeconds: Long,
    val eloResult: EloResult,
    /** True when the LOSS was already applied earlier (third life lost); do not apply ELO twice. */
    val eloAlreadyApplied: Boolean,
)

/** Why the Hint button is unavailable; the UI maps this to an explanation (SPEC 3b). */
enum class HintAvailability {
    AVAILABLE,
    /** Both hint levels for the current move were already shown. */
    LEVEL_REACHED,
    /** The configurable daily hint limit was reached. */
    DAILY_LIMIT,
    /** The puzzle is solved, skipped or otherwise finished. */
    FINISHED,
}

/**
 * Pure rules of one puzzle attempt: lives, post-loss penalties, hints, ELO result, reward.
 * Knows nothing about chess moves, ads, storage or time; the caller reports events.
 */
class PuzzleSession(
    val puzzleRating: Int,
    private val config: AppConfig = AppConfig.DEFAULT,
) {
    var livesLeft: Int = config.livesPerPuzzle
        private set
    var penaltyMoves: Int = 0
        private set
    var hintsUsed: Int = 0
        private set
    var currentHintLevel: Int = 0
        private set

    /** Set the moment ELO becomes decided (LOSS on the third lost life, else on solve). */
    var eloResult: EloResult? = null
        private set
    var finished: Boolean = false
        private set

    val hintUsed: Boolean get() = hintsUsed > 0

    /** What the puzzle would pay out if solved right now. */
    fun currentRewardSeconds(): Long =
        RewardCalculator.rewardSeconds(puzzleRating, hintUsed, penaltyMoves, config)

    fun wrongMove(): WrongMoveOutcome {
        check(!finished) { "Puzzle already finished" }
        if (livesLeft > 0) {
            livesLeft -= 1
            val lost = livesLeft == 0
            if (lost) eloResult = EloResult.LOSS
            return WrongMoveOutcome.LifeLost(livesLeft, eloLossApplied = lost)
        }
        penaltyMoves += 1
        return WrongMoveOutcome.RewardPenalty(penaltyMoves, currentRewardSeconds())
    }

    /** The hint level counter resets after every correct move. */
    fun correctMove() {
        check(!finished) { "Puzzle already finished" }
        currentHintLevel = 0
    }

    /**
     * Whether the Hint button may start a rewarded ad. [hintsUsedToday] counts all puzzles today.
     * Does not cover ad availability or connectivity; the ads layer checks those.
     */
    fun canRequestHint(hintsUsedToday: Int): Boolean =
        hintAvailability(hintsUsedToday) == HintAvailability.AVAILABLE

    /** Distinguishes "no more levels for this move" from "daily limit reached" for the UI note. */
    fun hintAvailability(hintsUsedToday: Int): HintAvailability = when {
        finished -> HintAvailability.FINISHED
        currentHintLevel >= config.hintLevelsPerMove -> HintAvailability.LEVEL_REACHED
        config.dailyHintLimit != null && hintsUsedToday >= config.dailyHintLimit -> HintAvailability.DAILY_LIMIT
        else -> HintAvailability.AVAILABLE
    }

    /**
     * Call ONLY from the ad's `onUserEarnedReward` callback. Returns the new hint level.
     * Never costs a life.
     */
    fun grantHint(): Int {
        check(!finished) { "Puzzle already finished" }
        check(currentHintLevel < config.hintLevelsPerMove) { "No more hint levels for this move" }
        hintsUsed += 1
        currentHintLevel += 1
        return currentHintLevel
    }

    fun solved(): SolveOutcome {
        check(!finished) { "Puzzle already finished" }
        finished = true
        val alreadyApplied = eloResult == EloResult.LOSS
        val result = eloResult ?: if (hintUsed) EloResult.WIN_WITH_HINT else EloResult.WIN
        eloResult = result
        return SolveOutcome(currentRewardSeconds(), result, alreadyApplied)
    }

    /**
     * Skip: no reward, no ELO change. If all lives were already lost, that LOSS stays applied
     * (see [eloResult]); otherwise [eloResult] stays null.
     */
    fun skip() {
        check(!finished) { "Puzzle already finished" }
        finished = true
    }
}
