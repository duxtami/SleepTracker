package com.sleeptracker.app.data.repository

import com.sleeptracker.app.data.local.dao.NoteDao
import com.sleeptracker.app.data.local.dao.SleepSessionDao
import com.sleeptracker.app.data.local.entity.NoteEntity
import com.sleeptracker.app.data.local.entity.toDomain
import com.sleeptracker.app.data.local.entity.toEntity
import com.sleeptracker.app.data.model.Mood
import com.sleeptracker.app.data.model.SleepSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId

/** Thrown when the caller tries to start a session while one is already active. */
class SessionAlreadyActiveException : Exception("A sleep session is already in progress.")

/** Thrown when trying to end/edit a session that no longer exists. */
class SessionNotFoundException : Exception("Sleep session not found.")

class SleepRepository(
    private val sessionDao: SleepSessionDao,
    private val noteDao: NoteDao
) {

    fun observeAllSessions(): Flow<List<SleepSession>> =
        sessionDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeActiveSession(): Flow<SleepSession?> =
        sessionDao.observeActiveSession().map { it?.toDomain() }

    fun observeLastCompletedSession(): Flow<SleepSession?> =
        sessionDao.observeLastCompletedSession().map { it?.toDomain() }

    fun observeSessionsSince(fromEpochMillis: Long): Flow<List<SleepSession>> =
        sessionDao.observeSince(fromEpochMillis).map { list -> list.map { it.toDomain() } }

    suspend fun getSessionById(id: Long): SleepSession? = sessionDao.getById(id)?.toDomain()

    /**
     * Starts a brand-new tracked session. Fails if one is already active (prevents overlap).
     * [startDelayMinutesUsed] records the Start Time Delay chosen for this session, if any -
     * [startEpochMillis] should already reflect the delayed planned start time.
     */
    suspend fun startSession(
        startEpochMillis: Long = System.currentTimeMillis(),
        startDelayMinutesUsed: Int = 0
    ): Long {
        if (sessionDao.activeSessionCount() > 0) throw SessionAlreadyActiveException()
        val entity = SleepSession(
            startEpochMillis = startEpochMillis,
            endEpochMillis = null,
            timeZoneId = ZoneId.systemDefault().id,
            mood = null,
            notes = "",
            tags = emptyList(),
            isManualEntry = false,
            startDelayMinutesUsed = startDelayMinutesUsed
        ).toEntity()
        return sessionDao.insert(entity)
    }

    /** Ends the currently active session, if any, stamping the end time and optional mood/notes/quality. */
    suspend fun endActiveSession(
        endEpochMillis: Long = System.currentTimeMillis(),
        mood: Mood? = null,
        notes: String? = null,
        qualityRating: Int? = null
    ) {
        val active = sessionDao.getActiveSessionOnce() ?: throw SessionNotFoundException()
        // If the session was paused right up until the end, fold that final paused span into
        // totalPausedMillis so the net sleep-duration calculation stays correct.
        val trailingPausedMillis = active.pausedAtEpochMillis?.let { pausedAt ->
            (endEpochMillis - pausedAt).coerceAtLeast(0L)
        } ?: 0L
        val finalPaused = active.totalPausedMillis + trailingPausedMillis
        val updated = active.copy(
            endEpochMillis = endEpochMillis,
            mood = mood?.name ?: active.mood,
            notes = notes ?: active.notes,
            qualityRating = qualityRating ?: active.qualityRating,
            pausedAtEpochMillis = null,
            totalPausedMillis = finalPaused
        )
        sessionDao.update(updated)
    }

    /** Marks the active session as paused. No-ops if already paused. */
    suspend fun pauseActiveSession(atEpochMillis: Long = System.currentTimeMillis()) {
        val active = sessionDao.getActiveSessionOnce() ?: throw SessionNotFoundException()
        if (active.pausedAtEpochMillis != null) return
        sessionDao.update(active.copy(pausedAtEpochMillis = atEpochMillis))
    }

    /** Resumes a paused active session, folding the paused span into totalPausedMillis. */
    suspend fun resumeActiveSession(atEpochMillis: Long = System.currentTimeMillis()) {
        val active = sessionDao.getActiveSessionOnce() ?: throw SessionNotFoundException()
        val pausedAt = active.pausedAtEpochMillis ?: return
        val pausedSpan = (atEpochMillis - pausedAt).coerceAtLeast(0L)
        sessionDao.update(
            active.copy(
                pausedAtEpochMillis = null,
                totalPausedMillis = active.totalPausedMillis + pausedSpan
            )
        )
    }

    /** Full manual create/edit path used by the Timeline screen, manual-entry flows, and undo-delete restores. */
    suspend fun upsertSession(session: SleepSession) {
        if (session.id == 0L) {
            // Guard against overlap only for genuinely new sessions with no end time.
            if (session.endEpochMillis == null && sessionDao.activeSessionCount() > 0) {
                throw SessionAlreadyActiveException()
            }
            sessionDao.insert(session.toEntity())
        } else {
            sessionDao.update(session.toEntity())
        }
    }

    suspend fun deleteSession(session: SleepSession) {
        sessionDao.delete(session.toEntity())
    }

    /** Re-inserts a previously deleted session as a new row - used to power the Timeline undo-delete Snackbar. */
    suspend fun restoreSession(session: SleepSession) {
        sessionDao.insert(session.copy(id = 0L).toEntity())
    }

    suspend fun addNote(sessionId: Long, text: String) {
        noteDao.insert(NoteEntity(sessionId = sessionId, createdAtEpochMillis = System.currentTimeMillis(), text = text))
    }

    fun observeNotesForSession(sessionId: Long): Flow<List<NoteEntity>> = noteDao.observeForSession(sessionId)

    suspend fun clearAllData() {
        sessionDao.deleteAll()
    }

    suspend fun importSessions(sessions: List<SleepSession>) {
        sessions.forEach { session ->
            sessionDao.insert(session.copy(id = 0L).toEntity())
        }
    }
}
