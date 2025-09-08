package com.womensafety.app.service

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.womensafety.app.data.firebase.FcmToken
import com.womensafety.app.data.firebase.FirestoreCollections
import kotlinx.coroutines.tasks.await

/**
 * Helper class for testing push notification functionality
 */
class PushNotificationTester(private val context: Context) {
    
    private val messaging = FirebaseMessaging.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "PushNotificationTester"
    }
    
    /**
     * Test FCM token generation and storage
     */
    suspend fun testFcmTokenGeneration(userId: String): Result<String> {
        return try {
            // Get FCM token
            val token = messaging.token.await()
            Log.d(TAG, "FCM Token generated: $token")
            
            // Save token to Firestore
            val fcmToken = FcmToken(
                token = token,
                userId = userId,
                deviceId = android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ),
                platform = "android",
                appVersion = getAppVersion(),
                createdAt = com.google.firebase.Timestamp.now(),
                lastUsed = com.google.firebase.Timestamp.now(),
                isActive = true
            )
            
            firestore.collection(FirestoreCollections.FCM_TOKENS)
                .document(token)
                .set(fcmToken)
                .await()
            
            Log.d(TAG, "FCM token saved to Firestore")
            Result.success(token)
            
        } catch (e: Exception) {
            Log.e(TAG, "FCM token generation failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Test topic subscription
     */
    suspend fun testTopicSubscription(): Result<List<String>> {
        return try {
            val topics = listOf(
                "safety_alerts",
                "community_alerts_general",
                "emergency_broadcasts",
                "safety_tips"
            )
            
            for (topic in topics) {
                messaging.subscribeToTopic(topic).await()
                Log.d(TAG, "Subscribed to topic: $topic")
            }
            
            Result.success(topics)
            
        } catch (e: Exception) {
            Log.e(TAG, "Topic subscription failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Test unsubscribing from topics
     */
    suspend fun testTopicUnsubscription(): Result<List<String>> {
        return try {
            val topics = listOf(
                "safety_alerts",
                "community_alerts_general",
                "emergency_broadcasts",
                "safety_tips"
            )
            
            for (topic in topics) {
                messaging.unsubscribeFromTopic(topic).await()
                Log.d(TAG, "Unsubscribed from topic: $topic")
            }
            
            Result.success(topics)
            
        } catch (e: Exception) {
            Log.e(TAG, "Topic unsubscription failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create test notification data in Firestore
     * This simulates what your backend would send
     */
    suspend fun createTestNotificationData(): Result<String> {
        return try {
            val testNotification = hashMapOf(
                "type" to "test_notification",
                "title" to "Firebase Test Notification",
                "body" to "This is a test notification from your Women Safety app",
                "data" to hashMapOf(
                    "test_id" to "test_${System.currentTimeMillis()}",
                    "priority" to "high",
                    "sound" to "default"
                ),
                "timestamp" to com.google.firebase.Timestamp.now(),
                "sent_by" to "system_test"
            )
            
            val docRef = firestore.collection("test_notifications")
                .add(testNotification)
                .await()
            
            Log.d(TAG, "Test notification data created with ID: ${docRef.id}")
            Result.success(docRef.id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create test notification data", e)
            Result.failure(e)
        }
    }
    
    /**
     * Test SOS alert notification structure
     */
    suspend fun createTestSosAlert(userId: String): Result<String> {
        return try {
            val sosAlert = hashMapOf(
                "type" to "sos_alert",
                "title" to "🚨 Emergency Alert",
                "body" to "Test SOS alert triggered - This is a test",
                "data" to hashMapOf(
                    "user_id" to userId,
                    "latitude" to "40.7128",
                    "longitude" to "-74.0060",
                    "address" to "New York, NY",
                    "sos_type" to "manual",
                    "severity" to "high",
                    "timestamp" to System.currentTimeMillis().toString()
                ),
                "priority" to "high",
                "sound" to "emergency",
                "vibrate" to true,
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            
            val docRef = firestore.collection("test_sos_alerts")
                .add(sosAlert)
                .await()
            
            Log.d(TAG, "Test SOS alert created with ID: ${docRef.id}")
            Result.success(docRef.id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create test SOS alert", e)
            Result.failure(e)
        }
    }
    
    /**
     * Test community alert notification
     */
    suspend fun createTestCommunityAlert(): Result<String> {
        return try {
            val communityAlert = hashMapOf(
                "type" to "community_alert",
                "title" to "⚠️ Community Safety Alert",
                "body" to "Test community alert in your area - This is a test",
                "data" to hashMapOf(
                    "alert_type" to "safety_concern",
                    "latitude" to "40.7128",
                    "longitude" to "-74.0060",
                    "radius" to "1000",
                    "severity" to "medium",
                    "description" to "Test community safety alert"
                ),
                "priority" to "default",
                "timestamp" to com.google.firebase.Timestamp.now()
            )
            
            val docRef = firestore.collection("test_community_alerts")
                .add(communityAlert)
                .await()
            
            Log.d(TAG, "Test community alert created with ID: ${docRef.id}")
            Result.success(docRef.id)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create test community alert", e)
            Result.failure(e)
        }
    }
    
    /**
     * Verify notification permissions
     */
    fun checkNotificationPermissions(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are enabled by default
            true
        }
    }
    
    /**
     * Get notification settings info
     */
    fun getNotificationInfo(): Map<String, Any> {
        val notificationManager = androidx.core.app.NotificationManagerCompat.from(context)
        
        return mapOf(
            "notifications_enabled" to notificationManager.areNotificationsEnabled(),
            "permission_granted" to checkNotificationPermissions(),
            "channels_created" to getNotificationChannels(),
            "android_version" to android.os.Build.VERSION.SDK_INT,
            "app_version" to getAppVersion()
        )
    }
    
    /**
     * Get created notification channels
     */
    private fun getNotificationChannels(): List<String> {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) 
                as android.app.NotificationManager
            notificationManager.notificationChannels.map { it.id }
        } else {
            emptyList()
        }
    }
    
    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    
    /**
     * Clean up test data
     */
    suspend fun cleanupTestData(): Result<Unit> {
        return try {
            // Delete test notifications
            val testNotifications = firestore.collection("test_notifications").get().await()
            for (doc in testNotifications.documents) {
                doc.reference.delete().await()
            }
            
            // Delete test SOS alerts
            val testSosAlerts = firestore.collection("test_sos_alerts").get().await()
            for (doc in testSosAlerts.documents) {
                doc.reference.delete().await()
            }
            
            // Delete test community alerts
            val testCommunityAlerts = firestore.collection("test_community_alerts").get().await()
            for (doc in testCommunityAlerts.documents) {
                doc.reference.delete().await()
            }
            
            Log.d(TAG, "Test data cleaned up successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup test data", e)
            Result.failure(e)
        }
    }
}
