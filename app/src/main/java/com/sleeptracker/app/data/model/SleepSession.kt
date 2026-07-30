package com.sleeptracker.app.data.model

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Domain-level representation of a single sleep session.
 * [endEpochMillis] is null while the session is actively being tracked.
 */
data class SleepSession(
    val id: Long = 0L,
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val timeZoneId: String,
    val mood: Mood?,
    val notes: String,
    val tags: List<String>,
    val isManualEntry: Boolean,
    val qualityRating: Int? = null,
    val startDelayMinutesUsed: Int = 0,
    val pausedAtEpochMillis: Long? = null,
    val totalPausedMillis: Long = 0L
) {
    val isActive: Boolean get() = endEpochMillis == null

    val isPaused: Boolean get() = pausedAtEpochMillis != null

    /** Raw elapsed time from start to end (or now), excluding any paused time. */
    val durationMillis: Long
        get() {
            val end = endEpochMillis ?: pausedAtEpochMillis ?: System.currentTimeMillis()
            return (end - startEpochMillis - totalPausedMillis).coerceAtLeast(0L)
        }

    val duration: Duration get() = Duration.ofMillis(durationMillis)

    val startInstant: Instant get() = Instant.ofEpochMilli(startEpochMillis)

    val endInstant: Instant? get() = endEpochMillis?.let { Instant.ofEpochMilli(it) }

    val zone: ZoneId get() = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
}
