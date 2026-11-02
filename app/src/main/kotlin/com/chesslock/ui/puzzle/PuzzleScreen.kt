package com.chesslock.ui.puzzle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chesslock.R
import com.chesslock.blocking.BlockingPermissions
import com.chesslock.ui.blocking.ProtectionStatusBanner
import com.chesslock.ui.settings.SettingsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    onOpenSettings: () -> Unit,
    onFixProtection: () -> Unit,
    viewModel: PuzzleViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val protectionRunning by settingsViewModel.protectionRunning.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionEpoch by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissionEpoch++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val usageAccessGranted = remember(permissionEpoch) {
        BlockingPermissions.hasUsageAccess(context)
    }
    val overlayGranted = remember(permissionEpoch) {
        BlockingPermissions.canDrawOverlays(context)
    }
    val protectionActive = settings.blockingEnabled &&
        usageAccessGranted &&
        overlayGranted &&
        protectionRunning
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.puzzle_title)) },
                actions = {
                    TextButton(onClick = onOpenSettings) {
                        Text(stringResource(R.string.settings_title))
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) { CircularProgressIndicator() }

            state.stockEmpty || state.loadError -> MessageScreen(state, padding, viewModel::loadPuzzle)
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                ProtectionStatusBanner(
                    blockingEnabled = settings.blockingEnabled,
                    active = protectionActive,
                    onOpenSettings = onOpenSettings,
                    onFixProtection = onFixProtection,
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.player_elo, state.playerElo))
                    Text(stringResource(R.string.puzzle_rating, state.puzzleRating))
                }
                Text(
                    text = stringResource(
                        if (state.playerIsWhite) R.string.side_white else R.string.side_black,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(stringResource(R.string.solving_time, formatDuration(state.elapsedSeconds)))
                ChessBoard(
                    pieces = state.pieces,
                    legalMoves = state.legalMoves,
                    lastMove = state.lastMove,
                    playerIsWhite = state.playerIsWhite,
                    solved = state.solved,
                    onMove = viewModel::onMove,
                    pieceStyle = settings.pieceStyle,
                    resetKey = state.puzzleId,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.lives, state.livesLeft))
                    Text(stringResource(R.string.reward_time, formatDuration(state.rewardSeconds)))
                }
                state.eloDelta?.let { delta ->
                    Text(
                        text = stringResource(R.string.elo_change, String.format(Locale.getDefault(), "%+d", delta)),
                        color = if (delta < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
                MoveStatusText(state.moveStatus)
                if (state.solved) {
                    Button(onClick = viewModel::loadPuzzle) {
                        Text(stringResource(R.string.next_puzzle))
                    }
                } else {
                    Button(
                        enabled = state.canSkip,
                        onClick = viewModel::skipPuzzle,
                    ) {
                        Text(stringResource(R.string.skip_puzzle))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MessageScreen(
    state: PuzzleUiState,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onRetry: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
    ) {
        Text(stringResource(if (state.stockEmpty) R.string.no_puzzles else R.string.puzzle_load_error))
        if (!state.stockEmpty) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

private fun formatDuration(seconds: Long): String =
    String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
