package com.example.urban_safety.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.urban_safety.MainActivity
import com.example.urban_safety.R
import com.example.urban_safety.data.model.HealthData
import com.example.urban_safety.data.model.IncidentStatus
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.model.SafetyIncident
import com.example.urban_safety.data.model.User
import com.example.urban_safety.data.repository.HealthRepository
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.data.repository.SafetyRepository
import com.example.urban_safety.repositories.EmergencyContactsRepository
import com.example.urban_safety.util.Constants
import com.example.urban_safety.util.PreferencesManager
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Service to monitor health data from wearables and detect anomalies
 */
@AndroidEntryPoint
class HealthMonitoringService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private lateinit var dataClient: DataClient
    
    @Inject
    lateinit var safetyRepository: SafetyRepository
    
    @Inject
    lateinit var locationRepository: LocationRepository
    
    @Inject
    lateinit var healthRepository: HealthRepository
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    @Inject
    lateinit var emergencyContactsRepository: EmergencyContactsRepository
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private var monitoringJob: Job? = null
    private var userPreferences: User? = null
    
    companion object {
        const val ACTION_START_MONITORING = "com.example.urban_safety.ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.urban_safety.ACTION_STOP_MONITORING"
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "health_monitoring"
        private const val CHANNEL_NAME = "Health Monitoring"
        
        // Extra parameters
        const val EXTRA_HEART_RATE_THRESHOLD = "heart_rate_threshold"
        const val EXTRA_CHECK_INTERVAL_MS = "check_interval_ms"
    }
    
    override fun onCreate() {
        super.onCreate()
        dataClient = Wearable.getDataClient(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startHealthMonitoring()
            ACTION_STOP_MONITORING -> stopHealthMonitoring()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // No binding supported
    }
    
    override fun onDestroy() {
        stopHealthMonitoring()
        super.onDestroy()
    }
    
    /**
     * Start monitoring health data
     */
    private fun startHealthMonitoring() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        monitoringJob = serviceScope.launch {
            val heartRateThreshold = preferencesManager.getHeartRateThreshold()
            val checkIntervalMs = preferencesManager.getHealthCheckIntervalMs()
            
            // Start health repository monitoring
            healthRepository.startHealthMonitoring()
            
            // Periodic check of health data
            while (isActive) {
                try {
                    // Read health data from repository or directly from device
                    val healthData = healthRepository.getLastHealthData() ?: readHealthData()
                    
                    // Check for anomalies if health data available
                    if (healthData != null) {
                        checkForHealthAnomalies(healthData, heartRateThreshold)
                    }
                    
                    // Wait for next check interval
                    delay(checkIntervalMs)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Wait shorter time on error
                    delay(5000)
                }
            }
        }
    }
    
    /**
     * Stop health monitoring
     */
    private fun stopHealthMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        serviceScope.launch {
            healthRepository.stopHealthMonitoring()
        }
        stopForeground(true)
        stopSelf()
    }
    
    /**
     * Read health data from Google Fit or other sources
     */
    private suspend fun readHealthData(): HealthData? {
        // First try to read from health repository
        val repoData = healthRepository.getLastHealthData()
        if (repoData != null) {
            return repoData
        }
        
        // For wearables integration, we would use the Wearable API
        // Check if we have any pending data from wearable
        try {
            val dataEvents = Wearable.getDataClient(this).getDataItems().await()
            
            for (event in dataEvents) {
                if (event.uri.path?.compareTo("/health_data") == 0) {
                    val dataMap = DataMapItem.fromDataItem(event).dataMap
                    
                    // Extract health data from the DataMap
                    val heartRate = dataMap.getInt("heart_rate", 0)
                    val stepCount = dataMap.getInt("step_count", 0)
                    val bloodOxygen = if (dataMap.containsKey("blood_oxygen")) 
                        dataMap.getInt("blood_oxygen") else null
                    
                    // Create and return health data object
                    return HealthData(
                        heartRate = heartRate,
                        stepCount = stepCount,
                        bloodOxygen = bloodOxygen,
                        timestamp = Date()
                    )
                }
            }
            
            // Close all items to avoid memory leaks
            dataEvents.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // For demonstration, return simulated data
        return createSimulatedHealthData()
    }
    
    /**
     * Check health data for anomalies
     */
    private suspend fun checkForHealthAnomalies(healthData: HealthData, heartRateThreshold: Int) {
        // Skip if health monitoring is not enabled in user preferences
        if (!preferencesManager.isHealthMonitoringEnabled) {
            return
        }
        
        // Check for anomalies
        var anomalyDetected = false
        var anomalyDescription = ""
        
        // Heart rate too high - emergency
        if (healthData.heartRate > heartRateThreshold) {
            anomalyDetected = true
            anomalyDescription = "High heart rate detected: ${healthData.heartRate} BPM (threshold: $heartRateThreshold BPM)"
        }
        
        // Very low heart rate - emergency
        else if (healthData.heartRate < 40 && healthData.heartRate > 0) {
            anomalyDetected = true
            anomalyDescription = "Low heart rate detected: ${healthData.heartRate} BPM (below safe threshold)"
        }
        
        // Check oxygen levels if available
        healthData.bloodOxygen?.let { oxygen ->
            if (oxygen < 90 && oxygen > 0) {
                // Low blood oxygen is a serious condition
                anomalyDetected = true
                anomalyDescription += "\nLow blood oxygen level: $oxygen% (below safe threshold of 90%)"
            }
        }
        
        // If anomaly detected, trigger SOS
        if (anomalyDetected) {
            triggerSOS(IncidentType.HEALTH_EMERGENCY, healthData)
            
            // Create and show notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Health Emergency Detected")
                .setContentText(anomalyDescription)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                
            // Add action to cancel the alert if it's a false alarm
            val cancelIntent = Intent(this, HealthMonitoringService::class.java).apply {
                action = "com.example.urban_safety.CANCEL_HEALTH_ALERT"
            }
            val cancelPendingIntent = PendingIntent.getService(
                this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_notification,
                "False Alarm",
                cancelPendingIntent
            )
            
            // Intent to open the app
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            builder.setContentIntent(pendingIntent)
            
            // Show notification
            notificationManager.notify(NOTIFICATION_ID + 1, builder.build())
        }
    }
    
    /**
     * Trigger SOS alert based on health anomaly
     */
    private suspend fun triggerSOS(incidentType: IncidentType, healthData: HealthData) {
        try {
            // Get current location
            val locationResult = locationRepository.getLastLocation()
            val location = locationResult.getOrNull()
            // Use the LocationData directly, no need for conversion since getLastLocation already returns LocationData
            val locationData = location
            
            // Create incident
            safetyRepository.createSafetyIncident(
                type = incidentType,
                locationData = locationData,
                description = "Automatically triggered by health monitoring: " + Constants.AUTO_HEALTH_TRIGGERED
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Health Monitoring Active")
            .setContentText("Monitoring your health data for safety")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Health monitoring for safety features"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun triggerEmergencyAlert(healthData: HealthData) {
        serviceScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // Get the last known location
                val locationResult = locationRepository.getLastLocation()
                locationResult.getOrNull()?.let { locationData ->
                    // No need for conversion, directly use locationData
                    
                    // Create and save the incident
                    val incident = SafetyIncident(
                        userId = currentUser.uid,
                        type = IncidentType.HEALTH_EMERGENCY,
                        description = "Abnormal health readings detected: ${healthData.heartRate} BPM",
                        location = locationData,
                        timestamp = Date(),
                        status = IncidentStatus.ACTIVE
                    )
                    
                    // Save the incident
                    safetyRepository.createSafetyIncident(
                        type = IncidentType.HEALTH_EMERGENCY,
                        locationData = locationData,
                        description = "Abnormal health readings detected: ${healthData.heartRate} BPM"
                    )
                    
                    // Notify emergency contacts
                    // Note: You may need to implement this method in EmergencyContactsRepository
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Create simulated health data for testing
     */
    private fun createSimulatedHealthData(): HealthData {
        // Normal simulation
        if (Math.random() < 0.9) {
            // 90% chance of normal values
            return HealthData(
                heartRate = (60..100).random(),
                stepCount = (100..1000).random(),
                bloodOxygen = (94..99).random(),
                timestamp = Date()
            )
        } else {
            // 10% chance of abnormal values for testing
            return HealthData(
                heartRate = (120..160).random(), // Abnormally high heart rate
                stepCount = (10..50).random(),
                bloodOxygen = (85..90).random(), // Lower than normal
                timestamp = Date()
            )
        }
    }
} 