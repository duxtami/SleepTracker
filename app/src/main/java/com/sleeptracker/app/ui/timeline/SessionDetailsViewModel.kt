package com.sleeptracker.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeptracker.app.data.datastore.AppSettings
import com.sleeptracker.app.data.datastore.SettingsRepository
import com.sleeptracker.app.data.model.Mood
import com.sleeptracker.app.data.model.SleepSession
import com.sleeptracker.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionDetailsUiState(
    val session: SleepSession? = null,
    val settings: AppSettings = AppSettings(),
    val deleted: Boolean = false
)

class SessionDetailsViewModel(
    private val repository: SleepRepository,
    settingsRepository: SettingsRepository,
    private val sessionId: Long
) : ViewModel() {

    private val _deleted = MutableStateFlow(false)

    val uiState: StateFlow<SessionDetailsUiState> = combine(
        repository.observeAllSessions(),
        settingsRepository.settingsFlow,
        _deleted
    ) { sessions, settings, deleted ->
        SessionDetailsUiState(
            session = sessions.firstOrNull { it.id == sessionId },
            settings = settings,
            deleted = deleted
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionDetailsUiState())

    fun updateSession(
        startEpochMillis: Long,
        endEpochMillis: Long?,
        mood: Mood?,
        qualityRating: Int?,
        notes: String,
        tags: List<String>,
        startDelayMinutesUsed: Int
    ) {
        val current = uiState.value.session ?: return
        viewModelScope.launch {
            repository.upsertSession(
                current.copy(
                    startEpochMillis = startEpochMillis,
                    endEpochMillis = endEpochMillis,
                    mood = mood,
                    qualityRating = qualityRating,
                    notes = notes,
                    tags = tags,
                    startDelayMinutesUsed = startDelayMinutesUsed,
                    isManualEntry = true
                )
            )
        }
    }

    fun deleteSession() {
        val current = uiState.value.session ?: return
        viewModelScope.launch {
            repository.deleteSession(current)
            _deleted.value = true
        }
    }
}
