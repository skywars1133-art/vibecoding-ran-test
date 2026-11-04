package com.chesslock.ui.home

import com.chesslock.data.AppSettings
import com.chesslock.data.PlayerState
import com.chesslock.domain.budget.BudgetState
import com.chesslock.domain.budget.TimeBudget

/** Everything the home screen shows, derived from the persisted stores. */
data class HomeUiState(
    val elo: Int,
    val remainingSeconds: Long,
    val totalSeconds: Long,
    val limitSeconds: Long,
    val earnedSeconds: Long,
    val usedSeconds: Long,
    val blockingEnabled: Boolean,
    val protectionRunning: Boolean,
) {
    val hasTimeLeft: Boolean get() = remainingSeconds > 0L

    /** Share of today's total budget still available, clamped for the progress bar. */
    val remainingFraction: Float
        get() = if (totalSeconds <= 0L) {
            0f
        } else {
            (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
        }
}

/** Pure mapping so the budget arithmetic is unit-testable without Android or coroutines. */
fun buildHomeUiState(
    player: PlayerState,
    settings: AppSettings,
    budget: BudgetState,
    protectionRunning: Boolean,
): HomeUiState {
    val limit = settings.dailyLimitSeconds
    val cap = settings.dailyCapSeconds
    return HomeUiState(
        elo = player.elo,
        remainingSeconds = TimeBudget.remainingSeconds(budget, limit, cap),
        totalSeconds = TimeBudget.totalSeconds(budget, limit, cap),
        limitSeconds = limit,
        earnedSeconds = budget.earnedSeconds,
        usedSeconds = budget.usedSeconds,
        blockingEnabled = settings.blockingEnabled,
        protectionRunning = protectionRunning,
    )
}
