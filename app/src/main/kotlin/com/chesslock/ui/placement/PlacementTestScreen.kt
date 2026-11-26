package com.chesslock.ui.placement

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chesslock.R
import com.chesslock.ui.puzzle.ChessBoard
import com.chesslock.ui.puzzle.LivesIndicator
import com.chesslock.ui.puzzle.MoveStatusText
import com.chesslock.ui.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacementTestScreen(
    viewModel: PlacementTestViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null,
    practice: Boolean = false,
) {
    val state by viewModel.state.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val exit = onBack
    if (exit != null) BackHandler(onBack = exit)
    val title: @Composable () -> Unit = {
        Text(
            stringResource(
                if (practice) R.string.placement_practice_title else R.string.placement_title,
            ),
        )
    }
    val navigationIcon: @Composable () -> Unit = exit?.let {
        { TextButton(onClick = it) { Text(stringResource(R.string.back)) } }
    } ?: {}
    Scaffold(
        topBar = { TopAppBar(title = title, navigationIcon = navigationIcon) },
    ) { padding ->
        when {
            state.showResult -> PlacementResult(
                state = state,
                padding = padding,
                practice = practice,
                onConfirm = if (practice) exit ?: {} else viewModel::confirmResult,
            )

            state.isLoading -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) { CircularProgressIndicator() }

            state.loadError -> PlacementError(padding = padding, onRetry = viewModel::retry)

            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        Text(
                            text = stringResource(
                                R.string.placement_progress,
                                state.currentPuzzleNumber,
                                state.total,
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.placement_progress_percent, state.completionPercent),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(stringResource(R.string.puzzle_rating, state.puzzleRating))
                }
                if (practice) {
                    Text(
                        text = stringResource(R.string.placement_practice_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = stringResource(
                        if (state.playerIsWhite) R.string.side_white else R.string.side_black,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ChessBoard(
                    pieces = state.pieces,
                    legalMoves = state.legalMoves,
                    lastMove = state.lastMove,
                    moveTimeline = state.moveTimeline,
                    playerIsWhite = state.playerIsWhite,
                    solved = state.puzzleSolved,
                    onMove = viewModel::onMove,
                    pieceStyle = settings.pieceStyle,
                    resetKey = state.puzzleId,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LivesIndicator(livesLeft = state.livesLeft, maxLives = state.maxLives)
                    Text(
                        text = stringResource(R.string.lives, state.livesLeft),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                MoveStatusText(state.moveStatus)
                if (state.puzzleFailed) {
                    Text(
                        text = stringResource(R.string.placement_failed),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                if (state.puzzleSolved || state.puzzleFailed) {
                    Button(onClick = viewModel::advance) {
                        Text(
                            stringResource(
                                if (state.testComplete) R.string.placement_see_result
                                else R.string.placement_next,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PlacementResult(
    state: PlacementUiState,
    padding: PaddingValues,
    practice: Boolean,
    onConfirm: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(
                if (practice) R.string.placement_practice_result_title else R.string.placement_result_title,
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = state.finalElo.toString(),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(
                when {
                    practice && state.endedEarly -> R.string.placement_practice_result_early
                    practice -> R.string.placement_practice_result_body
                    state.endedEarly -> R.string.placement_result_early
                    else -> R.string.placement_result_body
                },
                state.finalElo,
            ),
            textAlign = TextAlign.Center,
        )
        if (state.saveError) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.placement_save_error),
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(enabled = !state.saving, onClick = onConfirm) {
            Text(
                stringResource(
                    when {
                        practice -> R.string.placement_practice_done
                        state.saving -> R.string.placement_saving
                        else -> R.string.placement_start
                    },
                ),
            )
        }
    }
}

@Composable
private fun PlacementError(padding: PaddingValues, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
    ) {
        Text(stringResource(R.string.puzzle_load_error))
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}
