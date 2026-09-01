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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        // Deliberately content-hugging rather than a full-width bar: a compact floating pill
        // reads as an intentional, Expressive piece of UI - matching FloatingNavBar and the FAB,
        // which are both compact shapes rather than edge-to-edge bars - instead of a generic
        // full-width toast. It also sidesteps the FAB entirely for any message this short; the
        // max width below is only a safety cap for the rare long message, not a target width.
        modifier = Modifier.fillMaxWidth(),
        shape = com.sleeptracker.app.ui.theme.PillShape,
        // Previously inverseSurface/inverseOnSurface - the classic "toast" Snackbar look,
        // which is deliberately the *opposite* tone of the current theme (a light card in dark
        // mode, a dark card in light mode). That's normally intentional Material 3 spec
        // behavior, but here it reads as an off-theme, near-white card breaking out of an
        // otherwise dark/AMOLED/dynamic-color app. surfaceContainerHigh is the same elevated
        // container token FloatingNavBar and every card in this app already use, so the
        // Snackbar now inherits the same dynamic-color-aware Material You palette as everything
        // else instead of standing apart from it.
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Actionable messages (delete-with-undo) read as a confirmation, so they get the
            // filled checkmark in the theme's primary color - the same "done" language as a
            // system-style success toast. Plain messages (errors, status updates with no
            // action) keep a neutral outline icon instead, since a checkmark would misread as
            // success on an error message.
            val hasAction = data.visuals.actionLabel != null
            Icon(
                imageVector = if (hasAction) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = if (hasAction) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = data.visuals.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 10.dp, end = 4.dp)
            )
            val actionLabel = data.visuals.actionLabel
            if (actionLabel != null) {
                TextButton(
                    onClick = { data.performAction() },
                    shape = CircleShape,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(start = 4.dp)
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
