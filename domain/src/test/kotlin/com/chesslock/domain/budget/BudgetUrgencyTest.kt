package com.chesslock.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetUrgencyTest {

    private val warnings = listOf(300L, 60L)

    @Test
    fun `plenty of time is comfortable`() {
        assertEquals(BudgetUrgency.COMFORTABLE, BudgetUrgency.forRemaining(1_800L, warnings))
        assertEquals(BudgetUrgency.COMFORTABLE, BudgetUrgency.forRemaining(301L, warnings))
    }

    @Test
    fun `the higher threshold turns the budget into a warning`() {
        assertEquals(BudgetUrgency.WARNING, BudgetUrgency.forRemaining(300L, warnings))
        assertEquals(BudgetUrgency.WARNING, BudgetUrgency.forRemaining(61L, warnings))
    }

    @Test
    fun `the lower threshold and below is critical`() {
        assertEquals(BudgetUrgency.CRITICAL, BudgetUrgency.forRemaining(60L, warnings))
        assertEquals(BudgetUrgency.CRITICAL, BudgetUrgency.forRemaining(1L, warnings))
    }

    @Test
    fun `zero and overspent budgets are empty`() {
        assertEquals(BudgetUrgency.EMPTY, BudgetUrgency.forRemaining(0L, warnings))
        assertEquals(BudgetUrgency.EMPTY, BudgetUrgency.forRemaining(-120L, warnings))
    }

    @Test
    fun `threshold order in the input does not matter`() {
        assertEquals(BudgetUrgency.WARNING, BudgetUrgency.forRemaining(120L, listOf(60L, 300L)))
        assertEquals(BudgetUrgency.CRITICAL, BudgetUrgency.forRemaining(30L, listOf(60L, 300L)))
    }

    @Test
    fun `a missing threshold list stays comfortable`() {
        assertEquals(BudgetUrgency.COMFORTABLE, BudgetUrgency.forRemaining(5L, emptyList()))
        assertEquals(BudgetUrgency.EMPTY, BudgetUrgency.forRemaining(0L, emptyList()))
    }

    @Test
    fun `non-positive thresholds are ignored`() {
        assertEquals(BudgetUrgency.CRITICAL, BudgetUrgency.forRemaining(10L, listOf(-5L, 0L, 60L)))
        assertEquals(BudgetUrgency.COMFORTABLE, BudgetUrgency.forRemaining(10L, listOf(-5L, 0L)))
    }
}
