package com.chesslock.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import com.chesslock.ui.common.formatClock

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
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                MetricCard(
                    label = stringResource(R.string.home_elo_label),
                    value = state.elo.toString(),
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    label = stringResource(R.string.home_earned_today),
                    value = formatClock(state.earnedSeconds),
                    modifier = Modifier.weight(1f),
                )
            }
            Button(
                onClick = onSolve,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
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
    val container = if (state.hasTimeLeft) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (state.hasTimeLeft) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = container,
        contentColor = content,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.home_time_left),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = formatClock(state.remainingSeconds),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            LinearProgressIndicator(
                progress = { state.remainingFraction },
                color = content,
                trackColor = content.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
            )
            if (!state.hasTimeLeft) {
                Text(
                    text = stringResource(R.string.home_no_time),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            HorizontalDivider(
                color = content.copy(alpha = 0.2f),
                modifier = Modifier.padding(vertical = 4.dp),
            )
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
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
