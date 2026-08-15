package com.sleeptracker.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sleeptracker.app.util.BedtimeReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** AlarmManager alarms are wiped on reboot, so without this the bedtime reminder would silently
 *  stop working after every restart until the user happened to reopen Settings and re-touch the
 *  toggle. This puts it back the moment the device comes back up. */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

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
