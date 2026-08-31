package com.sleeptracker.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.sleeptracker.app.util.TimeUtils
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** A Material 3 DatePickerDialog wrapping the standard DatePicker, working in UTC-normalized epoch millis. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3DatePickerDialog(
    initialEpochMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (epochDayMillisUtc: Long) -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialEpochMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let(onConfirm)
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = state)
    }
}

/** A Material 3 TimePicker wrapped in a Dialog, since Material3 doesn't ship a TimePickerDialog itself. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    // Respect the device's actual 12h/24h clock preference (Settings > System > Date & time)
    // rather than assuming one - this is what every other clock/alarm surface on the device does.
    val context = LocalContext.current
    val is24Hour = remember { android.text.format.DateFormat.is24HourFormat(context) }
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = is24Hour)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = state)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        onConfirm(state.hour, state.minute)
                        onDismiss()
                    }) { Text("OK") }
                }
            }
        }
    }
}

/**
 * A labeled row showing a date button and a time button side by side, backed by a single
 * epoch-millis value in the given [zoneId]. Tapping either opens the matching Material3 picker
 * and recombines the result into a new epoch-millis value via [onChange].
 */
@Composable
fun DateTimeFieldRow(
    label: String,
    epochMillis: Long,
    zoneId: ZoneId,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showTimePicker by remember { androidx.compose.runtime.mutableStateOf(false) }

    val zoned = Instant.ofEpochMilli(epochMillis).atZone(zoneId)

    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                Text(TimeUtils.formatDate(epochMillis, zoneId.id))
            }
            OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                Text(TimeUtils.formatTime(epochMillis, zoneId.id))
            }
        }
    }

    if (showDatePicker) {
        // DatePicker works in UTC epoch-day millis; convert the picked UTC date onto our local time-of-day.
        val initialUtcMillis = zoned.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        M3DatePickerDialog(
            initialEpochMillis = initialUtcMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = { pickedUtcMillis ->
                val pickedDate = Instant.ofEpochMilli(pickedUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val newZoned = pickedDate.atTime(zoned.toLocalTime()).atZone(zoneId)
                onChange(newZoned.toInstant().toEpochMilli())
            }
        )
    }

    if (showTimePicker) {
        M3TimePickerDialog(
            initialHour = zoned.hour,
            initialMinute = zoned.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                val newZoned = zoned.toLocalDate().atTime(LocalTime.of(hour, minute)).atZone(zoneId)
                onChange(newZoned.toInstant().toEpochMilli())
            }
        )
    }
}

/** A simple labeled button that opens a time-only picker - used for reminder times in Settings. */
@Composable
fun TimeFieldButton(
    label: String,
    hour: Int,
    minute: Int,
    onChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start
) {
    var showTimePicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedButton(onClick = { showTimePicker = true }) {
            Text(String.format("%02d:%02d", hour, minute))
        }
    }

    if (showTimePicker) {
        M3TimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = onChange
        )
    }
}
