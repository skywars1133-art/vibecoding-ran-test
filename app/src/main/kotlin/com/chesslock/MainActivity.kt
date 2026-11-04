package com.chesslock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.chesslock.blocking.BlockingServiceController
import com.chesslock.data.AppLanguage
import com.chesslock.data.PuzzlePackScheduler
import com.chesslock.data.SettingsStore
import com.chesslock.data.ThemeMode
import com.chesslock.ui.common.ProvideAppLanguage
import com.chesslock.ui.home.HomeScreen
import com.chesslock.ui.onboarding.PermissionsScreen
import com.chesslock.ui.placement.PlacementTestScreen
import com.chesslock.ui.puzzle.PuzzleScreen
import com.chesslock.ui.settings.BlockedAppsScreen
import com.chesslock.ui.settings.SettingsScreen
import com.chesslock.ui.stats.StatisticsScreen
import com.chesslock.ui.theme.ChessLockTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var blockingController: BlockingServiceController
    @Inject lateinit var packScheduler: PuzzlePackScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val settings = settingsStore.settings.first()
            if (settings.blockingEnabled) blockingController.start()
            // Keep the periodic request current and top up the stock once per launch (the worker
            // itself skips when the last successful pack is less than 24 hours old).
            packScheduler.ensureScheduled()
            packScheduler.requestImmediateSync(force = false)
        }
        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val player by mainViewModel.player.collectAsState()
            val settings by mainViewModel.settings.collectAsState()
            val systemDark = isSystemInDarkTheme()
            ChessLockTheme(
                darkTheme = when (settings?.themeMode ?: ThemeMode.DEFAULT) {
                    ThemeMode.SYSTEM -> systemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
            ) {
                ProvideAppLanguage(settings?.language ?: AppLanguage.DEFAULT) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when {
                            player == null || settings == null -> LoadingScreen()

                            player?.placementCompleted != true -> PlacementTestScreen()

                            settings?.permissionsOnboardingSeen != true -> PermissionsScreen(
                                onDone = mainViewModel::markPermissionsOnboardingSeen,
                            )

                            else -> {
                                var showSettings by rememberSaveable { mutableStateOf(false) }
                                var showStatistics by rememberSaveable { mutableStateOf(false) }
                                var showPermissions by rememberSaveable { mutableStateOf(false) }
                                var showBlockedApps by rememberSaveable { mutableStateOf(false) }
                                var showPuzzle by rememberSaveable { mutableStateOf(false) }
                                when {
                                    showStatistics -> StatisticsScreen(
                                        onBack = { showStatistics = false },
                                    )

                                    showBlockedApps -> BlockedAppsScreen(
                                        onBack = { showBlockedApps = false },
                                    )

                                    showSettings -> SettingsScreen(
                                        onBack = { showSettings = false },
                                        onOpenApps = { showBlockedApps = true },
                                    )

                                    showPermissions -> PermissionsScreen(
                                        onDone = { showPermissions = false },
                                    )

                                    showPuzzle -> PuzzleScreen(
                                        onBack = { showPuzzle = false },
                                        onOpenSettings = { showSettings = true },
                                        onOpenStatistics = { showStatistics = true },
                                        onFixProtection = { showPermissions = true },
                                    )

                                    else -> HomeScreen(
                                        onSolve = { showPuzzle = true },
                                        onOpenSettings = { showSettings = true },
                                        onOpenStatistics = { showStatistics = true },
                                        onFixProtection = { showPermissions = true },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}
