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
    }

    fun activateSOS() {
        _isSOSActive.value = true
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val result = safetyRepository.createSafetyIncident(
                    type = IncidentType.MANUAL_SOS,
                    locationData = _currentLocation.value,
                    description = "Manual SOS Activated: " + Constants.MANUAL_SOS
                )
                
                if (result.isSuccess) {
                    _isSOSActive.value = true
                    
                    // Notify emergency contacts
                    notifyEmergencyContacts(_currentLocation.value)
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to activate SOS"
                    _isSOSActive.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unknown error occurred"
                _isSOSActive.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Notify emergency contacts about the SOS activation
     */
    private suspend fun notifyEmergencyContacts(locationData: LocationData?) {
        try {
            // Check if SMS permission is granted
            if (!smsService.hasSmsPermission()) {
                _error.value = "SMS permission not granted. Emergency contacts will not be notified."
                return
            }
            
            // Get emergency contacts
            val contacts = emergencyContactsRepository.getEmergencyContacts().first()
            
            if (contacts.isEmpty()) {
                _error.value = "No emergency contacts found. Please add emergency contacts in settings."
                return
            }
            
            // Send SMS to all contacts
            contacts.forEach { contact ->
                // Create emergency message with location link
                val message = "EMERGENCY SOS: I need help! " + 
                    "My location: https://maps.google.com/?q=${locationData?.latitude},${locationData?.longitude}"
                
                // Send emergency SMS
                val result = smsService.sendEmergencySMS(contact.phoneNumber, message)
                if (result.isFailure) {
                    _error.value = "Failed to send SMS to ${contact.name}: ${result.exceptionOrNull()?.message}"
                }
            }
        } catch (e: Exception) {
            _error.value = "Failed to notify emergency contacts: ${e.message}"
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
            
            // Send SMS to all contacts
            contacts.forEach { contact ->
                // Create message
                val message = "SOS DEACTIVATED: I'm safe now. Thank you."
                
                // Send SMS
                smsService.sendSMS(contact.phoneNumber, message)
            }
        } catch (e: Exception) {
            _error.value = "Failed to notify contacts about SOS deactivation: ${e.message}"
        }
    }

    fun updateLocation(location: LocationData) {
        _currentLocation.value = location
    }

    fun clearError() {
        _error.value = null
    }

    fun triggerSOS() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Use the last known location from repository
                val locationResult = locationRepository.getLastLocation()
                val locationData = locationResult.getOrNull()
                
                val result = safetyRepository.createSafetyIncident(
                    type = IncidentType.MANUAL_SOS,
                    locationData = locationData,
                    description = "SOS triggered"
                )
                
                if (result.isSuccess) {
                    _isSOSActive.value = true
                    
                    // Notify emergency contacts
                    notifyEmergencyContacts(locationData)
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to trigger SOS"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to trigger SOS"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 