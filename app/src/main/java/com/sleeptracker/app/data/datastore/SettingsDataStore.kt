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
    val sleepGoalMinutes: Int = 8 * 60,
    val bedtimeReminderEnabled: Boolean = false,
    val bedtimeReminderHour: Int = 22,
    val bedtimeReminderMinute: Int = 30,
    val wakeReminderEnabled: Boolean = false,
    val wakeReminderHour: Int = 7,
    val wakeReminderMinute: Int = 0,
    val startDelayMinutes: Int = 0,
    val showAwakeDuration: Boolean = false,
    val fontScale: Float = 1.0f
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val COLOR_STYLE = stringPreferencesKey("color_style")
        val SLEEP_GOAL_MINUTES = intPreferencesKey("sleep_goal_minutes")
        val BEDTIME_REMINDER_ENABLED = booleanPreferencesKey("bedtime_reminder_enabled")
        val BEDTIME_REMINDER_HOUR = intPreferencesKey("bedtime_reminder_hour")
        val BEDTIME_REMINDER_MINUTE = intPreferencesKey("bedtime_reminder_minute")
        val WAKE_REMINDER_ENABLED = booleanPreferencesKey("wake_reminder_enabled")
        val WAKE_REMINDER_HOUR = intPreferencesKey("wake_reminder_hour")
        val WAKE_REMINDER_MINUTE = intPreferencesKey("wake_reminder_minute")
        val START_DELAY_MINUTES = intPreferencesKey("start_delay_minutes")
        val SHOW_AWAKE_DURATION = booleanPreferencesKey("show_awake_duration")
        val FONT_SCALE = floatPreferencesKey("font_scale")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            colorStyle = runCatching { ColorStyle.valueOf(prefs[Keys.COLOR_STYLE] ?: ColorStyle.DYNAMIC.name) }
                .getOrDefault(ColorStyle.DYNAMIC),
            sleepGoalMinutes = prefs[Keys.SLEEP_GOAL_MINUTES] ?: 8 * 60,
            bedtimeReminderEnabled = prefs[Keys.BEDTIME_REMINDER_ENABLED] ?: false,
            bedtimeReminderHour = prefs[Keys.BEDTIME_REMINDER_HOUR] ?: 22,
            bedtimeReminderMinute = prefs[Keys.BEDTIME_REMINDER_MINUTE] ?: 30,
            wakeReminderEnabled = prefs[Keys.WAKE_REMINDER_ENABLED] ?: false,
            wakeReminderHour = prefs[Keys.WAKE_REMINDER_HOUR] ?: 7,
            wakeReminderMinute = prefs[Keys.WAKE_REMINDER_MINUTE] ?: 0,
            startDelayMinutes = prefs[Keys.START_DELAY_MINUTES] ?: 0,
            showAwakeDuration = prefs[Keys.SHOW_AWAKE_DURATION] ?: false,
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f
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

    suspend fun setSleepGoalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SLEEP_GOAL_MINUTES] = minutes }
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

    suspend fun setWakeReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WAKE_REMINDER_ENABLED] = enabled }
    }

    suspend fun setWakeReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.WAKE_REMINDER_HOUR] = hour
            it[Keys.WAKE_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setStartDelayMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.START_DELAY_MINUTES] = minutes }
    }

    suspend fun setShowAwakeDuration(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_AWAKE_DURATION] = enabled }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale }
    }
}
