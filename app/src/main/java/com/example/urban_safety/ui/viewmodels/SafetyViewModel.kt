package com.example.urban_safety.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.EmergencyContact
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.model.SafetyIncident
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.data.repository.SafetyRepository
import com.example.urban_safety.util.Constants
import com.example.urban_safety.util.ModelConverters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for safety-related functionality
 */
@HiltViewModel
class SafetyViewModel @Inject constructor(
    application: Application,
    private val safetyRepository: SafetyRepository,
    private val locationRepository: LocationRepository
) : AndroidViewModel(application) {
    
    private val _activeIncidents = MutableStateFlow<List<SafetyIncident>>(emptyList())
    val activeIncidents: StateFlow<List<SafetyIncident>> = _activeIncidents.asStateFlow()
    
    private val _currentIncident = MutableStateFlow<SafetyIncident?>(null)
    val currentIncident: StateFlow<SafetyIncident?> = _currentIncident.asStateFlow()
    
    private val _emergencyContacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContact>> = _emergencyContacts.asStateFlow()
    
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isSOSActive = MutableStateFlow(false)
    val isSOSActive: StateFlow<Boolean> = _isSOSActive
    
    init {
        loadActiveIncidents()
        loadEmergencyContacts()
        startLocationUpdates()
    }
    
    /**
     * Load active incidents for the current user
     */
    private fun loadActiveIncidents() {
        viewModelScope.launch {
            safetyRepository.getActiveUserIncidents().collectLatest { incidents ->
                _activeIncidents.value = incidents
                _currentIncident.value = incidents.firstOrNull()
            }
        }
    }
    
    /**
     * Load emergency contacts for the current user
     */
    private fun loadEmergencyContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Get Firebase user ID from auth
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            
            if (userId != null) {
                val result = safetyRepository.getEmergencyContacts(userId)
                if (result.isSuccess) {
                    _emergencyContacts.value = result.getOrDefault(emptyList())
                }
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Start location updates
     */
    private fun startLocationUpdates() {
        viewModelScope.launch {
            locationRepository.getLocationUpdates().collectLatest { location ->
                if (location != null) {
                    _currentLocation.value = com.example.urban_safety.data.repository.LocationRepository.fromLocation(location)
                } else {
                    _currentLocation.value = null
                }
            }
        }
    }
    
    /**
     * Send a manual SOS alert
     */
    fun sendManualSOS(
        context: Context,
        description: String = "Manual SOS Activated: " + Constants.MANUAL_SOS,
        useLastKnownLocation: Boolean = true,
        onComplete: (Result<SafetyIncident>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Get current location if enabled
            val location = if (useLastKnownLocation) {
                try {
                    locationRepository.getLastLocation().getOrNull()
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
            
            // Create the safety incident
            val result = safetyRepository.createSafetyIncident(
                type = IncidentType.MANUAL_SOS,
                locationData = location,
                description = description
            )
            
            if (result.isSuccess) {
                // Store as current incident
                _currentIncident.value = result.getOrNull()
                
                // Add to active incidents
                val updatedIncidents = _activeIncidents.value.toMutableList()
                result.getOrNull()?.let { updatedIncidents.add(it) }
                _activeIncidents.value = updatedIncidents
            }
            
            _isLoading.value = false
            onComplete(result)
        }
    }
    
    /**
     * Cancel an active SOS (mark as false alarm)
     */
    fun cancelActiveSOS(onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val incident = _currentIncident.value
            if (incident != null) {
                val result = safetyRepository.markIncidentAsFalseAlarm(incident.id)
                if (result.isSuccess) {
                    // Update local state
                    _currentIncident.value = null
                    loadActiveIncidents()
                }
                onComplete(result)
            } else {
                onComplete(Result.failure(Exception("No active incident found")))
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Resolve an active SOS
     */
    fun resolveActiveSOS(onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val incident = _currentIncident.value
            if (incident != null) {
                val result = safetyRepository.resolveSafetyIncident(incident.id)
                if (result.isSuccess) {
                    // Update local state
                    _currentIncident.value = null
                    loadActiveIncidents()
                }
                onComplete(result)
            } else {
                onComplete(Result.failure(Exception("No active incident found")))
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Add a new emergency contact
     */
    fun addEmergencyContact(contact: EmergencyContact, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = safetyRepository.addEmergencyContact(contact)
            if (result.isSuccess) {
                loadEmergencyContacts()
            }
            
            _isLoading.value = false
            onComplete(result)
        }
    }
    
    /**
     * Update an existing emergency contact
     */
    fun updateEmergencyContact(contact: EmergencyContact, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = safetyRepository.updateEmergencyContact(contact)
            if (result.isSuccess) {
                loadEmergencyContacts()
            }
            
            _isLoading.value = false
            onComplete(result)
        }
    }
    
    /**
     * Remove an emergency contact
     */
    fun removeEmergencyContact(contact: EmergencyContact, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = safetyRepository.removeEmergencyContact(contact.name)
            if (result.isSuccess) {
                loadEmergencyContacts()
            }
            
            _isLoading.value = false
            onComplete(result)
        }
    }
    
    /**
     * Send SOS with last known location
     */
    fun sendSOSWithLastLocation(
        type: IncidentType,
        description: String = "Emergency Alert",
        onComplete: (Result<SafetyIncident>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Try to get location
            val locationResult = locationRepository.getLastLocation()
            val location = locationResult.getOrNull()
            
            // Create the safety incident
            val result = safetyRepository.createSafetyIncident(
                type = type,
                locationData = location,
                description = description
            )
            
            if (result.isSuccess) {
                // Store as current incident
                _currentIncident.value = result.getOrNull()
                
                // Add to active incidents
                val updatedIncidents = _activeIncidents.value.toMutableList()
                result.getOrNull()?.let { updatedIncidents.add(it) }
                _activeIncidents.value = updatedIncidents
            }
            
            _isLoading.value = false
            onComplete(result)
        }
    }
    
    /**
     * Send an SMS directly to emergency contacts
     */
    fun sendEmergencySMS(message: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val contacts = _emergencyContacts.value
            val location = _currentLocation.value
            
            val result = safetyRepository.sendEmergencySMS(contacts, message, location)
            onComplete(result)
        }
    }

    fun activateSOS() {
        _isSOSActive.value = true
        // TODO: Implement SOS activation logic
        // - Send notifications to emergency contacts
        // - Share location with emergency contacts
        // - Contact emergency services if configured
    }

    fun deactivateSOS() {
        _isSOSActive.value = false
        // TODO: Implement SOS deactivation logic
        // - Notify contacts that the emergency is resolved
        // - Stop location sharing
    }
} 