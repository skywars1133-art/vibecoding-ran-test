package com.chesslock.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chesslock.blocking.AppInfoProvider
import com.chesslock.blocking.LaunchableApp
import com.chesslock.data.AppBlockOverride
import com.chesslock.data.AppSettings
import com.chesslock.data.SettingsStore
import com.chesslock.domain.config.AppConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class BlockedAppsViewModel @Inject constructor(
    private val appInfoProvider: AppInfoProvider,
    private val store: SettingsStore,
    config: AppConfig,
) : ViewModel() {
    /** Null until the installed-app query finishes. */
    private val installed = MutableStateFlow<List<LaunchableApp>?>(null)
    private val query = MutableStateFlow("")
    private val settings = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppSettings.defaults(config),
    )

    val loading: StateFlow<Boolean> = installed.map { it == null }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true,
    )

    val entries: StateFlow<List<AppPickerEntry>> = combine(
        installed,
        settings,
        query,
    ) { apps, currentSettings, currentQuery ->
        filterApps(
            apps.orEmpty().map {
                AppPickerEntry(
                    packageName = it.packageName,
                    label = it.label,
                    override = currentSettings.overrideFor(it.packageName),
                )
            },
            currentQuery,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    val searchQuery: StateFlow<String> = query

    init {
        viewModelScope.launch {
            installed.value = withContext(Dispatchers.IO) { appInfoProvider.launchableApps() }
        }
    }

    fun setSearchQuery(value: String) {
        query.value = value
    }

    fun setOverride(packageName: String, override: AppBlockOverride) {
        viewModelScope.launch {
            store.setAppOverride(packageName, override)
        }
    }
}
