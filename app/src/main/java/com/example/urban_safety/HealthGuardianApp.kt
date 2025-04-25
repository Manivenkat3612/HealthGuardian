package com.example.urban_safety

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration
import java.io.File

@HiltAndroidApp
class HealthGuardianApp : Application() {

    companion object {
        const val GENERAL_CHANNEL_ID = "health_guardian_general"
        const val LOCATION_CHANNEL_ID = "health_guardian_location"
        const val VOICE_RECOGNITION_CHANNEL_ID = "health_guardian_voice_recognition"
        const val EMERGENCY_CHANNEL_ID = "health_guardian_emergency"
        const val HEALTH_CHANNEL_ID = "health_guardian_health"
        
        lateinit var instance: HealthGuardianApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize the application instance
        instance = this
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize OSMDroid
        initializeOSMDroid()
        
        // Create notification channels
        createNotificationChannels()
    }
    
    private fun initializeOSMDroid() {
        // Configure osmdroid
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = packageName
        osmConfig.osmdroidBasePath = File(cacheDir, "osmdroid")
        osmConfig.osmdroidTileCache = File(osmConfig.osmdroidBasePath, "tiles")
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // General channel
            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications for the app"
            }
            
            // Location tracking channel
            val locationChannel = NotificationChannel(
                LOCATION_CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Location tracking service notifications"
            }
            
            // Voice recognition channel
            val voiceChannel = NotificationChannel(
                VOICE_RECOGNITION_CHANNEL_ID,
                "Voice Recognition",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice recognition service notifications"
            }
            
            // Emergency channel
            val emergencyChannel = NotificationChannel(
                EMERGENCY_CHANNEL_ID,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Emergency alerts and SOS notifications"
                enableVibration(true)
                enableLights(true)
            }
            
            // Health monitoring channel
            val healthChannel = NotificationChannel(
                HEALTH_CHANNEL_ID,
                "Health Monitoring",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Health monitoring service notifications"
            }
            
            notificationManager.createNotificationChannels(
                listOf(generalChannel, locationChannel, voiceChannel, emergencyChannel, healthChannel)
            )
        }
    }
} 