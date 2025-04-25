package com.example.urban_safety.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.data.repository.SafetyRepository
import com.example.urban_safety.util.SmsService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ManualSOSViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyRepository: SafetyRepository,
    private val locationRepository: LocationRepository,
    private val smsService: SmsService,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _isSosActive = MutableStateFlow(false)
    val isSosActive: StateFlow<Boolean> = _isSosActive.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _messagesSent = MutableStateFlow(0)
    val messagesSent: StateFlow<Int> = _messagesSent.asStateFlow()
    
    init {
        checkActiveIncident()
    }
    
    /**
     * Check if there's an active SOS incident
     */
    private fun checkActiveIncident() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            try {
                val activeIncident = safetyRepository.getActiveIncident(currentUser.uid)
                _isSosActive.value = activeIncident != null
            } catch (e: Exception) {
                _error.value = "Failed to check active incident: ${e.message}"
            }
        }
    }
    
    /**
     * Activate SOS
     */
    fun activateSOS() {
        val currentUser = auth.currentUser ?: run {
            _error.value = "You must be logged in to activate SOS"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current location
                val locationResult = locationRepository.getLastLocation()
                val currentLocation = locationResult.getOrNull()
                
                // Create the incident
                val result = safetyRepository.createSafetyIncident(
                    type = IncidentType.MANUAL_SOS,
                    locationData = currentLocation,
                    description = "Manual SOS triggered by user"
                )
                
                if (result.isSuccess) {
                    _isSosActive.value = true
                    // Send messages to emergency contacts
                    sendEmergencyMessages(currentLocation)
                    // Notify nearby community helpers
                    notifyNearbyHelpers(currentLocation)
                } else {
                    _error.value = "Failed to activate SOS: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to activate SOS: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Send emergency messages to contacts
     */
    private fun sendEmergencyMessages(location: LocationData?) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // Get emergency contacts
                val contactsSnapshot = firestore.collection("emergency_contacts")
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()
                
                val contacts = contactsSnapshot.documents.mapNotNull { 
                    it.getString("phoneNumber") 
                }
                
                // Prepare message
                val locationText = if (location != null) {
                    "Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
                } else {
                    "Location not available"
                }
                
                val message = "EMERGENCY ALERT: ${currentUser.displayName ?: "A user"} " +
                        "has triggered an SOS emergency alert. ${locationText}"
                
                // Send SMS messages
                var sent = 0
                for (contact in contacts) {
                    val result = smsService.sendEmergencySMS(contact, message)
                    if (result.isSuccess) {
                        sent++
                    }
                }
                
                _messagesSent.value = sent
            } catch (e: Exception) {
                Log.e("ManualSOS", "Error sending messages", e)
            }
        }
    }
    
    /**
     * Notify nearby community helpers
     */
    private fun notifyNearbyHelpers(location: LocationData?) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // Create a message for the notification
                val fcmMessage = mapOf(
                    "message" to mapOf(
                        "topic" to "sos_alerts",
                        "notification" to mapOf(
                            "title" to "EMERGENCY: SOS Alert",
                            "body" to "${currentUser.displayName ?: "Someone"} needs urgent help nearby!"
                        ),
                        "data" to mapOf(
                            "userId" to currentUser.uid,
                            "userName" to (currentUser.displayName ?: "User"),
                            "latitude" to (location?.latitude?.toString() ?: ""),
                            "longitude" to (location?.longitude?.toString() ?: ""),
                            "timestamp" to System.currentTimeMillis().toString(),
                            "type" to "MANUAL_SOS"
                        )
                    )
                )
                
                // Use Firebase Cloud Messaging Admin SDK (this would normally be done in a Cloud Function)
                // For demonstration, log that this would be sent in a real app
                Log.d("ManualSOS", "Would send FCM message to topic sos_alerts: $fcmMessage")
                
                // In a real app, you would call a cloud function or use your backend:
                // val call = yourApiService.sendAlert(fcmMessage)
                // call.execute()
                
                // For this demo, we're using Firebase Messaging topics
                // The actual message sending would be handled by a Cloud Function
            } catch (e: Exception) {
                Log.e("ManualSOS", "Error notifying helpers", e)
            }
        }
    }
    
    /**
     * Deactivate SOS
     */
    fun deactivateSOS() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = safetyRepository.resolveActiveIncident(currentUser.uid)
                
                if (result.isSuccess) {
                    _isSosActive.value = false
                } else {
                    _error.value = "Failed to deactivate SOS: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to deactivate SOS: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
} 