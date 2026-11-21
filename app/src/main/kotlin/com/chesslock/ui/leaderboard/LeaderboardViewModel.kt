package com.chesslock.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesslock.BuildConfig
import com.chesslock.data.LeaderboardIdentity
import com.chesslock.data.LeaderboardRepository
import com.chesslock.data.PlayerStore
import com.chesslock.domain.config.AppConfig
import com.chesslock.domain.leaderboard.LeaderboardEntry
import com.chesslock.domain.leaderboard.LeaderboardRanks
import com.chesslock.domain.leaderboard.LeaderboardSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LeaderboardError { LOAD }

data class LeaderboardUiState(
    val isLoading: Boolean = true,
    val snapshot: LeaderboardSnapshot? = null,
    val error: LeaderboardError? = null,
    /** True while the local preview source is bound instead of Firebase. */
    val isLocalPreview: Boolean = true,
)

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepository,
    private val identity: LeaderboardIdentity,
    private val playerStore: PlayerStore,
    private val config: AppConfig,
) : ViewModel() {
    private val _state = MutableStateFlow(
        LeaderboardUiState(isLocalPreview = !BuildConfig.FIREBASE_CONFIGURED),
    )
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Publishes the current local chess stats, then reloads the board. */
    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val player = playerStore.currentState()
                val entry = LeaderboardEntry(
                    playerId = identity.playerId(),
                    nickname = LeaderboardRanks.normalizeNickname(identity.nickname(), config),
                    elo = player.elo,
                    ratedPuzzlesPlayed = player.ratedPuzzlesPlayed,
                )
                repository.publish(entry)
                val snapshot = repository.load(entry.playerId)
                _state.value = LeaderboardUiState(
                    isLoading = false,
                    snapshot = snapshot,
                    isLocalPreview = !BuildConfig.FIREBASE_CONFIGURED,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = LeaderboardError.LOAD)
            }
        }
    }
}
