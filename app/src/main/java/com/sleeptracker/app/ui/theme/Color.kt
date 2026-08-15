package com.sleeptracker.app.ui.theme

import androidx.compose.ui.graphics.Color

// Lavender (default night-sky accent)
val LavenderPrimaryLight = Color(0xFF5B5FC7)
val LavenderPrimaryDark = Color(0xFFC1C2FF)
val LavenderSecondaryLight = Color(0xFF5D5C77)
val LavenderSecondaryDark = Color(0xFFC6C4E6)
val LavenderTertiaryLight = Color(0xFF77536A)
val LavenderTertiaryDark = Color(0xFFE7B8D3)

// Teal
val TealPrimaryLight = Color(0xFF00696C)
val TealPrimaryDark = Color(0xFF4CD9DD)

// Sunset
val SunsetPrimaryLight = Color(0xFFA6420A)
val SunsetPrimaryDark = Color(0xFFFFB68C)

// Forest
val ForestPrimaryLight = Color(0xFF3C6C34)
val ForestPrimaryDark = Color(0xFFA0D492)

// Rose
val RosePrimaryLight = Color(0xFFB01458)
val RosePrimaryDark = Color(0xFFFFB1C4)

val AmoledBlack = Color(0xFF000000)

// True AMOLED surface ladder: neutral grays only (equal R/G/B at every step), rising in
// lightness with elevation exactly like Material's dark elevation-overlay convention, but with
// zero hue tint. The bug this replaces was surfaceContainerHigh/Highest (used by FloatingNavBar,
// cards, dialogs, the Snackbar, bottom sheets) being left un-overridden in AMOLED mode, so they
// kept whatever chromatic tint the active dynamic/static dark scheme gave them - visible as a
// dark blue/gray cast instead of true black-based neutrals - plus the one level that *was*
// overridden (surfaceContainer/Low) used to point at SurfaceDim = 0xFF0E0E14, which itself
// leaned blue (its blue channel is highest of the three).
val AmoledSurfaceContainerLow = Color(0xFF0A0A0A)
val AmoledSurfaceContainer = Color(0xFF121212)
val AmoledSurfaceContainerHigh = Color(0xFF1C1C1C)
val AmoledSurfaceContainerHighest = Color(0xFF272727)
val AmoledSurfaceVariant = Color(0xFF1C1C1C)
val AmoledSurfaceBright = Color(0xFF2E2E2E)
