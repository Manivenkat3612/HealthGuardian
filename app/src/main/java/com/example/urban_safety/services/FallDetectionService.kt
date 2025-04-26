package com.example.urban_safety.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.urban_safety.MainActivity
import com.example.urban_safety.R
import com.example.urban_safety.data.model.EmergencyContact
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.repositories.EmergencyContactsRepository
import com.example.urban_safety.util.SmsService
import com.example.urban_safety.ui.EmergencyCancelActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt

@AndroidEntryPoint
class FallDetectionService : Service(), SensorEventListener {

    companion object {
        private const val TAG = "FallDetectionService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "fall_detection_channel"
        private const val CHANNEL_NAME = "Fall Detection"
        
        // Fall detection parameters
        private const val FALL_THRESHOLD = 15.0f  // Acceleration threshold for potential fall in m/s²
        private const val IMPACT_THRESHOLD = 20.0f // Impact threshold for confirming fall
        private const val INACTIVITY_THRESHOLD = 0.8f // Threshold for detecting inactivity after fall
        private const val INACTIVITY_DURATION = 2000 // Duration of inactivity in ms to confirm fall
        
        // Action constants
        private const val ACTION_CANCEL_FALL_ALERT = "com.example.urban_safety.CANCEL_FALL_ALERT"
        private const val ACTION_TEST_FALL_DETECTION = "com.example.urban_safety.TEST_FALL_DETECTION"
        
        /**
         * Creates an intent to cancel an ongoing fall alert
         */
        fun getCancelIntent(context: Context): Intent {
            return Intent(context, FallDetectionService::class.java).apply {
                action = ACTION_CANCEL_FALL_ALERT
            }
        }
    }
    
    @Inject lateinit var smsService: SmsService
    @Inject lateinit var emergencyContactsRepository: EmergencyContactsRepository
    @Inject lateinit var locationRepository: LocationRepository
    
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private var gyroscopeSensor: Sensor? = null
    
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    // Fall detection variables
    private var accelerationMagnitude = 0.0f
    private var previousAccelerationMagnitude = 0.0f
    private var lastAccelValues = floatArrayOf(0f, 0f, 0f)
    private var lastGyroValues = floatArrayOf(0f, 0f, 0f)
    private var potentialFallDetectedTime = 0L
    private var inactivityStartTime = 0L
    private var fallDetected = false
    private var orientationChanged = false
    private var isInactive = false
    private var alertCancelled = false
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Fall Detection Service created")
        
        try {
            // Initialize sensors
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            
            // Check if sensors are available
            if (accelerometerSensor == null) {
                Log.e(TAG, "Accelerometer sensor not available")
            }
            if (gyroscopeSensor == null) {
                Log.e(TAG, "Gyroscope sensor not available")
            }
            
            // Create wakelock to keep CPU running
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "UrbanSafety::FallDetectionWakeLock"
            )
            
            // Must start as foreground immediately in onCreate for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Creating notification and starting as foreground in onCreate")
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Fall Detection Service onStartCommand with action: ${intent?.action}")
        
        // Handle test action if present
        if (intent?.action == ACTION_TEST_FALL_DETECTION) {
            Log.d(TAG, "Received test fall detection command")
            testFallDetection()
            return START_STICKY
        }
        
        // Handle cancel action
        if (intent?.action == ACTION_CANCEL_FALL_ALERT) {
            Log.d(TAG, "Received cancel fall alert command")
            
            synchronized(this) {
                alertCancelled = true
                Log.d(TAG, "Alert cancelled flag set to: $alertCancelled")
            }
            
            // Cancel any ongoing notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(9999)
            
            // Don't call resetFallDetectionState() here as it might reset other important state
            // Only reset fall detection after a delay, this ensures the cancellation is processed
            serviceScope.launch {
                delay(1000) // Give the cancellation a second to take effect
                Log.d(TAG, "Fall detection state will be reset shortly")
                
                // Keep the alertCancelled flag true but reset other state
                synchronized(this@FallDetectionService) {
                    potentialFallDetectedTime = 0L
                    orientationChanged = false
                    isInactive = false
                    inactivityStartTime = 0L
                    fallDetected = false
                    
                    Log.d(TAG, "Partial reset complete, alertCancelled=$alertCancelled")
                }
            }
            
            return START_STICKY
        }
        
        try {
            // For pre-O devices, start foreground here
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                Log.d(TAG, "Starting as foreground in onStartCommand")
                startForeground(NOTIFICATION_ID, createNotification())
            }
            
