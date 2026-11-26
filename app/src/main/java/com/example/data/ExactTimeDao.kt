package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExactAlarmDao {
    @Query("SELECT * FROM exact_alarms ORDER BY isEnabled DESC, hour ASC, minute ASC, second ASC")
    fun observeAllAlarms(): Flow<List<ExactAlarmEntity>>

    @Query("SELECT * FROM exact_alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarms(): List<ExactAlarmEntity>

    @Query("SELECT * FROM exact_alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): ExactAlarmEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: ExactAlarmEntity): Long

    @Update
    suspend fun updateAlarm(alarm: ExactAlarmEntity)

    @Query("DELETE FROM exact_alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)
}

@Dao
interface CountdownEventDao {
    @Query("SELECT * FROM countdown_events ORDER BY targetEpochMillis ASC")
    fun observeAllEvents(): Flow<List<CountdownEventEntity>>

    @Query("SELECT * FROM countdown_events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: Long): CountdownEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CountdownEventEntity): Long

    @Query("DELETE FROM countdown_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)
}
