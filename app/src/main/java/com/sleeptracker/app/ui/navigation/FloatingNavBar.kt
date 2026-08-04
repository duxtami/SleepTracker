package com.sleeptracker.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.ui.theme.FloatingNavShape

/**
 * Floating expressive bottom navigation bar.
 *
 * Each pill's touch target is explicitly held to Material's 48dp accessibility minimum via
 * `.heightIn(min = 48.dp)`, rather than that height being an incidental side effect of the
 * padding/icon-size math - the visual chrome around the pills (Surface's own padding) is what
 * was trimmed to bring the bar's overall height down slightly, not the tappable area.
 *
 * Tap feedback still goes through the default Material3 ripple supplied by clickable() via
 * LocalIndication (an explicit interactionSource is passed through only so the press-scale
 * animation below can observe it), keeping this independent of whichever ripple API happens
 * to be available in a given Compose/Material3 version - same reasoning as SleepOrb.
 */
@Composable
fun FloatingNavBar(
    destinations: List<Destination>,
    selected: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = FloatingNavShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEach { destination ->
                NavPill(
                    destination = destination,
                    isSelected = destination == selected,
                    onClick = { onSelect(destination) }
                )
            }
        }
    }
}

@Composable
private fun NavPill(destination: Destination, isSelected: Boolean, onClick: () -> Unit) {
    val targetBackground = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val targetContentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    // Smooth color cross-fade on selection instead of an abrupt swap.
    val background by animateColorAsState(targetBackground, tween(220), label = "navPillBackground")
    val contentColor by animateColorAsState(targetContentColor, tween(220), label = "navPillContent")

    // A brief, tactile shrink-on-press rather than relying on the ripple alone for pressed
    // feedback - a small, springy scale reads as more "Expressive" and fluid.
    val interactionSource = remember { MutableInteractionSource() }
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> pressed = true
                is PressInteraction.Release, is PressInteraction.Cancel -> pressed = false
            }
        }
    }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navPillPressScale"
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(CircleShape)
            .background(background)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = if (isSelected) 20.dp else 16.dp, vertical = 12.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cross-fades + gently scales between the outlined and filled icon variants instead of
        // popping directly from one vector to the other.
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(initialScale = 0.7f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                    .togetherWith(fadeOut(tween(120)) + scaleOut(targetScale = 0.7f, animationSpec = tween(120)))
            },
            label = "navPillIcon"
        ) { selected ->
            Icon(
                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = if (selected) null else destination.label,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)) +
                expandHorizontally(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), expandFrom = Alignment.Start) +
                scaleIn(initialScale = 0.8f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)),
            exit = fadeOut(animationSpec = tween(120)) +
                shrinkHorizontally(animationSpec = tween(150), shrinkTowards = Alignment.Start) +
                scaleOut(targetScale = 0.8f, animationSpec = tween(120))
        ) {
            Text(
                text = destination.label,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
