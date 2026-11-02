package com.chesslock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.chesslock.ui.placement.PlacementTestScreen
import com.chesslock.ui.puzzle.PuzzleScreen
import com.chesslock.ui.settings.SettingsScreen
import com.chesslock.ui.theme.ChessLockTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChessLockTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val mainViewModel: MainViewModel = hiltViewModel()
                    val player by mainViewModel.player.collectAsState()
                    when {
                        player == null -> LoadingScreen()

                        player?.placementCompleted != true -> PlacementTestScreen()

                        else -> {
                            var showSettings by rememberSaveable { mutableStateOf(false) }
                            if (showSettings) {
                                SettingsScreen(onBack = { showSettings = false })
                            } else {
                                PuzzleScreen(onOpenSettings = { showSettings = true })
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
