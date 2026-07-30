package com.sleeptracker.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SleepSessionDto(
    val startEpochMillis: Long,
    val endEpochMillis: Long?,
    val timeZoneId: String,
    val mood: String?,
    val notes: String,
    val tags: List<String>,
    val qualityRating: Int? = null,
    val startDelayMinutesUsed: Int = 0
)

fun SleepSession.toDto(): SleepSessionDto = SleepSessionDto(
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    timeZoneId = timeZoneId,
    mood = mood?.name,
    notes = notes,
    tags = tags,
    qualityRating = qualityRating,
    startDelayMinutesUsed = startDelayMinutesUsed
)

fun SleepSessionDto.toDomain(): SleepSession = SleepSession(
    startEpochMillis = startEpochMillis,
    endEpochMillis = endEpochMillis,
    timeZoneId = timeZoneId,
    mood = Mood.fromNameOrNull(mood),
    notes = notes,
    tags = tags,
    isManualEntry = true,
    qualityRating = qualityRating,
    startDelayMinutesUsed = startDelayMinutesUsed
)
