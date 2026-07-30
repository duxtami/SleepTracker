package com.sleeptracker.app.ui.insights

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.ui.components.BarChart
import com.sleeptracker.app.ui.components.ExpressiveCard
import com.sleeptracker.app.ui.components.HeatmapGrid
import com.sleeptracker.app.ui.components.SectionHeader
import com.sleeptracker.app.ui.components.StatCard
import com.sleeptracker.app.util.TimeUtils

@Composable
fun InsightsScreen(viewModel: InsightsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("Insights", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightsRange.entries.forEach { range ->
                        FilterChip(
                            selected = state.range == range,
                            onClick = { viewModel.selectRange(range) },
                            label = { Text(range.label) }
                        )
                    }
                }
            }

            item {
                ExpressiveCard {
                    SectionHeader(title = "Sleep trend")
                    Spacer(modifier = Modifier.height(16.dp))
                    val values = state.dailyTotals.entries.sortedBy { it.key }.takeLast(14)
                        .map { it.value / 3_600_000f }
                    BarChart(
                        values = values,
                        goalFraction = if (values.isNotEmpty()) {
                            val maxVal = values.max().coerceAtLeast(0.01f)
                            (state.sleepGoalMinutes / 60f) / maxVal
                        } else null
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = "Average sleep",
                        value = TimeUtils.formatDurationShort(state.insights.averageDurationMillis),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Consistency",
                        value = "${state.insights.consistencyPercent}%",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = "Longest sleep",
                        value = TimeUtils.formatDurationShort(state.insights.longestDurationMillis),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Shortest sleep",
                        value = TimeUtils.formatDurationShort(state.insights.shortestDurationMillis),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = "Sleep streak",
                        value = "${state.insights.currentStreakDays} days",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Sleep debt",
                        value = TimeUtils.formatMinutesAsHoursMinutes(state.insights.sleepDebtMinutes),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard(
                        label = "Avg bedtime",
                        value = state.insights.averageBedtimeMinutesOfDay?.let { minutesToClock(it) } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Avg wake time",
                        value = state.insights.averageWakeMinutesOfDay?.let { minutesToClock(it) } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                ExpressiveCard {
                    SectionHeader(title = "Total hours")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = TimeUtils.formatDurationShort(state.insights.totalDurationMillis),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.insights.sessionCount} sessions tracked",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ExpressiveCard {
                    SectionHeader(title = "Last 12 weeks")
                    Spacer(modifier = Modifier.height(16.dp))
                    HeatmapGrid(
                        dailyTotalsMillis = state.dailyTotals,
                        goalMillis = state.sleepGoalMinutes * 60_000L
                    )
                }
            }
        }
    }
}

private fun minutesToClock(minutesOfDay: Int): String {
    val h24 = minutesOfDay / 60
    val m = minutesOfDay % 60
    val amPm = if (h24 < 12) "AM" else "PM"
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return String.format(java.util.Locale.getDefault(), "%d:%02d %s", h12, m, amPm)
}
