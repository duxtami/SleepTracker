package com.sleeptracker.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.sleeptracker.app.R
import kotlin.math.roundToInt

/**
 * Google Sans Flex's real, font-defined variation-axis ranges, read directly from the shipped
 * variable font's own `fvar` table (min/default/max per axis: wght 1/400/1000, wdth 25/100/151,
 * ROND 0/0/100). The Settings sliders are bound to exactly these ranges so the user can never
 * dial in a value the font itself doesn't support, and the "enable for the first time" defaults
 * below are this font's own real maximums - not approximated or hand-picked numbers.
 */
object GoogleSansFlexAxes {
    val WEIGHT_RANGE = 1f..1000f
    val WIDTH_RANGE = 25f..151f
    val ROUNDNESS_RANGE = 0f..100f

    const val WEIGHT_MAX = 1000f
    const val WIDTH_MAX = 151f
    const val ROUNDNESS_MAX = 100f

    /** The font's own neutral ("Regular") weight - the point every text style's weight delta
     *  (Bold vs Regular, SemiBold vs Regular, etc.) is measured relative to. */
    private const val NEUTRAL_WEIGHT = 400f

    /**
     * Resolves the actual `wght` axis value to use for a piece of text of [styleWeight], given
     * the user's chosen base [weightAxis]. Rather than flattening every Text in the app to one
     * identical weight (which would erase the app's whole typographic hierarchy - headlines no
     * longer reading as heavier than body copy), this keeps each text style's original weight
     * *offset* from the font's neutral weight, then re-centers that offset on the user's chosen
     * base. A user who drags Weight up still sees bolder headlines than body text, just all
     * shifted proportionally heavier.
     */
    fun resolveWeight(styleWeight: FontWeight, weightAxis: Float): Int {
        val delta = styleWeight.weight - NEUTRAL_WEIGHT
        return (weightAxis + delta).roundToInt().coerceIn(1, 1000)
    }
}

@OptIn(ExperimentalTextApi::class)
private fun buildGoogleSansFlexFamily(
    weightAxis: Float,
    widthAxis: Float,
    roundnessAxis: Float,
    isHeader: Boolean
): FontFamily {
    val weights = listOf(
        FontWeight.Thin, FontWeight.ExtraLight, FontWeight.Light, FontWeight.Normal,
        FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black
    )
    return FontFamily(
        weights.map { w ->
            val resolvedWeight = if (isHeader) {
                (GoogleSansFlexAxes.resolveWeight(w, weightAxis) + 150).coerceIn(1, 1000)
            } else {
                GoogleSansFlexAxes.resolveWeight(w, weightAxis)
            }
            val resolvedWidth = if (isHeader) {
                (widthAxis * 1.15f).coerceIn(GoogleSansFlexAxes.WIDTH_RANGE)
            } else {
                widthAxis.coerceIn(GoogleSansFlexAxes.WIDTH_RANGE)
            }
            Font(
                resId = R.font.google_sans_flex,
                weight = w,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(resolvedWeight),
                    FontVariation.width(resolvedWidth),
                    FontVariation.Setting("ROND", roundnessAxis.coerceIn(GoogleSansFlexAxes.ROUNDNESS_RANGE))
                )
            )
        }
    )
}

/**
 * Builds the app's [Typography]: either [SleepTrackerTypography] untouched (the plain Android
 * system font, when [useApplicationFont] is off) or the same type scale re-bound to Google Sans
 * Flex at the given variation-axis values. Every returned [TextStyle] carries its own freshly
 * built [FontFamily], so any composable reading `MaterialTheme.typography.*` - across every
 * screen, dialog, bottom sheet, Snackbar, and nav bar - recomposes with the new font the instant
 * a slider moves. No Activity or process restart is needed.
 */
@Composable
fun rememberAppTypography(
    useApplicationFont: Boolean,
    weightAxis: Float,
    widthAxis: Float,
    roundnessAxis: Float
): Typography = remember(useApplicationFont, weightAxis, widthAxis, roundnessAxis) {
    if (!useApplicationFont) {
        SleepTrackerTypography
    } else {
        val bodyFamily = buildGoogleSansFlexFamily(weightAxis, widthAxis, roundnessAxis, isHeader = false)
        val headerFamily = buildGoogleSansFlexFamily(weightAxis, widthAxis, roundnessAxis, isHeader = true)

        fun TextStyle.withGoogleSansFlex(isHeader: Boolean = false): TextStyle = copy(
            fontFamily = if (isHeader) headerFamily else bodyFamily,
            // SleepTrackerTypography's lineHeight values were tuned for the system font's
            // metrics. Google Sans Flex - especially pushed toward its heavier/rounder/wider
            // end - can report taller ascent+descent than that original font did, and a fixed
            // lineHeight box that's now too short for the substituted font's actual glyphs is a
            // well-known way to get exactly what this clips: the tops/bottoms of bold or large
            // text getting cut off right at the text box edge. Unspecified tells Compose to
            // derive line height from whichever font is actually active, so it's always sized
            // for the real glyphs being drawn instead of a value borrowed from a different font.
            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
            lineHeightStyle = null
        )
        with(SleepTrackerTypography) {
            copy(
                displayLarge = displayLarge.withGoogleSansFlex(isHeader = true),
                displayMedium = displayMedium.withGoogleSansFlex(isHeader = true),
                displaySmall = displaySmall.withGoogleSansFlex(isHeader = true),
                headlineLarge = headlineLarge.withGoogleSansFlex(isHeader = true),
                headlineMedium = headlineMedium.withGoogleSansFlex(isHeader = true),
                headlineSmall = headlineSmall.withGoogleSansFlex(isHeader = true),
                titleLarge = titleLarge.withGoogleSansFlex(isHeader = true),
                titleMedium = titleMedium.withGoogleSansFlex(isHeader = false),
                titleSmall = titleSmall.withGoogleSansFlex(isHeader = false),
                bodyLarge = bodyLarge.withGoogleSansFlex(isHeader = false),
                bodyMedium = bodyMedium.withGoogleSansFlex(isHeader = false),
                bodySmall = bodySmall.withGoogleSansFlex(isHeader = false),
                labelLarge = labelLarge.withGoogleSansFlex(isHeader = false),
                labelMedium = labelMedium.withGoogleSansFlex(isHeader = false),
                labelSmall = labelSmall.withGoogleSansFlex(isHeader = false)
            )
        }
    }
}
