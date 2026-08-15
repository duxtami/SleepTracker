package com.sleeptracker.app.receiver

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.sleeptracker.app.util.BedtimeReminderScheduler
import com.sleeptracker.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires when the alarm scheduled by [BedtimeReminderScheduler] goes off: shows the reminder
 *  notification, then immediately reschedules itself for tomorrow so the reminder repeats daily
 *  without needing a separate recurring-alarm mechanism. */
class BedtimeReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.ensureReminderChannel(context)

        val notificationsAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (notificationsAllowed) {
            val manager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            manager?.notify(NotificationHelper.REMINDER_NOTIFICATION_ID, NotificationHelper.buildBedtimeReminderNotification(context))
        }

        // BroadcastReceiver.onReceive must return promptly, but rescheduling reads the current
        // reminder time from DataStore (a suspend call) - goAsync() extends the receiver's
        // lifetime just long enough for that read to finish instead of racing it.
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                BedtimeReminderScheduler.rescheduleFromSettings(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
