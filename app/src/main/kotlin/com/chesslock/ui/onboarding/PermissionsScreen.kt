package com.chesslock.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.chesslock.R
import com.chesslock.blocking.BlockingPermissions
import com.chesslock.ui.common.PermissionCard

/**
 * First-run explanation and permission requests for blocking (SPEC 1). The user can continue
 * without granting everything; the puzzle screen then shows that protection is not active.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onDone: () -> Unit) {
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
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionEpoch++ }
    val allGranted = usageAccessGranted && overlayGranted && notificationsGranted

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.permissions_title)) })
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
                text = stringResource(R.string.permissions_body),
                style = MaterialTheme.typography.bodyLarge,
            )

            PermissionCard(
                title = R.string.settings_permission_usage_access,
                description = R.string.permissions_usage_description,
                granted = usageAccessGranted,
                actionLabel = R.string.settings_open_permission,
                onAction = { openSettings(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
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
                actionLabel = R.string.permissions_allow,
                onAction = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )

            StatusCard(allGranted)

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.permissions_continue))
            }
        }
    }
}

@Composable
private fun StatusCard(granted: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (granted) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        contentColor = if (granted) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(
                if (granted) R.string.permissions_active else R.string.permissions_inactive,
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
        )
    }
}

private fun openSettings(context: Context, intent: Intent) {
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
