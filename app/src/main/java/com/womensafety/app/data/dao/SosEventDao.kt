package com.womensafety.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.womensafety.app.data.model.SosEvent

@Dao
interface SosEventDao {
    
    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC")
    fun getAllSosEvents(): LiveData<List<SosEvent>>
    
    @Query("SELECT * FROM sos_events WHERE isResolved = 0 ORDER BY timestamp DESC")
    fun getActiveSosEvents(): LiveData<List<SosEvent>>
    
    @Query("SELECT * FROM sos_events WHERE id = :id")
    suspend fun getSosEventById(id: Long): SosEvent?
    
    @Query("SELECT * FROM sos_events WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getSosEventsByDateRange(startTime: Long, endTime: Long): List<SosEvent>
    
    @Query("SELECT * FROM sos_events WHERE type = :type ORDER BY timestamp DESC")
    suspend fun getSosEventsByType(type: String): List<SosEvent>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSosEvent(sosEvent: SosEvent): Long
    
    @Update
    suspend fun updateSosEvent(sosEvent: SosEvent)
    
    @Delete
    suspend fun deleteSosEvent(sosEvent: SosEvent)
    
    @Query("UPDATE sos_events SET isResolved = 1, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun resolveSosEvent(id: Long, resolvedAt: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM sos_events WHERE timestamp >= :startTime")
    suspend fun getSosEventCountSince(startTime: Long): Int
    
    @Query("SELECT * FROM sos_events ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSosEvent(): SosEvent?
    
    @Query("DELETE FROM sos_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSosEvents(cutoffTime: Long)
}
