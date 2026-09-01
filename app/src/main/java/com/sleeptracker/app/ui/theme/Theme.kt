package com.sleeptracker.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sleeptracker.app.data.model.ColorStyle
import com.sleeptracker.app.data.model.ThemeMode
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.SchemeNeutral

private fun DynamicScheme.toComposeColorScheme(): ColorScheme {
    val mdc = MaterialDynamicColors()
    return ColorScheme(
        primary = Color(mdc.primary().getArgb(this)),
        onPrimary = Color(mdc.onPrimary().getArgb(this)),
        primaryContainer = Color(mdc.primaryContainer().getArgb(this)),
        onPrimaryContainer = Color(mdc.onPrimaryContainer().getArgb(this)),
        inversePrimary = Color(mdc.inversePrimary().getArgb(this)),
        secondary = Color(mdc.secondary().getArgb(this)),
        onSecondary = Color(mdc.onSecondary().getArgb(this)),
        secondaryContainer = Color(mdc.secondaryContainer().getArgb(this)),
        onSecondaryContainer = Color(mdc.onSecondaryContainer().getArgb(this)),
        tertiary = Color(mdc.tertiary().getArgb(this)),
        onTertiary = Color(mdc.onTertiary().getArgb(this)),
        tertiaryContainer = Color(mdc.tertiaryContainer().getArgb(this)),
        onTertiaryContainer = Color(mdc.onTertiaryContainer().getArgb(this)),
        background = Color(mdc.background().getArgb(this)),
        onBackground = Color(mdc.onBackground().getArgb(this)),
        surface = Color(mdc.surface().getArgb(this)),
        onSurface = Color(mdc.onSurface().getArgb(this)),
        surfaceVariant = Color(mdc.surfaceVariant().getArgb(this)),
        onSurfaceVariant = Color(mdc.onSurfaceVariant().getArgb(this)),
        surfaceTint = Color(mdc.primary().getArgb(this)),
        inverseSurface = Color(mdc.inverseSurface().getArgb(this)),
        inverseOnSurface = Color(mdc.inverseOnSurface().getArgb(this)),
        error = Color(mdc.error().getArgb(this)),
        onError = Color(mdc.onError().getArgb(this)),
        errorContainer = Color(mdc.errorContainer().getArgb(this)),
        onErrorContainer = Color(mdc.onErrorContainer().getArgb(this)),
        outline = Color(mdc.outline().getArgb(this)),
        outlineVariant = Color(mdc.outlineVariant().getArgb(this)),
        scrim = Color(mdc.scrim().getArgb(this)),
        surfaceBright = Color(mdc.surfaceBright().getArgb(this)),
        surfaceContainer = Color(mdc.surfaceContainer().getArgb(this)),
        surfaceContainerHigh = Color(mdc.surfaceContainerHigh().getArgb(this)),
        surfaceContainerHighest = Color(mdc.surfaceContainerHighest().getArgb(this)),
        surfaceContainerLow = Color(mdc.surfaceContainerLow().getArgb(this)),
        surfaceContainerLowest = Color(mdc.surfaceContainerLowest().getArgb(this)),
        surfaceDim = Color(mdc.surfaceDim().getArgb(this)),
    )
}

private fun getSeedColor(style: ColorStyle): Color = when (style) {
    ColorStyle.TEAL -> SeedTeal
    ColorStyle.SUNSET -> SeedSunset
    ColorStyle.FOREST -> SeedForest
    ColorStyle.ROSE -> SeedRose
    ColorStyle.OCEAN -> SeedOcean
    ColorStyle.AMBER -> SeedAmber
    else -> SeedLavender
}

private fun generateScheme(style: ColorStyle, isDark: Boolean): ColorScheme {
    val seed = getSeedColor(style).toArgb()
    // Using SchemeNeutral as default for reduced chroma per requirements
    val scheme = SchemeNeutral(Hct.fromInt(seed), isDark, 0.0)
    return scheme.toComposeColorScheme()
}

@Composable
fun SleepTrackerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    colorStyle: ColorStyle = ColorStyle.DYNAMIC,
    useApplicationFont: Boolean = false,
    fontWeightAxis: Float = GoogleSansFlexAxes.WEIGHT_MAX,
    fontWidthAxis: Float = GoogleSansFlexAxes.WIDTH_MAX,
    fontRoundnessAxis: Float = GoogleSansFlexAxes.ROUNDNESS_MAX,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var colorScheme = when {
        dynamicColor && supportsDynamic && useDark -> dynamicDarkColorScheme(context)
        dynamicColor && supportsDynamic && !useDark -> dynamicLightColorScheme(context)
        else -> generateScheme(colorStyle, useDark)
    }

    if (themeMode == ThemeMode.AMOLED) {
        colorScheme = colorScheme.copy(
            background = AmoledBlack,
            surface = AmoledBlack,
            surfaceDim = AmoledBlack,
            surfaceContainerLowest = AmoledBlack,
            surfaceContainerLow = AmoledSurfaceContainerLow,
            surfaceContainer = AmoledSurfaceContainer,
            surfaceContainerHigh = AmoledSurfaceContainerHigh,
            surfaceContainerHighest = AmoledSurfaceContainerHighest,
            surfaceVariant = AmoledSurfaceVariant,
            surfaceBright = AmoledSurfaceBright,
            inverseSurface = AmoledSurfaceContainerHighest,
            inverseOnSurface = androidx.compose.ui.graphics.Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    val typography = rememberAppTypography(
        useApplicationFont = useApplicationFont,
        weightAxis = fontWeightAxis,
        widthAxis = fontWidthAxis,
        roundnessAxis = fontRoundnessAxis
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = SleepTrackerShapes,
        content = content
    )
}

