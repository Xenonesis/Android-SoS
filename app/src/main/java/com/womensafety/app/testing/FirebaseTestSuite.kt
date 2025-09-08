package com.womensafety.app.testing

import android.content.Context
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.womensafety.app.data.firebase.*
import com.womensafety.app.service.PushNotificationTester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Comprehensive Firebase test suite for all configured services
 */
class FirebaseTestSuite(private val context: Context, private val scope: CoroutineScope) {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    private val pushTester = PushNotificationTester(context)
    private val authProvider = AuthProviderConfig(context)
    
    companion object {
        private const val TAG = "FirebaseTestSuite"
    }
    
    /**
     * Run all Firebase tests
     */
    fun runAllTests(onResult: (TestResult) -> Unit) {
        scope.launch {
            val results = mutableListOf<TestResult>()
            
            // Test 1: Firebase Connection
            results.add(testFirebaseConnection())
            
            // Test 2: Authentication
            results.add(testAuthentication())
            
            // Test 3: Firestore Operations
            results.add(testFirestoreOperations())
            
            // Test 4: Push Notifications
            results.add(testPushNotifications())
            
            // Test 5: Security Rules
            results.add(testSecurityRules())
            
            // Compile overall result
            val overallResult = TestResult(
                testName = "Firebase Complete Setup",
                success = results.all { it.success },
                message = if (results.all { it.success }) {
                    "✅ All Firebase services are working correctly!"
                } else {
                    "❌ Some Firebase services have issues. Check individual test results."
                },
                details = results
            )
            
            onResult(overallResult)
        }
    }
    
