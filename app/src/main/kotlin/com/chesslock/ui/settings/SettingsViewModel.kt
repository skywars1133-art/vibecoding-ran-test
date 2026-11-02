package com.chesslock.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesslock.blocking.BlockingServiceController
import com.chesslock.data.AppSettings
import com.chesslock.data.PieceStyle
import com.chesslock.data.SettingsStore
import com.chesslock.domain.blocking.BlockCategory
import com.chesslock.domain.config.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val config: AppConfig,
    private val blockingController: BlockingServiceController,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings.defaults(config),
    )

    val protectionRunning: StateFlow<Boolean> = blockingController.running

    val dailyLimitOptions: List<Int> = config.dailyLimitOptionsMinutes
    val dailyCapOptions: List<Int> = config.dailyCapOptionsMinutes

    fun setPieceStyle(style: PieceStyle) {
        viewModelScope.launch {
            store.setPieceStyle(style)
        }
    }

    fun setBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            store.setBlockingEnabled(enabled)
            blockingController.setEnabled(enabled)
        }
    }

    fun setDailyLimitMinutes(minutes: Int) {
        viewModelScope.launch {
            store.setDailyLimitMinutes(minutes)
        }
    }

    fun setDailyCapMinutes(minutes: Int?) {
        viewModelScope.launch {
            store.setDailyCapMinutes(minutes)
        }
    }

    fun setCategoryBlocked(category: BlockCategory, blocked: Boolean) {
        viewModelScope.launch {
            store.setBlockedCategory(category, blocked)
        }
    }
}
