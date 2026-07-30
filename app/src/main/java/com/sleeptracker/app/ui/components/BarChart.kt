package com.sleeptracker.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

/**
 * Minimal, dependency-free bar chart: each entry in [values] (already sorted chronologically)
 * is rendered as a rounded bar scaled against the maximum value. [goalFraction] draws a
 * horizontal target line (e.g. the sleep goal) when between 0 and 1.
 */
@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    goalFraction: Float? = null,
    height: androidx.compose.ui.unit.Dp = 140.dp
) {
    val goalColor = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        if (values.isEmpty()) return@Canvas
        val maxValue = (values.maxOrNull() ?: 1f).coerceAtLeast(0.01f)
        val barSpacing = 6.dp.toPx()
        val barWidth = (size.width - barSpacing * (values.size - 1)) / values.size

        values.forEachIndexed { index, value ->
            val barHeight = (value / maxValue) * size.height
            val left = index * (barWidth + barSpacing)
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
                style = Fill
            )
        }

        if (goalFraction != null && goalFraction in 0f..1f) {
            val y = size.height * (1f - goalFraction)
            drawLine(
                color = goalColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
            )
        }
    }
}
