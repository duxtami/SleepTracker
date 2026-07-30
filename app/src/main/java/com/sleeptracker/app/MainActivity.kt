package com.sleeptracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SleepTrackerApp).container

        setContent {
            val settings by container.settingsRepository.settingsFlow.collectAsState(initial = AppSettings())

            SleepTrackerTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                colorStyle = settings.colorStyle
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SleepTrackerNavGraph(container = container)
                }
            }
        }
    }
}
