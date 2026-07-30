package com.sleeptracker.app.ui.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.ui.theme.FloatingNavShape

/**
 * Floating expressive bottom navigation bar. Tap feedback on each pill uses the
 * default Material3 ripple supplied by clickable() via LocalIndication, rather
 * than a manually constructed ripple instance, to stay independent of whichever
 * ripple API (rememberRipple / ripple()) happens to be available in a given
 * Compose/Material3 version.
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
            modifier = Modifier.padding(8.dp),
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
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isSelected) 20.dp else 16.dp, vertical = 14.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
            contentDescription = destination.label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        if (isSelected) {
            Text(
                text = destination.label,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
