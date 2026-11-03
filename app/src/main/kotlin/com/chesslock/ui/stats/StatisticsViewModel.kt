package com.chesslock.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesslock.data.BudgetStore
import com.chesslock.data.PlayerStore
import com.chesslock.data.PuzzleRepository
import com.chesslock.data.StatisticsStore
import com.chesslock.data.UsageDayEntity
import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.stats.Statistics
import com.chesslock.domain.stats.StatsSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val summary: StatsSummary? = null,
    /** Unique puzzles solved, including ones solved before statistics existed. */
    val solvedPuzzles: Int = 0,
    val todayUsedSeconds: Long = 0,
    val weekUsedSeconds: Long = 0,
)

/**
 * Combines the local attempt log, player ELO and the per-day usage history (SPEC 5).
 * Everything is read from the device; no account or network layer is involved.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    statisticsStore: StatisticsStore,
    playerStore: PlayerStore,
    budgetStore: BudgetStore,
    repository: PuzzleRepository,
    private val config: AppConfig,
) : ViewModel() {
    private val today = LocalDate.now()
    private val weekStart = today.minusDays((config.statsWeekDays - 1).coerceAtLeast(0).toLong())

    val state: StateFlow<StatisticsUiState> = combine(
        statisticsStore.observeAttempts(),
        playerStore.state,
        budgetStore.state,
        statisticsStore.observeUsageSince(weekStart),
        repository.solvedCount,
    ) { attempts, player, budget, usage, solvedPuzzles ->
        val summary = Statistics.summarize(
            attempts = attempts,
            currentElo = player.elo,
            today = today,
            config = config,
        )
        val usageTotals = weekUsage(usage, budget.usedSeconds)
        StatisticsUiState(
            isLoading = false,
            summary = summary,
            solvedPuzzles = solvedPuzzles,
            todayUsedSeconds = usageTotals.today,
            weekUsedSeconds = usageTotals.week,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatisticsUiState(),
    )

    /**
     * Today's total uses the larger of the history row and the live budget. They normally match,
     * but the budget may already hold usage recorded before this statistics table existed.
     */
    private fun weekUsage(usage: List<UsageDayEntity>, budgetTodaySeconds: Long): UsageTotals {
        val todayKey = today.toString()
        val historyToday = usage.firstOrNull { it.date == todayKey }?.usedSeconds ?: 0L
        val todaySeconds = maxOf(historyToday, budgetTodaySeconds)
        val earlierDays = usage
            .asSequence()
            .filter { it.date < todayKey }
            .sumOf { it.usedSeconds }
        return UsageTotals(today = todaySeconds, week = earlierDays + todaySeconds)
    }

    private data class UsageTotals(val today: Long, val week: Long)
}
