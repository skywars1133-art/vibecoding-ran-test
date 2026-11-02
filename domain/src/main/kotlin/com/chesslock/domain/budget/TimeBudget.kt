package com.chesslock.domain.budget

import java.time.LocalDate

/** Today's usage. Persisted by the data layer; [date] is the local date it belongs to. */
data class BudgetState(
    val date: LocalDate,
    val earnedSeconds: Long = 0L,
    val usedSeconds: Long = 0L,
)

/**
 * Shared daily budget (SPEC 1). Pure functions: remaining = limit + earned (optionally capped) - used.
 */
object TimeBudget {

    /**
     * Resets at local midnight, i.e. when [today] is after the stored date. If the clock or time
     * zone moves backwards, the state is kept so that changing the clock cannot refill the budget.
     */
    fun rollover(state: BudgetState, today: LocalDate): BudgetState =
        if (today.isAfter(state.date)) BudgetState(date = today) else state

    fun totalSeconds(state: BudgetState, limitSeconds: Long, capSeconds: Long?): Long {
        val total = limitSeconds + state.earnedSeconds
        return if (capSeconds != null) minOf(total, capSeconds) else total
    }

    fun remainingSeconds(state: BudgetState, limitSeconds: Long, capSeconds: Long?): Long =
        (totalSeconds(state, limitSeconds, capSeconds) - state.usedSeconds).coerceAtLeast(0L)

    /** Time counts only while a blocked app is in the foreground; the caller reports it. */
    fun addUsage(state: BudgetState, seconds: Long): BudgetState =
        state.copy(usedSeconds = state.usedSeconds + seconds.coerceAtLeast(0L))

    /** Earned time is added to today's budget (and expires at midnight), limited by the cap if set. */
    fun addEarned(
        state: BudgetState,
        seconds: Long,
        limitSeconds: Long,
        capSeconds: Long?,
    ): BudgetState {
        val maxEarned = if (capSeconds != null) (capSeconds - limitSeconds).coerceAtLeast(0L) else Long.MAX_VALUE
        val earned = maxOf(state.earnedSeconds, minOf(state.earnedSeconds + seconds.coerceAtLeast(0L), maxEarned))
        return state.copy(earnedSeconds = earned)
    }

    /** Warning thresholds crossed when remaining time dropped from [previous] to [current]. */
    fun crossedWarnings(previous: Long, current: Long, warnAtSeconds: List<Long>): List<Long> =
        warnAtSeconds.filter { previous > it && current <= it }
}
