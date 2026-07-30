package com.sleeptracker.app.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeptracker.app.data.datastore.AppSettings
import com.sleeptracker.app.data.datastore.SettingsRepository
import com.sleeptracker.app.data.model.ColorStyle
import com.sleeptracker.app.data.model.ThemeMode
import com.sleeptracker.app.data.repository.SleepRepository
import com.sleeptracker.app.util.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface BackupEvent {
    data class Success(val message: String) : BackupEvent
    data class Failure(val message: String) : BackupEvent
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val sleepRepository: SleepRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _backupEvent = MutableStateFlow<BackupEvent?>(null)
    val backupEvent: StateFlow<BackupEvent?> = _backupEvent.asStateFlow()

    private val _clearedAllData = MutableStateFlow(false)
    val clearedAllData: StateFlow<Boolean> = _clearedAllData.asStateFlow()

    // --- Appearance ---
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setColorStyle(style: ColorStyle) = viewModelScope.launch { settingsRepository.setColorStyle(style) }
    fun setFontScale(scale: Float) = viewModelScope.launch { settingsRepository.setFontScale(scale) }

    // --- Tracking ---
    fun setSleepGoalMinutes(minutes: Int) = viewModelScope.launch { settingsRepository.setSleepGoalMinutes(minutes) }
    fun setStartDelayMinutes(minutes: Int) = viewModelScope.launch { settingsRepository.setStartDelayMinutes(minutes) }

    // --- Reminders ---
    fun setBedtimeReminderEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setBedtimeReminderEnabled(enabled) }
    fun setBedtimeReminderTime(hour: Int, minute: Int) = viewModelScope.launch { settingsRepository.setBedtimeReminderTime(hour, minute) }
    fun setWakeReminderEnabled(enabled: Boolean) = viewModelScope.launch { settingsRepository.setWakeReminderEnabled(enabled) }
    fun setWakeReminderTime(hour: Int, minute: Int) = viewModelScope.launch { settingsRepository.setWakeReminderTime(hour, minute) }

    // --- History ---
    fun setShowAwakeDuration(enabled: Boolean) = viewModelScope.launch { settingsRepository.setShowAwakeDuration(enabled) }

    // --- Backup ---
    fun exportCsv(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val sessions = sleepRepository.observeAllSessions().first()
            BackupManager.exportCsv(context, uri, sessions)
        }.onSuccess {
            _backupEvent.value = BackupEvent.Success("Exported CSV successfully")
        }.onFailure {
            _backupEvent.value = BackupEvent.Failure("CSV export failed: ${it.message}")
        }
    }

    fun exportJson(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val sessions = sleepRepository.observeAllSessions().first()
            BackupManager.exportJson(context, uri, sessions)
        }.onSuccess {
            _backupEvent.value = BackupEvent.Success("Exported JSON successfully")
        }.onFailure {
            _backupEvent.value = BackupEvent.Failure("JSON export failed: ${it.message}")
        }
    }

    fun importCsv(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val sessions = BackupManager.importCsv(context, uri)
            sleepRepository.importSessions(sessions)
            sessions.size
        }.onSuccess {
            _backupEvent.value = BackupEvent.Success("Imported $it sessions from CSV")
        }.onFailure {
            _backupEvent.value = BackupEvent.Failure("CSV import failed: ${it.message}")
        }
    }

    fun importJson(context: Context, uri: Uri) = viewModelScope.launch {
        runCatching {
            val sessions = BackupManager.importJson(context, uri)
            sleepRepository.importSessions(sessions)
            sessions.size
        }.onSuccess {
            _backupEvent.value = BackupEvent.Success("Imported $it sessions from JSON")
        }.onFailure {
            _backupEvent.value = BackupEvent.Failure("JSON import failed: ${it.message}")
        }
    }

    fun clearBackupEvent() {
        _backupEvent.value = null
    }

    // --- Advanced ---
    fun clearAllData() = viewModelScope.launch {
        sleepRepository.clearAllData()
        _clearedAllData.value = true
    }

    fun clearClearedAllDataFlag() {
        _clearedAllData.value = false
    }
}
