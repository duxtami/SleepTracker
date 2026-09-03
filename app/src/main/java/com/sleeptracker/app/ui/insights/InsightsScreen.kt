package com.sleeptracker.app.ui.insights

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.ui.components.BarChart
import com.sleeptracker.app.ui.components.ExpressiveCard
import com.sleeptracker.app.ui.components.HeatmapGrid
import com.sleeptracker.app.ui.components.SectionHeader
import com.sleeptracker.app.ui.components.StatCard
import com.sleeptracker.app.ui.navigation.LocalBottomBarSpace
import com.sleeptracker.app.util.TimeUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: InsightsViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val bottomBarSpace = LocalBottomBarSpace.current

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + bottomBarSpace + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text("Insights", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                            label = { Text(range.label, fontWeight = if(state.range == range) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            if (!state.hasEnoughData) {
                item {
                    EmptyState()
                }
            } else {
                item {
                    AnimatedContent(
                        targetState = state,
                        transitionSpec = {
                            fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                                fadeOut(animationSpec = spring(stiffness = Spring.StiffnessLow))
                        }, label = "insights_content"
                    ) { targetState ->
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            HeroStat(targetState)
                            TrendsSection(targetState)
                            WeeklyRhythmSection(targetState)
                            SleepBreakdownSection(targetState)
                            ConsistencyScoreCard(targetState)
                            CalendarView(targetState)
                            RecordsSection(targetState)
                            RhythmChart(targetState)
                            HeatmapSection(targetState)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeroStat(state: InsightsUiState) {
    ExpressiveCard {
        SectionHeader(title = "Average sleep")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = TimeUtils.formatDurationShort(state.insights.averageDurationMillis),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Over ${state.insights.sessionCount} sessions",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TrendsSection(state: InsightsUiState) {
    ExpressiveCard {
        SectionHeader(title = "Trends")
        Spacer(modifier = Modifier.height(16.dp))
        
        val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        // Take at most 14 days for the chart
        val recentSessions = state.chronologicalData.takeLast(14)
        
        val chartData = recentSessions.map { session ->
            val date = session.startInstant.atZone(session.zone).toLocalDate()
            val dateStr = date.format(formatter)
            val hours = session.durationMillis / 3600000f
            com.sleeptracker.app.ui.components.BarData(
                value = hours,
                label = date.dayOfMonth.toString(), // Short label for x-axis
                tooltipText = dateStr + "\n" + TimeUtils.formatDurationShort(session.durationMillis)
            )
        }
        
        if (chartData.isNotEmpty()) {
            com.sleeptracker.app.ui.components.BarChart(
                data = chartData,
                yAxisLabelFormatter = { "${it.toInt()}h" }
            )
        } else {
            Text("Not enough data to show trends", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SleepBreakdownSection(state: InsightsUiState) {
    if (state.chronologicalData.isEmpty()) return
    
    val goodCount = state.chronologicalData.count { it.durationMillis >= 7 * 3600000L }
    val fairCount = state.chronologicalData.count { it.durationMillis in (5 * 3600000L)..(7 * 3600000L - 1) }
    val poorCount = state.chronologicalData.count { it.durationMillis < 5 * 3600000L }
    val total = (goodCount + fairCount + poorCount).coerceAtLeast(1)
    
    val goodPct = (goodCount.toFloat() / total * 100).toInt()
    val fairPct = (fairCount.toFloat() / total * 100).toInt()
    val poorPct = (poorCount.toFloat() / total * 100).toInt()

    val goodColor = MaterialTheme.colorScheme.primary
    val fairColor = MaterialTheme.colorScheme.tertiary
    val poorColor = MaterialTheme.colorScheme.error

    ExpressiveCard {
        SectionHeader(title = "Sleep Breakdown")
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(120.dp)) {
                    val strokeWidth = 24.dp.toPx()
                    var currentStartAngle = -90f
                    
                    val drawArcSegment = { count: Int, color: Color ->
                        if (count > 0) {
                            val sweepAngle = (count.toFloat() / total) * 360f
                            drawArc(
                                color = color,
                                startAngle = currentStartAngle,
                                sweepAngle = sweepAngle - 2f, // 2f gap
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                            )
                            currentStartAngle += sweepAngle
                        }
                    }
                    
                    drawArcSegment(goodCount, goodColor)
                    drawArcSegment(fairCount, fairColor)
                    drawArcSegment(poorCount, poorColor)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("nights", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.width(32.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                val drawLegendRow = @Composable { label: String, count: Int, pct: Int, color: Color ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                            Text("$pct% ($count nights)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (goodCount > 0) drawLegendRow("Good (7h+)", goodCount, goodPct, goodColor)
                if (fairCount > 0) drawLegendRow("Fair (5-7h)", fairCount, fairPct, fairColor)
                if (poorCount > 0) drawLegendRow("Poor (<5h)", poorCount, poorPct, poorColor)
            }
        }
    }
}

@Composable
fun WeeklyRhythmSection(state: InsightsUiState) {
    val selectedChartState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val selectedChart = selectedChartState.intValue
    val options = listOf("Duration", "Consistency", "Debt")

    ExpressiveCard {
        SectionHeader(title = "Weekly Rhythm")
        Spacer(modifier = Modifier.height(16.dp))
        
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = index == selectedChart,
                    onClick = { selectedChartState.intValue = index },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        val days = listOf("M", "T", "W", "T", "F", "S", "S")
        val chartData = (1..7).map { dayOfWeek ->
            val stat = state.weekdayStats[dayOfWeek] ?: WeekdayStats(0L, 100, 0)
            val value = when (selectedChart) {
                0 -> stat.durationMillis / 3_600_000f // Duration in hours
                1 -> stat.consistencyPercent.toFloat() // Consistency %
                else -> stat.debtMinutes.toFloat() / 60f // Debt in hours
            }
            com.sleeptracker.app.ui.components.BarData(
                value = value,
                label = days[dayOfWeek - 1]
            )
        }
        
        val maxIndex = chartData.indices.maxByOrNull { chartData[it].value } ?: 0
        val finalData = chartData.mapIndexed { index, data ->
            data.copy(isHighlight = index == maxIndex && data.value > 0)
        }

        val calloutText = if (finalData[maxIndex].value > 0) {
            val dayName = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")[maxIndex]
            val valueStr = when (selectedChart) {
                0 -> TimeUtils.formatDurationShort((finalData[maxIndex].value * 3_600_000f).toLong())
                1 -> "${finalData[maxIndex].value.toInt()}%"
                else -> "${finalData[maxIndex].value.toInt()}h ${(finalData[maxIndex].value % 1 * 60).toInt()}m"
            }
            val prefix = when (selectedChart) {
                0 -> "Best night:"
                1 -> "Most consistent:"
                else -> "Highest debt:"
            }
            "$prefix $dayName, $valueStr"
        } else null

        com.sleeptracker.app.ui.components.BarChart(
            data = finalData,
            calloutText = calloutText
        )
    }
}

@Composable
fun ConsistencyScoreCard(state: InsightsUiState) {
    ExpressiveCard {
        SectionHeader(title = "Consistency Score")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(
                    progress = { state.insights.consistencyPercent / 100f },
                    modifier = Modifier.size(120.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 12.dp,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${state.insights.consistencyPercent}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = "Avg Bedtime",
                value = state.insights.averageBedtimeMinutesOfDay?.let { minutesToClock(it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Avg Wake Time",
                value = state.insights.averageWakeMinutesOfDay?.let { minutesToClock(it) } ?: "—",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = "Current Streak",
                value = "${state.insights.currentStreakDays} days",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Total Sessions",
                value = "${state.insights.sessionCount}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CalendarView(state: InsightsUiState) {
    ExpressiveCard {
        SectionHeader(title = "Sleep History")
        Spacer(modifier = Modifier.height(16.dp))
        
        val today = LocalDate.now()
        // Show the last 12 months, ending on the current month.
        val pageCount = 12
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = pageCount - 1,
            pageCount = { pageCount }
        )
        
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val monthOffset = page - (pageCount - 1) // 0 for current month, negative for past
            val targetDate = today.plusMonths(monthOffset.toLong())
            val daysInMonth = targetDate.lengthOfMonth()
            val firstDayOfMonth = targetDate.withDayOfMonth(1)
            val startOffset = firstDayOfMonth.dayOfWeek.value % 7 // 0 for Sunday
            
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE
            
            Column {
                // Month Title
                Text(
                    text = "${targetDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${targetDate.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                        Text(text = it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                var day = 1
                while (day <= daysInMonth) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        for (col in 0..6) {
                            if ((day == 1 && col < startOffset) || day > daysInMonth) {
                                Spacer(modifier = Modifier.size(32.dp))
                            } else {
                                val date = firstDayOfMonth.withDayOfMonth(day)
                                val key = date.format(formatter)
                                val totalMillis = state.dailyTotals[key] ?: 0L
                                val fraction = (totalMillis.toFloat() / (state.sleepGoalMinutes * 60_000f)).coerceIn(0f, 1f)
                                
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (totalMillis > 0) heatColor(fraction) else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (totalMillis > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                day++
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun RecordsSection(state: InsightsUiState) {
    ExpressiveCard {
        SectionHeader(title = "Records")
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(
                    label = "Longest Sleep",
                    value = TimeUtils.formatDurationShort(state.insights.longestDurationMillis),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Shortest Sleep",
                    value = TimeUtils.formatDurationShort(state.insights.shortestDurationMillis),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RhythmChart(state: InsightsUiState) {
    ExpressiveCard {
        SectionHeader(title = "Hourly Sleep Rhythm")
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Typical sleeping hours based on bedtime and wake time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val startMins = state.insights.averageBedtimeMinutesOfDay ?: 0
        val endMins = state.insights.averageWakeMinutesOfDay ?: 0
        
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.Start
        ) {
            val totalMins = 24 * 60f
            if (startMins > endMins) { // goes across midnight
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(endMins / totalMins).background(MaterialTheme.colorScheme.primary))
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((startMins - endMins) / totalMins))
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth().background(MaterialTheme.colorScheme.primary))
            } else {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(startMins / totalMins))
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((endMins - startMins) / totalMins).background(MaterialTheme.colorScheme.primary))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("12 AM", style = MaterialTheme.typography.labelSmall)
            Text("12 PM", style = MaterialTheme.typography.labelSmall)
            Text("11 PM", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun HeatmapSection(state: InsightsUiState) {
    ExpressiveCard {
        SectionHeader(title = "Last 12 weeks")
        Spacer(modifier = Modifier.height(16.dp))
        HeatmapGrid(
            dailyTotalsMillis = state.dailyTotals,
            goalMillis = state.sleepGoalMinutes * 60_000L,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(0f, 0.3f, 0.6f, 1f).forEach { fraction ->
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (fraction == 0f) MaterialTheme.colorScheme.surfaceVariant else heatColor(fraction))
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text("Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.NightsStay,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Not enough data yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Track your sleep for a few days to unlock detailed insights and trends.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun heatColor(fraction: Float): Color {
    val base = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    return androidx.compose.ui.graphics.lerp(track, base, (0.15f + fraction * 0.85f).coerceIn(0f, 1f))
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
    return String.format(Locale.getDefault(), "%d:%02d %s", h12, m, amPm)
}
