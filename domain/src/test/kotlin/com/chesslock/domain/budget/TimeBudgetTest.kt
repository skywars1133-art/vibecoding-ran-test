package com.chesslock.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class TimeBudgetTest {
    private val monday = LocalDate.of(2026, 11, 2)
    private val tuesday = monday.plusDays(1)
    private val limit = 30 * 60L

    @Test
    fun remainingIsLimitMinusUsage() {
        val s = TimeBudget.addUsage(BudgetState(monday), 600)
        assertEquals(1200L, TimeBudget.remainingSeconds(s, limit, null))
    }

    @Test
    fun earnedTimeIsAddedToBudget() {
        val s = TimeBudget.addEarned(BudgetState(monday), 240, limit, null)
        assertEquals(limit + 240, TimeBudget.remainingSeconds(s, limit, null))
    }

    @Test
    fun remainingNeverNegative() {
        val s = TimeBudget.addUsage(BudgetState(monday), limit + 500)
        assertEquals(0L, TimeBudget.remainingSeconds(s, limit, null))
    }

    @Test
    fun negativeInputsAreIgnored() {
        val s = TimeBudget.addUsage(BudgetState(monday), -50)
        assertEquals(0L, s.usedSeconds)
        val e = TimeBudget.addEarned(BudgetState(monday), -50, limit, null)
        assertEquals(0L, e.earnedSeconds)
    }

    @Test
    fun dailyCapLimitsTotalTime() {
        val cap = 40 * 60L
        val s = TimeBudget.addEarned(BudgetState(monday), 20 * 60L, limit, cap)
        assertEquals(10 * 60L, s.earnedSeconds)
        assertEquals(cap, TimeBudget.remainingSeconds(s, limit, cap))
    }

    @Test
    fun capBelowLimitStillCapsTotal() {
        val cap = 20 * 60L
        val s = TimeBudget.addEarned(BudgetState(monday), 300, limit, cap)
        assertEquals(0L, s.earnedSeconds)
        assertEquals(cap, TimeBudget.remainingSeconds(s, limit, cap))
    }

    @Test
    fun loweringCapDoesNotShrinkAlreadyEarnedTime() {
        val earned = BudgetState(monday, earnedSeconds = 600)
        val s = TimeBudget.addEarned(earned, 60, limit, capSeconds = limit)
        assertEquals(600L, s.earnedSeconds)
    }

    @Test
    fun resetsAtLocalMidnight() {
        val used = BudgetState(monday, earnedSeconds = 900, usedSeconds = 1500)
        val next = TimeBudget.rollover(used, tuesday)
        assertEquals(BudgetState(tuesday), next)
        assertEquals(limit, TimeBudget.remainingSeconds(next, limit, null))
    }

    @Test
    fun sameDayKeepsState() {
        val used = BudgetState(monday, usedSeconds = 100)
        assertSame(used, TimeBudget.rollover(used, monday))
    }

    @Test
    fun clockMovedBackwardsDoesNotRefillBudget() {
        val used = BudgetState(tuesday, usedSeconds = 1700)
        assertSame(used, TimeBudget.rollover(used, monday))
    }

    @Test
    fun skippingSeveralDaysStillResets() {
        val used = BudgetState(monday, usedSeconds = 1700)
        assertEquals(BudgetState(monday.plusDays(5)), TimeBudget.rollover(used, monday.plusDays(5)))
    }

    @Test
    fun warningsAreReportedWhenThresholdsAreCrossed() {
        val warn = listOf(300L, 60L)
        assertEquals(listOf(300L), TimeBudget.crossedWarnings(400, 290, warn))
        assertEquals(listOf(60L), TimeBudget.crossedWarnings(100, 59, warn))
        assertEquals(listOf(300L, 60L), TimeBudget.crossedWarnings(400, 10, warn))
        assertEquals(emptyList<Long>(), TimeBudget.crossedWarnings(290, 280, warn))
        assertEquals(emptyList<Long>(), TimeBudget.crossedWarnings(500, 400, warn))
    }

    @Test
    fun warningFiresWhenRemainingHitsThresholdExactly() {
        assertEquals(listOf(300L), TimeBudget.crossedWarnings(301, 300, listOf(300L, 60L)))
    }
}
