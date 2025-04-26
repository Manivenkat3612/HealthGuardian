package com.example.urban_safety.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.urban_safety.HealthGuardianApp
import com.example.urban_safety.util.PreferencesManager
import com.example.urban_safety.services.LocationTrackingService
import com.example.urban_safety.services.VoiceRecognitionService
import com.example.urban_safety.services.FallDetectionService

// Remove Hilt dependency
class BootCompletedReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Get preferences manager without dependency injection
            val preferencesManager = PreferencesManager(context)
            
            // Restore any services that should be running
            if (preferencesManager.getLocationTracking()) {
                startService(context, LocationTrackingService::class.java)
            }
            
            if (preferencesManager.getVoiceRecognition()) {
                startService(context, VoiceRecognitionService::class.java)
            }
            
            if (preferencesManager.getFallDetection()) {
                startService(context, FallDetectionService::class.java)
            }
        }
    }
    
    private fun startService(context: Context, serviceClass: Class<*>) {
        val serviceIntent = Intent(context, serviceClass)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Log.e() not directly usable in BroadcastReceiver without tag
            e.printStackTrace()
        }
    }
} 