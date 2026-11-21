package com.chesslock.ui.leaderboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chesslock.R
import com.chesslock.domain.leaderboard.LeaderboardSnapshot
import com.chesslock.domain.leaderboard.RankedEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.leaderboard_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::refresh) {
                        Text(stringResource(R.string.leaderboard_refresh))
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

            state.error != null -> Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.leaderboard_error),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = viewModel::refresh) {
                    Text(stringResource(R.string.retry))
                }
            }

            else -> state.snapshot?.let { snapshot ->
                LeaderboardContent(
                    snapshot = snapshot,
                    isLocalPreview = state.isLocalPreview,
                    modifier = Modifier.padding(padding).padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun LeaderboardContent(
    snapshot: LeaderboardSnapshot,
    isLocalPreview: Boolean,
    modifier: Modifier = Modifier,
) {
    val myRankText = snapshot.myRank?.rank
        ?.let { stringResource(R.string.leaderboard_rank, it) }
        ?: stringResource(R.string.leaderboard_unranked)

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize(),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.leaderboard_my_rank, myRankText),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                snapshot.myRank?.entry?.let { mine ->
                    Text(
                        text = stringResource(
                            R.string.leaderboard_my_entry,
                            mine.nickname,
                            mine.elo,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.leaderboard_entries, snapshot.totalPlayers),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isLocalPreview) {
            Text(
                text = stringResource(R.string.leaderboard_local_preview),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.leaderboard_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            items(snapshot.top, key = { it.entry.playerId }) { ranked ->
                RankRow(ranked = ranked)
            }
            if (snapshot.top.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.leaderboard_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun RankRow(ranked: RankedEntry) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = stringResource(R.string.leaderboard_rank, ranked.rank),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(56.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ranked.entry.nickname,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(R.string.leaderboard_rated, ranked.entry.ratedPuzzlesPlayed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = ranked.entry.elo.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
