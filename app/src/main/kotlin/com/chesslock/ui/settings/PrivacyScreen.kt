package com.chesslock.ui.settings

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chesslock.R

/**
 * Plain-language privacy explainer for the Settings screen (SPEC 5 and SPEC 7). ChessLock keeps
 * everything on the device; the only third party involved is AdMob for the rewarded hint ads.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.privacy_title)) },
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
            PrivacySection(
                title = R.string.privacy_local_title,
                body = R.string.privacy_local_body,
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            PrivacySection(
                title = R.string.privacy_usage_title,
                body = R.string.privacy_usage_body,
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            PrivacySection(
                title = R.string.privacy_ads_title,
                body = R.string.privacy_ads_body,
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text(
                text = stringResource(R.string.privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PrivacySection(@StringRes title: Int, @StringRes body: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(body),
        style = MaterialTheme.typography.bodyMedium,
    )
}
