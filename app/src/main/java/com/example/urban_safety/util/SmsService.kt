package com.example.urban_safety.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for sending SMS messages in emergency situations
 */
@Singleton
class SmsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Send an emergency SMS message
     * @param phoneNumber The recipient's phone number
     * @param message The emergency message to send
     * @return Result indicating success or failure
     */
    fun sendEmergencySMS(phoneNumber: String, message: String): Result<Unit> {
        return try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return Result.failure(SecurityException("SMS permission not granted"))
            }
            
            val smsManager = SmsManager.getDefault()
            
            // For long messages, divide the message into parts
            val messageParts = smsManager.divideMessage(message)
            smsManager.sendMultipartTextMessage(
                phoneNumber,
                null,
                messageParts,
                null,
                null
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a regular SMS message
     * @param phoneNumber The recipient's phone number
     * @param message The message to send
     * @return Result indicating success or failure
     */
    fun sendSMS(phoneNumber: String, message: String): Result<Unit> {
        return sendEmergencySMS(phoneNumber, message)
    }
    
    /**
     * Check if the app has SMS permission
     * @return true if permission is granted, false otherwise
     */
    fun hasSmsPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
} 