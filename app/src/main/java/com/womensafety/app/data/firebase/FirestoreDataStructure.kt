package com.womensafety.app.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

/**
 * Data classes representing Firestore document structures
 */

// User Profile
data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val lastActiveAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true,
    val emergencyContacts: List<String> = emptyList(), // List of contact IDs
    val settings: UserSettings = UserSettings(),
    val fcmToken: String? = null,
    val location: UserLocation? = null
)

// User Settings
data class UserSettings(
    val shakeEnabled: Boolean = true,
    val autoCallEnabled: Boolean = true,
    val communityAlertsEnabled: Boolean = true,
    val shakeSensitivity: Float = 2.7f,
    val sosCountdown: Int = 5,
    val shareLocationWithContacts: Boolean = true,
    val allowCommunityAlerts: Boolean = true,
    val silentMode: Boolean = false,
    val emergencyNumber: String = "911"
)

// User Location
data class UserLocation(
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val lastUpdated: Timestamp = Timestamp.now()
)

// Emergency Contact
data class EmergencyContact(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val email: String? = null,
    val relationship: String = "", // "Family", "Friend", "Colleague", etc.
    val isPrimary: Boolean = false,
    val isVerified: Boolean = false,
    val addedAt: Timestamp = Timestamp.now()
)

// SOS Event
data class SosEvent(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // "manual", "shake", "panic_button", "route_deviation"
    val status: String = "active", // "active", "resolved", "cancelled", "false_alarm"
    val severity: String = "high", // "low", "medium", "high", "critical"
    val timestamp: Timestamp = Timestamp.now(),
    val resolvedAt: Timestamp? = null,
    val location: SosLocation = SosLocation(),
    val notifiedContacts: List<String> = emptyList(),
    val notificationsSent: List<NotificationRecord> = emptyList(),
    val userInfo: SosUserInfo = SosUserInfo(),
    val notes: String = "",
    val evidence: List<Evidence> = emptyList(),
    val responseTime: Long? = null, // Time to resolution in milliseconds
    val isTestAlert: Boolean = false
)

// SOS Location
data class SosLocation(
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val address: String = "",
    val accuracy: Float = 0f,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val speed: Float? = null,
    val timestamp: Timestamp = Timestamp.now()
)

// SOS User Info (snapshot at time of SOS)
data class SosUserInfo(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val profileImageUrl: String? = null
)

// Notification Record
data class NotificationRecord(
    val contactId: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val method: String = "", // "sms", "call", "push", "email"
    val status: String = "", // "sent", "delivered", "failed", "read"
    val sentAt: Timestamp = Timestamp.now(),
    val deliveredAt: Timestamp? = null,
    val readAt: Timestamp? = null,
    val errorMessage: String? = null
)

// Evidence (photos, audio, video)
data class Evidence(
    val id: String = "",
    val type: String = "", // "photo", "audio", "video", "text"
    val url: String = "",
    val filename: String = "",
    val size: Long = 0,
    val mimeType: String = "",
    val uploadedAt: Timestamp = Timestamp.now(),
    val description: String = ""
)

// Community Alert
data class CommunityAlert(
    val id: String = "",
    val createdBy: String = "",
    val creatorName: String = "",
    val type: String = "", // "safety_concern", "suspicious_activity", "incident", "warning"
    val severity: String = "medium", // "low", "medium", "high", "critical"
    val status: String = "active", // "active", "resolved", "verified", "dismissed"
    val title: String = "",
    val description: String = "",
    val location: SosLocation = SosLocation(),
    val radius: Double = 1000.0, // Alert radius in meters
    val timestamp: Timestamp = Timestamp.now(),
    val resolvedAt: Timestamp? = null,
    val verifiedBy: List<String> = emptyList(), // User IDs who verified
    val reportedBy: List<String> = emptyList(), // User IDs who reported as false
    val evidence: List<Evidence> = emptyList(),
    val tags: List<String> = emptyList(),
    val isAnonymous: Boolean = false
)

// Location History
data class LocationHistory(
    val id: String = "",
    val userId: String = "",
    val location: SosLocation = SosLocation(),
    val activityType: String = "unknown", // "still", "walking", "running", "driving", "cycling"
    val confidence: Float = 0f,
    val sosEventId: String? = null, // If related to an SOS event
    val isShared: Boolean = false,
    val sharedWith: List<String> = emptyList() // Contact IDs
)

// Safety Tip
data class SafetyTip(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val category: String = "", // "general", "travel", "home", "workplace", "digital"
    val priority: Int = 0, // 0-10, higher is more important
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val videoUrl: String? = null,
    val externalLink: String? = null
)

// User Report
data class UserReport(
    val id: String = "",
    val reportedBy: String = "",
    val reportedUser: String? = null,
    val reportedContent: String? = null, // Alert ID, tip ID, etc.
    val type: String = "", // "spam", "inappropriate", "false_alert", "harassment"
    val reason: String = "",
    val description: String = "",
    val status: String = "pending", // "pending", "reviewed", "resolved", "dismissed"
    val timestamp: Timestamp = Timestamp.now(),
    val reviewedAt: Timestamp? = null,
    val reviewedBy: String? = null,
    val resolution: String = ""
)

// FCM Token
data class FcmToken(
    val token: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val platform: String = "android",
    val appVersion: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val lastUsed: Timestamp = Timestamp.now(),
    val isActive: Boolean = true
)

// App Settings (global configuration)
data class AppSettings(
    val id: String = "",
    val emergencyNumbers: Map<String, String> = mapOf(
        "police" to "911",
        "fire" to "911",
        "medical" to "911"
    ),
    val sosCountdownDefault: Int = 5,
    val maxEmergencyContacts: Int = 10,
    val communityAlertRadius: Double = 5000.0, // 5km default
    val maintenanceMode: Boolean = false,
    val minAppVersion: String = "1.0.0",
    val supportEmail: String = "support@womensafety.com",
    val privacyPolicyUrl: String = "",
    val termsOfServiceUrl: String = "",
    val lastUpdated: Timestamp = Timestamp.now()
)

/**
 * Firestore collection names
 */
object FirestoreCollections {
    const val USERS = "users"
    const val SOS_EVENTS = "sos_events"
    const val COMMUNITY_ALERTS = "community_alerts"
    const val LOCATION_HISTORY = "location_history"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val SAFETY_TIPS = "safety_tips"
    const val USER_REPORTS = "user_reports"
    const val FCM_TOKENS = "fcm_tokens"
    const val APP_SETTINGS = "app_settings"
}

/**
 * Firestore subcollection names
 */
object FirestoreSubcollections {
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val LOCATION_HISTORY = "location_history"
    const val NOTIFICATIONS = "notifications"
    const val EVIDENCE = "evidence"
}
