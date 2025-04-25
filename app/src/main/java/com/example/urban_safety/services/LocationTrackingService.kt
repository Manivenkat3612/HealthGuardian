package com.example.urban_safety.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.urban_safety.MainActivity
import com.example.urban_safety.R
import com.example.urban_safety.HealthGuardianApp
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.model.EmergencyContact
import com.example.urban_safety.data.model.User
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.util.PreferencesManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.*

/**
 * Foreground service for continuous location tracking and sharing
 */
@Singleton
@AndroidEntryPoint
class LocationTrackingService @Inject constructor() : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    
    @Inject
    lateinit var locationRepository: LocationRepository
    
    private val auth = FirebaseAuth.getInstance()
    
    private lateinit var lastKnownLocation: LocationData
    private lateinit var lastLocationUpdateTime: Date
    
    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "location_tracking"
        const val CHANNEL_NAME = "Location Tracking"
        
        // Service actions
        const val ACTION_START_TRACKING = "com.example.urban_safety.ACTION_START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.example.urban_safety.ACTION_STOP_TRACKING"
        const val ACTION_SHARE_LOCATION = "com.example.urban_safety.SHARE_LOCATION"
        const val ACTION_STOP_SHARING = "com.example.urban_safety.STOP_SHARING"
        
        // Extra parameters
        const val EXTRA_INTERVAL_MS = "interval_ms"
        const val EXTRA_SHARE_WITH_CONTACTS = "share_with_contacts"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                onNewLocation(result.lastLocation)
            }
        }
        
        setupLocationCallback()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startLocationTracking()
            ACTION_STOP_TRACKING -> stopLocationTracking()
            ACTION_SHARE_LOCATION -> startLocationSharing()
            ACTION_STOP_SHARING -> stopLocationSharing()
        }
        
        // Make the service more resilient to system kills
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // No binding supported
    }
    
    override fun onDestroy() {
        stopLocationTracking()
        stopLocationSharing()
        serviceScope.cancel()
        super.onDestroy()
    }
    
    /**
     * Start tracking location
     */
    private fun startLocationTracking() {
        val notification = createForegroundNotification()
        
        startForeground(NOTIFICATION_ID, notification)
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setMinUpdateIntervalMillis(5000)
            .build()
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Handle permission denied
        }
    }
    
    /**
     * Stop tracking location
     */
    private fun stopLocationTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(true)
        stopSelf()
    }
    
    /**
     * Start sharing location with contacts
     */
    private fun startLocationSharing() {
        // Flag to prevent duplicate sharing
        val isSharing = true
    }
    
    /**
     * Stop sharing location
     */
    private fun stopLocationSharing() {
        val user = auth.currentUser ?: return
        
        serviceScope.launch {
            try {
                locationRepository.stopSharingLocation(user.uid)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Handle new location update
     */
    private fun onNewLocation(location: Location?) {
        location ?: return
        
        val locationData = createLocationData(location)
        locationRepository.updateLocation(locationData)
        updateNotification(locationData)
    }
    
    private fun updateNotification(locationData: LocationData) {
        val notification = createForegroundNotification()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val locationData = createLocationData(location)
                    serviceScope.launch {
                        locationRepository.updateLocation(locationData)
                    }
                }
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Location tracking for safety features"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createLocationData(location: Location): LocationData {
        return LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = Date(System.currentTimeMillis())
        )
    }
    
    private fun updateLastKnownLocation(location: Location) {
        lastKnownLocation = createLocationData(location)
        lastLocationUpdateTime = Date(System.currentTimeMillis())
    }
    
    /**
     * Returns the last known location data for other services to use
     */
    fun getLastKnownLocation(): LocationData? {
        return if (::lastKnownLocation.isInitialized) {
            lastKnownLocation
        } else {
            null
        }
    }
    
    private fun createForegroundNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, pendingIntentFlags
        )

        return NotificationCompat.Builder(this, HealthGuardianApp.LOCATION_CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Your location is being monitored for safety")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
} 