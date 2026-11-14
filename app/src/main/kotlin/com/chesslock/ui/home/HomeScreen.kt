package com.chesslock.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.chesslock.domain.budget.BudgetUrgency
import com.chesslock.ui.blocking.ProtectionStatusBanner
import com.chesslock.ui.common.UrgencyPalette
import com.chesslock.ui.common.formatClock
import com.chesslock.ui.common.urgencyPalette
import kotlin.math.roundToInt

/**
 * Root screen: shows how much blocked-app time is left today and the one action that matters,
 * solving a puzzle (SPEC 1). The budget is read from [HomeViewModel], so it stays live while the
 * blocking service counts usage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSolve: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStatistics: () -> Unit,
    onFixProtection: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
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
    val protectionActive = state.blockingEnabled &&
        usageAccessGranted &&
        overlayGranted &&
        state.protectionRunning

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            ProtectionStatusBanner(
                blockingEnabled = state.blockingEnabled,
                active = protectionActive,
                onOpenSettings = onOpenSettings,
                onFixProtection = onFixProtection,
            )
            RemainingTimeCard(state)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                MetricTile(
                    icon = Icons.Filled.Star,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    label = stringResource(R.string.home_elo_label),
                    value = state.elo.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricTile(
                    icon = Icons.Filled.Check,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = stringResource(R.string.home_earned_today),
                    value = formatClock(state.earnedSeconds),
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = onSolve,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_solve),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                text = stringResource(R.string.home_local_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun RemainingTimeCard(state: HomeUiState) {
    val palette = urgencyPalette(state.urgency)
    val background = Brush.linearGradient(
        colors = listOf(
            palette.container,
            lerp(palette.container, palette.accent, 0.22f),
        ),
    )
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        contentColor = palette.content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(background)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BudgetRing(
                    progress = state.remainingFraction,
                    contentColor = palette.content,
                    accentColor = palette.accent,
                    timeText = formatClock(state.remainingSeconds),
                    modifier = Modifier.size(BudgetRingSize),
                )
                Spacer(Modifier.width(20.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.home_time_left),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    UrgencyChip(urgency = state.urgency, palette = palette)
                    Text(
                        text = if (state.hasTimeLeft) {
                            stringResource(
                                R.string.home_remaining_share,
                                (state.remainingFraction * 100f).roundToInt(),
                            )
                        } else {
                            stringResource(R.string.home_no_time)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            HorizontalDivider(color = palette.content.copy(alpha = 0.2f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                BreakdownColumn(
                    label = stringResource(R.string.home_daily_limit),
                    seconds = state.limitSeconds,
                    modifier = Modifier.weight(1f),
                )
                BreakdownColumn(
                    label = stringResource(R.string.home_earned_today),
                    seconds = state.earnedSeconds,
                    modifier = Modifier.weight(1f),
                )
                BreakdownColumn(
                    label = stringResource(R.string.home_used_today),
                    seconds = state.usedSeconds,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Small status pill: a coloured dot plus "Plenty of time" / "Running low" / ... */
@Composable
private fun UrgencyChip(urgency: BudgetUrgency, palette: UrgencyPalette) {
    Surface(
        shape = CircleShape,
        color = palette.accent.copy(alpha = 0.16f),
        contentColor = palette.content,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Box(Modifier.size(8.dp).background(palette.accent, CircleShape))
            Text(
                text = stringResource(urgencyLabel(urgency)),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@StringRes
private fun urgencyLabel(urgency: BudgetUrgency): Int = when (urgency) {
    BudgetUrgency.COMFORTABLE -> R.string.home_urgency_comfortable
    BudgetUrgency.WARNING -> R.string.home_urgency_warning
    BudgetUrgency.CRITICAL -> R.string.home_urgency_critical
    BudgetUrgency.EMPTY -> R.string.home_urgency_empty
}

@Composable
private fun BreakdownColumn(label: String, seconds: Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = formatClock(seconds),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MetricTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.14f),
                contentColor = iconTint,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp),
                )
            }
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
