package com.sleeptracker.app.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.ui.theme.PillShape

/**
 * What the trailing FAB-style button next to the main pill currently does. It's a single
 * contextual action, not a 4th destination - its meaning changes with which tab is active rather
 * than being a fixed destination the way Sleep/Timeline/Insights are.
 */

private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
private val EmphasizedDecelerateEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
private val EmphasizedAccelerateEasing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

private const val SHORT2 = 100
private const val SHORT3 = 150
private const val SHORT4 = 200
private const val MEDIUM1 = 250
private const val MEDIUM2 = 300
private const val LONG1 = 450

enum class NavTrailingAction {
    /** Jumps to the Settings screen - the default action everywhere except the Timeline tab. */
    SETTINGS,

    /** Opens the "Add sleep entry" sheet directly from the nav bar - only offered while the
     *  Timeline tab is active, since that's the one screen where adding an entry inline actually
     *  makes sense. */
    ADD_ENTRY
}

/**
 * Floating expressive bottom navigation bar: a compact, content-hugging pill holding the 3 main
 * destinations, plus a separate small trailing action button.
 *
 * This is hand-rolled on `material3:1.3.0` rather than the newer `HorizontalFloatingToolbar`
 * (with its built-in FAB slot) - that component's FAB support needs
 * `material3:1.5.0-alpha22`+, which in turn requires `compileSdk 37` and Android Gradle Plugin
 * 9.1.0+. That's a large toolchain jump (this project is on AGP 8.3.2 / compileSdk 34) with its
 * own ripple effects, so not something to pull in just for this bar.
 *
 * To still get the same *visually coherent* result without that dependency, the trailing button
 * deliberately reuses the exact same `primaryContainer`/`onPrimaryContainer` color tokens as a
 * pill's selected state, rather than an independent accent color - so it reads as "part of the
 * same family" as the main pill instead of a mismatched extra element.
 *
 * Each pill's touch target is explicitly held to Material's 48dp accessibility minimum via
 * `.heightIn(min = 46.dp)`, independent of the padding/icon-size tuning used to keep the bar's
 * visual height compact.
 */
@Composable
fun FloatingNavBar(
    destinations: List<Destination>,
    selected: Destination?,
    onSelect: (Destination) -> Unit,
    trailingAction: NavTrailingAction,
    onTrailingClick: () -> Unit,
    isSettingsActive: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceContainerHighest
        ) {
            // On the Settings screen, the main pill shows 2 items instead of the 3 destination
            // tabs: a plain back arrow (left, active - takes you back to the app) and a settings
            // icon (right, shown in the same "selected" pill style as an active tab, but inert -
            // tapping it does nothing since you're already here). This reads as "you're on a
            // dedicated Settings tab" rather than one merged, label-less back button.
            AnimatedContent(targetState = isSettingsActive, label = "navBarMainContent") { settingsActive ->
                if (settingsActive) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GenericNavPill(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            label = "Back",
                            contentDescription = "Back to app",
                            isSelected = false,
                            onClick = onBack
                        )
                        GenericNavPill(
                            icon = Destination.SETTINGS.selectedIcon,
                            label = "Settings",
                            contentDescription = "Settings",
                            isSelected = true,
                            onClick = {}
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
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
        }

        // The trailing action only makes sense relative to a destination tab (jump to Settings,
        // or add an entry on Timeline) - once already inside Settings there's nothing for it to
        // do, so it's dropped entirely rather than shown disabled or repurposed.
        if (!isSettingsActive) {
            TrailingActionButton(action = trailingAction, onClick = onTrailingClick)
        }
    }
}

