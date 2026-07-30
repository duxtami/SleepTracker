package com.sleeptracker.app.ui.sleep

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeptracker.app.data.datastore.AppSettings
import com.sleeptracker.app.data.datastore.SettingsRepository
import com.sleeptracker.app.data.datastore.TrackingPrefsRepository
import com.sleeptracker.app.data.model.Mood
import com.sleeptracker.app.data.model.SleepSession
import com.sleeptracker.app.data.repository.SleepRepository
import com.sleeptracker.app.service.SleepTrackingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The three mutually-exclusive states the Home screen can be in. */
sealed interface TrackingPhase {
    data object Idle : TrackingPhase
    data class Waiting(val remainingMillis: Long, val plannedStartEpochMillis: Long, val delayMinutesUsed: Int) : TrackingPhase
    data class Tracking(val session: SleepSession) : TrackingPhase
}

data class SleepUiState(
    val greeting: String = "Good Evening",
    val phase: TrackingPhase = TrackingPhase.Idle,
    val lastCompletedSession: SleepSession? = null,
    val settings: AppSettings = AppSettings(),
    val errorMessage: String? = null,
    // Exists purely so every per-second recomputation produces a value that is NOT equal
    // (by data class equals) to the previous one, in every phase - including Idle, which is
    // when the "Awake for" live timer below needs to keep ticking. Without this, StateFlow's
    // conflation would silently drop ticks whenever nothing else in the state actually changed,
    // and collectAsState() would never recompose.
    val nowMillis: Long = System.currentTimeMillis()
) {
    val goalProgress: Float
        get() {
            val goalMillis = settings.sleepGoalMinutes * 60_000L
            if (goalMillis <= 0) return 0f
            val relevantMillis = when (val p = phase) {
                is TrackingPhase.Tracking -> p.session.durationMillis
                else -> lastCompletedSession?.durationMillis ?: 0L
            }
            return (relevantMillis.toFloat() / goalMillis.toFloat()).coerceIn(0f, 1f)
        }

    /** Live "Awake for" value: now minus the last completed session's wake-up time. Null if there's no completed sleep to measure from. */
    val awakeSinceWakeMillis: Long?
        get() = lastCompletedSession?.endEpochMillis?.let { (nowMillis - it).coerceAtLeast(0L) }
}

class SleepViewModel(
    private val repository: SleepRepository,
    private val settingsRepository: SettingsRepository,
    private val trackingPrefsRepository: TrackingPrefsRepository,
    private val appContext: Context
) : ViewModel() {

    private val _errorMessage = MutableStateFlow<String?>(null)

    private val ticker = flow {
        while (true) {
            emit(Unit)
            delay(1000L)
        }
    }

    private val baseState = combine(
        repository.observeActiveSession(),
        repository.observeLastCompletedSession(),
        settingsRepository.settingsFlow,
        trackingPrefsRepository.pendingDelayFlow,
        _errorMessage
    ) { active, last, settings, pending, error ->
        val phase = when {
            active != null -> TrackingPhase.Tracking(active)
            pending != null -> TrackingPhase.Waiting(
                remainingMillis = (pending.plannedStartEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L),
                plannedStartEpochMillis = pending.plannedStartEpochMillis,
                delayMinutesUsed = pending.delayMinutesUsed
            )
            else -> TrackingPhase.Idle
        }
        SleepUiState(
            greeting = com.sleeptracker.app.util.TimeUtils.greeting(),
            phase = phase,
            lastCompletedSession = last,
            settings = settings,
            errorMessage = error
        )
    }

    // Re-derive every second so every live timer actually ticks (countdown, elapsed, and the
    // Home screen's "Awake for" value), without needing a separate competing timer loop.
    val uiState: StateFlow<SleepUiState> = combine(baseState, ticker) { state, _ ->
        val phase = when (val p = state.phase) {
            is TrackingPhase.Waiting -> p.copy(
                remainingMillis = (p.plannedStartEpochMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            )
            else -> p
        }
        state.copy(phase = phase, nowMillis = System.currentTimeMillis())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SleepUiState())

    private fun sendServiceAction(action: String, configureIntent: Intent.() -> Unit = {}) {
        val intent = Intent(appContext, SleepTrackingService::class.java).apply {
            this.action = action
            configureIntent()
        }
        try {
            ContextCompat.startForegroundService(appContext, intent)
        } catch (e: Exception) {
            Log.e("SleepViewModel", "Failed to send service action=$action", e)
            _errorMessage.value = "Couldn't update sleep tracking. Please try again."
        }
    }

    /** Starts a new sleep session, honoring the current Start Time Delay setting unless overridden. */
    fun startSession(delayMinutesOverride: Int? = null) {
        val delayMinutes = delayMinutesOverride ?: uiState.value.settings.startDelayMinutes
        sendServiceAction(SleepTrackingService.ACTION_START) {
            putExtra(SleepTrackingService.EXTRA_DELAY_MINUTES, delayMinutes)
        }
    }

    fun cancelPendingDelay() {
        sendServiceAction(SleepTrackingService.ACTION_CANCEL_DELAY)
    }

    fun pauseSession() {
        sendServiceAction(SleepTrackingService.ACTION_PAUSE)
    }

    fun resumeSession() {
        sendServiceAction(SleepTrackingService.ACTION_RESUME)
    }

    fun endSession(mood: Mood? = null, notes: String? = null, qualityRating: Int? = null) {
        sendServiceAction(SleepTrackingService.ACTION_STOP) {
            putExtra(SleepTrackingService.EXTRA_MOOD, mood?.name)
            putExtra(SleepTrackingService.EXTRA_NOTES, notes)
            putExtra(SleepTrackingService.EXTRA_QUALITY, qualityRating ?: -1)
        }
    }

    fun addNoteToActiveSession(text: String) {
        val activeId = (uiState.value.phase as? TrackingPhase.Tracking)?.session?.id ?: return
        viewModelScope.launch { repository.addNote(activeId, text) }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
