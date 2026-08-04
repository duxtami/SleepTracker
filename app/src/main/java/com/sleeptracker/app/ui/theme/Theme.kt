package com.sleeptracker.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sleeptracker.app.data.model.ColorStyle
import com.sleeptracker.app.data.model.ThemeMode

private fun staticLightScheme(style: ColorStyle) = when (style) {
    ColorStyle.TEAL -> lightColorScheme(primary = TealPrimaryLight)
    ColorStyle.SUNSET -> lightColorScheme(primary = SunsetPrimaryLight)
    ColorStyle.FOREST -> lightColorScheme(primary = ForestPrimaryLight)
    ColorStyle.ROSE -> lightColorScheme(primary = RosePrimaryLight)
    else -> lightColorScheme(
        primary = LavenderPrimaryLight,
        secondary = LavenderSecondaryLight,
        tertiary = LavenderTertiaryLight
    )
}

private fun staticDarkScheme(style: ColorStyle) = when (style) {
    ColorStyle.TEAL -> darkColorScheme(primary = TealPrimaryDark)
    ColorStyle.SUNSET -> darkColorScheme(primary = SunsetPrimaryDark)
    ColorStyle.FOREST -> darkColorScheme(primary = ForestPrimaryDark)
    ColorStyle.ROSE -> darkColorScheme(primary = RosePrimaryDark)
    else -> darkColorScheme(
        primary = LavenderPrimaryDark,
        secondary = LavenderSecondaryDark,
        tertiary = LavenderTertiaryDark
    )
}

@Composable
fun SleepTrackerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    colorStyle: ColorStyle = ColorStyle.DYNAMIC,
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
        useDark -> staticDarkScheme(colorStyle)
        else -> staticLightScheme(colorStyle)
    }

    if (themeMode == ThemeMode.AMOLED) {
        colorScheme = colorScheme.copy(
            background = AmoledBlack,
            surface = AmoledBlack,
            surfaceContainerLowest = AmoledBlack,
            surfaceContainerLow = SurfaceDim,
            surfaceContainer = SurfaceDim
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            // statusBarColor/navigationBarColor only control the bars' own paint - the
            // platform can separately draw its own protective contrast scrim behind them
            // regardless of that color. Without turning this off too, a gray strip can
            // still appear behind the status bar (notably on some OEM skins) even though
            // the bar color itself is fully transparent.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SleepTrackerTypography,
        shapes = SleepTrackerShapes,
        content = content
    )
}
