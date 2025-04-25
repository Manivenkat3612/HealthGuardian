package com.example.urban_safety.ui.viewmodels

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.model.SafetyIncident
import com.example.urban_safety.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for location-related functionality
 */
@HiltViewModel
class LocationViewModel @Inject constructor(
    application: Application,
    private val locationRepository: LocationRepository
) : AndroidViewModel(application) {
    
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation
    
    private val _isLocationSharing = MutableStateFlow(false)
    val isLocationSharing: StateFlow<Boolean> = _isLocationSharing.asStateFlow()
    
    private val _nearbyIncidents = MutableStateFlow<List<SafetyIncident>>(emptyList())
    val nearbyIncidents: StateFlow<List<SafetyIncident>> = _nearbyIncidents.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        startLocationUpdates()
    }
    
    /**
     * Start location updates
     */
    private fun startLocationUpdates() {
        viewModelScope.launch {
            locationRepository.getLocationUpdates().collect { location ->
                _currentLocation.value = location?.let { 
                    com.example.urban_safety.data.repository.LocationRepository.fromLocation(it) 
                }
                
                // If location sharing is enabled, update shared location
                if (_isLocationSharing.value) {
                    updateSharedLocation(_currentLocation.value)
                }
                
                // Update nearby incidents if needed
                if (_currentLocation.value != null) {
                    updateNearbyIncidents(_currentLocation.value!!)
                }
            }
        }
    }
    
    /**
     * Start sharing location with emergency contacts
     */
    fun startLocationSharing() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            val location = _currentLocation.value
            
            if (userId != null && location != null) {
                try {
                    locationRepository.shareLocationWithContacts(userId, location)
                    _isLocationSharing.value = true
                } catch (e: Exception) {
                    // Handle error
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Stop sharing location with emergency contacts
     */
    fun stopLocationSharing() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            
            if (userId != null) {
                try {
                    locationRepository.stopSharingLocation(userId)
                    _isLocationSharing.value = false
                } catch (e: Exception) {
                    // Handle error
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Update shared location
     */
    private suspend fun updateSharedLocation(location: LocationData?) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        
        if (userId != null && location != null) {
            try {
                locationRepository.shareLocationWithContacts(userId, location)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Get the last known location
     */
    fun getCurrentLocation(onComplete: (Result<LocationData>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = locationRepository.getLastLocation()
            if (result.isSuccess) {
                val location = result.getOrNull()
                _currentLocation.value = location
                
                _currentLocation.value?.let {
                    onComplete(Result.success(it))
                } ?: onComplete(Result.failure(Exception("Location not available")))
            } else {
                onComplete(Result.failure(result.exceptionOrNull() ?: Exception("Unknown error")))
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Update nearby incidents
     */
    private fun updateNearbyIncidents(location: LocationData) {
        viewModelScope.launch {
            try {
                locationRepository.getNearbyIncidents(location, 5.0).collect { incidents ->
                    _nearbyIncidents.value = incidents
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    /**
     * Get nearby app users who can help in emergency
     */
    fun getNearbyUsers(radiusKm: Double, onComplete: (Result<List<String>>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val location = _currentLocation.value
            if (location != null) {
                try {
                    val nearbyUsers = mutableListOf<String>()
                    locationRepository.getNearbyUsers(location, radiusKm).collect { users ->
                        nearbyUsers.clear()
                        nearbyUsers.addAll(users)
                    }
                    onComplete(Result.success(nearbyUsers))
                } catch (e: Exception) {
                    onComplete(Result.failure(e))
                }
            } else {
                onComplete(Result.failure(Exception("Location not available")))
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Get safe route between two points
     */
    fun getSafeRoute(
        start: LocationData,
        end: LocationData,
        onComplete: (Result<List<LocationData>>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val routes = mutableListOf<LocationData>()
                locationRepository.getSafeRoute(start, end).collect { routePoints ->
                    routes.clear()
                    routes.addAll(routePoints)
                }
                onComplete(Result.success(routes))
            } catch (e: Exception) {
                onComplete(Result.failure(e))
            }
            
            _isLoading.value = false
        }
    }

    fun updateLocation(location: Location) {
        _currentLocation.value = com.example.urban_safety.data.repository.LocationRepository.fromLocation(location)
    }
} 