package com.sleeptracker.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sleeptracker.app.data.datastore.SettingsRepository
import com.sleeptracker.app.data.repository.SleepRepository
import com.sleeptracker.app.util.SleepCalculator
import com.sleeptracker.app.util.SleepInsights
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class InsightsRange(val label: String, val days: Long?) {
    WEEKLY("Weekly", 7),
    MONTHLY("Monthly", 30),
    YEARLY("Yearly", 365),
    ALL_TIME("All Time", null)
}

data class InsightsUiState(
    val range: InsightsRange = InsightsRange.WEEKLY,
    val insights: SleepInsights = SleepInsights(0, 0, 0, 0, 0, 0, 0, null, null, 0),
    val previousInsights: SleepInsights? = null,
    val dailyTotals: Map<String, Long> = emptyMap(),
    val hasEnoughData: Boolean = false,
    val sleepGoalMinutes: Int = 480
)

class InsightsViewModel(
    private val repository: SleepRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _range = MutableStateFlow(InsightsRange.WEEKLY)

    val uiState: StateFlow<InsightsUiState> = combine(
        repository.observeAllSessions(),
        settingsRepository.settingsFlow,
        _range
    ) { sessions, settings, range ->
        val cutoff = range.days?.let { Instant.now().minus(it, ChronoUnit.DAYS).toEpochMilli() }
        val prevCutoff = range.days?.let { Instant.now().minus(it * 2, ChronoUnit.DAYS).toEpochMilli() }
        
        val scoped = if (cutoff != null) sessions.filter { it.startEpochMillis >= cutoff } else sessions
        val previousScoped = if (cutoff != null && prevCutoff != null) {
            sessions.filter { it.startEpochMillis in prevCutoff until cutoff }
        } else null

        val hasEnoughData = scoped.isNotEmpty()

        InsightsUiState(
            range = range,
            insights = SleepCalculator.computeInsights(scoped, settings.sleepGoalMinutes),
            previousInsights = previousScoped?.let { SleepCalculator.computeInsights(it, settings.sleepGoalMinutes) },
            dailyTotals = SleepCalculator.dailyTotals(scoped),
            hasEnoughData = hasEnoughData,
            sleepGoalMinutes = settings.sleepGoalMinutes
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState())

    fun selectRange(range: InsightsRange) {
        _range.value = range
    }
}
