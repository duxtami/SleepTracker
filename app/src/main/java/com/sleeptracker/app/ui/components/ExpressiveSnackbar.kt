package com.sleeptracker.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.ui.theme.SnackbarShape

/**
 * Material 3 Expressive replacement for the stock [androidx.compose.material3.SnackbarHost].
 *
 * The default SnackbarHost's message styling, shape, and enter/exit transition are all fixed
 * inside the library composable and aren't parameterized, so getting Expressive shape,
 * elevation, spacing, typography, action styling, and motion together means reimplementing
 * the host rather than configuring one. This still drives entirely off [hostState] - the
 * calling screen's `showSnackbar(...)`/undo logic is unchanged.
 */
@Composable
fun ExpressiveSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val current = hostState.currentSnackbarData
    // AnimatedVisibility's exit transition needs content to keep rendering the outgoing data
    // while it plays, but hostState.currentSnackbarData already flips to null the instant a
    // snackbar is dismissed - so the last non-null data is held here and only ever replaced
    // (never cleared) while a new one is showing, keeping the exit animation showing real
    // content instead of a blank frame.
    var lastData by remember { mutableStateOf<SnackbarData?>(null) }
    LaunchedEffect(current) {
        if (current != null) lastData = current
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = current != null,
            enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                ) +
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                ),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight / 3 },
                    animationSpec = tween(durationMillis = 150)
                ) +
                scaleOut(targetScale = 0.9f, animationSpec = tween(durationMillis = 150))
        ) {
            lastData?.let { data -> ExpressiveSnackbar(data) }
        }
    }
}

@Composable
private fun ExpressiveSnackbar(data: SnackbarData) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 480.dp),
        shape = SnackbarShape,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f, fill = false)
            )
            val actionLabel = data.visuals.actionLabel
            if (actionLabel != null) {
                TextButton(
                    onClick = { data.performAction() },
                    shape = CircleShape,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.inversePrimary
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
