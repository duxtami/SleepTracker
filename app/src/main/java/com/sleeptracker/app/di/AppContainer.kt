package com.sleeptracker.app.di

import android.content.Context
import com.sleeptracker.app.data.datastore.SettingsRepository
import com.sleeptracker.app.data.datastore.TrackingPrefsRepository
import com.sleeptracker.app.data.local.AppDatabase
import com.sleeptracker.app.data.repository.SleepRepository

/**
 * Hand-rolled dependency container. The app intentionally avoids Hilt/Dagger:
 * one fewer annotation-processing toolchain keeps first-time Gradle builds in
 * constrained environments (like Termux) fast and predictable.
 */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    private val database = AppDatabase.getInstance(appContext)

    val sleepRepository: SleepRepository by lazy {
        SleepRepository(database.sleepSessionDao(), database.noteDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext)
    }

    val trackingPrefsRepository: TrackingPrefsRepository by lazy {
        TrackingPrefsRepository(appContext)
    }
}
