package com.womensafety.app.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Configuration and helper class for Firebase Authentication providers
 */
class AuthProviderConfig(private val context: Context) {
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val TAG = "AuthProviderConfig"
    }
    
    /**
     * Configure Email/Password Authentication
     */
    suspend fun signUpWithEmail(
        email: String, 
        password: String, 
        userProfile: UserProfile
    ): Result<com.google.firebase.auth.FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("User creation failed")
            
            // Update user profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(userProfile.name)
                .build()
            user.updateProfile(profileUpdates).await()
            
            // Send email verification
            user.sendEmailVerification().await()
            
            // Save user profile to Firestore
            saveUserProfile(user.uid, userProfile.copy(uid = user.uid, email = email))
            
            Log.d(TAG, "Email sign up successful for: $email")
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Email sign up failed", e)
            Result.failure(e)
        }
    }
    
    suspend fun signInWithEmail(email: String, password: String): Result<com.google.firebase.auth.FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign in failed")
            
            // Update last active timestamp
            updateLastActive(user.uid)
            
            Log.d(TAG, "Email sign in successful for: $email")
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Email sign in failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Configure Phone Authentication
     */
    fun startPhoneVerification(
        phoneNumber: String,
        activity: androidx.fragment.app.FragmentActivity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        
        PhoneAuthProvider.verifyPhoneNumber(options)
        Log.d(TAG, "Phone verification started for: $phoneNumber")
    }
    
    suspend fun verifyPhoneCode(
        verificationId: String, 
        code: String,
        userProfile: UserProfile? = null
    ): Result<com.google.firebase.auth.FirebaseUser> {
        return try {
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Phone verification failed")
            
            // If this is a new user and profile is provided, save it
            if (userProfile != null && result.additionalUserInfo?.isNewUser == true) {
                saveUserProfile(user.uid, userProfile.copy(uid = user.uid, phone = user.phoneNumber ?: ""))
            } else {
                updateLastActive(user.uid)
            }
            
            Log.d(TAG, "Phone verification successful")
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Phone verification failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Configure Google Sign-In
     * Note: You'll need to add Google Sign-In dependency and configure it
     */
    suspend fun signInWithGoogle(idToken: String, userProfile: UserProfile? = null): Result<com.google.firebase.auth.FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google sign in failed")
            
            // If this is a new user, create profile
            if (result.additionalUserInfo?.isNewUser == true) {
                val profile = userProfile ?: UserProfile(
                    uid = user.uid,
                    name = user.displayName ?: "",
                    email = user.email ?: "",
                    phone = user.phoneNumber ?: "",
                    profileImageUrl = user.photoUrl?.toString()
                )
                saveUserProfile(user.uid, profile)
            } else {
                updateLastActive(user.uid)
            }
            
            Log.d(TAG, "Google sign in successful")
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Anonymous Authentication (for emergency situations)
     */
    suspend fun signInAnonymously(): Result<com.google.firebase.auth.FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw Exception("Anonymous sign in failed")
            
            // Create minimal anonymous profile
            val anonymousProfile = UserProfile(
                uid = user.uid,
                name = "Anonymous User",
                email = "",
                phone = "",
                isActive = true
            )
            saveUserProfile(user.uid, anonymousProfile)
            
            Log.d(TAG, "Anonymous sign in successful")
            Result.success(user)
            
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign in failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Link Anonymous Account with Email/Password
     */
    suspend fun linkAnonymousWithEmail(email: String, password: String): Result<com.google.firebase.auth.FirebaseUser> {
        return try {
            val user = auth.currentUser ?: throw Exception("No current user")
            if (!user.isAnonymous) throw Exception("User is not anonymous")
            
            val credential = EmailAuthProvider.getCredential(email, password)
            val result = user.linkWithCredential(credential).await()
            val linkedUser = result.user ?: throw Exception("Account linking failed")
            
            // Update user profile with email
            updateUserProfile(linkedUser.uid, mapOf("email" to email))
            
            Log.d(TAG, "Anonymous account linked with email")
            Result.success(linkedUser)
            
        } catch (e: Exception) {
            Log.e(TAG, "Account linking failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Password Reset
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent to: $email")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update Password
     */
    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No current user")
            user.updatePassword(newPassword).await()
            Log.d(TAG, "Password updated successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Password update failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Re-authenticate User (required for sensitive operations)
     */
    suspend fun reauthenticateWithEmail(email: String, password: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No current user")
            val credential = EmailAuthProvider.getCredential(email, password)
            user.reauthenticate(credential).await()
            Log.d(TAG, "Re-authentication successful")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Re-authentication failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete User Account
     */
    suspend fun deleteUserAccount(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No current user")
            val userId = user.uid
            
            // Delete user data from Firestore first
            deleteUserData(userId)
            
            // Delete Firebase Auth user
            user.delete().await()
            
            Log.d(TAG, "User account deleted successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "Account deletion failed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Sign Out
     */
    fun signOut() {
        auth.signOut()
        Log.d(TAG, "User signed out")
    }
    
    /**
     * Get Current User
     */
    fun getCurrentUser(): com.google.firebase.auth.FirebaseUser? = auth.currentUser
    
    /**
     * Check if user is signed in
     */
    fun isUserSignedIn(): Boolean = auth.currentUser != null
    
    /**
     * Private helper methods
     */
    private suspend fun saveUserProfile(userId: String, profile: UserProfile) {
        firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .set(profile)
            .await()
    }
    
    private suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .update(updates)
            .await()
    }
    
    private suspend fun updateLastActive(userId: String) {
        firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .update("lastActiveAt", com.google.firebase.Timestamp.now())
            .await()
    }
    
    private suspend fun deleteUserData(userId: String) {
        // Delete user document
        firestore.collection(FirestoreCollections.USERS)
            .document(userId)
            .delete()
            .await()
        
        // Delete user's SOS events
        val sosEvents = firestore.collection(FirestoreCollections.SOS_EVENTS)
            .whereEqualTo("userId", userId)
            .get()
            .await()
        
        for (document in sosEvents.documents) {
            document.reference.delete().await()
        }
        
        // Delete user's location history
        val locationHistory = firestore.collection(FirestoreCollections.LOCATION_HISTORY)
            .whereEqualTo("userId", userId)
            .get()
            .await()
        
        for (document in locationHistory.documents) {
            document.reference.delete().await()
        }
        
        // Delete FCM tokens
        val fcmTokens = firestore.collection(FirestoreCollections.FCM_TOKENS)
            .whereEqualTo("userId", userId)
            .get()
            .await()
        
        for (document in fcmTokens.documents) {
            document.reference.delete().await()
        }
    }
}
