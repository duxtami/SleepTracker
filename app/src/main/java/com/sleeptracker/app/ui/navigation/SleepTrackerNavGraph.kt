package com.sleeptracker.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sleeptracker.app.di.AppContainer
import com.sleeptracker.app.ui.insights.InsightsScreen
import com.sleeptracker.app.ui.insights.InsightsViewModel
import com.sleeptracker.app.ui.settings.SettingsScreen
import com.sleeptracker.app.ui.settings.SettingsViewModel
import com.sleeptracker.app.ui.sleep.SleepScreen
import com.sleeptracker.app.ui.sleep.SleepViewModel
import com.sleeptracker.app.ui.timeline.SessionDetailsScreen
import com.sleeptracker.app.ui.timeline.SessionDetailsViewModel
import com.sleeptracker.app.ui.timeline.TimelineScreen
import com.sleeptracker.app.ui.timeline.TimelineViewModel
import com.sleeptracker.app.util.ViewModelFactory

private const val SESSION_DETAILS_ROUTE = "session_details/{sessionId}"

@Composable
fun SleepTrackerNavGraph(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = Destination.entries.firstOrNull { it.route == currentRoute } ?: Destination.SLEEP
    val showNav = currentRoute == null || Destination.entries.any { it.route == currentRoute }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Destination.SLEEP.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Destination.SLEEP.route) {
                val vm: SleepViewModel = viewModel(
                    factory = ViewModelFactory {
                        SleepViewModel(container.sleepRepository, container.settingsRepository, container.trackingPrefsRepository, container.appContext)
                    }
                )
                SleepScreen(viewModel = vm)
            }
            composable(Destination.TIMELINE.route) {
                val vm: TimelineViewModel = viewModel(
                    factory = ViewModelFactory { TimelineViewModel(container.sleepRepository) }
                )
                TimelineScreen(
                    viewModel = vm,
                    onOpenDetails = { id -> navController.navigate("session_details/$id") }
                )
            }
            composable(Destination.INSIGHTS.route) {
                val vm: InsightsViewModel = viewModel(
                    factory = ViewModelFactory { InsightsViewModel(container.sleepRepository, container.settingsRepository) }
                )
                InsightsScreen(viewModel = vm)
            }
            composable(Destination.SETTINGS.route) {
                val vm: SettingsViewModel = viewModel(
                    factory = ViewModelFactory { SettingsViewModel(container.settingsRepository, container.sleepRepository) }
                )
                SettingsScreen(viewModel = vm)
            }
            composable(
                route = SESSION_DETAILS_ROUTE,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@composable
                val vm: SessionDetailsViewModel = viewModel(
                    factory = ViewModelFactory { SessionDetailsViewModel(container.sleepRepository, container.settingsRepository, sessionId) }
                )
                SessionDetailsScreen(viewModel = vm, onBack = { navController.popBackStack() })
            }
        }

        if (showNav) {
            FloatingNavBar(
                destinations = Destination.entries,
                selected = currentDestination,
                onSelect = { destination ->
                    if (destination.route != currentRoute) {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
            )
        }
    }
}
