package com.womensafety.app.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.womensafety.app.data.model.Invitation
import com.womensafety.app.data.model.SosEvent
import com.womensafety.app.utils.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.*

class FirebaseRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    
    private val usersCollection = firestore.collection(Constants.COLLECTION_USERS)
    private val sosEventsCollection = firestore.collection(Constants.COLLECTION_SOS_EVENTS)
    private val communityAlertsCollection = firestore.collection(Constants.COLLECTION_COMMUNITY_ALERTS)
    private val locationsCollection = firestore.collection(Constants.COLLECTION_LOCATIONS)
    private val invitationsCollection = firestore.collection("invitations")
    
    // User Management
    suspend fun createUser(user: FirebaseUser): Result<String> {
        return try {
            val docRef = usersCollection.add(user).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error creating user", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(userId: String, user: FirebaseUser): Result<Unit> {
        return try {
            usersCollection.document(userId).set(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating user", e)
            Result.failure(e)
        }
    }
    
    suspend fun getUser(userId: String): Result<FirebaseUser?> {
        return try {
            val document = usersCollection.document(userId).get().await()
            val user = document.toObject(FirebaseUser::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error getting user", e)
            Result.failure(e)
        }
    }
    
    // SOS Events
    suspend fun createSosEvent(sosEvent: SosEvent): String {
        val firebaseSosEvent = FirebaseSosEvent(
            userId = auth.currentUser?.uid ?: "",
            type = sosEvent.type,
            latitude = sosEvent.latitude,
            longitude = sosEvent.longitude,
            address = sosEvent.address,
            timestamp = Date(sosEvent.timestamp),
            isResolved = sosEvent.isResolved,
            resolvedAt = sosEvent.resolvedAt?.let { Date(it) },
            notes = sosEvent.notes,
            contactsNotified = sosEvent.contactsNotified
        )
        val result = createSosEvent(firebaseSosEvent)
        if (result.isSuccess) {
            return result.getOrThrow()
        } else {
            throw result.exceptionOrNull()!!
        }
    }

    private suspend fun createSosEvent(sosEvent: FirebaseSosEvent): Result<String> {
        return try {
            val docRef = sosEventsCollection.add(sosEvent).await()
            
            // Send push notifications to emergency contacts
            sendSosNotifications(sosEvent, docRef.id)
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error creating SOS event", e)
            Result.failure(e)
        }
    }
    
    suspend fun updateSosEvent(eventId: String, sosEvent: FirebaseSosEvent): Result<Unit> {
        return try {
            sosEventsCollection.document(eventId).set(sosEvent).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating SOS event", e)
            Result.failure(e)
        }
    }
    
    fun observeSosEvents(userId: String): Flow<List<FirebaseSosEvent>> = callbackFlow {
        val listener = sosEventsCollection
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepository", "Error observing SOS events", error)
                    return@addSnapshotListener
                }
                
                val events = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseSosEvent::class.java)
                } ?: emptyList()
                
                trySend(events)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Invitations
    suspend fun createInvitation(invitation: Invitation): Result<String> {
        return try {
            val docRef = invitationsCollection.add(invitation).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error creating invitation", e)
            Result.failure(e)
        }
    }

    // Community Alerts
    suspend fun createCommunityAlert(alert: FirebaseCommunityAlert): Result<String> {
        return try {
            val docRef = communityAlertsCollection.add(alert).await()
            
            // Send notifications to nearby users
            sendCommunityAlertNotifications(alert, docRef.id)
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error creating community alert", e)
            Result.failure(e)
        }
    }
    
    fun observeNearbyCommunityAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 5.0
    ): Flow<List<FirebaseCommunityAlert>> = callbackFlow {
        // Note: For production, you'd want to use geohashing or Firebase's geospatial queries
        val listener = communityAlertsCollection
            .whereEqualTo("isResolved", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepository", "Error observing community alerts", error)
                    return@addSnapshotListener
                }
                
                val alerts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseCommunityAlert::class.java)
                }?.filter { alert ->
                    // Simple distance calculation (for production, use proper geospatial queries)
                    val distance = calculateDistance(
                        latitude, longitude,
                        alert.latitude, alert.longitude
                    )
                    distance <= radiusKm
                } ?: emptyList()
                
                trySend(alerts)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Location Sharing
    suspend fun shareLocation(locationShare: FirebaseLocationShare): Result<String> {
        return try {
            val docRef = locationsCollection.add(locationShare).await()
            
            // Send notification to shared user
            sendLocationShareNotification(locationShare, docRef.id)
            
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error sharing location", e)
            Result.failure(e)
        }
    }
    
    fun observeSharedLocations(userId: String): Flow<List<FirebaseLocationShare>> = callbackFlow {
        val listener = locationsCollection
            .whereEqualTo("sharedWithUserId", userId)
            .whereEqualTo("isActive", true)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseRepository", "Error observing shared locations", error)
                    return@addSnapshotListener
                }
                
                val locations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(FirebaseLocationShare::class.java)
                } ?: emptyList()
                
                trySend(locations)
            }
        
        awaitClose { listener.remove() }
    }
    
    // FCM Token Management
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            usersCollection.document(userId)
                .update("fcmToken", token, "updatedAt", Date())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error updating FCM token", e)
            Result.failure(e)
        }
    }
    
    // Private helper methods
    private suspend fun sendSosNotifications(sosEvent: FirebaseSosEvent, eventId: String) {
        try {
            // Get user's emergency contacts
            val user = getUser(sosEvent.userId).getOrNull()
            user?.emergencyContacts?.forEach { contactId ->
                val contact = getUser(contactId).getOrNull()
                contact?.fcmToken?.let { token ->
                    sendPushNotification(
                        token = token,
                        title = "🚨 Emergency Alert",
                        body = "Your emergency contact needs help!",
                        data = mapOf(
                            "type" to "sos_alert",
                            "event_id" to eventId,
                            "user_id" to sosEvent.userId,
                            "location" to "${sosEvent.latitude},${sosEvent.longitude}"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error sending SOS notifications", e)
        }
    }
    
    private suspend fun sendCommunityAlertNotifications(alert: FirebaseCommunityAlert, alertId: String) {
        // TODO: Implement geospatial query to find nearby users and send notifications
    }
    
    private suspend fun sendLocationShareNotification(locationShare: FirebaseLocationShare, shareId: String) {
        try {
            val sharedWithUser = getUser(locationShare.sharedWithUserId).getOrNull()
            val sharingUser = getUser(locationShare.userId).getOrNull()
            
            sharedWithUser?.fcmToken?.let { token ->
                sendPushNotification(
                    token = token,
                    title = "Location Shared",
                    body = "${sharingUser?.name} is sharing their location with you",
                    data = mapOf(
                        "type" to "location_share",
                        "share_id" to shareId,
                        "user_id" to locationShare.userId
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Error sending location share notification", e)
        }
    }
    
    private suspend fun sendPushNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        // TODO: Implement server-side push notification sending
        // This would typically be done through your backend server
        // using Firebase Admin SDK
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
}
