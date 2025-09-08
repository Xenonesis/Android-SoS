package com.womensafety.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.womensafety.app.data.database.SosDatabase
import com.womensafety.app.data.firebase.FirebaseAuthRepository
import com.womensafety.app.data.firebase.FirebaseRepository
import com.womensafety.app.data.repository.ContactRepository
import com.womensafety.app.data.repository.LocationRepository
import com.womensafety.app.data.repository.SosRepository
import com.womensafety.app.utils.Constants

class SosApplication : Application() {

    // Database - with error handling
    val database by lazy { 
        try {
            SosDatabase.getDatabase(this)
        } catch (e: Exception) {
            android.util.Log.e("SosApplication", "Database initialization failed", e)
            null
        }
    }
    
    // Local Repositories - with null safety
    val contactRepository by lazy { 
        database?.let { db ->
            ContactRepository(db.contactDao(), firebaseRepository, FirebaseAuth.getInstance())
        }
    }
    val locationRepository by lazy { LocationRepository(this) }
    val sosRepository by lazy { 
        database?.let { db ->
            contactRepository?.let { contactRepo ->
                SosRepository(db.sosEventDao(), contactRepo, locationRepository, firebaseRepository)
            }
        }
    }
    
    // Firebase Repositories
    val firebaseRepository by lazy { FirebaseRepository() }
    val firebaseAuthRepository by lazy { FirebaseAuthRepository() }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        initializeFirebase()
        
        // Initialize WorkManager
        initializeWorkManager()
        
        // Create notification channels
        createNotificationChannels()
        
        // Setup Firebase messaging
        setupFirebaseMessaging()
    }
    
    private fun initializeWorkManager() {
        try {
            WorkManager.initialize(
                this,
                Configuration.Builder().build()
            )
            android.util.Log.d("SosApplication", "WorkManager initialized successfully")
        } catch (e: Exception) {
            // Handle WorkManager initialization error
            android.util.Log.e("SosApplication", "WorkManager initialization failed", e)
        }
    }
    
    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            
            // Enable Firestore offline persistence
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder(firestore.firestoreSettings).build()
            firestore.firestoreSettings = settings
            
            // Set Firebase Auth language
            FirebaseAuth.getInstance().setLanguageCode("en")
            
        } catch (e: Exception) {
            // Handle Firebase initialization error
            android.util.Log.e("SosApplication", "Firebase initialization failed", e)
        }
    }
    
    private fun setupFirebaseMessaging() {
        try {
            // Subscribe to general safety alerts topic
            FirebaseMessaging.getInstance().subscribeToTopic("safety_alerts")
            
            // Subscribe to community alerts for the user's region
            // This would typically be based on user's location
            FirebaseMessaging.getInstance().subscribeToTopic("community_alerts_general")
            
        } catch (e: Exception) {
            android.util.Log.e("SosApplication", "Firebase messaging setup failed", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // SOS Channel
            val sosChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_SOS,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency SOS alerts and notifications"
                enableVibration(true)
                setShowBadge(true)
            }

            // Tracking Channel
            val trackingChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_TRACKING,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background location tracking notifications"
                enableVibration(false)
                setShowBadge(false)
            }

            // Community Channel
            val communityChannel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_COMMUNITY,
                "Community Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Community safety alerts and notifications"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(
                listOf(sosChannel, trackingChannel, communityChannel)
            )
        }
    }
}
