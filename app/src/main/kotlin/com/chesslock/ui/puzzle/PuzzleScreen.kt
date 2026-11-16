package com.chesslock.ui.puzzle

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chesslock.R
import com.chesslock.ads.AdsEntryPoint
import com.chesslock.ads.NetworkStatus
import com.chesslock.ads.RewardedAdManager
import com.chesslock.ads.RewardedAdState
import com.chesslock.blocking.BlockingPermissions
import com.chesslock.domain.puzzle.HintAvailability
import com.chesslock.ui.blocking.ProtectionStatusBanner
import com.chesslock.ui.common.formatClock
import com.chesslock.ui.settings.SettingsViewModel
import dagger.hilt.android.EntryPointAccessors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    onOpenSettings: () -> Unit,
    onOpenStatistics: () -> Unit,
    onFixProtection: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: PuzzleViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val protectionRunning by settingsViewModel.protectionRunning.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionEpoch by remember { mutableIntStateOf(0) }
    var online by remember { mutableStateOf(NetworkStatus.isOnline(context)) }
    var showHintDialog by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionEpoch++
                online = NetworkStatus.isOnline(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val adManager: RewardedAdManager = remember(context.applicationContext) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            AdsEntryPoint::class.java,
        ).rewardedAdManager()
    }
    val adState by adManager.state.collectAsState()
    // A failed load is usually a lost connection; refresh the note instead of only saying "unavailable".
    LaunchedEffect(adState) {
        if (adState is RewardedAdState.Unavailable) {
            online = NetworkStatus.isOnline(context)
        }
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
    // A short tick confirms a correct move; a heavier buzz marks a lost life (SPEC 6).
    LaunchedEffect(state.moveStatus) {
        if (!settings.hapticsEnabled) return@LaunchedEffect
        when (state.moveStatus) {
            PuzzleMoveStatus.WRONG -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            PuzzleMoveStatus.CORRECT,
            PuzzleMoveStatus.SOLVED,
            -> haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            PuzzleMoveStatus.NONE -> Unit
        }
    }
    val hintAvailable = state.hintAvailability == HintAvailability.AVAILABLE
    // Premium hints need neither a network nor a loaded ad.
    val hintNeedsAd = state.hintsRequireAd
    val hintEnabled = !state.solved && hintAvailable &&
        (!hintNeedsAd || (online && adState is RewardedAdState.Ready))
    val hintNote = when {
        state.solved -> null
        state.hintAvailability == HintAvailability.DAILY_LIMIT -> R.string.hint_daily_limit
        state.hintAvailability == HintAvailability.LEVEL_REACHED -> R.string.hint_level_reached
        !hintNeedsAd -> null
        !online -> R.string.hint_offline
        adState is RewardedAdState.Loading -> R.string.hint_ad_loading
        adState is RewardedAdState.Unavailable -> R.string.hint_ad_unavailable
        else -> null
    }
    if (onBack != null) {
        BackHandler(onBack = onBack)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.puzzle_title)) },
                navigationIcon = {
                    if (onBack != null) {
                        TextButton(onClick = onBack) {
                            Text(stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onOpenStatistics) {
                        Text(stringResource(R.string.stats_open))
                    }
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

            state.stockEmpty || state.loadError -> MessageScreen(
                state = state,
                padding = padding,
                onRetry = viewModel::loadPuzzle,
                onOpenSettings = onOpenSettings,
            )
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    InfoChip(
                        text = stringResource(R.string.player_elo, state.playerElo),
                        modifier = Modifier.weight(1f),
                    )
                    InfoChip(
                        text = stringResource(R.string.puzzle_rating, state.puzzleRating),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    InfoChip(
                        text = stringResource(
                            if (state.playerIsWhite) R.string.side_white else R.string.side_black,
                        ),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.solving_time, formatClock(state.elapsedSeconds)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ChessBoard(
                        pieces = state.pieces,
                        legalMoves = state.legalMoves,
                        lastMove = state.lastMove,
                        playerIsWhite = state.playerIsWhite,
                        solved = state.solved,
                        onMove = viewModel::onMove,
                        pieceStyle = settings.pieceStyle,
                        hintSourceSquare = state.hintSourceSquare,
                        hintTargetSquare = state.hintTargetSquare,
                        resetKey = state.puzzleId,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                    )
                }
                if (state.hintLevel > 0 && !state.solved) {
                    Text(
                        text = stringResource(
                            if (state.hintTargetSquare != null) {
                                R.string.hint_level_target
                            } else {
                                R.string.hint_level_piece
                            },
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LivesIndicator(
                            livesLeft = state.livesLeft,
                            maxLives = state.maxLives,
                        )
                        Text(
                            text = stringResource(R.string.lives, state.livesLeft),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    InfoChip(
                        text = stringResource(R.string.reward_time, formatClock(state.rewardSeconds)),
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                state.eloDelta?.let { delta ->
                    Text(
                        text = stringResource(R.string.elo_change, String.format(Locale.getDefault(), "%+d", delta)),
                        color = if (delta < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
                MoveStatusText(state.moveStatus)
                AnimatedVisibility(
                    visible = state.solved,
                    enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.92f, animationSpec = tween(220)),
                    exit = fadeOut(tween(120)),
                ) {
                    SolvedCard(
                        solvingTime = formatClock(state.elapsedSeconds),
                        onNext = viewModel::loadPuzzle,
                    )
                }
                AnimatedVisibility(
                    visible = !state.solved,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(120)),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                enabled = hintEnabled,
                                onClick = {
                                    if (state.hintsRequireAd) {
                                        showHintDialog = true
                                    } else {
                                        viewModel.onHintWithoutAd()
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.hint_button))
                            }
                            Button(
                                enabled = state.canSkip,
                                onClick = viewModel::skipPuzzle,
                            ) {
                                Text(stringResource(R.string.skip_puzzle))
                            }
                        }
                        hintNote?.let { note ->
                            Text(
                                text = stringResource(note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (state.hintsRequireAd && online && hintAvailable &&
                            adState is RewardedAdState.Unavailable
                        ) {
                            TextButton(onClick = adManager::preload) {
                                Text(stringResource(R.string.hint_retry_ad))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showHintDialog) {
        AlertDialog(
            onDismissRequest = { showHintDialog = false },
            title = { Text(stringResource(R.string.hint_dialog_title)) },
            text = { Text(stringResource(R.string.hint_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showHintDialog = false
                        context.findActivity()?.let { activity ->
                            adManager.show(activity, onReward = viewModel::onAdRewardEarned)
                        }
                    },
                ) {
                    Text(stringResource(R.string.hint_watch_ad))
                }
            },
            dismissButton = {
                TextButton(onClick = { showHintDialog = false }) {
                    Text(stringResource(R.string.hint_cancel))
                }
            },
        )
    }
}

/** Compose may wrap the Activity in a ContextWrapper; AdMob needs the real Activity to show an ad. */
private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

/** Compact label pill for ELO, rating, side to move and the live time reward. */
@Composable
private fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** Filled hearts for the lives still available, outlined hearts for the ones already lost. */
@Composable
private fun LivesIndicator(livesLeft: Int, maxLives: Int) {
    val total = maxLives.coerceAtLeast(livesLeft).coerceAtLeast(1)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(total) { index ->
            val alive = index < livesLeft
            Icon(
                imageVector = if (alive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = if (alive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Celebratory card shown after the last correct move instead of the hint/skip actions. */
@Composable
private fun SolvedCard(solvingTime: String, onNext: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = stringResource(R.string.puzzle_solved),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(R.string.solving_time, solvingTime),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onNext, shape = MaterialTheme.shapes.large) {
                Text(stringResource(R.string.next_puzzle))
            }
        }
    }
}

@Composable
private fun MessageScreen(
    state: PuzzleUiState,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
    ) {
        Text(stringResource(if (state.stockEmpty) R.string.no_puzzles else R.string.puzzle_load_error))
        Spacer(Modifier.height(16.dp))
        if (state.stockEmpty) {
            Button(onClick = onOpenSettings) { Text(stringResource(R.string.no_puzzles_action)) }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        } else {
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}
