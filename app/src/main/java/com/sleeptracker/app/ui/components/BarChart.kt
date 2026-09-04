package com.sleeptracker.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import kotlin.math.roundToInt

enum class ChartStyle {
    Bar, Line, Area
}

data class BarData(
    val value: Float,
    val label: String,
    val tooltipText: String? = null,
    val isHighlight: Boolean = false,
    val isToday: Boolean = false,
    val isEmpty: Boolean = false
)

@Composable
fun BarChart(
    data: List<BarData>,
    modifier: Modifier = Modifier,
    chartStyle: ChartStyle = ChartStyle.Bar,
    barColor: Color = MaterialTheme.colorScheme.primary,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary,
    height: androidx.compose.ui.unit.Dp = 160.dp,
    calloutText: String? = null,
    yAxisLabelFormatter: (Float) -> String = { it.toInt().toString() }
) {
    if (data.isEmpty()) return

    val maxValue = (data.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(0.01f)
    
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
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
    val tooltipStyle = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.inverseOnSurface, fontWeight = FontWeight.Bold)
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    var pressOffset by remember { mutableStateOf(Offset.Zero) }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(data) {
                    detectDragGestures(
                        onDragStart = { offset -> pressOffset = offset },
                        onDragEnd = { pressedIndex = null },
                        onDragCancel = { pressedIndex = null },
                        onDrag = { change, _ ->
                            pressOffset = change.position
                        }
                    )
                }
                .pointerInput(data) {
                    detectTapGestures(
                        onPress = { offset ->
                            pressOffset = offset
                            tryAwaitRelease()
                            pressedIndex = null
                        }
                    )
                }
        ) {
            val yAxisWidth = 32.dp.toPx()
            val canvasWidth = size.width - yAxisWidth
            val canvasHeight = size.height - 24.dp.toPx() // Reserve space for X-axis labels

            // Gridlines & Y-axis labels
            val steps = 3
            for (i in 0..steps) {
                val y = canvasHeight - (i.toFloat() / steps) * canvasHeight
                val valueAtGrid = (i.toFloat() / steps) * maxValue
                
                drawLine(
                    color = gridColor,
                    start = Offset(yAxisWidth, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                
                val yLabel = yAxisLabelFormatter(valueAtGrid)
                val textLayoutResult = textMeasurer.measure(yLabel, style = labelStyle)
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(yAxisWidth - textLayoutResult.size.width - 8.dp.toPx(), y - textLayoutResult.size.height / 2f)
                )
            }

            val barSpacing = 8.dp.toPx()
            val totalSpacing = barSpacing * (data.size - 1)
            val barWidth = (canvasWidth - totalSpacing) / data.size

            var newPressedIndex: Int? = null

            if (chartStyle == ChartStyle.Line || chartStyle == ChartStyle.Area) {
                if (data.size > 1) {
                    val xPositions = data.mapIndexed { index, _ -> yAxisWidth + index * (barWidth + barSpacing) + barWidth / 2f }
                    val yPositions = data.map { item -> canvasHeight - ((item.value / maxValue) * canvasHeight) * animationProgress.value }
                    
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(xPositions.first(), yPositions.first())
                        for (i in 1 until data.size) {
                            val cpX1 = (xPositions[i - 1] + xPositions[i]) / 2f
                            cubicTo(
                                cpX1, yPositions[i - 1],
                                cpX1, yPositions[i],
                                xPositions[i], yPositions[i]
                            )
                        }
                    }
                    if (chartStyle == ChartStyle.Area) {
                        val areaPath = androidx.compose.ui.graphics.Path().apply {
                            addPath(path)
                            lineTo(xPositions.last(), canvasHeight)
                            lineTo(xPositions.first(), canvasHeight)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(barColor.copy(alpha = 0.5f), Color.Transparent),
                                startY = 0f,
                                endY = canvasHeight
                            )
                        )
                    }
                    drawPath(
                        path = path,
                        color = barColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                    
                    data.forEachIndexed { index, item ->
                        if (item.isHighlight || item.isToday || item.tooltipText != null) {
                            drawCircle(
                                color = if (item.isHighlight) highlightColor else barColor,
                                radius = 4.dp.toPx(),
                                center = Offset(xPositions[index], yPositions[index])
                            )
                        }
                    }
                }
            }

            data.forEachIndexed { index, item ->
                val targetBarHeight = (item.value / maxValue) * canvasHeight
                val barHeight = targetBarHeight * animationProgress.value
                val left = yAxisWidth + index * (barWidth + barSpacing)
                
                if (chartStyle == ChartStyle.Bar) {
                    // Draw background track
                    if (!item.isEmpty) {
                        drawRoundRect(
                            color = trackColor.copy(alpha = 0.5f),
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, canvasHeight),
                            cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f)
                        )
                    }

                    if (item.isEmpty) {
                        drawRoundRect(
                            color = gridColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, canvasHeight),
                            cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        )
                    } else {
                        // Draw actual bar
                        val color = if (item.isHighlight) highlightColor else barColor
                        val barTop = canvasHeight - barHeight
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f)
                        )
                    }
                    
                    // Draw 'Today' outline
                    if (item.isToday) {
                        val hlTop = if (item.isEmpty) 0f else canvasHeight - barHeight
                        val hlHeight = if (item.isEmpty) canvasHeight else barHeight
                        drawRoundRect(
                            color = onSurfaceColor,
                            topLeft = Offset(left - 2.dp.toPx(), hlTop - 2.dp.toPx()),
                            size = Size(barWidth + 4.dp.toPx(), hlHeight + 4.dp.toPx()),
                            cornerRadius = CornerRadius((barWidth + 4.dp.toPx()) / 2.5f, (barWidth + 4.dp.toPx()) / 2.5f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // X-axis label
                val xLabelColor = if (item.isToday) onSurfaceColor else if (item.isHighlight) highlightColor else labelColor
                val xLabelWeight = if (item.isToday || item.isHighlight) FontWeight.Bold else FontWeight.Normal
                val xLabelResult = textMeasurer.measure(item.label, style = labelStyle.copy(fontWeight = xLabelWeight, color = xLabelColor))
                drawText(
                    textLayoutResult = xLabelResult,
                    topLeft = Offset(left + (barWidth - xLabelResult.size.width) / 2f, canvasHeight + 8.dp.toPx())
                )

                if (pressOffset.x >= left && pressOffset.x <= left + barWidth) {
                    newPressedIndex = index
                }
            }

            pressedIndex = newPressedIndex

            // Draw Tooltip
            pressedIndex?.let { idx ->
                val item = data[idx]
                if (item.tooltipText != null) {
                    val tooltipLayout = textMeasurer.measure(item.tooltipText, style = tooltipStyle)
                    val tooltipPadding = 8.dp.toPx()
                    val tooltipWidth = tooltipLayout.size.width + tooltipPadding * 2
                    val tooltipHeight = tooltipLayout.size.height + tooltipPadding * 2
                    
                    val left = yAxisWidth + idx * (barWidth + barSpacing)
                    val barCenter = left + barWidth / 2f
                    var tooltipX = barCenter - tooltipWidth / 2f
                    tooltipX = tooltipX.coerceIn(0f, size.width - tooltipWidth)
                    val barTop = canvasHeight - (item.value / maxValue) * canvasHeight
                    var tooltipY = barTop - tooltipHeight - 8.dp.toPx()
                    if (tooltipY < 0) tooltipY = barTop + 8.dp.toPx()

                    drawRoundRect(
                        color = Color(0xFF1E1E1E), // InverseSurface-like
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipWidth, tooltipHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawText(
                        textLayoutResult = tooltipLayout,
                        topLeft = Offset(tooltipX + tooltipPadding, tooltipY + tooltipPadding)
                    )
                }
            }
        }
        
        if (calloutText != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Star, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 8.dp).size(16.dp)
                    )
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
}
