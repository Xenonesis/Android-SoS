package com.womensafety.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sos_events")
data class SosEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // manual, shake, panic_button, route_deviation
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val resolvedAt: Long? = null,
    val notes: String? = null,
    val contactsNotified: List<String> = emptyList(), // JSON array of contact IDs
    val emergencyServicesCalled: Boolean = false,
    val responseTime: Long? = null // Time taken for help to arrive
)

enum class SosEventType(val value: String) {
    MANUAL("manual"),
    SHAKE("shake"),
    PANIC_BUTTON("panic_button"),
    ROUTE_DEVIATION("route_deviation")
}

data class SosEventWithLocation(
    val event: SosEvent,
    val locationName: String?,
    val distanceFromHome: Float?
)
