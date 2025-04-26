package com.example.urban_safety.viewmodels

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.services.FallDetectionService
import com.example.urban_safety.util.PreferencesManager
import com.example.urban_safety.util.ServiceUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FallDetectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "FallDetectionVM"
    }
    
    private val _isFallDetectionActive = MutableStateFlow(false)
    val isFallDetectionActive: StateFlow<Boolean> = _isFallDetectionActive.asStateFlow()
    
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted.asStateFlow()
    
    init {
        // Check if fall detection service is running
        _isFallDetectionActive.value = preferencesManager.getFallDetection()
        
        // Check for required permissions
        checkPermissions()
    }
    
    /**
     * Start fall detection service
     */
    fun startFallDetection() {
        Log.d(TAG, "Starting fall detection. Permissions granted: ${_permissionsGranted.value}")
        
        /* Removed for testing
        if (!_permissionsGranted.value) {
            Log.e(TAG, "Cannot start fall detection: permissions not granted")
            return
        }
        */
        
        Log.d(TAG, "Attempting to start fall detection service...")
        val serviceIntent = Intent(context, FallDetectionService::class.java)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Using startForegroundService for Android O+")
                context.startForegroundService(serviceIntent)
            } else {
                Log.d(TAG, "Using startService for pre-Android O")
                context.startService(serviceIntent)
            }
            _isFallDetectionActive.value = true
            preferencesManager.enableFallDetection(true)
            Log.d(TAG, "Fall detection service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start fall detection service: ${e.message}", e)
        }
    }
    
    /**
     * Stop fall detection service
     */
    fun stopFallDetection() {
        Log.d(TAG, "Stopping fall detection service")
        val serviceIntent = Intent(context, FallDetectionService::class.java)
        context.stopService(serviceIntent)
        _isFallDetectionActive.value = false
        preferencesManager.enableFallDetection(false)
        Log.d(TAG, "Fall detection service stopped successfully")
    }
    
    /**
     * Toggle fall detection service
     */
    fun toggleFallDetection() {
        Log.d(TAG, "Toggle called. Current state: ${_isFallDetectionActive.value}")
        
        // Check permissions again to be sure, but log result only
        val permissionsOk = checkPermissions()
        Log.d(TAG, "Permissions check result: $permissionsOk")
        
        if (_isFallDetectionActive.value) {
            stopFallDetection()
        } else {
            // For testing, start detection regardless of permissions
            startFallDetection()
            /* Original conditional code, commented out for testing
            if (permissionsOk) {
                startFallDetection()
            } else {
                Log.e(TAG, "Cannot toggle on: permissions not granted")
            }
            */
        }
    }
    
    /**
     * Check if the service is running
     */
    private fun checkServiceStatus() {
        // Use the preference to determine if service should be running
        viewModelScope.launch {
            _isFallDetectionActive.value = preferencesManager.getFallDetection()
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun checkPermissions(): Boolean {
        Log.d(TAG, "Checking permissions...")
        
        val permissionsMap = mutableMapOf<String, Boolean>()
        
        // Check SMS permission
        val smsPermission = android.Manifest.permission.SEND_SMS
        val smsGranted = ContextCompat.checkSelfPermission(context, smsPermission) == 
                PackageManager.PERMISSION_GRANTED
        permissionsMap[smsPermission] = smsGranted
        Log.d(TAG, "SMS permission: $smsGranted")
        
        // Check location permissions
        val fineLocationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, fineLocationPermission) == 
                PackageManager.PERMISSION_GRANTED
        permissionsMap[fineLocationPermission] = fineLocationGranted
        Log.d(TAG, "Fine location permission: $fineLocationGranted")
        
        val coarseLocationPermission = android.Manifest.permission.ACCESS_COARSE_LOCATION
        val coarseLocationGranted = ContextCompat.checkSelfPermission(context, coarseLocationPermission) == 
                PackageManager.PERMISSION_GRANTED
        permissionsMap[coarseLocationPermission] = coarseLocationGranted
        Log.d(TAG, "Coarse location permission: $coarseLocationGranted")
        
        // Background location check for Android 10+ (optional)
        var backgroundLocationGranted = true // Default to true for API < Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val backgroundLocationPermission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            backgroundLocationGranted = ContextCompat.checkSelfPermission(context, backgroundLocationPermission) == 
                    PackageManager.PERMISSION_GRANTED
            permissionsMap[backgroundLocationPermission] = backgroundLocationGranted
            Log.d(TAG, "Background location permission: $backgroundLocationGranted")
        }
        
        // Required permissions must all be granted
        val requiredGranted = smsGranted && fineLocationGranted && coarseLocationGranted
        
        // On Android 10+, background location is also required but we'll handle it separately
        val allGranted = requiredGranted && backgroundLocationGranted
        
        Log.d(TAG, "All permissions granted: $allGranted (required: $requiredGranted, background: $backgroundLocationGranted)")
        
        _permissionsGranted.value = allGranted
        return allGranted
    }
    
    /**
     * Required permissions for fall detection
     */
    fun getRequiredPermissions(): List<String> {
        return mutableListOf(
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    /**
     * Get background location permission - this should be requested separately
     * after the basic permissions are granted
     */
    fun getBackgroundLocationPermission(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        } else {
            null
        }
    }

    /**
     * Check if all required permissions are granted
     * @return true if all permissions are granted, false otherwise
     */
    fun hasPermission(): Boolean {
        return checkPermissions()
    }

    fun checkAndRequestPermission(): Boolean {
        return hasPermission()
    }

    /**
     * Handle the result of permission requests
     * @param permissions Map of permissions and their grant status
     */
    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        Log.d(TAG, "Permission result: $permissions")
        val allGranted = permissions.all { it.value }
        
        if (allGranted) {
            Log.d(TAG, "All permissions granted, updating permission state")
            _permissionsGranted.value = true
            // If the user was trying to enable fall detection, start the service now
            if (_isFallDetectionActive.value == false) {
                toggleFallDetection()
            }
        } else {
            Log.d(TAG, "Some permissions denied")
            _permissionsGranted.value = false
        }
    }

    /**
     * For testing purposes only: Manually trigger a fall detection alert
     * This can be used to verify that fall detection is working correctly
     */
    fun testFallDetection() {
        Log.d(TAG, "Manually triggering fall detection for testing")
        try {
            // Create a new intent to send a command to the service
            val intent = Intent(context, FallDetectionService::class.java)
            intent.action = "com.example.urban_safety.TEST_FALL_DETECTION"
            
            // Start the service if not already running
            if (!_isFallDetectionActive.value) {
                startFallDetection()
            }
            
            // Send the test command to the service
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error when testing fall detection: ${e.message}", e)
        }
    }
} 