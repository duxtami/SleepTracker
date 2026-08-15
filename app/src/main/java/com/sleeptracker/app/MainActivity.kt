package com.sleeptracker.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.animation.PathInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sleeptracker.app.data.datastore.AppSettings
import com.sleeptracker.app.ui.navigation.SleepTrackerNavGraph
import com.sleeptracker.app.ui.theme.SleepTrackerTheme

/** Duration of the splash's fade+scale exit, in milliseconds. */
private const val SPLASH_EXIT_DURATION_MS = 400L

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Exactly one splash screen: the platform SplashScreen API (Theme.SleepTracker.Splash)
        // shows the uploaded artwork, centered at its original aspect ratio, on a plain
        // Material 3 background. There is no second, in-app splash composable - once this
        // exits, the real content (already composed underneath) is simply revealed.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // enableEdgeToEdge() with no arguments defaults to SystemBarStyle.auto(...), which
        // paints a semi-opaque scrim behind the status/navigation bars whenever it decides
        // content contrast might be insufficient. Passing fully transparent styles for both
        // bars, in both light and dark scrim slots, removes that initial scrim.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        // Separately from statusBarColor/navigationBarColor, the platform can paint its own
        // protective contrast scrim behind the system bars whenever it judges the content
        // underneath might not be legible - independent of any color we set. This is the
        // actual, distinct API for turning that off; without it, a gray strip can still show
        // up (especially on some OEM skins) even with fully transparent bar colors.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val interpolator = PathInterpolator(0.2f, 0f, 0.2f, 1f)
            splashScreenView.view.animate()
                .alpha(0f)
                .scaleX(1.06f)
                .scaleY(1.06f)
                .setDuration(SPLASH_EXIT_DURATION_MS)
                .setInterpolator(interpolator)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        splashScreenView.remove()
                    }
                })
                .start()
        }

        val container = (application as SleepTrackerApp).container

        setContent {
            val settings by container.settingsRepository.settingsFlow.collectAsState(initial = AppSettings())

            SleepTrackerTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                colorStyle = settings.colorStyle,
                useApplicationFont = settings.useApplicationFont,
                fontWeightAxis = settings.fontWeightAxis,
                fontWidthAxis = settings.fontWidthAxis,
                fontRoundnessAxis = settings.fontRoundnessAxis
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SleepTrackerNavGraph(container = container)
                }
            }
        }
    }
}
