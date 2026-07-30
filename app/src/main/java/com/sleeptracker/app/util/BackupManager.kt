package com.sleeptracker.app.util

import android.content.Context
import android.net.Uri
import com.sleeptracker.app.data.model.SleepSession
import com.sleeptracker.app.data.model.SleepSessionDto
import com.sleeptracker.app.data.model.toDomain
import com.sleeptracker.app.data.model.toDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Reads/writes sleep session backups as CSV or JSON through the Storage Access Framework. */
object BackupManager {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private const val CSV_HEADER = "start_epoch_millis,end_epoch_millis,time_zone,mood,notes,tags,quality_rating,start_delay_minutes_used"

    suspend fun exportCsv(context: Context, uri: Uri, sessions: List<SleepSession>) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.bufferedWriter().use { writer ->
                writer.appendLine(CSV_HEADER)
                sessions.filter { !it.isActive }.forEach { s ->
                    val tags = s.tags.joinToString("|")
                    val notes = s.notes.replace("\"", "'").replace("\n", " ")
                    writer.appendLine(
                        "${s.startEpochMillis},${s.endEpochMillis ?: ""},${s.timeZoneId},${s.mood?.name ?: ""},\"$notes\",\"$tags\"," +
                            "${s.qualityRating ?: ""},${s.startDelayMinutesUsed}"
                    )
                }
            }
        }
    }

    suspend fun exportJson(context: Context, uri: Uri, sessions: List<SleepSession>) = withContext(Dispatchers.IO) {
        val dtos = sessions.filter { !it.isActive }.map { it.toDto() }
        val text = json.encodeToString(dtos)
        context.contentResolver.openOutputStream(uri)?.use { it.write(text.toByteArray()) }
    }

    suspend fun importJson(context: Context, uri: Uri): List<SleepSession> = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return@withContext emptyList()
        val dtos = json.decodeFromString<List<SleepSessionDto>>(text)
        dtos.map { it.toDomain() }
    }

    suspend fun importCsv(context: Context, uri: Uri): List<SleepSession> = withContext(Dispatchers.IO) {
        val lines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readLines() } ?: return@withContext emptyList()
        if (lines.isEmpty()) return@withContext emptyList()
        lines.drop(1).mapNotNull { line ->
            runCatching {
                val cols = parseCsvLine(line)
                if (cols.size < 6) return@runCatching null
                val start = cols[0].toLong()
                val end = cols[1].toLongOrNull()
                val zone = cols[2].ifBlank { java.time.ZoneId.systemDefault().id }
                val mood = com.sleeptracker.app.data.model.Mood.fromNameOrNull(cols[3].ifBlank { null })
                val notes = cols[4]
                val tags = cols[5].split("|").map { it.trim() }.filter { it.isNotBlank() }
                val qualityRating = cols.getOrNull(6)?.toIntOrNull()
                val startDelayUsed = cols.getOrNull(7)?.toIntOrNull() ?: 0
                SleepSession(
                    startEpochMillis = start,
                    endEpochMillis = end,
                    timeZoneId = zone,
                    mood = mood,
                    notes = notes,
                    tags = tags,
                    isManualEntry = true,
                    qualityRating = qualityRating,
                    startDelayMinutesUsed = startDelayUsed
                )
            }.getOrNull()
        }
    }

    /** Minimal CSV parser handling quoted fields with commas, since our export can quote notes/tags. */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString()); current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
