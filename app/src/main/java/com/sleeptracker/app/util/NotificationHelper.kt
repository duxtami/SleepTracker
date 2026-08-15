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

    const val REMINDER_CHANNEL_ID = "bedtime_reminder_channel"
    const val REMINDER_NOTIFICATION_ID = 4202

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

    /** Separate from [CHANNEL_ID] on purpose: that channel is IMPORTANCE_LOW (a silent, ongoing
     *  status notification), which would make a bedtime reminder easy to miss entirely - no
     *  heads-up, no sound. A reminder that's supposed to actually get your attention at a
     *  specific time needs its own higher-importance channel. */
    fun ensureReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val existing = manager.getNotificationChannel(REMINDER_CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    REMINDER_CHANNEL_ID,
                    "Bedtime reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Nudges you when it's time to wind down for bed"
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
     *  battery-efficient live-updating elapsed time, instead of manually reposting every second.
     *
     *  This is deliberately made as hard to dismiss as the platform allows:
     *  - `setOngoing(true)` blocks swipe-to-dismiss on the vast majority of devices/OEMs.
     *  - `setDeleteIntent` is a safety net for the rare OEM that still lets it be swiped away —
     *    it fires [SleepTrackingService.ACTION_REPOST_NOTIFICATION], which immediately reposts
     *    this same notification as long as the session is still actively (non-paused) tracking.
     *  - `FOREGROUND_SERVICE_IMMEDIATE` (API 31+) asks the system to show it without delay. */
    fun buildSleepingNotification(context: Context, startEpochMillis: Long): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.sleeptracker.app.R.drawable.ic_notification)
            .setContentTitle("Sleeping…")
            .setContentText("Sleep session in progress · started ${TimeUtils.formatTime(startEpochMillis)}")
            .setWhen(startEpochMillis)
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent(context))
            .setDeleteIntent(servicePendingIntent(context, SleepTrackingService.ACTION_REPOST_NOTIFICATION, 4))
            .addAction(0, "Pause", servicePendingIntent(context, SleepTrackingService.ACTION_PAUSE, 1))
            .addAction(0, "Stop", servicePendingIntent(context, SleepTrackingService.ACTION_STOP, 2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.foregroundServiceBehavior = NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
        }
        return builder.build()
    }

    /** Shown while a session is paused. Pausing is one of the two states (along with Stop) where
     *  the persistent notification is allowed to go away, so unlike the sleeping notification this
     *  one is dismissible and has no repost safety net. */
    fun buildPausedNotification(context: Context, elapsedSoFarMillis: Long): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.sleeptracker.app.R.drawable.ic_notification)
            .setContentTitle("Sleep tracking paused")
            .setContentText("Elapsed so far: ${TimeUtils.formatDurationShort(elapsedSoFarMillis)}")
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent(context))
            .addAction(0, "Resume", servicePendingIntent(context, SleepTrackingService.ACTION_RESUME, 1))
            .addAction(0, "Stop", servicePendingIntent(context, SleepTrackingService.ACTION_STOP, 2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /** Shown by [com.sleeptracker.app.receiver.BedtimeReminderReceiver] when the alarm scheduled
     *  by [com.sleeptracker.app.util.BedtimeReminderScheduler] fires. */
    fun buildBedtimeReminderNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(com.sleeptracker.app.R.drawable.ic_notification)
            .setContentTitle("Time to wind down")
            .setContentText("Your bedtime is coming up - this is your nudge to start getting ready for sleep.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
    }
}
