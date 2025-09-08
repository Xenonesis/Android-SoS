package com.womensafety.app.data.firebase

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.*

data class FirebaseUser(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String? = null,
    val fcmToken: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null,
    
    // Emergency settings
    val emergencyContacts: List<String> = emptyList(), // List of contact IDs
    val allowCommunityAlerts: Boolean = true,
    val shareLocationWithContacts: Boolean = true,
    val autoCallEmergencyServices: Boolean = false
)

data class FirebaseSosEvent(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val type: String = "", // manual, shake, panic_button, route_deviation
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val isResolved: Boolean = false,
    val resolvedAt: Date? = null,
    val notes: String? = null,
    val contactsNotified: List<String> = emptyList(),
    val emergencyServicesCalled: Boolean = false,
    val responseTime: Long? = null,
    @ServerTimestamp
    val timestamp: Date? = null
)

data class FirebaseCommunityAlert(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val alertType: String = "", // suspicious_activity, accident, harassment, other
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val isVerified: Boolean = false,
    val verifiedBy: String? = null,
    val isResolved: Boolean = false,
    val resolvedAt: Date? = null,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val reportedBy: List<String> = emptyList(),
    @ServerTimestamp
    val timestamp: Date? = null
)

data class FirebaseLocationShare(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val sharedWithUserId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String? = null,
    val isActive: Boolean = true,
    val expiresAt: Date? = null,
    val tripName: String? = null,
    val estimatedArrival: Date? = null,
    @ServerTimestamp
    val timestamp: Date? = null
)

data class FirebaseEmergencyContact(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String? = null,
    val relationship: String? = null,
    val isPrimary: Boolean = false,
    val isEmergencyService: Boolean = false,
    val canReceiveAlerts: Boolean = true,
    @ServerTimestamp
    val createdAt: Date? = null,
    @ServerTimestamp
    val updatedAt: Date? = null
)

// Response models for real-time updates
data class SosResponse(
    val eventId: String = "",
    val responderId: String = "",
    val responderName: String = "",
    val responderPhone: String? = null,
    val responseType: String = "", // on_way, arrived, false_alarm
    val estimatedArrival: Date? = null,
    val currentLocation: Map<String, Double>? = null,
    @ServerTimestamp
    val timestamp: Date? = null
)

data class SafetyCheckRequest(
    @DocumentId
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val message: String = "",
    val isResponded: Boolean = false,
    val response: String? = null,
    val respondedAt: Date? = null,
    @ServerTimestamp
    val timestamp: Date? = null
)
