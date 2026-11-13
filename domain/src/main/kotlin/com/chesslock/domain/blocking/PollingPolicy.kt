package com.chesslock.domain.blocking

/**
 * Chooses how long the blocking service waits before its next foreground poll (SPEC 6).
 *
 * Polling quickly is only important when the budget is nearly gone, because that is when the block
 * screen has to appear promptly. While the player still has plenty of time, a slower poll saves
 * battery without a noticeable effect: the few seconds before counting starts are insignificant
 * against several minutes of remaining budget.
 */
object PollingPolicy {

    /**
     * Returns the next poll interval in seconds. Non-positive intervals are clamped so the service
     * can never busy-loop, and the relaxed interval is never shorter than the active one.
     */
    fun nextIntervalSeconds(
        remainingSeconds: Long,
        lowBudgetThresholdSeconds: Long,
        activeIntervalSeconds: Long,
        relaxedIntervalSeconds: Long,
    ): Long {
        val active = activeIntervalSeconds.coerceAtLeast(1L)
        val relaxed = relaxedIntervalSeconds.coerceAtLeast(active)
        return if (remainingSeconds <= lowBudgetThresholdSeconds.coerceAtLeast(0L)) active else relaxed
    }
}
