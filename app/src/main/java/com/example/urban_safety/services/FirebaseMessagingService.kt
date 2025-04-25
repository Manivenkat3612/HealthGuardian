package com.example.urban_safety.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.urban_safety.MainActivity
import com.example.urban_safety.R
import com.example.urban_safety.ui.screens.community.SosResponseActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UrbanSafetyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FirebaseMsgService"
        private const val CHANNEL_ID = "urban_safety_notifications"
        private const val CHANNEL_NAME = "Urban Safety Notifications"
        private const val SOS_CHANNEL_ID = "urban_safety_sos_alerts"
        private const val SOS_CHANNEL_NAME = "SOS Emergency Alerts"
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Send token to your server for targeting notifications
        updateTokenInFirestore(token)
    }

    private fun updateTokenInFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { currentUser ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update FCM token: ${e.message}")
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            // Check if this is an SOS alert
            if (remoteMessage.data["type"] == "MANUAL_SOS" || 
                remoteMessage.from?.contains("sos_alerts") == true) {
                handleSosAlert(remoteMessage)
                return
            }
            
            // Handle other data payloads
            handleDataMessage(remoteMessage)
        }

        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title ?: "Urban Safety Alert", it.body ?: "")
        }
    }
    
    private fun handleSosAlert(remoteMessage: RemoteMessage) {
        val userId = remoteMessage.data["userId"] ?: ""
        val userName = remoteMessage.data["userName"] ?: "Someone"
        val latitude = remoteMessage.data["latitude"] ?: "0.0"
        val longitude = remoteMessage.data["longitude"] ?: "0.0"
        val timestamp = remoteMessage.data["timestamp"] ?: System.currentTimeMillis().toString()
        
        // Create an intent to open the SOS response activity
        val intent = Intent(this, SosResponseActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("USER_ID", userId)
            putExtra("USER_NAME", userName)
            putExtra("LATITUDE", latitude)
            putExtra("LONGITUDE", longitude)
            putExtra("TIMESTAMP", timestamp)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        createSosNotificationChannel()
        
        val notificationBuilder = NotificationCompat.Builder(this, SOS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("EMERGENCY: SOS Alert")
            .setContentText("$userName needs urgent help nearby!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            .setLights(0xFF0000, 3000, 3000) // Red light
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Use a unique ID based on timestamp to avoid overriding
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    
    private fun handleDataMessage(remoteMessage: RemoteMessage) {
        // Extract data from the message
        val title = remoteMessage.data["title"] ?: "Urban Safety"
        val message = remoteMessage.data["message"] ?: "New notification"
        
        // Send as a normal notification
        sendNotification(title, message)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Urban Safety Notifications"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createSosNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SOS_CHANNEL_ID,
                SOS_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency SOS Alerts"
                enableVibration(true)
                enableLights(true)
                lightColor = 0xFF0000 // Red
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 