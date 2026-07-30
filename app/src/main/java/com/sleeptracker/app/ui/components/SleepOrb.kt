package com.sleeptracker.app.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.ui.theme.OrbActiveEnd
import com.sleeptracker.app.ui.theme.OrbActiveStart
import com.sleeptracker.app.ui.theme.OrbNightEnd
import com.sleeptracker.app.ui.theme.OrbNightStart

/**
 * The large expressive orb on the Home screen. It gently pulses and glows at rest,
 * and shifts to a brighter, faster-breathing palette while a sleep session is active.
 *
 * Tap feedback uses the default Material3 ripple supplied by clickable() via
 * LocalIndication, rather than a manually constructed ripple instance. This avoids
 * depending on any specific ripple API surface (rememberRipple / ripple()) that
 * varies across Compose versions, while still giving standard themed tap feedback.
 */
@Composable
fun SleepOrb(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    onClick: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "orb")

    val pulse: Float by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 2600 else 3800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val glow: Float by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 2200 else 3200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val morphRotation: Float by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 14000 else 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "morph"
    )

    val startColor = if (isActive) OrbActiveStart else OrbNightStart
    val endColor = if (isActive) OrbActiveEnd else OrbNightEnd

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val radius: Float = (this.size.minDimension / 2f) * pulse
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            // Outer soft glow halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(endColor.copy(alpha = 0.35f * glow), Color.Transparent),
                    center = center,
                    radius = radius * 1.6f
                ),
                radius = radius * 1.6f,
                center = center
            )

            // Morphing inner blob made of overlapping soft circles for an organic feel
            rotate(degrees = morphRotation, pivot = center) {
                for (i in 0 until 3) {
                    val angle: Double = (i * 120.0) * (Math.PI / 180.0)
                    val offset = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * radius * 0.18f,
                        y = center.y + kotlin.math.sin(angle).toFloat() * radius * 0.18f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(startColor.copy(alpha = 0.9f), endColor.copy(alpha = 0.6f)),
                            center = offset,
                            radius = radius * 0.85f
                        ),
                        radius = radius * 0.85f,
                        center = offset
                    )
                }
            }

            // Core solid gradient sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(endColor, startColor),
                    center = center.copy(y = center.y - radius * 0.3f),
                    radius = radius * 1.3f
                ),
                radius = radius * 0.72f,
                center = center
            )
        }

        Text(
            text = if (isActive) "Tap to end" else "Tap to sleep",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
    }
}
