package com.chesslock.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesslock.blocking.BlockingServiceController
import com.chesslock.data.AppSettings
import com.chesslock.data.BudgetStore
import com.chesslock.data.PlayerState
import com.chesslock.data.PlayerStore
import com.chesslock.data.SettingsStore
import com.chesslock.domain.budget.BudgetState
import com.chesslock.domain.config.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    playerStore: PlayerStore,
    settingsStore: SettingsStore,
    budgetStore: BudgetStore,
    blockingController: BlockingServiceController,
    config: AppConfig,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(
        playerStore.state,
        settingsStore.settings,
        budgetStore.state,
        blockingController.running,
    ) { player, settings, budget, running ->
        buildHomeUiState(player, settings, budget, running)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = buildHomeUiState(
            player = PlayerState(
                elo = config.eloStart,
                ratedPuzzlesPlayed = 0,
                freeSkipsUsedToday = 0,
                hintsUsedToday = 0,
                placementCompleted = true,
            ),
            settings = AppSettings.defaults(config),
            budget = BudgetState(date = LocalDate.now()),
            protectionRunning = false,
        ),
    )
}
