package com.sleeptracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sleeptracker.app.data.local.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(session: SleepSessionEntity): Long

    @Update
    suspend fun update(session: SleepSessionEntity)

    @Delete
    suspend fun delete(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions ORDER BY startEpochMillis DESC")
    fun observeAll(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE endEpochMillis IS NULL LIMIT 1")
    fun observeActiveSession(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE endEpochMillis IS NULL LIMIT 1")
    suspend fun getActiveSessionOnce(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun getById(id: Long): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE endEpochMillis IS NOT NULL ORDER BY startEpochMillis DESC LIMIT 1")
    fun observeLastCompletedSession(): Flow<SleepSessionEntity?>

    @Query(
        "SELECT * FROM sleep_sessions WHERE endEpochMillis IS NOT NULL AND startEpochMillis >= :fromEpochMillis ORDER BY startEpochMillis DESC"
    )
    fun observeSince(fromEpochMillis: Long): Flow<List<SleepSessionEntity>>

    @Query("DELETE FROM sleep_sessions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM sleep_sessions WHERE endEpochMillis IS NULL")
    suspend fun activeSessionCount(): Int
}
