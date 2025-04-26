package com.example.urban_safety.util

import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * Utility methods for working with services
 */
object ServiceUtils {
    private const val TAG = "ServiceUtils"
    
    /**
     * Check if a service is running
     */
    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    Log.d(TAG, "Service ${serviceClass.simpleName} is running")
                    return true
                }
            }
            Log.d(TAG, "Service ${serviceClass.simpleName} is NOT running")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking service status: ${e.message}")
            return false
        }
    }
} 