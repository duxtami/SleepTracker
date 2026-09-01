package com.sleeptracker.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sleeptracker.app.data.model.ColorStyle
import com.sleeptracker.app.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sleep_tracker_settings")

/** Valid Start Time Delay choices, in minutes. 0 means "Off" (start tracking immediately). */
val START_DELAY_OPTIONS_MINUTES = listOf(0, 5, 10, 15, 20, 30, 45, 60)

/** Immutable snapshot of all user-configurable app settings. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val colorStyle: ColorStyle = ColorStyle.DYNAMIC,
    val scheduleBedtimeHour: Int = 22,
    val scheduleBedtimeMinute: Int = 30,
    val scheduleWakeHour: Int = 6,
    val scheduleWakeMinute: Int = 30,
    val bedtimeReminderEnabled: Boolean = false,
    val bedtimeReminderHour: Int = 22,
    val bedtimeReminderMinute: Int = 30,
    val startDelayMinutes: Int = 0,
    val showAwakeDuration: Boolean = false,
    val useApplicationFont: Boolean = false,
    val smartAnalyzeThresholdMinutes: Int = 5,
    // Google Sans Flex variation-axis values (wght/wdth/ROND). These default to the font's own
    // real, font-defined maximums (read from its fvar table: wght tops out at 1000, wdth at 151,
    // ROND at 100) so that the very first time a user flips "Use Application Font" on - before
    // any of these keys have ever been written to DataStore - they land on that maxed-out preset
    // exactly as specced, with no separate "is this the first time" flag needed.
    val fontWeightAxis: Float = 1000f,
    val fontWidthAxis: Float = 151f,
    val fontRoundnessAxis: Float = 100f
) {
    /**
     * The user's expected nightly sleep duration, in minutes - the single source of truth for
     * every "sleep goal" used across the app (Home's goal card, Insights' sleep-debt trend,
     * session detail goal comparisons, etc). This is always *derived* from the configured
     * [scheduleBedtimeHour]/[scheduleWakeHour] Sleep Schedule rather than stored independently,
     * so there is no separate value that can silently drift out of sync with the schedule the
     * user actually set: changing the schedule changes this, everywhere, automatically.
     *
     * Handles the schedule crossing midnight (the normal case - e.g. 22:30 -> 06:30 is 8h) as
     * well as same-day schedules (e.g. a 09:00 -> 17:00 nap-shift schedule is also 8h).
     */
    val sleepGoalMinutes: Int
        get() {
            val bedtimeOfDayMinutes = scheduleBedtimeHour * 60 + scheduleBedtimeMinute
            val wakeOfDayMinutes = scheduleWakeHour * 60 + scheduleWakeMinute
            val diff = wakeOfDayMinutes - bedtimeOfDayMinutes
            return if (diff <= 0) diff + 24 * 60 else diff
        }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val COLOR_STYLE = stringPreferencesKey("color_style")
        val SCHEDULE_BEDTIME_HOUR = intPreferencesKey("schedule_bedtime_hour")
        val SCHEDULE_BEDTIME_MINUTE = intPreferencesKey("schedule_bedtime_minute")
        val SCHEDULE_WAKE_HOUR = intPreferencesKey("schedule_wake_hour")
        val SCHEDULE_WAKE_MINUTE = intPreferencesKey("schedule_wake_minute")
        val BEDTIME_REMINDER_ENABLED = booleanPreferencesKey("bedtime_reminder_enabled")
        val BEDTIME_REMINDER_HOUR = intPreferencesKey("bedtime_reminder_hour")
        val BEDTIME_REMINDER_MINUTE = intPreferencesKey("bedtime_reminder_minute")
        val START_DELAY_MINUTES = intPreferencesKey("start_delay_minutes")
        val SHOW_AWAKE_DURATION = booleanPreferencesKey("show_awake_duration")
        val USE_APPLICATION_FONT = booleanPreferencesKey("use_application_font")
        val SMART_ANALYZE_THRESHOLD_MINUTES = intPreferencesKey("smart_analyze_threshold_minutes")
        val FONT_WEIGHT_AXIS = floatPreferencesKey("font_weight_axis")
        val FONT_WIDTH_AXIS = floatPreferencesKey("font_width_axis")
        val FONT_ROUNDNESS_AXIS = floatPreferencesKey("font_roundness_axis")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            colorStyle = runCatching { ColorStyle.valueOf(prefs[Keys.COLOR_STYLE] ?: ColorStyle.DYNAMIC.name) }
                .getOrDefault(ColorStyle.DYNAMIC),
            scheduleBedtimeHour = prefs[Keys.SCHEDULE_BEDTIME_HOUR] ?: 22,
            scheduleBedtimeMinute = prefs[Keys.SCHEDULE_BEDTIME_MINUTE] ?: 30,
            scheduleWakeHour = prefs[Keys.SCHEDULE_WAKE_HOUR] ?: 6,
            scheduleWakeMinute = prefs[Keys.SCHEDULE_WAKE_MINUTE] ?: 30,
            bedtimeReminderEnabled = prefs[Keys.BEDTIME_REMINDER_ENABLED] ?: false,
            bedtimeReminderHour = prefs[Keys.BEDTIME_REMINDER_HOUR] ?: 22,
            bedtimeReminderMinute = prefs[Keys.BEDTIME_REMINDER_MINUTE] ?: 30,
            startDelayMinutes = prefs[Keys.START_DELAY_MINUTES] ?: 0,
            showAwakeDuration = prefs[Keys.SHOW_AWAKE_DURATION] ?: false,
            useApplicationFont = prefs[Keys.USE_APPLICATION_FONT] ?: false,
            smartAnalyzeThresholdMinutes = prefs[Keys.SMART_ANALYZE_THRESHOLD_MINUTES] ?: 5,
            fontWeightAxis = prefs[Keys.FONT_WEIGHT_AXIS] ?: 1000f,
            fontWidthAxis = prefs[Keys.FONT_WIDTH_AXIS] ?: 151f,
            fontRoundnessAxis = prefs[Keys.FONT_ROUNDNESS_AXIS] ?: 100f
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setColorStyle(style: ColorStyle) {
        context.dataStore.edit { it[Keys.COLOR_STYLE] = style.name }
    }

    suspend fun setScheduleBedtime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.SCHEDULE_BEDTIME_HOUR] = hour
            it[Keys.SCHEDULE_BEDTIME_MINUTE] = minute
        }
    }

    suspend fun setScheduleWakeTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.SCHEDULE_WAKE_HOUR] = hour
            it[Keys.SCHEDULE_WAKE_MINUTE] = minute
        }
    }

    suspend fun setBedtimeReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BEDTIME_REMINDER_ENABLED] = enabled }
    }

    suspend fun setBedtimeReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.BEDTIME_REMINDER_HOUR] = hour
            it[Keys.BEDTIME_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setStartDelayMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.START_DELAY_MINUTES] = minutes }
    }

    suspend fun setShowAwakeDuration(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_AWAKE_DURATION] = enabled }
    }

    suspend fun setUseApplicationFont(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_APPLICATION_FONT] = enabled }
    }

    suspend fun setSmartAnalyzeThresholdMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SMART_ANALYZE_THRESHOLD_MINUTES] = minutes }
    }

    suspend fun setFontWeightAxis(value: Float) {
        context.dataStore.edit { it[Keys.FONT_WEIGHT_AXIS] = value }
    }

    suspend fun setFontWidthAxis(value: Float) {
        context.dataStore.edit { it[Keys.FONT_WIDTH_AXIS] = value }
    }

    suspend fun setFontRoundnessAxis(value: Float) {
        context.dataStore.edit { it[Keys.FONT_ROUNDNESS_AXIS] = value }
    }
}
