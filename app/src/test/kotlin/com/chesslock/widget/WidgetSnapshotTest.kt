package com.chesslock.widget

import com.chesslock.domain.budget.BudgetState
import com.chesslock.domain.budget.BudgetUrgency
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class WidgetSnapshotTest {

    private val today = LocalDate.of(2026, 11, 14)
    private val warnAt = listOf(300L, 60L)

    private fun snapshot(
        earnedSeconds: Long = 0L,
        usedSeconds: Long = 0L,
        limitSeconds: Long = 1_800L,
        capSeconds: Long? = null,
        elo: Int = 1667,
    ) = buildWidgetSnapshot(
        budget = BudgetState(date = today, earnedSeconds = earnedSeconds, usedSeconds = usedSeconds),
        limitSeconds = limitSeconds,
        capSeconds = capSeconds,
        elo = elo,
        warnAtSeconds = warnAt,
    )

    @Test
    fun `full budget fills the bar`() {
        val state = snapshot(elo = 1667)

        assertEquals(1_800L, state.remainingSeconds)
        assertEquals(1_800L, state.totalSeconds)
        assertEquals(1.0f, state.fraction, 0.001f)
        assertEquals(100, state.progressPercent)
        assertEquals(BudgetUrgency.COMFORTABLE, state.urgency)
        assertEquals(1667, state.elo)
    }

    @Test
    fun `earned minutes raise the bar and used minutes lower it`() {
        val state = snapshot(earnedSeconds = 600L, usedSeconds = 300L)

        assertEquals(2_400L, state.totalSeconds)
        assertEquals(2_100L, state.remainingSeconds)
        assertEquals(0.875f, state.fraction, 0.001f)
        assertEquals(88, state.progressPercent)
    }

    @Test
    fun `daily cap limits the total budget`() {
        val state = snapshot(earnedSeconds = 1_200L, capSeconds = 2_400L)

        assertEquals(2_400L, state.totalSeconds)
        assertEquals(2_400L, state.remainingSeconds)
        assertEquals(100, state.progressPercent)
    }

    @Test
    fun `over-used budget is empty and never negative`() {
        val state = snapshot(usedSeconds = 2_000L)

        assertEquals(0L, state.remainingSeconds)
        assertEquals(0f, state.fraction, 0.001f)
        assertEquals(0, state.progressPercent)
        assertEquals(BudgetUrgency.EMPTY, state.urgency)
    }

    @Test
    fun `zero total does not divide by zero`() {
        val state = snapshot(limitSeconds = 0L)

        assertEquals(0f, state.fraction, 0.001f)
        assertEquals(0, state.progressPercent)
        assertEquals(BudgetUrgency.EMPTY, state.urgency)
    }

    @Test
    fun `urgency follows the same thresholds as the warnings`() {
        assertEquals(BudgetUrgency.WARNING, snapshot(usedSeconds = 1_500L).urgency)
        assertEquals(BudgetUrgency.CRITICAL, snapshot(usedSeconds = 1_740L).urgency)
        assertEquals(BudgetUrgency.COMFORTABLE, snapshot(usedSeconds = 1_439L).urgency)
    }
}
