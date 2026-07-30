package com.sleeptracker.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.R
import com.sleeptracker.app.data.datastore.START_DELAY_OPTIONS_MINUTES
import com.sleeptracker.app.data.model.ColorStyle
import com.sleeptracker.app.data.model.ThemeMode
import com.sleeptracker.app.ui.components.ExpressiveCard
import com.sleeptracker.app.ui.components.SectionHeader
import com.sleeptracker.app.ui.components.TimeFieldButton
import com.sleeptracker.app.util.TimeUtils

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.settings.collectAsState()
    val backupEvent by viewModel.backupEvent.collectAsState()
    val clearedAllData by viewModel.clearedAllData.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(backupEvent) {
        val event = backupEvent ?: return@LaunchedEffect
        val message = when (event) {
            is BackupEvent.Success -> event.message
            is BackupEvent.Failure -> event.message
        }
        snackbarHostState.showSnackbar(message)
        viewModel.clearBackupEvent()
    }

    LaunchedEffect(clearedAllData) {
        if (clearedAllData) {
            snackbarHostState.showSnackbar("All sleep data cleared")
            viewModel.clearClearedAllDataFlag()
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { viewModel.exportCsv(context, it) }
    }
    val exportJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportJson(context, it) }
    }
    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importCsv(context, it) }
    }
    val importJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importJson(context, it) }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { Text("Settings", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold) }

            // ---------------- Appearance ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Appearance")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Theme", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            val isSelected = settings.themeMode == mode
                            SegmentedButton(
                                selected = isSelected,
                                onClick = { viewModel.setThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size),
                                icon = {} // Disable the default animated icon slot - it grows/shrinks width and can push text out of the segment.
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    RowSwitch(
                        title = "Dynamic color",
                        subtitle = "Match your wallpaper's accent color - applies instantly",
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor
                    )
                    if (!settings.dynamicColor) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorStyle.entries.filter { it != ColorStyle.DYNAMIC }.forEach { style ->
                                FilterChip(
                                    selected = settings.colorStyle == style,
                                    onClick = { viewModel.setColorStyle(style) },
                                    label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Font scale: ${"%.1f".format(settings.fontScale)}x", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = settings.fontScale,
                        onValueChange = { viewModel.setFontScale(it) },
                        valueRange = 0.85f..1.3f
                    )
                }
            }

            // ---------------- Tracking ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Tracking")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Sleep goal: ${TimeUtils.formatMinutesAsHoursMinutes(settings.sleepGoalMinutes)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = settings.sleepGoalMinutes.toFloat(),
                        onValueChange = { viewModel.setSleepGoalMinutes(it.toInt()) },
                        valueRange = 240f..720f,
                        steps = 15
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StartDelayPicker(
                        selectedMinutes = settings.startDelayMinutes,
                        onSelect = viewModel::setStartDelayMinutes
                    )
                    Text(
                        "Most people don't fall asleep the instant they press Start. Choose a delay and tracking will automatically begin once it elapses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ---------------- Reminders ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Reminders")
                    Spacer(modifier = Modifier.height(16.dp))
                    RowSwitch(
                        title = "Bedtime reminder",
                        subtitle = "Get nudged when it's time to wind down",
                        checked = settings.bedtimeReminderEnabled,
                        onCheckedChange = viewModel::setBedtimeReminderEnabled
                    )
                    if (settings.bedtimeReminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeFieldButton(
                            label = "Bedtime reminder time",
                            hour = settings.bedtimeReminderHour,
                            minute = settings.bedtimeReminderMinute,
                            onChange = viewModel::setBedtimeReminderTime
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    RowSwitch(
                        title = "Wake-up reminder",
                        subtitle = "Get nudged when it's time to get up",
                        checked = settings.wakeReminderEnabled,
                        onCheckedChange = viewModel::setWakeReminderEnabled
                    )
                    if (settings.wakeReminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeFieldButton(
                            label = "Wake-up reminder time",
                            hour = settings.wakeReminderHour,
                            minute = settings.wakeReminderMinute,
                            onChange = viewModel::setWakeReminderTime
                        )
                    }
                }
            }

            // ---------------- Awake since wake-up ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Awake since wake-up")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Show the time elapsed since your last wake-up on the Home screen.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RowSwitch(
                        title = "Awake since wake-up",
                        subtitle = "Shown on the Home screen only",
                        checked = settings.showAwakeDuration,
                        onCheckedChange = viewModel::setShowAwakeDuration
                    )
                }
            }

            // ---------------- Backup ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Backup")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Export", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { exportCsvLauncher.launch("sleep_tracker_backup.csv") }, modifier = Modifier.weight(1f)) {
                            Text("Export CSV")
                        }
                        Button(onClick = { exportJsonLauncher.launch("sleep_tracker_backup.json") }, modifier = Modifier.weight(1f)) {
                            Text("Export JSON")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Import", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/*")) }, modifier = Modifier.weight(1f)) {
                            Text("Import CSV")
                        }
                        OutlinedButton(onClick = { importJsonLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.weight(1f)) {
                            Text("Import JSON")
                        }
                    }
                }
            }

            // ---------------- About ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "About")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(androidx.compose.ui.graphics.Color(0xFF3A4750)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "SleepTracker logo",
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("SleepTracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "SleepTracker is fully offline-first. Your sleep data never leaves this device unless you explicitly export it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---------------- Advanced ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Advanced")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Permanently delete every sleep session stored on this device. This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showClearDataConfirm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Clear all data")
                    }
                }
            }
        }
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear all sleep data?") },
            text = { Text("Every recorded sleep session will be permanently deleted. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDataConfirm = false
                    viewModel.clearAllData()
                }) { Text("Clear everything") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StartDelayPicker(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text("Start Time Delay", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (selectedMinutes == 0) "Off" else "$selectedMinutes minutes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            START_DELAY_OPTIONS_MINUTES.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(if (minutes == 0) "Off" else "$minutes minutes") },
                    onClick = { onSelect(minutes); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun RowSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
