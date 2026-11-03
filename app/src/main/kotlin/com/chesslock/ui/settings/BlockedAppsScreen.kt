package com.chesslock.ui.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chesslock.R
import com.chesslock.data.AppBlockOverride

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedAppsScreen(
    onBack: () -> Unit,
    viewModel: BlockedAppsViewModel = hiltViewModel(),
) {
    val entries by viewModel.entries.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val loading by viewModel.loading.collectAsState()
    var editing by remember { mutableStateOf<AppPickerEntry?>(null) }
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_apps_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = stringResource(R.string.settings_apps_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setSearchQuery,
                label = { Text(stringResource(R.string.settings_apps_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
            when {
                loading -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    CircularProgressIndicator()
                }

                entries.isEmpty() -> Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_apps_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    items(entries, key = { it.packageName }) { entry ->
                        AppRow(entry = entry, onClick = { editing = entry })
                    }
                }
            }
        }
    }

    editing?.let { entry ->
        AppOverrideDialog(
            entry = entry,
            options = AppBlockOverride.entries.map { option ->
                AppOverrideOption(
                    override = option,
                    label = stringResource(option.labelRes()),
                    description = stringResource(option.descriptionRes()),
                )
            },
            doneLabel = stringResource(R.string.settings_apps_done),
            onDismiss = { editing = null },
            onSelect = { override ->
                viewModel.setOverride(entry.packageName, override)
                editing = null
            },
        )
    }
}

@Composable
private fun AppRow(entry: AppPickerEntry, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = entry.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(entry.override.labelRes()),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun AppOverrideDialog(
    entry: AppPickerEntry,
    options: List<AppOverrideOption>,
    doneLabel: String,
    onDismiss: () -> Unit,
    onSelect: (AppBlockOverride) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                options.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = entry.override == option.override,
                                role = Role.RadioButton,
                                onClick = { onSelect(option.override) },
                            )
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = entry.override == option.override, onClick = null)
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(doneLabel)
            }
        },
    )
}

/** Dialog strings are resolved by the caller because a platform dialog uses the system resources. */
private data class AppOverrideOption(
    val override: AppBlockOverride,
    val label: String,
    val description: String,
)

@StringRes
private fun AppBlockOverride.labelRes(): Int = when (this) {
    AppBlockOverride.DEFAULT -> R.string.settings_apps_override_default
    AppBlockOverride.ALWAYS_BLOCK -> R.string.settings_apps_override_always
    AppBlockOverride.NEVER_BLOCK -> R.string.settings_apps_override_never
}

@StringRes
private fun AppBlockOverride.descriptionRes(): Int = when (this) {
    AppBlockOverride.DEFAULT -> R.string.settings_apps_override_default_description
    AppBlockOverride.ALWAYS_BLOCK -> R.string.settings_apps_override_always_description
    AppBlockOverride.NEVER_BLOCK -> R.string.settings_apps_override_never_description
}
