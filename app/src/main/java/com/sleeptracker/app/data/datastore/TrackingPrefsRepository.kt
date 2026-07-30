package com.sleeptracker.app.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.trackingDataStore by preferencesDataStore(name = "sleep_tracker_tracking_state")

/** The Start Time Delay countdown that is currently in progress, if any. */
data class PendingDelay(val plannedStartEpochMillis: Long, val delayMinutesUsed: Int)

/**
 * Persists the "waiting to begin tracking" state (Start Time Delay countdown) outside of Room,
 * so the UI can correctly resume showing the countdown even if the app process was killed and
 * relaunched while the foreground service was still waiting for the delay to elapse.
 */
class TrackingPrefsRepository(private val context: Context) {

    private object Keys {
        val PLANNED_START = longPreferencesKey("pending_planned_start_epoch_millis")
        val DELAY_MINUTES = intPreferencesKey("pending_delay_minutes_used")
    }

    val pendingDelayFlow: Flow<PendingDelay?> = context.trackingDataStore.data.map { prefs ->
        val plannedStart = prefs[Keys.PLANNED_START]
        val delayMinutes = prefs[Keys.DELAY_MINUTES]
        if (plannedStart != null && delayMinutes != null) PendingDelay(plannedStart, delayMinutes) else null
    }

    suspend fun setPendingDelay(pending: PendingDelay) {
        context.trackingDataStore.edit {
            it[Keys.PLANNED_START] = pending.plannedStartEpochMillis
            it[Keys.DELAY_MINUTES] = pending.delayMinutesUsed
        }
    }

    suspend fun clearPendingDelay() {
        context.trackingDataStore.edit {
            it.remove(Keys.PLANNED_START)
            it.remove(Keys.DELAY_MINUTES)
        }
    }
}
