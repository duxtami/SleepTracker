package com.sleeptracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.sleeptracker.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: NoteEntity): Long

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE sessionId = :sessionId ORDER BY createdAtEpochMillis DESC")
    fun observeForSession(sessionId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Long)
}
