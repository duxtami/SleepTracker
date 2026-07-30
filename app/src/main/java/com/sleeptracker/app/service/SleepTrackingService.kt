package com.sleeptracker.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.sleeptracker.app.SleepTrackerApp
import com.sleeptracker.app.data.datastore.PendingDelay
import com.sleeptracker.app.data.model.Mood
import com.sleeptracker.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SleepTrackingService : Service() {

    companion object {
        private const val TAG = "SleepTrackingService"

        const val ACTION_START = "com.sleeptracker.app.action.START"
        const val ACTION_CANCEL_DELAY = "com.sleeptracker.app.action.CANCEL_DELAY"
        const val ACTION_PAUSE = "com.sleeptracker.app.action.PAUSE"
        const val ACTION_RESUME = "com.sleeptracker.app.action.RESUME"
        const val ACTION_STOP = "com.sleeptracker.app.action.STOP"

        const val EXTRA_DELAY_MINUTES = "extra_delay_minutes"
        const val EXTRA_MOOD = "extra_mood"
        const val EXTRA_NOTES = "extra_notes"
        const val EXTRA_QUALITY = "extra_quality"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var countdownJob: Job? = null

    private val container by lazy { (application as SleepTrackerApp).container }

    override fun onCreate() {
        super.onCreate()
        runCatching { NotificationHelper.ensureChannel(this) }
            .onFailure { Log.e(TAG, "Failed to create notification channel", it) }

        // Recover a countdown that was in progress if this process was killed and restarted.
        serviceScope.launch {
            runCatching {
                val pending = container.trackingPrefsRepository.pendingDelayFlow.first()
                val hasActive = container.sleepRepository.observeActiveSession().first() != null
                if (pending != null && !hasActive) {
                    startForegroundSafely { startForegroundWaiting(pending.plannedStartEpochMillis) }
                    runCountdown(pending.plannedStartEpochMillis, pending.delayMinutesUsed)
                } else if (hasActive) {
                    showCurrentActiveNotification()
                }
            }.onFailure { Log.e(TAG, "Failed to recover tracking state on service create", it) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START -> handleStart(intent)
                ACTION_CANCEL_DELAY -> handleCancelDelay()
                ACTION_PAUSE -> handlePause()
                ACTION_RESUME -> handleResume()
                ACTION_STOP -> handleStop(intent)
                else -> Log.w(TAG, "Received unknown or null action: ${intent?.action}")
            }
        } catch (e: Exception) {
            // Never let a tracking-control failure crash the whole app process.
            Log.e(TAG, "Unhandled error while processing action=${intent?.action}", e)
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            runCatching { stopSelf() }
        }
        return START_STICKY
    }

    /**
     * Calls startForeground() defensively. On some OEMs/API levels this can throw
     * (e.g. background-start restrictions, missing foreground service type permissions).
     * If it fails, we log it and stop the service instead of crashing the app.
     */
    private fun startForegroundSafely(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed - stopping service instead of crashing", e)
            runCatching { stopSelf() }
        }
    }

    private fun handleStart(intent: Intent) {
        val delayMinutes = intent.getIntExtra(EXTRA_DELAY_MINUTES, 0)
        val pressTime = System.currentTimeMillis()
        val plannedStart = pressTime + delayMinutes * 60_000L

        if (delayMinutes > 0) {
            startForegroundSafely { startForegroundWaiting(plannedStart) }
            serviceScope.launch {
                runCatching { container.trackingPrefsRepository.setPendingDelay(PendingDelay(plannedStart, delayMinutes)) }
                    .onFailure { Log.e(TAG, "Failed to persist pending delay", it) }
            }
            runCountdown(plannedStart, delayMinutes)
        } else {
            startForegroundSafely {
                startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildSleepingNotification(this, plannedStart))
            }
            serviceScope.launch {
                runCatching { container.sleepRepository.startSession(plannedStart, 0) }
                    .onFailure { Log.e(TAG, "Failed to start sleep session", it) }
            }
        }
    }

    private fun startForegroundWaiting(plannedStart: Long) {
        val remaining = (plannedStart - System.currentTimeMillis()).coerceAtLeast(0L)
        startForeground(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildWaitingNotification(this, remaining))
    }

    private fun runCountdown(plannedStart: Long, delayMinutes: Int) {
        countdownJob?.cancel()
        countdownJob = serviceScope.launch {
            runCatching {
                while (true) {
                    val remaining = plannedStart - System.currentTimeMillis()
                    if (remaining <= 0L) break
                    runCatching {
                        NotificationManagerCompat.from(this@SleepTrackingService)
                            .notify(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildWaitingNotification(this@SleepTrackingService, remaining))
                    }.onFailure { Log.e(TAG, "Failed to update waiting notification", it) }
                    delay(1000L)
                }
                // Delay elapsed: begin actual tracking.
                container.trackingPrefsRepository.clearPendingDelay()
                runCatching { container.sleepRepository.startSession(plannedStart, delayMinutes) }
                    .onFailure { Log.e(TAG, "Failed to start sleep session after delay", it) }
                runCatching {
                    NotificationManagerCompat.from(this@SleepTrackingService)
                        .notify(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildSleepingNotification(this@SleepTrackingService, plannedStart))
                }.onFailure { Log.e(TAG, "Failed to post sleeping notification", it) }
            }.onFailure { Log.e(TAG, "Countdown failed", it) }
        }
    }

    private fun handleCancelDelay() {
        countdownJob?.cancel()
        serviceScope.launch {
            runCatching { container.trackingPrefsRepository.clearPendingDelay() }
                .onFailure { Log.e(TAG, "Failed to clear pending delay", it) }
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            runCatching { stopSelf() }
        }
    }

    private fun handlePause() {
        serviceScope.launch {
            runCatching { container.sleepRepository.pauseActiveSession() }
                .onFailure { Log.e(TAG, "Failed to pause session", it) }
            runCatching { showCurrentActiveNotification() }
                .onFailure { Log.e(TAG, "Failed to update paused notification", it) }
        }
    }

    private fun handleResume() {
        serviceScope.launch {
            runCatching { container.sleepRepository.resumeActiveSession() }
                .onFailure { Log.e(TAG, "Failed to resume session", it) }
            runCatching { showCurrentActiveNotification() }
                .onFailure { Log.e(TAG, "Failed to update resumed notification", it) }
        }
    }

    private fun handleStop(intent: Intent) {
        val moodName = intent.getStringExtra(EXTRA_MOOD)
        val notes = intent.getStringExtra(EXTRA_NOTES)
        val quality = intent.getIntExtra(EXTRA_QUALITY, -1).let { if (it < 0) null else it }
        countdownJob?.cancel()
        serviceScope.launch {
            runCatching { container.trackingPrefsRepository.clearPendingDelay() }
                .onFailure { Log.e(TAG, "Failed to clear pending delay on stop", it) }
            runCatching {
                container.sleepRepository.endActiveSession(
                    mood = Mood.fromNameOrNull(moodName),
                    notes = notes,
                    qualityRating = quality
                )
            }.onFailure { Log.e(TAG, "Failed to end session", it) }
            // Only stop once the final write has actually completed, so the
            // service isn't torn down mid-write.
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            runCatching { stopSelf() }
        }
    }

    private suspend fun showCurrentActiveNotification() {
        val active = container.sleepRepository.observeActiveSession().first() ?: return
        val notification = if (active.isPaused) {
            NotificationHelper.buildPausedNotification(this, active.durationMillis)
        } else {
            NotificationHelper.buildSleepingNotification(this, active.startEpochMillis)
        }
        NotificationManagerCompat.from(this).notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
