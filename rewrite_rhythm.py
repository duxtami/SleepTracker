import re

with open('app/src/main/java/com/sleeptracker/app/ui/insights/InsightsScreen.kt', 'r') as f:
    content = f.read()

old_rhythm = """@Composable
fun RhythmChart(state: InsightsUiState) {
    ExpressiveCard {
        SectionHeader(title = "Hourly Sleep Rhythm")
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Typical sleeping hours based on bedtime and wake time.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val startMins = state.insights.averageBedtimeMinutesOfDay ?: 0
        val endMins = state.insights.averageWakeMinutesOfDay ?: 0
        
        Row(
            modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.Start
        ) {
            val totalMins = 24 * 60f
            if (startMins > endMins) { // goes across midnight
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(endMins / totalMins).background(MaterialTheme.colorScheme.primary))
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((startMins - endMins) / totalMins))
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth().background(MaterialTheme.colorScheme.primary))
            } else {
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(startMins / totalMins))
                Box(modifier = Modifier.fillMaxHeight().fillMaxWidth((endMins - startMins) / totalMins).background(MaterialTheme.colorScheme.primary))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("12 AM", style = MaterialTheme.typography.labelSmall)
            Text("12 PM", style = MaterialTheme.typography.labelSmall)
            Text("11 PM", style = MaterialTheme.typography.labelSmall)
        }
    }
}"""

new_rhythm = """@Composable
fun RhythmChart(state: InsightsUiState) {
    ExpressiveCard {
        SectionHeader(title = "Hourly Sleep Rhythm")
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sleep frequency by hour across all recorded sessions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        val hourlyCounts = FloatArray(24)
        var totalValid = 0
        state.chronologicalData.forEach { session ->
            val endMillis = session.endEpochMillis ?: return@forEach
            val zone = session.zone
            val startInstant = java.time.Instant.ofEpochMilli(session.startEpochMillis).atZone(zone)
            val endInstant = java.time.Instant.ofEpochMilli(endMillis).atZone(zone)
            
            var current = startInstant.truncatedTo(java.time.temporal.ChronoUnit.HOURS)
            val endTruncated = endInstant.plusHours(1).truncatedTo(java.time.temporal.ChronoUnit.HOURS)
            
            while (current.isBefore(endTruncated)) {
                hourlyCounts[current.hour] += 1f
                current = current.plusHours(1)
            }
            totalValid++
        }
        
        val chartData = (12..35).map { h ->
            val hour = h % 24
            val label = if (hour % 6 == 0) {
                val ampm = if (hour < 12) "AM" else "PM"
                val h12 = if (hour % 12 == 0) 12 else hour % 12
                "$h12$ampm"
            } else ""
            com.sleeptracker.app.ui.components.BarData(
                value = if (totalValid > 0) (hourlyCounts[hour] / totalValid) * 100f else 0f,
                label = label,
                tooltipText = "${if(hour%12==0) 12 else hour%12}${if(hour<12) "AM" else "PM"}: ${(if (totalValid > 0) (hourlyCounts[hour] / totalValid) * 100f else 0f).toInt()}%"
            )
        }
        
        com.sleeptracker.app.ui.components.BarChart(
            data = chartData,
            yAxisLabelFormatter = { "${it.toInt()}%" }
        )
    }
}"""

content = content.replace(old_rhythm, new_rhythm)

with open('app/src/main/java/com/sleeptracker/app/ui/insights/InsightsScreen.kt', 'w') as f:
    f.write(content)

