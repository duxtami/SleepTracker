package com.sleeptracker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sleeptracker.app.data.local.dao.NoteDao
import com.sleeptracker.app.data.local.dao.SleepSessionDao
import com.sleeptracker.app.data.local.entity.NoteEntity
import com.sleeptracker.app.data.local.entity.SleepSessionEntity

@Database(
    entities = [SleepSessionEntity::class, NoteEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sleepSessionDao(): SleepSessionDao
    abstract fun noteDao(): NoteDao

    companion object {

        /** Adds awake-duration, quality-rating, start-delay and pause tracking columns. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN awakeMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN qualityRating INTEGER")
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN startDelayMinutesUsed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN pausedAtEpochMillis INTEGER")
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN totalPausedMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sleep_tracker.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
    }
}
