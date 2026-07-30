package com.sleeptracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sleeptracker.app.data.model.Mood
import com.sleeptracker.app.data.model.SleepSession

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val timeZoneId: String,
    val mood: String?,
    val notes: String,
    val tags: List<String>,
    val isManualEntry: Boolean,
    // Unused legacy column, kept only so existing installs don't need a destructive migration.
    // No longer read or written by the app - the feature it powered has been removed.
    val awakeMinutes: Int = 0,
    // 1-5 star sleep quality rating, independent of mood.
    val qualityRating: Int? = null,
    // The Start Time Delay (minutes) that was used when this session was started, for record keeping.
    val startDelayMinutesUsed: Int = 0,
    // Epoch millis of when the session was most recently paused, null if not currently paused.
    val pausedAtEpochMillis: Long? = null,
    // Total accumulated paused time across the whole session, in millis.
    val totalPausedMillis: Long = 0L
)

fun SleepSessionEntity.toDomain(): SleepSession = SleepSession(
    id = id,
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    timeZoneId = timeZoneId,
    mood = Mood.fromNameOrNull(mood),
    notes = notes,
    tags = tags,
    isManualEntry = isManualEntry,
    qualityRating = qualityRating,
    startDelayMinutesUsed = startDelayMinutesUsed,
    pausedAtEpochMillis = pausedAtEpochMillis,
    totalPausedMillis = totalPausedMillis
)

fun SleepSession.toEntity(): SleepSessionEntity = SleepSessionEntity(
    id = id,
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    timeZoneId = timeZoneId,
    mood = mood?.name,
    notes = notes,
    tags = tags,
    isManualEntry = isManualEntry,
    qualityRating = qualityRating,
    startDelayMinutesUsed = startDelayMinutesUsed,
    pausedAtEpochMillis = pausedAtEpochMillis,
    totalPausedMillis = totalPausedMillis
)
