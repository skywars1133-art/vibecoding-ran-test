package com.chesslock.domain.blocking

import org.junit.Assert.assertEquals
import org.junit.Test

class PollingPolicyTest {

    private fun interval(remainingSeconds: Long) = PollingPolicy.nextIntervalSeconds(
        remainingSeconds = remainingSeconds,
        lowBudgetThresholdSeconds = 300,
        activeIntervalSeconds = 2,
        relaxedIntervalSeconds = 5,
    )

    @Test
    fun `a comfortable budget uses the relaxed interval`() {
        assertEquals(5L, interval(remainingSeconds = 1_800))
        assertEquals(5L, interval(remainingSeconds = 301))
    }

    @Test
    fun `the low-budget threshold switches to the active interval`() {
        assertEquals(2L, interval(remainingSeconds = 300))
        assertEquals(2L, interval(remainingSeconds = 120))
    }

    @Test
    fun `an empty or overspent budget stays on the active interval`() {
        assertEquals(2L, interval(remainingSeconds = 0))
        assertEquals(2L, interval(remainingSeconds = -10))
    }

    @Test
    fun `the relaxed interval is never shorter than the active one`() {
        assertEquals(
            2L,
            PollingPolicy.nextIntervalSeconds(
                remainingSeconds = 1_800,
                lowBudgetThresholdSeconds = 300,
                activeIntervalSeconds = 2,
                relaxedIntervalSeconds = 1,
            ),
        )
    }

    @Test
    fun `non-positive intervals are clamped to one second`() {
        assertEquals(
            1L,
            PollingPolicy.nextIntervalSeconds(
                remainingSeconds = 0,
                lowBudgetThresholdSeconds = 300,
                activeIntervalSeconds = 0,
                relaxedIntervalSeconds = -5,
            ),
        )
        assertEquals(
            1L,
            PollingPolicy.nextIntervalSeconds(
                remainingSeconds = 1_800,
                lowBudgetThresholdSeconds = -10,
                activeIntervalSeconds = 0,
                relaxedIntervalSeconds = 0,
            ),
        )
    }
}