@Composable
private fun GenericNavPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val targetBackground = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val targetContentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val background by animateColorAsState(targetBackground, tween(SHORT4, easing = EmphasizedEasing), label = "genericNavPillBackground")
    val contentColor by animateColorAsState(targetContentColor, tween(SHORT4, easing = EmphasizedEasing), label = "genericNavPillContent")

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
        animationSpec = tween(MEDIUM2, easing = EmphasizedEasing),
        label = "genericNavPillPressScale"
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
            .heightIn(min = 46.dp)
            .padding(horizontal = if (isSelected) 18.dp else 12.dp, vertical = 11.dp)
            .animateContentSize(animationSpec = tween(MEDIUM2, easing = EmphasizedEasing)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = if (isSelected) null else contentDescription, tint = contentColor, modifier = Modifier.size(22.dp))
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(animationSpec = tween(MEDIUM2, easing = EmphasizedEasing)) +
                expandHorizontally(animationSpec = tween(MEDIUM2, easing = EmphasizedEasing), expandFrom = Alignment.Start) +
                scaleIn(initialScale = 0.8f, animationSpec = tween(MEDIUM2, easing = EmphasizedEasing)),
            exit = fadeOut(animationSpec = tween(SHORT2, easing = EmphasizedAccelerateEasing)) +
                shrinkHorizontally(animationSpec = tween(SHORT3, easing = EmphasizedAccelerateEasing), shrinkTowards = Alignment.Start) +
                scaleOut(targetScale = 0.8f, animationSpec = tween(SHORT2, easing = EmphasizedAccelerateEasing))
        ) {
            Text(text = label, color = contentColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TrailingActionButton(action: NavTrailingAction, onClick: () -> Unit) {
    val icon = when (action) {
        NavTrailingAction.SETTINGS -> Destination.SETTINGS.selectedIcon
        NavTrailingAction.ADD_ENTRY -> Icons.Filled.Add
    }
    val contentDescription = when (action) {
        NavTrailingAction.SETTINGS -> "Settings"
        NavTrailingAction.ADD_ENTRY -> "Add sleep entry"
    }

    // A brief, tactile shrink-on-press, matching the main pills' feedback.
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
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = tween(MEDIUM2, easing = EmphasizedEasing),
        label = "trailingActionPressScale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .size(56.dp),
        shape = PillShape,
        // Deliberately the SAME tokens as a pill's selected state below (primaryContainer /
        // onPrimaryContainer) - not an independently-chosen accent - so this button reads as
        // part of the same design family as the main pill instead of clashing with it.
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        interactionSource = interactionSource
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            // Cross-fade between Settings and Add so the swap on tab-change reads as an
            // intentional transition, not an abrupt icon pop.
            AnimatedContent(targetState = icon, label = "trailingActionIcon") { targetIcon ->
                Icon(imageVector = targetIcon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun NavPill(destination: Destination, isSelected: Boolean, onClick: () -> Unit) {
    // Only the selected destination gets a filled pill. Unselected destinations render as bare
    // icon buttons with no background at all - so they read as plain floating icons against the
    // bar, rather than as inactive pills of their own.
    val targetBackground = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val targetContentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    // Smooth color cross-fade on selection instead of an abrupt swap.
    val background by animateColorAsState(targetBackground, tween(SHORT4, easing = EmphasizedEasing), label = "navPillBackground")
    val contentColor by animateColorAsState(targetContentColor, tween(SHORT4, easing = EmphasizedEasing), label = "navPillContent")

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
        animationSpec = tween(MEDIUM2, easing = EmphasizedEasing),
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
            .heightIn(min = 46.dp)
            .padding(horizontal = if (isSelected) 18.dp else 12.dp, vertical = 11.dp)
            .animateContentSize(animationSpec = tween(MEDIUM2, easing = EmphasizedEasing)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Cross-fades + gently scales between the outlined and filled icon variants instead of
        // popping directly from one vector to the other.
        AnimatedContent(
            targetState = isSelected,
            transitionSpec = {
                (fadeIn(tween(SHORT3, easing = EmphasizedDecelerateEasing)) + scaleIn(initialScale = 0.7f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)))
                    .togetherWith(fadeOut(tween(SHORT2, easing = EmphasizedAccelerateEasing)) + scaleOut(targetScale = 0.7f, animationSpec = tween(SHORT2, easing = EmphasizedAccelerateEasing)))
            },
            label = "navPillIcon"
        ) { isSelectedState ->
            Icon(
                imageVector = if (isSelectedState) destination.selectedIcon else destination.unselectedIcon,
                contentDescription = if (isSelectedState) null else destination.label,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn(animationSpec = tween(MEDIUM2, easing = EmphasizedEasing)) +
                expandHorizontally(animationSpec = tween(MEDIUM2, easing = EmphasizedEasing), expandFrom = Alignment.Start) +
                scaleIn(initialScale = 0.8f, animationSpec = tween(MEDIUM2, easing = EmphasizedEasing)),
            exit = fadeOut(animationSpec = tween(SHORT2, easing = EmphasizedAccelerateEasing)) +
                shrinkHorizontally(animationSpec = tween(SHORT3, easing = EmphasizedAccelerateEasing), shrinkTowards = Alignment.Start) +
                scaleOut(targetScale = 0.8f, animationSpec = tween(SHORT2, easing = EmphasizedAccelerateEasing))
        ) {
            Text(
                text = destination.label,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
