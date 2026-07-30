package com.sleeptracker.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Renders the last [weeks] weeks of sleep duration as a contribution-style heatmap.
 * [dailyTotalsMillis] keys are ISO local dates (yyyy-MM-dd), values are total sleep millis that day.
 */
@Composable
fun HeatmapGrid(
    dailyTotalsMillis: Map<String, Long>,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
    goalMillis: Long = 8L * 60 * 60 * 1000
) {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    val days = (weeks * 7 - 1) downTo 0
    val columns = weeks

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (col in 0 until columns) {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                for (row in 0 until 7) {
                    val dayIndex = col * 7 + row
                    val date = today.minusDays((weeks * 7 - 1 - dayIndex).toLong())
                    val key = date.format(formatter)
                    val total = dailyTotalsMillis[key] ?: 0L
                    val fraction = (total.toFloat() / goalMillis.toFloat()).coerceIn(0f, 1f)
                    val color = heatColor(fraction)
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(color)
                    )
                }
            }
        }
    }
}

@Composable
private fun heatColor(fraction: Float): androidx.compose.ui.graphics.Color {
    val base = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    return androidx.compose.ui.graphics.lerp(track, base, (0.15f + fraction * 0.85f).coerceIn(0f, 1f))
}
