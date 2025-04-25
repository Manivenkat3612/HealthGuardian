package com.example.urban_safety.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.data.repository.SafetyRepository
import com.example.urban_safety.repositories.EmergencyContactsRepository
import com.example.urban_safety.util.Constants
import com.example.urban_safety.util.SmsService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SOSViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyRepository: SafetyRepository,
    private val locationRepository: LocationRepository,
    private val emergencyContactsRepository: EmergencyContactsRepository,
    private val smsService: SmsService
) : ViewModel() {
    private val _isSOSActive = MutableStateFlow(false)
    val isSOSActive: StateFlow<Boolean> = _isSOSActive.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    val currentLocation: StateFlow<LocationData?> = _currentLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Start location updates
        viewModelScope.launch {
            locationRepository.getLocationUpdates().collect { location ->
                if (location != null) {
                    _currentLocation.value = LocationData.fromLocation(location)
                }
            }
        }
        
        // Pre-fetch emergency contacts for faster access during emergencies
        viewModelScope.launch(Dispatchers.IO) {
            try {
                emergencyContactsRepository.getEmergencyContacts().first()
            } catch (e: Exception) {
                // Just pre-fetching, ok if it fails
            }
        }
    }

    fun activateSOS() {
        _isSOSActive.value = true
        _isLoading.value = true
        _error.value = null

        // Start notification process immediately on IO thread
        viewModelScope.launch(Dispatchers.IO) {
            try {
                try {
                    // Create incident
                    safetyRepository.createSafetyIncident(
                        type = IncidentType.MANUAL_SOS,
                        locationData = _currentLocation.value,
                        description = "Manual SOS Activated: " + Constants.MANUAL_SOS
                    )
                    
                    withContext(Dispatchers.Main) {
                        _isSOSActive.value = true
                    }
                    
                    // Notify emergency contacts directly in this thread
                    notifyEmergencyContactsDirectly(_currentLocation.value)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _error.value = e.message ?: "Failed to activate SOS"
                        _isSOSActive.value = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = e.message ?: "An unknown error occurred"
                    _isSOSActive.value = false
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }

    /**
     * Notify emergency contacts directly without relying on repository calls
     * for faster emergency notifications
     */
    private suspend fun notifyEmergencyContactsDirectly(locationData: LocationData?) {
        try {
            // Check if SMS permission is granted
            if (!smsService.hasSmsPermission()) {
                withContext(Dispatchers.Main) {
                    _error.value = "SMS permission not granted. Emergency contacts will not be notified."
                }
                return
            }
            
            // Get emergency contacts
            val contacts = emergencyContactsRepository.getEmergencyContacts().first()
            
            if (contacts.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _error.value = "No emergency contacts found. Please add emergency contacts in settings."
                }
                return
            }
            
            // Send SMS to all contacts in parallel
            contacts.map { contact ->
                viewModelScope.launch(Dispatchers.IO) {
                    // Create emergency message with location link
                    val message = "EMERGENCY SOS: I need help! " + 
                        "My location: https://maps.google.com/?q=${locationData?.latitude},${locationData?.longitude}"
                    
                    // Send emergency SMS
                    try {
                        smsService.sendEmergencySMS(contact.phoneNumber, message)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            _error.value = "Failed to send SMS to ${contact.name}: ${e.message}"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _error.value = "Failed to notify emergency contacts: ${e.message}"
            }
        }
    }

    fun deactivateSOS() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // In a real app, this would call an API to deactivate the SOS
                // and notify contacts that the emergency is resolved
                
                // Notify emergency contacts that SOS is deactivated
                notifyContactsSOSDeactivated()
                
                _isSOSActive.value = false
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to deactivate SOS"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Notify emergency contacts that SOS has been deactivated
     */
    private suspend fun notifyContactsSOSDeactivated() {
        try {
            // Check if SMS permission is granted
            if (!smsService.hasSmsPermission()) {
                return
            }
            
            // Get emergency contacts
            val contacts = emergencyContactsRepository.getEmergencyContacts().first()
            
            if (contacts.isEmpty()) {
                return
            }
            
            // Send SMS to all contacts in parallel
            contacts.forEach { contact ->
                viewModelScope.launch(Dispatchers.IO) {
                    // Create message
                    val message = "SOS DEACTIVATED: I'm safe now. Thank you."
                    
                    // Send SMS
                    try {
                        smsService.sendSMS(contact.phoneNumber, message)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            _error.value = "Failed to send deactivation SMS: ${e.message}"
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _error.value = "Failed to notify contacts about SOS deactivation: ${e.message}"
            }
        }
    }

    fun updateLocation(location: LocationData) {
        _currentLocation.value = location
    }

    fun clearError() {
        _error.value = null
    }

    fun triggerSOS() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _isLoading.value = true
                }
                
                // Use the last known location from repository
                val locationResult = locationRepository.getLastLocation()
                val locationData = try { locationResult.getOrThrow() } catch (e: Exception) { null }
                
                try {
                    safetyRepository.createSafetyIncident(
                        type = IncidentType.MANUAL_SOS,
                        locationData = locationData,
                        description = "SOS triggered"
                    )
                    
                    withContext(Dispatchers.Main) {
                        _isSOSActive.value = true
                    }
                    
                    // Notify emergency contacts
                    notifyEmergencyContactsDirectly(locationData)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _error.value = e.message ?: "Failed to trigger SOS"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = e.message ?: "Failed to trigger SOS"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
} 