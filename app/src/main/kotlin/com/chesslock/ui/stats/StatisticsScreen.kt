package com.chesslock.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chesslock.R
import com.chesslock.domain.stats.EloPoint
import com.chesslock.domain.stats.ThemeStat
import com.chesslock.ui.common.formatClock
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsState()
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stats_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val summary = uiState.summary
        if (uiState.isLoading || summary == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            StatCard(title = stringResource(R.string.stats_elo_title)) {
                Text(
                    text = stringResource(R.string.stats_elo_current, summary.currentElo),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                EloChart(
                    points = summary.eloHistory,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
                if (summary.ratedAttempts == 0) {
                    Text(
                        text = stringResource(R.string.stats_elo_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.stats_elo_window,
                            (summary.eloHistory.size - 1).coerceAtLeast(0),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            StatCard(title = stringResource(R.string.stats_success_title)) {
                val success = summary.successPercent
                if (success == null) {
                    Text(
                        text = stringResource(R.string.stats_success_none),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.stats_success_overall,
                            success,
                            summary.solvedAttempts,
                            summary.ratedAttempts,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (summary.themeStats.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.stats_themes_title),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        summary.themeStats.forEach { theme -> ThemeRow(theme) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = stringResource(R.string.stats_streak_title),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.stats_streak_days,
                            summary.currentStreakDays,
                            summary.currentStreakDays,
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(
                            R.string.stats_streak_best,
                            pluralStringResource(
                                R.plurals.stats_streak_days,
                                summary.bestStreakDays,
                                summary.bestStreakDays,
                            ),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatCard(
                    title = stringResource(R.string.stats_solved_title),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = uiState.solvedPuzzles.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            StatCard(title = stringResource(R.string.stats_reward_title)) {
                Text(
                    text = stringResource(
                        R.string.stats_reward_total,
                        formatClock(summary.totalRewardSeconds),
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            StatCard(title = stringResource(R.string.stats_usage_title)) {
                Text(
                    text = stringResource(
                        R.string.stats_usage_today,
                        formatClock(uiState.todayUsedSeconds),
                    ),
                )
                Text(
                    text = stringResource(
                        R.string.stats_usage_week,
                        formatClock(uiState.weekUsedSeconds),
                    ),
                )
            }

            Text(
                text = stringResource(R.string.stats_local_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun ThemeRow(theme: ThemeStat) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = themeLabel(theme.theme),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(
                R.string.stats_theme_value,
                theme.successPercent,
                theme.solved,
                theme.attempts,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Simple line chart of the latest rated attempts; the x axis is attempt order, not time. */
@Composable
private fun EloChart(points: List<EloPoint>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        if (points.isEmpty()) return@Canvas
        val inset = 10.dp.toPx()
        val usableWidth = (size.width - inset * 2).coerceAtLeast(1f)
        val usableHeight = (size.height - inset * 2).coerceAtLeast(1f)
        val values = points.map { it.elo }
        val min = values.minOrNull() ?: return@Canvas
        val max = values.maxOrNull() ?: return@Canvas
        val range = (max - min).coerceAtLeast(1)

        repeat(3) { index ->
            val y = inset + usableHeight * index / 2f
            drawLine(
                color = gridColor,
                start = Offset(inset, y),
                end = Offset(size.width - inset, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        fun x(index: Int): Float =
            if (points.size == 1) size.width / 2f
            else inset + usableWidth * index / (points.size - 1)

        fun y(value: Int): Float =
            inset + usableHeight * (1f - (value - min).toFloat() / range)

        val path = Path().apply {
            points.forEachIndexed { index, point ->
                val px = x(index)
                val py = y(point.elo)
                if (index == 0) moveTo(px, py) else lineTo(px, py)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        points.forEachIndexed { index, point ->
            drawCircle(
                color = dotColor,
                radius = 3.dp.toPx(),
                center = Offset(x(index), y(point.elo)),
            )
        }
    }
}

/** "mateIn2" reads better as "Mate in 2"; localized theme names are otherwise out of scope. */
@Composable
private fun themeLabel(tag: String): String {
    if (tag.startsWith("mateIn")) {
        return stringResource(R.string.stats_theme_mate_in, tag.removePrefix("mateIn"))
    }
    val spaced = tag.replace(Regex("([a-z])([A-Z])"), "$1 $2")
    return spaced.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

