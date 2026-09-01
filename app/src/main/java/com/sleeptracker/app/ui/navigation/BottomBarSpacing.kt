package com.sleeptracker.app.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much space, measured from the bottom of the screen, a route's own floating content
 * (a FAB, a Snackbar) needs to reserve so it never overlaps [FloatingNavBar].
 *
 * FloatingNavBar isn't part of any individual screen's Scaffold - [SleepTrackerNavGraph]
 * overlays it on top of the NavHost in a shared Box, so no screen's Scaffold insets know it
 * exists. Rather than hand-copying the nav bar's own layout constants (its bottom offset, its
 * internal padding, its content height) into every screen that needs to clear it - values
 * that would silently drift out of sync the next time FloatingNavBar's own padding changes -
 * SleepTrackerNavGraph measures the nav bar's actual rendered height once per composition and
 * provides the real total reserved space here. Defaults to 0.dp for any screen composed
 * outside that Box (there is currently none, but this keeps the default safe/inert rather than
 * reserving space that might not exist).
 *
 * This value is exactly enough to clear the nav bar - it does NOT include any extra breathing
 * room on top of that. Scaffold already applies its own standard ~16dp margin around a
 * floatingActionButton, so a FAB only needs `Modifier.padding(bottom = LocalBottomBarSpace
 * .current)` and Scaffold's own margin supplies the final "standard M3 spacing above the nav
 * bar". A Snackbar host doesn't get a margin for free the same way, so callers should add
 * their own explicit gap on top of this value for one, e.g. `LocalBottomBarSpace.current +
 * 16.dp`.
 */
val LocalBottomBarSpace = compositionLocalOf<Dp> { 0.dp }
val LocalNavBarWidth = compositionLocalOf<Dp> { 0.dp }
