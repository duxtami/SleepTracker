package com.sleeptracker.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    SLEEP("sleep", "Sleep", Icons.Filled.Bedtime, Icons.Outlined.Bedtime),
    TIMELINE("timeline", "Timeline", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    INSIGHTS("insights", "Insights", Icons.Filled.Insights, Icons.Outlined.Insights),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}
