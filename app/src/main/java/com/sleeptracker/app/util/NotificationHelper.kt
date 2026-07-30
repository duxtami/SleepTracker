package com.sleeptracker.app.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sleeptracker.app.MainActivity
import com.sleeptracker.app.service.SleepTrackingService

/** Builds and manages the single ongoing notification shown while a sleep session is being tracked. */
object NotificationHelper {

    const val CHANNEL_ID = "sleep_tracking_channel"
    const val NOTIFICATION_ID = 4201

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Sleep tracking",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows the status of an in-progress sleep session"
                    setShowBadge(false)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun servicePendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, SleepTrackingService::class.java).apply { this.action = action }
        return PendingIntent.getService(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Shown while the Start Time Delay countdown is running, before tracking actually begins. */
    fun buildWaitingNotification(context: Context, remainingMillis: Long): Notification {
        val minutes = (remainingMillis / 60000L).toInt().coerceAtLeast(0)
        val seconds = ((remainingMillis / 1000L) % 60).toInt().coerceAtLeast(0)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.sleeptracker.app.R.drawable.ic_notification)
            .setContentTitle("Waiting to begin sleep tracking…")
            .setContentText(String.format("Starting in %02d:%02d", minutes, seconds))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(context))
            .addAction(0, "Cancel", servicePendingIntent(context, SleepTrackingService.ACTION_CANCEL_DELAY, 3))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** Shown while a session is actively tracking. Uses the system chronometer for a
     *  battery-efficient live-updating elapsed time, instead of manually reposting every second. */
    fun buildSleepingNotification(context: Context, startEpochMillis: Long): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.sleeptracker.app.R.drawable.ic_notification)
            .setContentTitle("Sleeping…")
            .setContentText("Sleep session in progress · started ${TimeUtils.formatTime(startEpochMillis)}")
            .setWhen(startEpochMillis)
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(context))
            .addAction(0, "Pause", servicePendingIntent(context, SleepTrackingService.ACTION_PAUSE, 1))
            .addAction(0, "Stop", servicePendingIntent(context, SleepTrackingService.ACTION_STOP, 2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun buildPausedNotification(context: Context, elapsedSoFarMillis: Long): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.sleeptracker.app.R.drawable.ic_notification)
            .setContentTitle("Sleep tracking paused")
            .setContentText("Elapsed so far: ${TimeUtils.formatDurationShort(elapsedSoFarMillis)}")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(context))
            .addAction(0, "Resume", servicePendingIntent(context, SleepTrackingService.ACTION_RESUME, 1))
            .addAction(0, "Stop", servicePendingIntent(context, SleepTrackingService.ACTION_STOP, 2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
