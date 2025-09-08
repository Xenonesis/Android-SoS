package com.womensafety.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.womensafety.app.data.model.LocationData

@Dao
interface LocationDao {
    
    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    fun getAllLocations(): LiveData<List<LocationData>>
    
    @Query("SELECT * FROM location_history WHERE isTracking = 1 ORDER BY timestamp DESC")
    fun getTrackingLocations(): LiveData<List<LocationData>>
    
    @Query("SELECT * FROM location_history WHERE isSosLocation = 1 ORDER BY timestamp DESC")
    fun getSosLocations(): LiveData<List<LocationData>>
    
    @Query("SELECT * FROM location_history WHERE id = :id")
    suspend fun getLocationById(id: Long): LocationData?
    
    @Query("SELECT * FROM location_history WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp ASC")
    suspend fun getLocationsByDateRange(startTime: Long, endTime: Long): List<LocationData>
    
    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLocation(): LocationData?
    
    @Query("SELECT * FROM location_history WHERE isTracking = 1 ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentTrackingLocations(limit: Int = 10): List<LocationData>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationData): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationData>)
    
    @Update
    suspend fun updateLocation(location: LocationData)
    
    @Delete
    suspend fun deleteLocation(location: LocationData)
    
    @Query("DELETE FROM location_history WHERE timestamp < :cutoffTime AND isTracking = 0 AND isSosLocation = 0")
    suspend fun deleteOldLocations(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM location_history WHERE timestamp >= :startTime")
    suspend fun getLocationCountSince(startTime: Long): Int
    
    @Query("UPDATE location_history SET isTracking = 0")
    suspend fun clearAllTrackingFlags()
}
