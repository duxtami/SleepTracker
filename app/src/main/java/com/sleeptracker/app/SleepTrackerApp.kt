package com.sleeptracker.app

import android.app.Application
import com.sleeptracker.app.di.AppContainer
import com.sleeptracker.app.util.BedtimeReminderScheduler
import com.sleeptracker.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SleepTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
        NotificationHelper.ensureReminderChannel(this)
        // Without this, a reminder that was already enabled from a previous session would stay
        // silently unscheduled after the app process is killed and restarted, since AlarmManager
        // alarms don't survive that any more than they survive a full device reboot. This reads
        // current settings off the main thread so app startup is never blocked on it.
        CoroutineScope(Dispatchers.Default).launch {
            BedtimeReminderScheduler.rescheduleFromSettings(this@SleepTrackerApp)
        }
    }
}
