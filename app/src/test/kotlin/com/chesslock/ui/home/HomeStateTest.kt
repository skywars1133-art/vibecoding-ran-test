package com.chesslock.ui.home

import com.chesslock.data.AppSettings
import com.chesslock.data.PlayerState
import com.chesslock.domain.budget.BudgetState
import com.chesslock.domain.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HomeStateTest {

    private val settings = AppSettings.defaults(AppConfig.DEFAULT).copy(
        dailyLimitMinutes = 30,
        dailyCapMinutes = null,
    )
    private val player = PlayerState(
        elo = 1559,
        ratedPuzzlesPlayed = 3,
        freeSkipsUsedToday = 0,
        hintsUsedToday = 0,
        placementCompleted = true,
    )
    private val today = LocalDate.of(2026, 11, 3)

    @Test
    fun `remaining time is limit plus earned minus used`() {
        val state = buildHomeUiState(
            player = player,
            settings = settings,
            budget = BudgetState(date = today, earnedSeconds = 600, usedSeconds = 300),
            protectionRunning = true,
        )

        assertEquals(1559, state.elo)
        assertEquals(1_800L, state.limitSeconds)
        assertEquals(2_400L, state.totalSeconds)
        assertEquals(2_100L, state.remainingSeconds)
        assertEquals(600L, state.earnedSeconds)
        assertEquals(300L, state.usedSeconds)
        assertTrue(state.hasTimeLeft)
        assertTrue(state.protectionRunning)
        assertEquals(0.875f, state.remainingFraction)
    }

    @Test
    fun `the optional cap limits the total budget`() {
        val state = buildHomeUiState(
            player = player,
            settings = settings.copy(dailyCapMinutes = 40),
            budget = BudgetState(date = today, earnedSeconds = 900, usedSeconds = 300),
            protectionRunning = false,
        )

        assertEquals(2_400L, state.totalSeconds)
        assertEquals(2_100L, state.remainingSeconds)
    }

    @Test
    fun `used time beyond the budget clamps remaining to zero`() {
        val state = buildHomeUiState(
            player = player,
            settings = settings,
            budget = BudgetState(date = today, earnedSeconds = 0, usedSeconds = 2_000),
            protectionRunning = false,
        )

        assertEquals(0L, state.remainingSeconds)
        assertEquals(0f, state.remainingFraction)
        assertFalse(state.hasTimeLeft)
    }

    @Test
    fun `a zero total never divides by zero`() {
        val state = buildHomeUiState(
            player = player,
            settings = settings.copy(dailyLimitMinutes = 0),
            budget = BudgetState(date = today),
            protectionRunning = false,
        )

        assertEquals(0L, state.totalSeconds)
        assertEquals(0L, state.remainingSeconds)
        assertEquals(0f, state.remainingFraction)
    }

    @Test
    fun `blocking state comes from settings and the service`() {
        val enabled = buildHomeUiState(
            player = player,
            settings = settings.copy(blockingEnabled = true),
            budget = BudgetState(date = today),
            protectionRunning = true,
        )
        assertTrue(enabled.blockingEnabled)
        assertTrue(enabled.protectionRunning)

        val disabled = buildHomeUiState(
            player = player,
            settings = settings.copy(blockingEnabled = false),
            budget = BudgetState(date = today),
            protectionRunning = false,
        )
        assertFalse(disabled.blockingEnabled)
        assertFalse(disabled.protectionRunning)
    }
}
