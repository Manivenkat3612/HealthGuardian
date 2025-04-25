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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
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
        
        // Set active immediately for UI responsiveness
        _isSosActive.value = true
        _isLoading.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Pre-fetch contacts in parallel immediately
                val contactsFuture = async {
                    fetchEmergencyContacts(currentUser.uid)
                }
                
                // Get current location
                try {
                    val locationResult = locationRepository.getLastLocation()
                    val currentLocation = try { locationResult.getOrThrow() } catch (e: Exception) { null }
                    
                    // Create the incident - this also triggers notifications via SafetyRepository
                    try {
                        safetyRepository.createSafetyIncident(
                            type = IncidentType.MANUAL_SOS,
                            locationData = currentLocation,
                            description = "Manual SOS triggered by user"
                        )
                        
                        // In parallel with repository notification, also send directly
                        // for redundancy and to ensure contacts are notified quickly
                        coroutineScope {
                            launch {
                                // Get contacts from our pre-fetch operation
                                val contacts = contactsFuture.await()
                                if (contacts.isNotEmpty()) {
                                    sendEmergencyMessagesDirectly(contacts, currentLocation, currentUser)
                                }
                            }
                        }
                        
                        // Notify nearby community helpers in parallel
                        coroutineScope {
                            launch {
                                notifyNearbyHelpers(currentLocation)
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            _isSosActive.value = true
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            _error.value = "Failed to activate SOS: ${e.message}"
                            _isSosActive.value = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        _error.value = "Failed to get location: ${e.message}"
                        _isSosActive.value = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = "Failed to activate SOS: ${e.message}"
                    _isSosActive.value = false
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Fetch emergency contacts with caching
     */
    private suspend fun fetchEmergencyContacts(userId: String): List<String> {
        // Check in-memory cache first
        val cacheKey = "emergency_contacts_$userId"
        val cachedContacts = _contactsCache[cacheKey]
        if (cachedContacts != null && System.currentTimeMillis() - _contactsCacheTime < CACHE_EXPIRY_MS) {
            return cachedContacts
        }
        
        try {
            val contactsSnapshot = firestore.collection("emergency_contacts")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val contacts = contactsSnapshot.documents.mapNotNull { 
                it.getString("phoneNumber") 
            }
            
            // Update the cache
            _contactsCache[cacheKey] = contacts
            _contactsCacheTime = System.currentTimeMillis()
            
            return contacts
        } catch (e: Exception) {
            Log.e("ManualSOS", "Failed to fetch contacts", e)
            // Return cached contacts even if expired in case of error
            return cachedContacts ?: emptyList()
        }
    }
    
    /**
     * Send emergency messages directly to contacts for immediate notification
     */
    private suspend fun sendEmergencyMessagesDirectly(
        contacts: List<String>,
        location: LocationData?,
        currentUser: com.google.firebase.auth.FirebaseUser
    ) {
        try {
            if (!smsService.hasSmsPermission()) {
                Log.e("ManualSOS", "SMS permission not granted")
                return
            }
            
            // Prepare message
            val locationText = if (location != null) {
                "Location: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                "Location not available"
            }
            
            val message = "EMERGENCY ALERT: ${currentUser.displayName ?: "A user"} " +
                    "has triggered an SOS emergency alert. ${locationText}"
            
            // Use parallel sending for better performance
            val jobs = mutableListOf<Job>()
            coroutineScope {
                contacts.forEach { phoneNumber ->
                    val job = launch {
                        try {
                            smsService.sendEmergencySMS(phoneNumber, message)
                        } catch (e: Exception) {
                            Log.e("ManualSOS", "Failed to send direct SMS to $phoneNumber", e)
                        }
                    }
                    jobs.add(job)
                }
            
                // Wait for all SMS sending jobs to complete
                jobs.forEach { it.join() }
            }
            
            // Update message count on main thread
            withContext(Dispatchers.Main) {
                _messagesSent.value = contacts.size
            }
        } catch (e: Exception) {
            Log.e("ManualSOS", "Error sending direct messages", e)
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
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUser = auth.currentUser
                    ?: throw IllegalStateException("User must be logged in to deactivate SOS")

                try {
                    safetyRepository.resolveActiveIncident(currentUser.uid)
                    _isSosActive.value = false
                    
                    // Inform emergency contacts that the emergency is resolved
                    notifyContactsSOSDeactivated()
                } catch (e: Exception) {
                    _error.value = "Failed to deactivate SOS: ${e.message}"
                }
            } catch (e: Exception) {
                _error.value = "Failed to deactivate SOS: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Notify emergency contacts that SOS has been deactivated
     */
    private fun notifyContactsSOSDeactivated() {
        viewModelScope.launch {
            try {
                // Check if SMS permission is granted
                if (!smsService.hasSmsPermission()) {
                    return@launch
                }
                
                val currentUser = auth.currentUser ?: return@launch
                
                // Get emergency contacts from Firestore
                val contactsSnapshot = firestore.collection("emergency_contacts")
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .await()
                
                val contactPhones = contactsSnapshot.documents.mapNotNull { 
                    it.getString("phoneNumber") 
                }
                
                if (contactPhones.isEmpty()) {
                    return@launch
                }
                
                // Send SMS to all contacts
                for (phoneNumber in contactPhones) {
                    // Create message
                    val message = "SOS DEACTIVATED: I'm safe now. Thank you."
                    
                    // Send SMS
                    try {
                        smsService.sendSMS(phoneNumber, message)
                    } catch (e: Exception) {
                        _error.value = "Failed to notify contacts about SOS deactivation: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to notify contacts about SOS deactivation: ${e.message}"
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }
    
    // Cache for emergency contacts
    private val _contactsCache = mutableMapOf<String, List<String>>()
    private var _contactsCacheTime = 0L
    private val CACHE_EXPIRY_MS = 5 * 60 * 1000 // 5 minutes
} 