            // Acquire wakelock
            wakeLock?.acquire(10*60*1000L /*10 minutes*/)
            
            // Register sensor listeners
            accelerometerSensor?.let {
                sensorManager.registerListener(
                    this,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                Log.d(TAG, "Registered accelerometer listener")
            }
            
            gyroscopeSensor?.let {
                sensorManager.registerListener(
                    this,
                    it,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
                Log.d(TAG, "Registered gyroscope listener")
            }
            
            Log.d(TAG, "Fall detection service successfully started")
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand: ${e.message}", e)
            stopSelf()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> processAccelerometerData(event)
            Sensor.TYPE_GYROSCOPE -> processGyroscopeData(event)
        }
        
        // Check for fall
        detectFall()
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
    
    private fun processAccelerometerData(event: SensorEvent) {
        // Get accelerometer values
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        lastAccelValues = floatArrayOf(x, y, z)
        
        // Calculate acceleration magnitude
        previousAccelerationMagnitude = accelerationMagnitude
        accelerationMagnitude = sqrt(x * x + y * y + z * z)
        
        // Check for inactivity after potential fall
        if (potentialFallDetectedTime > 0 && !isInactive) {
            // If the device is relatively still (close to gravity only)
            val movement = abs(accelerationMagnitude - 9.8f)
            if (movement < INACTIVITY_THRESHOLD) {
                if (inactivityStartTime == 0L) {
                    inactivityStartTime = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - inactivityStartTime > INACTIVITY_DURATION) {
                    isInactive = true
                    Log.d(TAG, "Inactivity detected after potential fall")
                }
            } else {
                // Reset inactivity timer if there's movement
                inactivityStartTime = 0L
            }
        }
    }
    
    private fun processGyroscopeData(event: SensorEvent) {
        // Get gyroscope values (rotation rate around each axis in rad/s)
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        lastGyroValues = floatArrayOf(x, y, z)
        
        // Check for significant orientation change after potential fall
        if (potentialFallDetectedTime > 0 && !orientationChanged) {
            // Calculate rotation magnitude
            val rotationMagnitude = sqrt(x * x + y * y + z * z)
            
            // If there's significant rotation, consider orientation changed
            if (rotationMagnitude > 1.5f) {
                orientationChanged = true
                Log.d(TAG, "Orientation change detected during potential fall")
            }
        }
    }
    
    private fun detectFall() {
        // Step 1: Detect sudden motion (spike in acceleration)
        if (accelerationMagnitude > FALL_THRESHOLD && previousAccelerationMagnitude < FALL_THRESHOLD) {
            Log.d(TAG, "Potential fall detected: Acceleration spike ($accelerationMagnitude)")
            potentialFallDetectedTime = System.currentTimeMillis()
            // Reset fall detection variables
            orientationChanged = false
            isInactive = false
            inactivityStartTime = 0L
            fallDetected = false
        }
        
        // If we have a potential fall, check other conditions
        if (potentialFallDetectedTime > 0 && !fallDetected) {
            // Step 2: Check for impact (stronger acceleration)
            if (accelerationMagnitude > IMPACT_THRESHOLD) {
                Log.d(TAG, "Impact detected during potential fall")
            }
            
            // Step 3 & 4: If we have orientation change AND inactivity, it's likely a fall
            if (orientationChanged && isInactive) {
                fallDetected = true
                Log.d(TAG, "FALL DETECTED - Notifying emergency contacts")
                
                // Handle fall detection
                handleFallDetection()
                
                // Reset fall detection state
                resetFallDetectionState()
            }
            
            // If too much time has passed without confirming fall, reset
            if (System.currentTimeMillis() - potentialFallDetectedTime > 10000) { // 10 seconds
                Log.d(TAG, "Resetting potential fall detection - timeout")
                resetFallDetectionState()
            }
        }
    }
    
    private fun handleFallDetection() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Handling fall detection")
                // Use the new emergency alert method
                triggerEmergencyAlert()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling fall detection: ${e.message}")
            }
        }
    }
    
    private suspend fun notifyEmergencyContacts(location: LocationData?) {
        try {
            // Get emergency contacts
            val contacts = emergencyContactsRepository.getEmergencyContacts().first()
            
            if (contacts.isEmpty()) {
                Log.d(TAG, "No emergency contacts available to notify")
                return
            }
            
            // Get location string
            val locationText = if (location != null) {
                "Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                "Location not available"
            }
            
            // Create fall detection message
            val message = "FALL DETECTED: A fall has been detected. This may be an emergency. $locationText"
            
            // Send SMS to all emergency contacts
            for (contact in contacts) {
                try {
                    withContext(Dispatchers.IO) {
                        smsService.sendEmergencySMS(contact.phoneNumber, message)
                    }
                    Log.d(TAG, "Emergency SMS sent to ${contact.name} (${contact.phoneNumber})")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send SMS to ${contact.phoneNumber}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error notifying emergency contacts: ${e.message}")
        }
    }
    
    private fun resetFallDetectionState() {
        Log.d(TAG, "Resetting fall detection state, current alertCancelled=$alertCancelled")
        potentialFallDetectedTime = 0L
        orientationChanged = false
        isInactive = false
        inactivityStartTime = 0L
        fallDetected = false
        // Don't reset alertCancelled here as it might interfere with cancellation
        // Logic for alertCancelled is handled in onStartCommand and triggerEmergencyAlert
        
        // Log the final state
        Log.d(TAG, "Fall detection state reset complete, alertCancelled=$alertCancelled")
    }
    
    private fun createNotification(): Notification {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fall Detection Active")
            .setContentText("Monitoring for potential falls")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Fall detection service notification channel"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Fall Detection Service destroyed")
        
        // Unregister sensor listeners
        sensorManager.unregisterListener(this)
        
        // Release wakelock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        
        // Cancel the job
        serviceJob.cancel()
    }

    /**
     * For testing: Manually trigger fall detection alert
     * This method simulates a fall by setting the necessary conditions and calling triggerEmergencyAlert
     */
    fun testFallDetection() {
        Log.d(TAG, "Testing fall detection by manually triggering an alert")
        fallDetected = true
        serviceScope.launch {
            triggerEmergencyAlert()
        }
    }

    /**
     * Trigger an emergency alert with notification and SMS
     * Used for testing and actual fall detection scenarios
     */
    private suspend fun triggerEmergencyAlert() {
        try {
            Log.d(TAG, "Triggering emergency alert with 5-second cancellation window")
            
            // Explicitly reset the cancelled flag at the start
            synchronized(this) {
                alertCancelled = false
                Log.d(TAG, "Reset alertCancelled to false at start of emergency sequence")
            }
            
            // Launch the EmergencyCancelActivity instead of showing a notification
            val cancelIntent = Intent(this, EmergencyCancelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EmergencyCancelActivity.EXTRA_EMERGENCY_TYPE, EmergencyCancelActivity.TYPE_FALL_DETECTION)
            }
            startActivity(cancelIntent)
            
            // Wait for 5 seconds to allow user to cancel via the activity
            for (i in 5 downTo 1) {
                Log.d(TAG, "Fall alert countdown: $i seconds remaining, alertCancelled=$alertCancelled")
                delay(1000) // Wait for 1 second
                
                // Check if alert was cancelled during the delay
                synchronized(this) {
                    if (alertCancelled) {
                        Log.d(TAG, "Fall alert was cancelled during countdown - STOPPING NOTIFICATIONS")
                        return
                    }
                }
            }
            
            // Double-check cancelled flag before proceeding
            synchronized(this) {
                if (alertCancelled) {
                    Log.d(TAG, "Alert cancelled after countdown - not sending notifications")
                    return
                }
                
                // If we got here, alert wasn't cancelled, proceed with sending notifications
                Log.d(TAG, "Countdown complete, alertCancelled=$alertCancelled - proceeding with emergency notifications")
            }
            
            // Create and show a high-priority notification that emergency contacts are being notified
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create a special channel for emergency alerts if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "fall_detection_emergency",
                    "Fall Detection Emergency",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Used for fall detection emergency alerts"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    setBypassDnd(true) // Important: Bypass Do Not Disturb
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // Create view details intent
            val viewIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("SHOW_FALL_DETAILS", true)
            }
            val viewPendingIntent = PendingIntent.getActivity(
                this, 1002, viewIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build the notification
            val builder = NotificationCompat.Builder(this, "fall_detection_emergency")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Fall Detected")
                .setContentText("Emergency contacts are being notified")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .setLights(Color.RED, 3000, 3000)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(viewPendingIntent)
                .addAction(R.drawable.ic_notification, "View Details", viewPendingIntent)
            
            notificationManager.notify(9999, builder.build())
            
            // Get user's location and notify emergency contacts
            val location = try {
                locationRepository.getLastLocation().getOrNull()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location: ${e.message}")
                null
            }
            
            // Send SMS to emergency contacts
            notifyEmergencyContacts(location)
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering emergency alert: ${e.message}", e)
        }
    }
} 