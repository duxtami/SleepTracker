package com.sleeptracker.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.sleeptracker.app.SleepTrackerApp
import com.sleeptracker.app.receiver.BedtimeReminderReceiver
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Schedules and cancels the bedtime reminder alarm. This existed only as a Settings toggle and a
 * stored hour/minute before - nothing ever actually asked [AlarmManager] to do anything with
 * them, which is why the reminder never fired. This is what closes that gap.
 *
 * AlarmManager alarms are one-shot and don't survive a reboot, so this is deliberately
 * self-rescheduling: [BedtimeReminderReceiver] calls [rescheduleFromSettings] again every time it
 * fires, and [com.sleeptracker.app.receiver.BootCompletedReceiver] does the same after a restart.
 */
object BedtimeReminderScheduler {

    private const val REQUEST_CODE = 5301

    /** Re-reads the current bedtime-reminder settings from DataStore and schedules or cancels
     *  the alarm to match. This is the single entry point used everywhere (app startup, boot,
     *  and every time the user changes the toggle or time in Settings) so there's exactly one
     *  place that decides whether an alarm should exist right now. */
    suspend fun rescheduleFromSettings(context: Context) {
        val app = context.applicationContext as? SleepTrackerApp ?: return
        val settings = app.container.settingsRepository.settingsFlow.first()
        if (settings.bedtimeReminderEnabled) {
            schedule(context, settings.bedtimeReminderHour, settings.bedtimeReminderMinute)
        } else {
            cancel(context)
        }
    }

    private fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = nextTriggerTime(hour, minute)
        val pendingIntent = pendingIntent(context)

        // canScheduleExactAlarms() only exists from API 31 - below that, SCHEDULE_EXACT_ALARM
        // is an ordinary install-time permission and every app can schedule exact alarms freely.
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

        if (canScheduleExact) {
            AlarmManagerCompat.setExactAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            // Exact-alarm access isn't granted - still schedule it, just without the exactness
            // guarantee. setAndAllowWhileIdle still wakes the device through Doze, it just gives
            // the OS some latitude on the precise minute, so the reminder still reliably fires
            // rather than silently never happening.
            AlarmManagerCompat.setAndAllowWhileIdle(alarmManager, AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(pendingIntent(context))
    }

    /** The next occurrence of [hour]:[minute] - today if it hasn't happened yet, otherwise
     *  tomorrow. This is also how the reminder naturally repeats daily: each firing reschedules
     *  itself for "the next occurrence" again. */
    private fun nextTriggerTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, BedtimeReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
