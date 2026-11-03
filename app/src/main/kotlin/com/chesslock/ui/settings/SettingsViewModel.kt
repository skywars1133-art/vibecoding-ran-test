package com.chesslock.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesslock.blocking.BlockingServiceController
import com.chesslock.data.AppLanguage
import com.chesslock.data.AppSettings
import com.chesslock.data.PieceStyle
import com.chesslock.data.PuzzlePackScheduler
import com.chesslock.data.PuzzleRepository
import com.chesslock.data.PuzzleSyncState
import com.chesslock.data.PuzzleSyncStore
import com.chesslock.data.SettingsStore
import com.chesslock.data.ThemeMode
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
    private val repository: PuzzleRepository,
    private val syncStore: PuzzleSyncStore,
    private val packScheduler: PuzzlePackScheduler,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings.defaults(config),
    )

    val protectionRunning: StateFlow<Boolean> = blockingController.running

    /** Null while the first Room count is loading. */
    val stockCount: StateFlow<Int?> = repository.unsolvedCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val syncState: StateFlow<PuzzleSyncState> = syncStore.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PuzzleSyncState(),
    )

    val dailyLimitOptions: List<Int> = config.dailyLimitOptionsMinutes
    val dailyCapOptions: List<Int> = config.dailyCapOptionsMinutes
    val puzzlePackSize: Int = config.puzzlePackSize

    init {
        // The Settings screen may be the first screen that needs the offline stock count.
        viewModelScope.launch { repository.prepareStock() }
    }

    fun setPieceStyle(style: PieceStyle) {
        viewModelScope.launch {
            store.setPieceStyle(style)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            store.setLanguage(language)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            store.setThemeMode(mode)
        }
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        viewModelScope.launch {
            store.setWifiOnlyDownloads(enabled)
            // Re-enqueue so the network constraint changes immediately, including a pending sync.
            packScheduler.ensureScheduled()
            packScheduler.requestImmediateSync(force = false, replaceExisting = true)
        }
    }

    /** Manual "Download now": bypasses the 24-hour check but still respects the network setting. */
    fun downloadPuzzles() {
        viewModelScope.launch {
            packScheduler.requestImmediateSync(force = true)
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
