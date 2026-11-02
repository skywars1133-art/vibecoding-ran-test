package com.chesslock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesslock.data.AppSettings
import com.chesslock.data.PlayerState
import com.chesslock.data.PlayerStore
import com.chesslock.data.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    playerStore: PlayerStore,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    /** Null until the stored player state has been read (used to avoid flashing the wrong screen). */
    val player: StateFlow<PlayerState?> = playerStore.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    /** Null until settings are read, so routing never flashes the permissions screen. */
    val settings: StateFlow<AppSettings?> = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    fun markPermissionsOnboardingSeen() {
        viewModelScope.launch {
            settingsStore.setPermissionsOnboardingSeen(true)
        }
    }
}
