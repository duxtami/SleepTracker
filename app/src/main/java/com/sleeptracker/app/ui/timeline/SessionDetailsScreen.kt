package com.sleeptracker.app.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.text.drawText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.ui.components.ExpressiveCard
import com.sleeptracker.app.ui.components.SectionHeader
import com.sleeptracker.app.ui.components.StatCard
import com.sleeptracker.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionDetailsScreen(viewModel: SessionDetailsViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var awakePeriods by remember { mutableStateOf<List<Pair<Long, Long>>>(emptyList()) }

    LaunchedEffect(state.session?.startEpochMillis, state.session?.endEpochMillis) {
        val session = state.session ?: return@LaunchedEffect
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val end = session.endEpochMillis ?: System.currentTimeMillis()
            val periods = com.sleeptracker.app.util.BedtimeDetector.findScreenOnPeriods(context, session.startEpochMillis, end)
            awakePeriods = periods
        }
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // TopAppBar's default containerColor is colorScheme.surfaceContainer, a distinct
            // tonal-elevation gray. Since the bar's own windowInsets extend it up behind the
            // (transparent) status bar, that default color is what was showing through as a
            // gray strip behind the clock/system icons - not a real status bar background.
            // Pinning it to the same background color as the rest of the screen makes the
            // status bar area blend seamlessly instead.
            TopAppBar(
                title = { Text("Sleep details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        val session = state.session
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("This entry no longer exists.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                ExpressiveCard {
                    SectionHeader(title = "Overview")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        StatCard(label = "Duration", value = TimeUtils.formatDurationShort(session.durationMillis), modifier = Modifier.weight(1f))
                        StatCard(label = "Start", value = TimeUtils.formatTime(session.startEpochMillis), modifier = Modifier.weight(1f))
                        StatCard(
                            label = "End",
                            value = session.endEpochMillis?.let { TimeUtils.formatTime(it) } ?: "—",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${TimeUtils.formatDate(session.startEpochMillis)} → ${session.endEpochMillis?.let { TimeUtils.formatDate(it) } ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ExpressiveCard {
                    SectionHeader(title = "Goal progress")
                    Spacer(modifier = Modifier.height(12.dp))
                    val goalMillis = state.settings.sleepGoalMinutes * 60_000L
                    val progress = if (goalMillis > 0) (session.durationMillis.toFloat() / goalMillis.toFloat()).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Goal: ${TimeUtils.formatMinutesAsHoursMinutes(state.settings.sleepGoalMinutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                ExpressiveCard {
                    SectionHeader(title = "Timeline")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your sleep states.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    NightTimelineBar(
                        startEpochMillis = session.startEpochMillis,
                        endEpochMillis = session.endEpochMillis ?: session.startEpochMillis,
                        awakePeriods = awakePeriods
                    )
                }
            }

            item {
                ExpressiveCard {
                    SectionHeader(title = "Mood & quality")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { showEditSheet = true })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        session.mood?.let {
                            Text(it.emoji, style = MaterialTheme.typography.headlineSmall)
                            Text(it.label, style = MaterialTheme.typography.bodyMedium)
                        } ?: Text("No mood recorded - tap to add", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    session.qualityRating?.let { quality ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            repeat(5) { i ->
                                Icon(
                                    if (i < quality) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            if (session.notes.isNotBlank() || session.tags.isNotEmpty()) {
                item {
                    ExpressiveCard {
                        SectionHeader(title = "Notes & tags")
                        if (session.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(session.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (session.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                session.tags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { showEditSheet = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Edit")
                    }
                    Button(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Delete")
                    }
                }
            }
        }

        if (showEditSheet) {
            SessionEditorSheet(
                initial = session,
                settings = state.settings,
                onDismiss = { showEditSheet = false },
                onSave = { start, end, mood, quality, notes, tags, delayUsed, totalPausedMillis ->
                    viewModel.updateSession(start, end, mood, quality, notes, tags, delayUsed, totalPausedMillis)
                    showEditSheet = false
                }
            )
        }

        if (showDeleteConfirm) {
            var backProgress by remember { mutableStateOf(0f) }
            androidx.activity.compose.PredictiveBackHandler { progress ->
                try {
                    progress.collect { backProgress = it.progress }
                    showDeleteConfirm = false
                } catch (e: java.util.concurrent.CancellationException) {
                    backProgress = 0f
                }
            }
            AlertDialog(
                modifier = Modifier.graphicsLayer {
                    val scale = 1f - (0.1f * backProgress)
                    scaleX = scale
                    scaleY = scale
                    alpha = 1f - (0.5f * backProgress)
                },
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete this sleep entry?") },
                text = { Text("This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSession()
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun NightTimelineBar(startEpochMillis: Long, endEpochMillis: Long, awakePeriods: List<Pair<Long, Long>>) {
    val durationMillis = (endEpochMillis - startEpochMillis).coerceAtLeast(1L)
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val barColor = MaterialTheme.colorScheme.primary

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(vertical = 8.dp)
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val yAxisWidth = 48.dp.toPx()
            val canvasWidth = size.width
            val canvasHeight = size.height - 24.dp.toPx()
            val chartLeft = yAxisWidth
            val chartWidth = canvasWidth - yAxisWidth
            
            val yAwake = canvasHeight * 0.15f
            val yAsleep = canvasHeight * 0.85f

            // Draw horizontal grid lines for the two states
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(chartLeft, yAwake),
                end = androidx.compose.ui.geometry.Offset(canvasWidth, yAwake),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            drawLine(
                color = gridColor,
                start = androidx.compose.ui.geometry.Offset(chartLeft, yAsleep),
                end = androidx.compose.ui.geometry.Offset(canvasWidth, yAsleep),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Y-axis labels
            val awakeLabel = textMeasurer.measure("Awake", style = labelStyle)
            val asleepLabel = textMeasurer.measure("Asleep", style = labelStyle)
            drawText(
                awakeLabel,
                topLeft = androidx.compose.ui.geometry.Offset(chartLeft - awakeLabel.size.width - 8.dp.toPx(), yAwake - awakeLabel.size.height / 2f)
            )
            drawText(
                asleepLabel,
                topLeft = androidx.compose.ui.geometry.Offset(chartLeft - asleepLabel.size.width - 8.dp.toPx(), yAsleep - asleepLabel.size.height / 2f)
            )

            val path = androidx.compose.ui.graphics.Path()
            val linePath = androidx.compose.ui.graphics.Path()

            path.moveTo(chartLeft, canvasHeight)
            path.lineTo(chartLeft, yAsleep)
            linePath.moveTo(chartLeft, yAsleep)
            
            val sortedAwake = awakePeriods.sortedBy { it.first }.filterNot { it.second >= endEpochMillis - 30000L }
            sortedAwake.forEach { (awakeStart, awakeEnd) ->
                val startOffset = ((awakeStart - startEpochMillis).toFloat() / durationMillis).coerceIn(0f, 1f)
                val endOffset = ((awakeEnd - startEpochMillis).toFloat() / durationMillis).coerceIn(0f, 1f)
                val startX = chartLeft + startOffset * chartWidth
                val endX = chartLeft + endOffset * chartWidth
                
                path.lineTo(startX, yAsleep)
                linePath.lineTo(startX, yAsleep)
                
                path.lineTo(startX, yAwake)
                linePath.lineTo(startX, yAwake)
                
                path.lineTo(endX, yAwake)
                linePath.lineTo(endX, yAwake)
                
                path.lineTo(endX, yAsleep)
                linePath.lineTo(endX, yAsleep)
            }
            path.lineTo(chartLeft + chartWidth, yAsleep)
            linePath.lineTo(chartLeft + chartWidth, yAsleep)
            
            path.lineTo(chartLeft + chartWidth, canvasHeight)
            path.close()

            drawPath(
                path = path,
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(barColor.copy(alpha = 0.3f), androidx.compose.ui.graphics.Color.Transparent),
                    startY = yAwake,
                    endY = canvasHeight
                )
            )

            drawPath(
                path = linePath,
                color = barColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            // X-axis labels
            val startText = TimeUtils.formatTime(startEpochMillis)
            val endText = TimeUtils.formatTime(endEpochMillis)
            
            val startLayout = textMeasurer.measure(startText, style = labelStyle)
            val endLayout = textMeasurer.measure(endText, style = labelStyle)
            
            drawText(
                textLayoutResult = startLayout,
                topLeft = androidx.compose.ui.geometry.Offset(chartLeft, canvasHeight + 8.dp.toPx())
            )
            drawText(
                textLayoutResult = endLayout,
                topLeft = androidx.compose.ui.geometry.Offset(canvasWidth - endLayout.size.width, canvasHeight + 8.dp.toPx())
            )
        }
    }
}
