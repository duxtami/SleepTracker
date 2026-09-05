package com.sleeptracker.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.sleeptracker.app.ui.navigation.LocalBottomBarSpace
import com.sleeptracker.app.ui.theme.GoogleSansFlexAxes
import com.sleeptracker.app.util.TimeUtils
import kotlin.math.roundToInt

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

    val bottomBarSpace = LocalBottomBarSpace.current

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 12.dp, bottom = padding.calculateBottomPadding() + bottomBarSpace + 24.dp
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
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ColorStyle.entries.filter { it != ColorStyle.DYNAMIC }.forEach { style ->
                                val seedColor = when (style) {
                                    ColorStyle.TEAL -> androidx.compose.ui.graphics.Color(0xFF00696C)
                                    ColorStyle.SUNSET -> androidx.compose.ui.graphics.Color(0xFFA6420A)
                                    ColorStyle.FOREST -> androidx.compose.ui.graphics.Color(0xFF3C6C34)
                                    ColorStyle.ROSE -> androidx.compose.ui.graphics.Color(0xFFB01458)
                                    ColorStyle.OCEAN -> androidx.compose.ui.graphics.Color(0xFF005AC1)
                                    ColorStyle.AMBER -> androidx.compose.ui.graphics.Color(0xFF795900)
                                    else -> androidx.compose.ui.graphics.Color(0xFF5B5FC7) // LAVENDER
                                }
                                FilterChip(
                                    selected = settings.colorStyle == style,
                                    onClick = { viewModel.setColorStyle(style) },
                                    label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(seedColor)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    RowSwitch(
                        title = "Use Application Font",
                        subtitle = "Use custom typeface.",
                        checked = settings.useApplicationFont,
                        onCheckedChange = viewModel::setUseApplicationFont
                    )
                }
            }

            // ---------------- Google Sans Flex customization ----------------
            item {
                AnimatedVisibility(
                    visible = settings.useApplicationFont,
                    enter = fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) +
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)),
                    exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(200))
                ) {
                    GoogleSansFlexCustomizationCard(
                        weightAxis = settings.fontWeightAxis,
                        widthAxis = settings.fontWidthAxis,
                        roundnessAxis = settings.fontRoundnessAxis,
                        onWeightChange = viewModel::setFontWeightAxis,
                        onWidthChange = viewModel::setFontWidthAxis,
                        onRoundnessChange = viewModel::setFontRoundnessAxis,
                        onReset = {
                            viewModel.setFontWeightAxis(500f)
                            viewModel.setFontWidthAxis(100f)
                            viewModel.setFontRoundnessAxis(50f)
                        }
                    )
                }
            }

            // ---------------- Sleep Schedule ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Sleep Schedule")
                    Text(
                        "Your default schedule. Used to calculate your sleep goal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        TimeFieldButton(
                            label = "Bedtime",
                            hour = settings.scheduleBedtimeHour,
                            minute = settings.scheduleBedtimeMinute,
                            onChange = viewModel::setScheduleBedtime,
                            modifier = Modifier.weight(1f)
                        )
                        TimeFieldButton(
                            label = "Wake-up time",
                            hour = settings.scheduleWakeHour,
                            minute = settings.scheduleWakeMinute,
                            onChange = viewModel::setScheduleWakeTime,
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Sleep goal",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                TimeUtils.formatMinutesAsHoursMinutes(settings.sleepGoalMinutes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // ---------------- Tracking ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Tracking")
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsDropdownPicker(
                        label = "Start Time Delay",
                        selectedMinutes = settings.startDelayMinutes,
                        onSelect = viewModel::setStartDelayMinutes
                    )
                    Text(
                        "Wait before tracking begins.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    SettingsDropdownPicker(
                        label = "Smart Analyze Threshold",
                        selectedMinutes = settings.smartAnalyzeThresholdMinutes,
                        onSelect = viewModel::setSmartAnalyzeThresholdMinutes
                    )
                    Text(
                        "Ignore short wake-ups.",
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
                        subtitle = "Nudge before winding down.",
                        checked = settings.bedtimeReminderEnabled,
                        onCheckedChange = { enabled -> viewModel.setBedtimeReminderEnabled(context, enabled) }
                    )
                    if (settings.bedtimeReminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TimeFieldButton(
                            label = "Bedtime reminder time",
                            hour = settings.bedtimeReminderHour,
                            minute = settings.bedtimeReminderMinute,
                            onChange = { hour, minute -> viewModel.setBedtimeReminderTime(context, hour, minute) }
                        )
                    }
                }
            }

            // ---------------- Awake since wake-up ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Awake since wake-up")
                    Spacer(modifier = Modifier.height(8.dp))
                    RowSwitch(
                        title = "Show time awake on the Home screen.",
                        subtitle = "Home screen only.",
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
                        Image(
                            painter = painterResource(id = R.drawable.ic_splash_logo),
                            contentDescription = "SleepTracker logo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("SleepTracker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Version 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "SleepTracker is offline-first. Your data stays on your device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.large)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/duxtami/SleepTracker"))
                                runCatching { context.startActivity(intent) }
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Source Code",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Open GitHub repository",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---------------- Advanced ----------------
            item {
                ExpressiveCard {
                    SectionHeader(title = "Advanced")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Delete all sleep data permanently. Cannot be undone.",
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
        var backProgress by remember { mutableStateOf(0f) }
        androidx.activity.compose.PredictiveBackHandler { progress ->
            try {
                progress.collect { backProgress = it.progress }
                showClearDataConfirm = false
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
private fun SettingsDropdownPicker(label: String, selectedMinutes: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    Box {
        Column(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            DropdownMenuItem(
                text = { Text("Custom...") },
                onClick = { showCustomDialog = true; expanded = false }
            )
        }
    }

    if (showCustomDialog) {
        var customValue by remember { mutableStateOf(if (selectedMinutes == 0) "" else selectedMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom value") },
            text = {
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it.filter { char -> char.isDigit() } },
                    label = { Text("Minutes") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    customValue.toIntOrNull()?.let { onSelect(it) }
                    showCustomDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun GoogleSansFlexCustomizationCard(
    weightAxis: Float,
    widthAxis: Float,
    roundnessAxis: Float,
    onWeightChange: (Float) -> Unit,
    onWidthChange: (Float) -> Unit,
    onRoundnessChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "flexCardChevronRotation"
    )

    ExpressiveCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .clickable(onClickLabel = if (expanded) "Collapse" else "Expand") { expanded = !expanded }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    "Typography",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Adjust font weight, width, and roundness.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation }
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) +
                expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)),
            exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(200))
        ) {
            Column(modifier = Modifier.padding(top = 20.dp, start = 8.dp, end = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onReset) {
                        Text("Reset to default")
                    }
                }
                AxisSliderRow(
                    label = "Weight",
                    value = weightAxis,
                    range = GoogleSansFlexAxes.WEIGHT_RANGE,
                    valueLabel = weightAxis.roundToInt().toString(),
                    onValueChange = onWeightChange
                )
                Spacer(modifier = Modifier.height(20.dp))
                AxisSliderRow(
                    label = "Width",
                    value = widthAxis,
                    range = GoogleSansFlexAxes.WIDTH_RANGE,
                    valueLabel = "${widthAxis.roundToInt()}%",
                    onValueChange = onWidthChange
                )
                Spacer(modifier = Modifier.height(20.dp))
                AxisSliderRow(
                    label = "Roundness",
                    value = roundnessAxis,
                    range = GoogleSansFlexAxes.ROUNDNESS_RANGE,
                    valueLabel = "${roundnessAxis.roundToInt()}%",
                    onValueChange = onRoundnessChange
                )
            }
        }
    }
}

@Composable
private fun AxisSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valueLabel, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.semantics { contentDescription = "$label: $valueLabel" }
        )
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
