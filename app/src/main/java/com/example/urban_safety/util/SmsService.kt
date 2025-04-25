package com.example.urban_safety.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for sending SMS messages in emergency situations with optimized performance
 */
@Singleton
class SmsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "SmsService"
    
    // Cache permission check result
    private var smsPermissionCache: Boolean? = null
    private var lastPermissionCheckTime = 0L
    private val PERMISSION_CACHE_DURATION = 30 * 1000 // 30 seconds
    
    /**
     * Send an emergency SMS message with high priority
     * Optimized for speed and reliability
     * 
     * @param phoneNumber The recipient's phone number
     * @param message The emergency message to send
     * @return Result indicating success or failure
     */
    suspend fun sendEmergencySMS(phoneNumber: String, message: String): Result<Unit> = withContext(Dispatchers.IO) {
        // Debug logging
        Log.d(TAG, "Attempting to send emergency SMS to: $phoneNumber")
        
        if (phoneNumber.isBlank()) {
            Log.e(TAG, "Cannot send SMS: Phone number is blank")
            return@withContext Result.failure(IllegalArgumentException("Phone number cannot be blank"))
        }

        return@withContext try {
            if (!hasSmsPermission()) {
                Log.e(TAG, "SMS permission not granted! Cannot send messages.")
                return@withContext Result.failure(SecurityException("SMS permission not granted"))
            }
            
            // Get SmsManager instance
            val smsManager = getSmsManager()
            
            // For emergency messages, we want to ensure delivery by handling long messages properly
            val messageParts = smsManager.divideMessage(message)
            
            Log.d(TAG, "Message divided into ${messageParts.size} parts")
            
            if (messageParts.size > 1) {
                // For multi-part messages, use sendMultipartTextMessage
                Log.d(TAG, "Sending multipart SMS to $phoneNumber")
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    messageParts,
                    null,
                    null
                )
            } else {
                // For single part messages, use the simpler method
                Log.d(TAG, "Sending single-part SMS to $phoneNumber")
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
                )
            }
            
            Log.d(TAG, "Emergency SMS sent successfully to $phoneNumber")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send emergency SMS to $phoneNumber: ${e.message}", e)
            
            // More detailed logging about the error
            when (e) {
                is IllegalArgumentException -> Log.e(TAG, "Invalid argument (possibly malformed phone number): $phoneNumber")
                is SecurityException -> Log.e(TAG, "Security exception - permissions issue")
                else -> Log.e(TAG, "Unknown error sending SMS: ${e.javaClass.simpleName}")
            }
            
            Result.failure(e)
        }
    }
    
    /**
     * Send a regular SMS message
     * @param phoneNumber The recipient's phone number
     * @param message The message to send
     * @return Result indicating success or failure
     */
    suspend fun sendSMS(phoneNumber: String, message: String): Result<Unit> {
        Log.d(TAG, "Regular SMS request to: $phoneNumber")
        // For regular (non-emergency) messages, we can use the same implementation
        return sendEmergencySMS(phoneNumber, message)
    }
    
    /**
     * Check if the app has SMS permission with caching for better performance
     * @return true if permission is granted, false otherwise
     */
    fun hasSmsPermission(): Boolean {
        val currentTime = System.currentTimeMillis()
        
        // Use cached result if recent enough
        smsPermissionCache?.let {
            if (currentTime - lastPermissionCheckTime < PERMISSION_CACHE_DURATION) {
                return it
            }
        }
        
        // Otherwise check permission and update cache
        val hasPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG, "SMS permission check result: $hasPermission")
        
        smsPermissionCache = hasPermission
        lastPermissionCheckTime = currentTime
        
        return hasPermission
    }
    
    /**
     * Get the appropriate SmsManager instance
     */
    private fun getSmsManager(): SmsManager {
        return try {
            SmsManager.getDefault()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SmsManager: ${e.message}", e)
            throw e
        }
    }
} 