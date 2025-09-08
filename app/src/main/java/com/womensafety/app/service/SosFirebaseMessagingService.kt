package com.womensafety.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.womensafety.app.R
import com.womensafety.app.ui.MainActivity
import com.womensafety.app.utils.Constants

class SosFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle FCM messages here
        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Smart SOS Alert",
                body = notification.body ?: "Emergency notification received",
                data = remoteMessage.data
            )
        }

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            handleDataMessage(remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Send token to your server or save locally
        saveTokenToPreferences(token)
        
        // TODO: Send token to your backend server
        sendTokenToServer(token)
    }

    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"]
        
        when (type) {
            "sos_alert" -> {
                val userId = data["user_id"]
                val location = data["location"]
                val message = data["message"]
                
                showSosAlert(userId, location, message)
            }
            "community_alert" -> {
                val alertType = data["alert_type"]
                val location = data["location"]
                
                showCommunityAlert(alertType, location)
            }
            "safety_check" -> {
                val fromUser = data["from_user"]
                
                showSafetyCheckNotification(fromUser)
            }
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            // Add data to intent
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_SOS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID_SOS, notificationBuilder.build())
    }

    private fun showSosAlert(userId: String?, location: String?, message: String?) {
        val title = "🚨 Emergency Alert"
        val body = message ?: "Someone nearby needs help!"
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "sos_alert")
            putExtra("user_id", userId)
            putExtra("location", location)
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_SOS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setLights(0xFFFF0000.toInt(), 1000, 500)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(Constants.NOTIFICATION_ID_SOS, notificationBuilder.build())
    }

    private fun showCommunityAlert(alertType: String?, location: String?) {
        val title = "Community Safety Alert"
        val body = "Safety alert in your area: $alertType"
        
        showNotification(title, body, mapOf(
            "type" to "community_alert",
            "alert_type" to (alertType ?: ""),
            "location" to (location ?: "")
        ))
    }

    private fun showSafetyCheckNotification(fromUser: String?) {
        val title = "Safety Check"
        val body = "$fromUser is checking on your safety"
        
        showNotification(title, body, mapOf(
            "type" to "safety_check",
            "from_user" to (fromUser ?: "")
        ))
    }

    private fun saveTokenToPreferences(token: String) {
        val prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_token", token).apply()
    }

    private fun sendTokenToServer(token: String) {
        // TODO: Implement server communication
        // This would typically send the token to your backend server
        // along with user identification for targeted notifications
    }
}
