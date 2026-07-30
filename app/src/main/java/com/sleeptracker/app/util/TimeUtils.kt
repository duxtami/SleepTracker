package com.sleeptracker.app.util

import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object TimeUtils {

    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    fun greeting(hour: Int = LocalTime.now().hour): String = when (hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    fun formatTime(epochMillis: Long, zoneId: String = ZoneId.systemDefault().id): String =
        Instant.ofEpochMilli(epochMillis).atZone(runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()))
            .format(timeFormatter)

    fun formatDate(epochMillis: Long, zoneId: String = ZoneId.systemDefault().id): String =
        Instant.ofEpochMilli(epochMillis).atZone(runCatching { ZoneId.of(zoneId) }.getOrDefault(ZoneId.systemDefault()))
            .format(dateFormatter)

    fun monthYearLabel(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(monthYearFormatter)

    fun monthKey(epochMillis: Long): String {
        val z = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        return "${z.year}-${z.monthValue.toString().padStart(2, '0')}"
    }

    fun dayOfWeekLabel(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).dayOfWeek
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())

    /** e.g. "7h 32m" */
    fun formatDurationShort(durationMillis: Long): String {
        val duration = Duration.ofMillis(durationMillis)
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    /** e.g. "07:32:15" for a live timer. */
    fun formatDurationClock(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatMinutesAsHoursMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }
}
