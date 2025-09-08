package com.womensafety.app.data.firebase

import android.util.Log
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FirebaseAuthRepository {
    
    private val auth = FirebaseAuth.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    private val firebaseRepository = FirebaseRepository()
    
    // Authentication state
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    val isUserLoggedIn: Boolean
        get() = currentUser != null
    
    // Observe authentication state changes
    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    
    // Phone number authentication
    suspend fun sendVerificationCode(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ): Result<Unit> {
        return try {
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            
            PhoneAuthProvider.verifyPhoneNumber(options)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error sending verification code", e)
            Result.failure(e)
        }
    }
    
    suspend fun verifyPhoneNumber(
        verificationId: String,
        code: String
    ): Result<FirebaseUser> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val authResult = auth.signInWithCredential(credential).await()
            
            authResult.user?.let { user ->
                // Create or update user profile in Firestore
                setupUserProfile(user)
                Result.success(user)
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error verifying phone number", e)
            Result.failure(e)
        }
    }
    
    // Email and password authentication
    suspend fun signInWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            
            authResult.user?.let { user ->
                // Update user profile in Firestore
                setupUserProfile(user)
                Result.success(user)
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error signing in with email", e)
            Result.failure(e)
        }
    }
    
    suspend fun signUpWithEmail(
        email: String,
        password: String
    ): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            
            authResult.user?.let { user ->
                // Update user profile in Firestore
                setupUserProfile(user)
                Result.success(user)
            } ?: Result.failure(Exception("Sign up failed"))
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error signing up with email", e)
            Result.failure(e)
        }
    }
    
    suspend fun signInWithCredential(credential: AuthCredential): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            
            authResult.user?.let { user ->
                setupUserProfile(user)
                Result.success(user)
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error signing in with credential", e)
            Result.failure(e)
        }
    }
    
    // Anonymous authentication (for demo/testing)
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInAnonymously().await()
            
            authResult.user?.let { user ->
                setupUserProfile(user)
                Result.success(user)
            } ?: Result.failure(Exception("Anonymous authentication failed"))
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error signing in anonymously", e)
            Result.failure(e)
        }
    }
    
    // Password reset
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d("FirebaseAuthRepository", "Password reset email sent to: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error sending password reset email", e)
            Result.failure(e)
        }
    }
    
    // User profile management
    suspend fun updateUserProfile(
        name: String? = null,
        phoneNumber: String? = null
    ): Result<Unit> {
        return try {
            val user = currentUser ?: return Result.failure(Exception("User not authenticated"))
            
            // Update Firebase Auth profile
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
            name?.let { profileUpdates.setDisplayName(it) }
            profileUpdates.build()
            
            user.updateProfile(profileUpdates.build()).await()
            
            // Update Firestore user document
            val firestoreUser = FirebaseUser(
                id = user.uid,
                name = name ?: user.displayName ?: "",
                phoneNumber = phoneNumber ?: user.phoneNumber ?: "",
                email = user.email
            )
            
            firebaseRepository.updateUser(user.uid, firestoreUser)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error updating user profile", e)
            Result.failure(e)
        }
    }
    
    // FCM Token management
    suspend fun updateFcmToken(): Result<Unit> {
        return try {
            val user = currentUser ?: return Result.failure(Exception("User not authenticated"))
            val token = messaging.token.await()
            
            firebaseRepository.updateFcmToken(user.uid, token)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error updating FCM token", e)
            Result.failure(e)
        }
    }
    
    // Sign out
    suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error signing out", e)
            Result.failure(e)
        }
    }
    
    // Delete account
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val user = currentUser ?: return Result.failure(Exception("User not authenticated"))
            
            // Delete user data from Firestore first
            // TODO: Implement cleanup of user data
            
            // Delete Firebase Auth account
            user.delete().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error deleting account", e)
            Result.failure(e)
        }
    }
    
    // Private helper methods
    private suspend fun setupUserProfile(user: FirebaseUser) {
        try {
            // Get FCM token
            val fcmToken = messaging.token.await()
            
            // Create or update user in Firestore
            val firestoreUser = FirebaseUser(
                id = user.uid,
                name = user.displayName ?: "",
                phoneNumber = user.phoneNumber ?: "",
                email = user.email,
                fcmToken = fcmToken,
                isOnline = true
            )
            
            firebaseRepository.updateUser(user.uid, firestoreUser)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error setting up user profile", e)
        }
    }
    
    // User status management
    suspend fun setUserOnline(isOnline: Boolean): Result<Unit> {
        return try {
            val user = currentUser ?: return Result.failure(Exception("User not authenticated"))
            
            val existingUser = firebaseRepository.getUser(user.uid).getOrNull()
            existingUser?.let { userData ->
                val updatedUser = userData.copy(
                    isOnline = isOnline,
                    lastSeen = if (!isOnline) java.util.Date() else userData.lastSeen
                )
                firebaseRepository.updateUser(user.uid, updatedUser)
            } ?: Result.failure(Exception("User data not found"))
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error updating user status", e)
            Result.failure(e)
        }
    }
    
    // Emergency settings
    suspend fun updateEmergencySettings(
        allowCommunityAlerts: Boolean? = null,
        shareLocationWithContacts: Boolean? = null,
        autoCallEmergencyServices: Boolean? = null
    ): Result<Unit> {
        return try {
            val user = currentUser ?: return Result.failure(Exception("User not authenticated"))
            
            val existingUser = firebaseRepository.getUser(user.uid).getOrNull()
            existingUser?.let { userData ->
                val updatedUser = userData.copy(
                    allowCommunityAlerts = allowCommunityAlerts ?: userData.allowCommunityAlerts,
                    shareLocationWithContacts = shareLocationWithContacts ?: userData.shareLocationWithContacts,
                    autoCallEmergencyServices = autoCallEmergencyServices ?: userData.autoCallEmergencyServices
                )
                firebaseRepository.updateUser(user.uid, updatedUser)
            } ?: Result.failure(Exception("User data not found"))
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepository", "Error updating emergency settings", e)
            Result.failure(e)
        }
    }
}
