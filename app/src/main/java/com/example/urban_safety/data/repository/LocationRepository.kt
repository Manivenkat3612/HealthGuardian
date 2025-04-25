package com.example.urban_safety.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.model.SafetyIncident
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date

@Singleton
class LocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val auth = FirebaseAuth.getInstance()
    private val realtime = FirebaseDatabase.getInstance()
    private val locationsRef = realtime.getReference("user_locations")
    private val incidentsRef = realtime.getReference("safety_incidents")
    private val usersRef = realtime.getReference("users")
    
    private var lastKnownLocation: Location? = null
    private var lastKnownLocationData: LocationData? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        // Static methods for the companion object
        fun fromLocation(location: Location): LocationData {
            return LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = Date(location.time)
            )
        }
        
        fun fromModelLocationData(model: com.example.urban_safety.models.LocationData): LocationData {
            return LocationData(
                latitude = model.latitude,
                longitude = model.longitude,
                accuracy = model.accuracy ?: 0f,
                timestamp = Date(model.timestamp)
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        return REQUIRED_PERMISSIONS.any { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getLastKnownLocation(): Location? = lastKnownLocation

    fun updateLocation(locationData: LocationData) {
        lastKnownLocationData = locationData
        lastKnownLocation = Location("").apply {
            latitude = locationData.latitude
            longitude = locationData.longitude
            accuracy = locationData.accuracy ?: 0f
            time = locationData.timestamp.time
        }
    }
    
    suspend fun getLastLocation(): Result<LocationData?> {
        return try {
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }
            
            val locationData = lastKnownLocationData
            if (locationData != null) {
                Result.success(locationData)
            } else {
                Result.success(null)
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun getLocationUpdates(): Flow<Location?> = callbackFlow {
        if (!hasLocationPermission()) {
            trySend(null)
            awaitClose()
            return@callbackFlow
        }

        val locationCallback = object : android.location.LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
                lastKnownLocation = location
                lastKnownLocationData = LocationData.fromLocation(location)
            }
        }
        
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                10000L, // 10 seconds
                10f, // 10 meters
                locationCallback
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
            trySend(null)
        } catch (e: Exception) {
            e.printStackTrace()
            trySend(null)
        }
        
        awaitClose {
            try {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationManager.removeUpdates(locationCallback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    suspend fun shareLocationWithContacts(userId: String, location: LocationData) {
        try {
            // Convert to UI model before saving to Firebase
            val uiModel = location.toModelLocationData()
            locationsRef.child(userId).setValue(uiModel).await()
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun stopSharingLocation(userId: String) {
        try {
            locationsRef.child(userId).removeValue().await()
        } catch (e: Exception) {
            throw e
        }
    }
    
    fun getNearbyIncidents(location: LocationData, radiusKm: Double): Flow<List<SafetyIncident>> = callbackFlow {
        val incidentsListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val incidents = mutableListOf<SafetyIncident>()
                for (child in snapshot.children) {
                    val incident = child.getValue<SafetyIncident>()
                    if (incident != null && isWithinRadius(incident.location, location, radiusKm)) {
                        incidents.add(incident)
                    }
                }
                trySend(incidents)
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // Handle error
            }
        }
        
        incidentsRef.addValueEventListener(incidentsListener)
        
        awaitClose {
            incidentsRef.removeEventListener(incidentsListener)
        }
    }
    
    fun getNearbyUsers(location: LocationData, radiusKm: Double): Flow<List<String>> = callbackFlow {
        val usersListener = object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val nearbyUsers = mutableListOf<String>()
                for (child in snapshot.children) {
                    // Firebase stores the UI model, so convert it to data model
                    val userLocationUI = child.getValue<com.example.urban_safety.models.LocationData>()
                    val userLocation = userLocationUI?.let { 
                        LocationData.fromModelLocationData(it)
                    }
                    if (userLocation != null && isWithinRadius(userLocation, location, radiusKm)) {
                        nearbyUsers.add(child.key ?: "")
                    }
                }
                trySend(nearbyUsers)
            }
            
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // Handle error
            }
        }
        
        locationsRef.addValueEventListener(usersListener)
        
        awaitClose {
            locationsRef.removeEventListener(usersListener)
        }
    }
    
    fun getSafeRoute(start: LocationData, end: LocationData): Flow<List<LocationData>> = callbackFlow {
        // In a real implementation, this would use a routing service
        // For now, we'll just return a direct route
        val route = listOf(start, end)
        trySend(route)
        awaitClose()
    }
    
    private fun isWithinRadius(loc1: LocationData?, loc2: LocationData, radiusKm: Double): Boolean {
        if (loc1 == null) return false
        
        val earthRadius = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(loc2.latitude - loc1.latitude)
        val dLon = Math.toRadians(loc2.longitude - loc1.longitude)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(loc1.latitude)) * Math.cos(Math.toRadians(loc2.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = earthRadius * c
        
        return distance <= radiusKm
    }

    // Extension functions to help with model conversions
    private fun LocationData.toModelLocationData(): com.example.urban_safety.models.LocationData {
        return com.example.urban_safety.models.LocationData(
            latitude = this.latitude,
            longitude = this.longitude,
            accuracy = this.accuracy,
            timestamp = this.timestamp.time
        )
    }
} 