package com.example.urban_safety.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.urban_safety.HealthGuardianApp
import com.example.urban_safety.util.PreferencesManager
import com.example.urban_safety.services.LocationTrackingService
import com.example.urban_safety.services.VoiceRecognitionService

// Remove Hilt dependency
class BootCompletedReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Get preferences manager without dependency injection
            val preferencesManager = PreferencesManager(context)
            
            // Restore any services that should be running
            if (preferencesManager.getLocationTrackingEnabled()) {
                context.startService(Intent(context, LocationTrackingService::class.java))
            }
            
            if (preferencesManager.getVoiceRecognitionEnabled()) {
                context.startService(Intent(context, VoiceRecognitionService::class.java))
            }
        }
    }
} 