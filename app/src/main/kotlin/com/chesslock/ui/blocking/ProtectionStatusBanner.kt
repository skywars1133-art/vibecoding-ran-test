package com.chesslock.ui.blocking

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chesslock.R

/**
 * Shown when blocking is intentionally off or when a required permission/service is missing, so
 * the user never believes they are protected while they are not (SPEC 1).
 */
@Composable
fun ProtectionStatusBanner(
    blockingEnabled: Boolean,
    active: Boolean,
    onOpenSettings: () -> Unit,
    onFixProtection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (active) return
    val message = if (blockingEnabled) R.string.protection_inactive else R.string.protection_disabled
    val action = if (blockingEnabled) R.string.protection_fix else R.string.settings_title
    val onClick = if (blockingEnabled) onFixProtection else onOpenSettings
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClick) {
                Text(stringResource(action))
            }
        }
    }
}