    /**
     * Test 1: Basic Firebase Connection
     */
    private suspend fun testFirebaseConnection(): TestResult {
        return try {
            // Test basic Firestore connection
            val testDoc = firestore.collection("connection_test").document("test")
            val testData = hashMapOf(
                "timestamp" to com.google.firebase.Timestamp.now(),
                "test_id" to "connection_${System.currentTimeMillis()}"
            )
            
            testDoc.set(testData).await()
            val snapshot = testDoc.get().await()
            testDoc.delete().await()
            
            if (snapshot.exists()) {
                TestResult(
                    testName = "Firebase Connection",
                    success = true,
                    message = "✅ Firebase connection successful"
                )
            } else {
                TestResult(
                    testName = "Firebase Connection",
                    success = false,
                    message = "❌ Firebase connection failed - document not found"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Firebase connection test failed", e)
            TestResult(
                testName = "Firebase Connection",
                success = false,
                message = "❌ Firebase connection failed: ${e.message}"
            )
        }
    }
    
    /**
     * Test 2: Authentication Services
     */
    private suspend fun testAuthentication(): TestResult {
        return try {
            val testEmail = "test_${System.currentTimeMillis()}@example.com"
            val testPassword = "testPassword123"
            val testProfile = UserProfile(
                name = "Test User",
                email = testEmail,
                phone = "+1234567890"
            )
            
            // Test sign up
            val signUpResult = authProvider.signUpWithEmail(testEmail, testPassword, testProfile)
            if (signUpResult.isFailure) {
                return TestResult(
                    testName = "Authentication",
                    success = false,
                    message = "❌ Sign up failed: ${signUpResult.exceptionOrNull()?.message}"
                )
            }
            
            // Test sign out
            authProvider.signOut()
            
            // Test sign in
            val signInResult = authProvider.signInWithEmail(testEmail, testPassword)
            if (signInResult.isFailure) {
                return TestResult(
                    testName = "Authentication",
                    success = false,
                    message = "❌ Sign in failed: ${signInResult.exceptionOrNull()?.message}"
                )
            }
            
            // Clean up test user
            val user = auth.currentUser
            user?.delete()?.await()
            
            TestResult(
                testName = "Authentication",
                success = true,
                message = "✅ Authentication working correctly"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Authentication test failed", e)
            TestResult(
                testName = "Authentication",
                success = false,
                message = "❌ Authentication test failed: ${e.message}"
            )
        }
    }
    
    /**
     * Test 3: Firestore Operations
     */
    private suspend fun testFirestoreOperations(): TestResult {
        return try {
            val testUserId = "test_user_${System.currentTimeMillis()}"
            
            // Test user profile creation
            val userProfile = UserProfile(
                uid = testUserId,
                name = "Test User",
                email = "test@example.com",
                phone = "+1234567890"
            )
            
            firestore.collection(FirestoreCollections.USERS)
                .document(testUserId)
                .set(userProfile)
                .await()
            
            // Test reading user profile
            val userDoc = firestore.collection(FirestoreCollections.USERS)
                .document(testUserId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                return TestResult(
                    testName = "Firestore Operations",
                    success = false,
                    message = "❌ Failed to read user profile"
                )
            }
            
            // Test SOS event creation
            val sosEvent = SosEvent(
                id = "test_sos_${System.currentTimeMillis()}",
                userId = testUserId,
                type = "test",
                location = SosLocation(
                    geoPoint = com.google.firebase.firestore.GeoPoint(40.7128, -74.0060),
                    address = "Test Location"
                )
            )
            
            val sosDoc = firestore.collection(FirestoreCollections.SOS_EVENTS)
                .add(sosEvent)
                .await()
            
            // Test community alert creation
            val communityAlert = CommunityAlert(
                id = "test_alert_${System.currentTimeMillis()}",
                createdBy = testUserId,
                creatorName = "Test User",
                type = "test",
                title = "Test Alert",
                description = "This is a test alert"
            )
            
            val alertDoc = firestore.collection(FirestoreCollections.COMMUNITY_ALERTS)
                .add(communityAlert)
                .await()
            
            // Clean up test data
            userDoc.reference.delete().await()
            sosDoc.delete().await()
            alertDoc.delete().await()
            
            TestResult(
                testName = "Firestore Operations",
                success = true,
                message = "✅ Firestore CRUD operations working correctly"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Firestore operations test failed", e)
            TestResult(
                testName = "Firestore Operations",
                success = false,
                message = "❌ Firestore operations failed: ${e.message}"
            )
        }
    }
    
    /**
     * Test 4: Push Notifications
     */
    private suspend fun testPushNotifications(): TestResult {
        return try {
            val testUserId = "test_user_${System.currentTimeMillis()}"
            
            // Test FCM token generation
            val tokenResult = pushTester.testFcmTokenGeneration(testUserId)
            if (tokenResult.isFailure) {
                return TestResult(
                    testName = "Push Notifications",
                    success = false,
                    message = "❌ FCM token generation failed: ${tokenResult.exceptionOrNull()?.message}"
                )
            }
            
            // Test topic subscription
            val subscriptionResult = pushTester.testTopicSubscription()
            if (subscriptionResult.isFailure) {
                return TestResult(
                    testName = "Push Notifications",
                    success = false,
                    message = "❌ Topic subscription failed: ${subscriptionResult.exceptionOrNull()?.message}"
                )
            }
            
            // Test notification data creation
            val notificationResult = pushTester.createTestNotificationData()
            if (notificationResult.isFailure) {
                return TestResult(
                    testName = "Push Notifications",
                    success = false,
                    message = "❌ Notification data creation failed: ${notificationResult.exceptionOrNull()?.message}"
                )
            }
            
            // Check notification permissions
            val permissionsGranted = pushTester.checkNotificationPermissions()
            val notificationInfo = pushTester.getNotificationInfo()
            
            // Clean up test data
            pushTester.cleanupTestData()
            
            TestResult(
                testName = "Push Notifications",
                success = true,
                message = "✅ Push notifications configured correctly",
                details = listOf(
                    TestResult("FCM Token", true, "Token: ${tokenResult.getOrNull()?.take(20)}..."),
                    TestResult("Topics", true, "Subscribed to: ${subscriptionResult.getOrNull()?.joinToString(", ")}"),
                    TestResult("Permissions", permissionsGranted, if (permissionsGranted) "Granted" else "Not granted"),
                    TestResult("Channels", true, "Channels: ${notificationInfo["channels_created"]}")
                )
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Push notifications test failed", e)
            TestResult(
                testName = "Push Notifications",
                success = false,
                message = "❌ Push notifications test failed: ${e.message}"
            )
        }
    }
    
    /**
     * Test 5: Security Rules (basic validation)
     */
    private suspend fun testSecurityRules(): TestResult {
        return try {
            // This is a basic test - in production you'd want more comprehensive security testing
            val testUserId = "test_user_${System.currentTimeMillis()}"
            
            // Test that unauthenticated users can't access user data
            try {
                firestore.collection(FirestoreCollections.USERS)
                    .document(testUserId)
                    .get()
                    .await()
                
                // If we get here without authentication, security rules might not be working
                TestResult(
                    testName = "Security Rules",
                    success = false,
                    message = "⚠️ Security rules may not be properly configured - unauthenticated access allowed"
                )
                
            } catch (e: Exception) {
                // This is expected - unauthenticated users should not be able to access data
                TestResult(
                    testName = "Security Rules",
                    success = true,
                    message = "✅ Security rules are working - unauthenticated access denied"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Security rules test failed", e)
            TestResult(
                testName = "Security Rules",
                success = false,
                message = "❌ Security rules test failed: ${e.message}"
            )
        }
    }
}

/**
 * Test result data class
 */
data class TestResult(
    val testName: String,
    val success: Boolean,
    val message: String,
    val details: List<TestResult> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
