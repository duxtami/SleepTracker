package com.sleeptracker.app.ui.timeline

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Sleep details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                        modifier = Modifier.fillMaxWidth().height(10.dp),
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
                    Spacer(modifier = Modifier.height(12.dp))
                    NightTimelineBar(startEpochMillis = session.startEpochMillis, endEpochMillis = session.endEpochMillis ?: session.startEpochMillis)
                }
            }

            item {
                ExpressiveCard {
                    SectionHeader(title = "Mood & quality")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        session.mood?.let {
                            Text(it.emoji, style = MaterialTheme.typography.headlineSmall)
                            Text(it.label, style = MaterialTheme.typography.bodyMedium)
                        } ?: Text("No mood recorded", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                onDismiss = { showEditSheet = false },
                onSave = { start, end, mood, quality, notes, tags, delayUsed ->
                    viewModel.updateSession(start, end, mood, quality, notes, tags, delayUsed)
                    showEditSheet = false
                }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
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
private fun NightTimelineBar(startEpochMillis: Long, endEpochMillis: Long) {
    val zone = java.time.ZoneId.systemDefault()
    val startTime = java.time.Instant.ofEpochMilli(startEpochMillis).atZone(zone).toLocalTime()
    val endTime = java.time.Instant.ofEpochMilli(endEpochMillis).atZone(zone).toLocalTime()
    val startFraction = (startTime.toSecondOfDay().toFloat()) / 86400f
    val rawEndFraction = (endTime.toSecondOfDay().toFloat()) / 86400f
    val endFraction = (if (rawEndFraction < startFraction) rawEndFraction + 1f else rawEndFraction).coerceAtMost(1f)
    val durationFraction = (endFraction - startFraction).coerceIn(0.02f, 1f - startFraction)
    val trailingFraction = (1f - startFraction - durationFraction).coerceAtLeast(0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.small)
    ) {
        if (startFraction > 0f) {
            Box(modifier = Modifier.weight(startFraction))
        }
        Box(
            modifier = Modifier
                .weight(durationFraction)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
        )
        if (trailingFraction > 0f) {
            Box(modifier = Modifier.weight(trailingFraction))
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(TimeUtils.formatTime(startEpochMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(TimeUtils.formatTime(endEpochMillis), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
