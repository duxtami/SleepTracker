package com.sleeptracker.app

import android.app.Application
import com.sleeptracker.app.di.AppContainer
import com.sleeptracker.app.util.NotificationHelper

class SleepTrackerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannel(this)
    }
}
