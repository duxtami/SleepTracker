package com.sleeptracker.app.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Serializes a List<String> (used for tags) into a single JSON column and back. */
class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromTagList(tags: List<String>): String = json.encodeToString(tags)

    @TypeConverter
    fun toTagList(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else runCatching {
            json.decodeFromString<List<String>>(raw)
        }.getOrDefault(emptyList())
}
