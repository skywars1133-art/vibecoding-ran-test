package com.chesslock.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chesslock.R
import com.chesslock.blocking.BlockingPermissions
import com.chesslock.data.AppLanguage
import com.chesslock.data.AppSettings
import com.chesslock.data.PieceStyle
import com.chesslock.domain.blocking.BlockCategory
import com.chesslock.ui.common.PermissionCard
import com.chesslock.ui.puzzle.PieceStylePreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val protectionRunning by viewModel.protectionRunning.collectAsState()
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
    val notificationsGranted = remember(permissionEpoch) {
        BlockingPermissions.canPostNotifications(context)
    }
    val protectionActive = settings.blockingEnabled &&
        usageAccessGranted &&
        overlayGranted &&
        protectionRunning
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_language_section),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_language_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AppLanguage.entries.forEach { language ->
                val selected = settings.language == language
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { viewModel.setLanguage(language) },
                        )
                        .padding(vertical = 4.dp),
                ) {
                    RadioButton(selected = selected, onClick = null)
                    Text(
                        text = stringResource(language.labelRes()),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.settings_board_section),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.piece_style_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.piece_style_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PieceStyle.entries.forEach { style ->
                val selected = settings.pieceStyle == style
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    border = if (selected) {
                        null
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selected,
                            role = Role.RadioButton,
                            onClick = { viewModel.setPieceStyle(style) },
                        ),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        RadioButton(selected = selected, onClick = null)
                        Text(
                            text = stringResource(style.labelRes()),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        PieceStylePreview(style = style, fontSize = 20.sp)
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.piece_style_preview),
                style = MaterialTheme.typography.titleMedium,
            )
            PieceStylePreview(
                style = settings.pieceStyle,
                fontSize = 36.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            BlockingSettings(
                settings = settings,
                protectionActive = protectionActive,
                limitOptions = viewModel.dailyLimitOptions,
                capOptions = viewModel.dailyCapOptions,
                onEnabledChange = viewModel::setBlockingEnabled,
                onLimitChange = viewModel::setDailyLimitMinutes,
                onCapChange = viewModel::setDailyCapMinutes,
                onCategoryChange = viewModel::setCategoryBlocked,
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.settings_permissions_section),
                style = MaterialTheme.typography.titleMedium,
            )
            PermissionCard(
                title = R.string.settings_permission_usage_access,
                description = R.string.permissions_usage_description,
                granted = usageAccessGranted,
                actionLabel = R.string.settings_open_permission,
                onAction = {
                    openSettings(
                        context,
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                    )
                },
            )
            PermissionCard(
                title = R.string.settings_permission_overlay,
                description = R.string.permissions_overlay_description,
                granted = overlayGranted,
                actionLabel = R.string.settings_open_permission,
                onAction = {
                    openSettings(
                        context,
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
            )
            PermissionCard(
                title = R.string.permissions_notifications,
                description = R.string.permissions_notifications_description,
                granted = notificationsGranted,
                actionLabel = R.string.settings_open_permission,
                onAction = {
                    openSettings(
                        context,
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                    )
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlockingSettings(
    settings: AppSettings,
    protectionActive: Boolean,
    limitOptions: List<Int>,
    capOptions: List<Int>,
    onEnabledChange: (Boolean) -> Unit,
    onLimitChange: (Int) -> Unit,
    onCapChange: (Int?) -> Unit,
    onCategoryChange: (BlockCategory, Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_blocking_section),
        style = MaterialTheme.typography.titleMedium,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_blocking_enabled),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.settings_blocking_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = settings.blockingEnabled, onCheckedChange = onEnabledChange)
    }
    Text(
        text = stringResource(
            when {
                !settings.blockingEnabled -> R.string.protection_disabled
                protectionActive -> R.string.protection_active
                else -> R.string.protection_inactive
            },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = if (settings.blockingEnabled && protectionActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
    )

    if (!settings.blockingEnabled) return

    Text(
        text = stringResource(R.string.settings_daily_limit),
        style = MaterialTheme.typography.titleSmall,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(limitOptions) { minutes ->
            FilterChip(
                selected = settings.dailyLimitMinutes == minutes,
                onClick = { onLimitChange(minutes) },
                label = { Text(stringResource(R.string.settings_minutes, minutes)) },
            )
        }
    }

    Text(
        text = stringResource(R.string.settings_daily_cap),
        style = MaterialTheme.typography.titleSmall,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = settings.dailyCapMinutes == null,
                onClick = { onCapChange(null) },
                label = { Text(stringResource(R.string.settings_daily_cap_off)) },
            )
        }
        items(capOptions) { minutes ->
            FilterChip(
                selected = settings.dailyCapMinutes == minutes,
                onClick = { onCapChange(minutes) },
                label = { Text(stringResource(R.string.settings_minutes, minutes)) },
            )
        }
    }

    Text(
        text = stringResource(R.string.settings_blocked_categories),
        style = MaterialTheme.typography.titleSmall,
    )
    BlockCategory.entries.forEach { category ->
        val selected = category in settings.blockedCategories
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    role = Role.Checkbox,
                    onClick = { onCategoryChange(category, !selected) },
                )
                .padding(vertical = 4.dp),
        ) {
            Checkbox(checked = selected, onCheckedChange = null)
            Text(
                text = stringResource(category.labelRes()),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

private fun openSettings(context: Context, intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

@StringRes
private fun PieceStyle.labelRes(): Int = when (this) {
    PieceStyle.CLASSIC -> R.string.piece_style_classic
    PieceStyle.OUTLINE -> R.string.piece_style_outline
    PieceStyle.LETTERS -> R.string.piece_style_letters
    PieceStyle.CBURNETT -> R.string.piece_style_cburnett
    PieceStyle.CHESSNUT -> R.string.piece_style_chessnut
    PieceStyle.FANTASY -> R.string.piece_style_fantasy
    PieceStyle.CELTIC -> R.string.piece_style_celtic
    PieceStyle.SPATIAL -> R.string.piece_style_spatial
}

@StringRes
private fun AppLanguage.labelRes(): Int = when (this) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.ENGLISH -> R.string.settings_language_english
    AppLanguage.SLOVAK -> R.string.settings_language_slovak
}

@StringRes
private fun BlockCategory.labelRes(): Int = when (this) {
    BlockCategory.SOCIAL -> R.string.settings_category_social
    BlockCategory.GAMES -> R.string.settings_category_games
    BlockCategory.VIDEO -> R.string.settings_category_video
    BlockCategory.COMMUNICATION -> R.string.settings_category_communication
    BlockCategory.SHOPPING -> R.string.settings_category_shopping
    BlockCategory.OTHER -> R.string.settings_category_other
}
