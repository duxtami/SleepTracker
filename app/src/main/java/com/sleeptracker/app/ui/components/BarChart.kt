package com.sleeptracker.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class BarData(
    val value: Float,
    val label: String,
    val isHighlight: Boolean = false
)

@Composable
fun BarChart(
    data: List<BarData>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary,
    height: androidx.compose.ui.unit.Dp = 160.dp,
    calloutText: String? = null
) {
    if (data.isEmpty()) return

    val maxValue = (data.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(0.01f)
    
    // Animation for all bars to grow from the bottom
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            val barSpacing = 8.dp.toPx()
            val totalSpacing = barSpacing * (data.size - 1)
            val barWidth = (size.width - totalSpacing) / data.size

            data.forEachIndexed { index, item ->
                val targetBarHeight = (item.value / maxValue) * size.height
                val barHeight = targetBarHeight * animationProgress.value
                val left = index * (barWidth + barSpacing)
                
                // Draw background track
                drawRoundRect(
                    color = trackColor.copy(alpha = 0.5f),
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f)
                )

                // Draw actual bar
                val color = if (item.isHighlight) highlightColor else barColor
                drawRoundRect(
                    color = color,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Labels
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            data.forEach { item ->
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (item.isHighlight) highlightColor else labelColor,
                    fontWeight = if (item.isHighlight) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        if (calloutText != null) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = calloutText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
