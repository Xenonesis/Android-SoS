package com.womensafety.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_history")
data class LocationData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val address: String? = null,
    val isTracking: Boolean = false,
    val isSosLocation: Boolean = false
)

data class LocationWithDistance(
    val location: LocationData,
    val distanceFromPrevious: Float = 0f,
    val timeDifference: Long = 0L
)

data class RouteInfo(
    val startLocation: LocationData,
    val endLocation: LocationData?,
    val currentLocation: LocationData,
    val plannedRoute: List<LocationData> = emptyList(),
    val actualRoute: List<LocationData> = emptyList(),
    val deviationDistance: Float = 0f,
    val isDeviated: Boolean = false,
    val estimatedArrival: Long? = null
)
