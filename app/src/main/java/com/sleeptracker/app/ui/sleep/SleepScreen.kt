package com.sleeptracker.app.ui.sleep

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.data.model.Mood
import com.sleeptracker.app.ui.components.ExpressiveCard
import com.sleeptracker.app.ui.components.SectionHeader
import com.sleeptracker.app.ui.components.SleepOrb
import com.sleeptracker.app.ui.components.StatCard
import com.sleeptracker.app.ui.navigation.LocalBottomBarSpace
import com.sleeptracker.app.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(viewModel: SleepViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEndSheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* proceed regardless */ }

    fun requestStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.startSession()
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val bottomBarSpace = LocalBottomBarSpace.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + bottomBarSpace + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                val phase = state.phase
                Column {
                    Text(
                        text = state.greeting,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (phase) {
                            is TrackingPhase.Tracking -> if (phase.session.isPaused) "Paused" else "Sleeping since ${TimeUtils.formatTime(phase.session.startEpochMillis)}"
                            is TrackingPhase.Waiting -> "Sleep tracking begins at ${TimeUtils.formatTime(phase.plannedStartEpochMillis)}"
                            TrackingPhase.Idle -> "Ready when you are"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                val phase = state.phase
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (phase is TrackingPhase.Waiting) {
                            WaitingCountdown(remainingMillis = phase.remainingMillis, onCancel = viewModel::cancelPendingDelay)
                        } else {
                            SleepOrb(
                                isActive = phase is TrackingPhase.Tracking,
                                onClick = {
                                    when (phase) {
                                        is TrackingPhase.Tracking -> showEndSheet = true
                                        else -> requestStart()
                                    }
                                }
                            )
                            if (phase is TrackingPhase.Tracking) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = TimeUtils.formatDurationClock(phase.session.durationMillis),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            val trackingPhase = state.phase as? TrackingPhase.Tracking
            if (trackingPhase != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showEndSheet = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("End Sleep")
                        }
                        OutlinedButton(
                            onClick = { if (trackingPhase.session.isPaused) viewModel.resumeSession() else viewModel.pauseSession() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (trackingPhase.session.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(if (trackingPhase.session.isPaused) "Resume" else "Pause")
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { showNoteSheet = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Add Note")
                    }
                }
            }

            item {
                ExpressiveCard {
                    SectionHeader(title = "Sleep goal")
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { state.goalProgress },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Goal: ${TimeUtils.formatMinutesAsHoursMinutes(state.settings.sleepGoalMinutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            state.lastCompletedSession?.let { last ->
                item {
                    ExpressiveCard {
                        SectionHeader(title = "Last sleep")
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.settings.showAwakeDuration) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard(label = "Duration", value = TimeUtils.formatDurationShort(last.durationMillis), modifier = Modifier.weight(1f))
                                StatCard(label = "Bedtime", value = TimeUtils.formatTime(last.startEpochMillis), modifier = Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard(
                                    label = "Wake up",
                                    value = last.endEpochMillis?.let { TimeUtils.formatTime(it) } ?: "—",
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Awake for",
                                    value = state.awakeSinceWakeMillis?.let { TimeUtils.formatDurationShort(it) } ?: "—",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StatCard(label = "Duration", value = TimeUtils.formatDurationShort(last.durationMillis), modifier = Modifier.weight(1f))
                                StatCard(label = "Bedtime", value = TimeUtils.formatTime(last.startEpochMillis), modifier = Modifier.weight(1f))
                                StatCard(
                                    label = "Wake up",
                                    value = last.endEpochMillis?.let { TimeUtils.formatTime(it) } ?: "—",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEndSheet) {
        EndSessionSheet(
            onDismiss = { showEndSheet = false },
            onConfirm = { mood, notes, quality ->
                viewModel.endSession(mood, notes, quality)
                showEndSheet = false
            }
        )
    }

    if (showNoteSheet) {
        AddNoteSheet(
            onDismiss = { showNoteSheet = false },
            onConfirm = { text ->
                viewModel.addNoteToActiveSession(text)
                showNoteSheet = false
            }
        )
    }
}

@Composable
private fun WaitingCountdown(remainingMillis: Long, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .padding(horizontal = 32.dp, vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = TimeUtils.formatDurationClock(remainingMillis).removePrefix("00:"),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "until sleep tracking begins",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onCancel) {
            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Cancel")
        }
    }
}

@Composable
private fun QualityStars(quality: Int?, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            val filled = (quality ?: 0) >= i
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Quality $i",
                tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onChange(i) }
                    .padding(4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndSessionSheet(onDismiss: () -> Unit, onConfirm: (Mood?, String?, Int?) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var quality by remember { mutableStateOf<Int?>(null) }
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 24.dp)) {
            Text("How did you sleep?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Mood.entries.forEach { mood ->
                    val selected = mood == selectedMood
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { selectedMood = mood }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = mood.emoji, style = MaterialTheme.typography.headlineSmall)
                        Text(text = mood.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Sleep quality", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            QualityStars(quality = quality, onChange = { quality = it })

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { onConfirm(selectedMood, notes.ifBlank { null }, quality) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("End Sleep Session")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNoteSheet(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 24.dp)) {
            Text("Add a note", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Note")
            }
        }
    }
}
