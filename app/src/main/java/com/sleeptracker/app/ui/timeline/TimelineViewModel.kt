package com.sleeptracker.app.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeptracker.app.data.model.Mood
import com.sleeptracker.app.data.model.SleepSession
import com.sleeptracker.app.data.repository.SleepRepository
import com.sleeptracker.app.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.sleeptracker.app.data.datastore.AppSettings
import com.sleeptracker.app.data.datastore.SettingsRepository

data class MonthGroup(val monthKey: String, val label: String, val sessions: List<SleepSession>)

data class TimelineUiState(
    val groups: List<MonthGroup> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val settings: AppSettings = AppSettings()
)

class TimelineViewModel(
    private val repository: SleepRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<TimelineUiState> = combine(
        repository.observeAllSessions(),
        _searchQuery,
        settingsRepository.settingsFlow
    ) { sessions, query, settings ->
        val completedOnly = sessions.filter { !it.isActive }
        val filtered = if (query.isBlank()) {
            completedOnly
        } else {
            completedOnly.filter { s ->
                val dateStr = TimeUtils.formatDate(s.startEpochMillis)
                s.notes.contains(query, ignoreCase = true) ||
                    s.tags.any { it.contains(query, ignoreCase = true) } ||
                    (s.mood?.label?.contains(query, ignoreCase = true) == true) ||
                    dateStr.contains(query, ignoreCase = true)
            }
        }
        val groups = filtered
            .groupBy { TimeUtils.monthKey(it.startEpochMillis) }
            .toSortedMap(compareByDescending { it })
            .map { (key, list) ->
                MonthGroup(
                    monthKey = key,
                    label = TimeUtils.monthYearLabel(list.first().startEpochMillis),
                    sessions = list.sortedByDescending { it.startEpochMillis }
                )
            }
        TimelineUiState(groups = groups, searchQuery = query, isLoading = false, settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState())

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun deleteSession(session: SleepSession) {
        viewModelScope.launch { repository.deleteSession(session) }
    }

    /** Re-inserts a deleted session, used to power the Timeline's undo-delete Snackbar. */
    fun restoreSession(session: SleepSession) {
        viewModelScope.launch { repository.restoreSession(session) }
    }

    /** Called once a delete's undo window has passed without the user tapping Undo, to clean up
     *  the journal notes that were intentionally left behind in case of a restore. */
    fun confirmPermanentDelete(sessionId: Long) {
        viewModelScope.launch { repository.purgeNotesForSession(sessionId) }
    }

    fun updateSession(
        session: SleepSession,
        startEpochMillis: Long,
        endEpochMillis: Long?,
        mood: Mood?,
        qualityRating: Int?,
        notes: String,
        tags: List<String>,
        startDelayMinutesUsed: Int,
        totalPausedMillis: Long
    ) {
        viewModelScope.launch {
            repository.upsertSession(
                session.copy(
                    startEpochMillis = startEpochMillis,
                    endEpochMillis = endEpochMillis,
                    mood = mood,
                    qualityRating = qualityRating,
                    notes = notes,
                    tags = tags,
                    startDelayMinutesUsed = startDelayMinutesUsed,
                    totalPausedMillis = totalPausedMillis,
                    isManualEntry = true
                )
            )
        }
    }

    fun addManualSession(
        startEpochMillis: Long,
        endEpochMillis: Long,
        mood: Mood?,
        qualityRating: Int?,
        notes: String,
        tags: List<String>,
        startDelayMinutesUsed: Int,
        totalPausedMillis: Long
    ) {
        viewModelScope.launch {
            repository.upsertSession(
                SleepSession(
                    startEpochMillis = startEpochMillis,
                    endEpochMillis = endEpochMillis,
                    timeZoneId = java.time.ZoneId.systemDefault().id,
                    mood = mood,
                    qualityRating = qualityRating,
                    notes = notes,
                    tags = tags,
                    isManualEntry = true,
                    startDelayMinutesUsed = startDelayMinutesUsed,
                    totalPausedMillis = totalPausedMillis
                )
            )
        }
    }
}
