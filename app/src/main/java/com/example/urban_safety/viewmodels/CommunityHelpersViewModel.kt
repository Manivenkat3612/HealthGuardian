package com.example.urban_safety.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.CommunityHelper
import com.example.urban_safety.data.model.HelpRequest
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.repository.LocationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CommunityHelpersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _nearbyHelpers = MutableStateFlow<List<CommunityHelper>>(emptyList())
    val nearbyHelpers: StateFlow<List<CommunityHelper>> = _nearbyHelpers.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _requestSent = MutableStateFlow(false)
    val requestSent: StateFlow<Boolean> = _requestSent.asStateFlow()
    
    // Search radius in kilometers
    private var searchRadius = 10.0
    
    private val helpersCollection
        get() = firestore.collection("community_helpers")
    
    private val helpRequestsCollection
        get() = firestore.collection("help_requests")
    
    init {
        loadNearbyHelpers()
        subscribeToSosAlerts()
    }
    
    /**
     * Load nearby community helpers
     */
    private fun loadNearbyHelpers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get current location
                val locationResult = locationRepository.getLastLocation()
                val currentLocation = locationResult.getOrNull()
                
                if (currentLocation == null) {
                    _error.value = "Unable to determine your location"
                    _nearbyHelpers.value = emptyList()
                    return@launch
                }
                
                // Get all available helpers
                val snapshot = helpersCollection
                    .whereEqualTo("isAvailable", true)
                    .get()
                    .await()
                
                val allHelpers = snapshot.documents.mapNotNull { 
                    it.toObject(CommunityHelper::class.java) 
                }
                
                // Filter by distance and sort by distance
                val nearbyHelpersWithDistance = allHelpers
                    .map { helper ->
                        val distance = CommunityHelper.calculateDistanceKm(helper, currentLocation)
                        helper.copy(distanceKm = distance)
                    }
                    .filter { it.distanceKm <= searchRadius }
                    .sortedBy { it.distanceKm }
                
                _nearbyHelpers.value = nearbyHelpersWithDistance
            } catch (e: Exception) {
                _error.value = "Failed to load nearby helpers: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh the list of helpers
     */
    fun refreshHelpers() {
        loadNearbyHelpers()
    }
    
    /**
     * Set the search radius in kilometers
     */
    fun setSearchRadius(radiusKm: Double) {
        searchRadius = radiusKm
        refreshHelpers()
    }
    
    /**
     * Request help from a community helper
     */
    fun requestHelp(helper: CommunityHelper, message: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                // Get current location
                val locationResult = locationRepository.getLastLocation()
                val currentLocation = locationResult.getOrNull()
                
                // Create help request
                val helpRequest = HelpRequest(
                    id = UUID.randomUUID().toString(),
                    requesterId = currentUser.uid,
                    requesterName = currentUser.displayName ?: "User",
                    helperId = helper.id,
                    helperName = helper.name,
                    message = message,
                    requesterLocation = currentLocation,
                    status = HelpRequest.RequestStatus.PENDING,
                    createdAt = Date()
                )
                
                // Save to Firestore
                helpRequestsCollection
                    .document(helpRequest.id)
                    .set(helpRequest)
                    .await()
                
                // Update success flag
                _requestSent.value = true
            } catch (e: Exception) {
                _error.value = "Failed to send help request: ${e.message}"
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
    
    /**
     * Clear request sent flag
     */
    fun clearRequestSent() {
        _requestSent.value = false
    }
    
    /**
     * Register as a community helper
     */
    fun registerAsHelper(
        name: String,
        phoneNumber: String,
        email: String,
        helperType: CommunityHelper.HelperType,
        skills: List<String>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                // Get current location
                val locationResult = locationRepository.getLastLocation()
                val currentLocation = locationResult.getOrNull()
                
                // Create helper profile
                val helper = CommunityHelper(
                    id = UUID.randomUUID().toString(),
                    userId = currentUser.uid,
                    name = name,
                    phoneNumber = phoneNumber,
                    email = email,
                    helperType = helperType,
                    isVerified = false,
                    isAvailable = true,
                    location = currentLocation,
                    skills = skills,
                    joinDate = Date(),
                    lastActive = Date()
                )
                
                // Save to Firestore
                helpersCollection
                    .document(helper.id)
                    .set(helper)
                    .await()
                
                // Refresh list
                refreshHelpers()
            } catch (e: Exception) {
                _error.value = "Failed to register as helper: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Check if user is registered as a helper
     */
    fun checkHelperStatus() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                val snapshot = helpersCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .limit(1)
                    .get()
                    .await()
                
                val isHelper = !snapshot.isEmpty
                
                // Here we would update some UI state to show the user is a helper
                println("User is a helper: $isHelper")
            } catch (e: Exception) {
                _error.value = "Failed to check helper status: ${e.message}"
            }
        }
    }
    
    /**
     * Update helper availability status
     */
    fun updateAvailability(isAvailable: Boolean) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                val snapshot = helpersCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .limit(1)
                    .get()
                    .await()
                
                if (snapshot.isEmpty) return@launch
                
                val helperDoc = snapshot.documents[0]
                helperDoc.reference.update("isAvailable", isAvailable).await()
                
                // Update location if becoming available
                if (isAvailable) {
                    val locationResult = locationRepository.getLastLocation()
                    val currentLocation = locationResult.getOrNull()
                    
                    if (currentLocation != null) {
                        helperDoc.reference.update("location", currentLocation).await()
                    }
                }
                
                // Update last active timestamp
                helperDoc.reference.update("lastActive", Date()).await()
            } catch (e: Exception) {
                _error.value = "Failed to update availability: ${e.message}"
            }
        }
    }
    
    /**
     * Subscribe to SOS alert notifications for nearby helpers
     */
    private fun subscribeToSosAlerts() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // Check if user is a helper
                val snapshot = helpersCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .limit(1)
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    // Subscribe to SOS alerts topic
                    FirebaseMessaging.getInstance().subscribeToTopic("sos_alerts")
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Log.d("CommunityHelpers", "Subscribed to SOS alerts")
                            } else {
                                Log.e("CommunityHelpers", "Failed to subscribe to SOS alerts", task.exception)
                            }
                        }
                    
                    // Update helper's location periodically
                    startLocationUpdates()
                }
            } catch (e: Exception) {
                _error.value = "Failed to subscribe to SOS alerts: ${e.message}"
            }
        }
    }
    
    /**
     * Start periodic location updates for helper
     */
    private fun startLocationUpdates() {
        viewModelScope.launch {
            while (true) {
                try {
                    updateHelperLocation()
                    delay(300000) // Update location every 5 minutes
                } catch (e: Exception) {
                    Log.e("CommunityHelpers", "Error updating location", e)
                    delay(600000) // Retry after 10 minutes on error
                }
            }
        }
    }
    
    /**
     * Update helper's location in Firestore
     */
    private fun updateHelperLocation() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // Get current location
                val locationResult = locationRepository.getLastLocation()
                val currentLocation = locationResult.getOrNull() ?: return@launch
                
                // Find helper document
                val snapshot = helpersCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .limit(1)
                    .get()
                    .await()
                
                if (!snapshot.isEmpty) {
                    val helperDoc = snapshot.documents[0]
                    
                    // Update location and last active timestamp
                    helperDoc.reference.update(
                        mapOf(
                            "location" to currentLocation,
                            "lastActive" to Date()
                        )
                    ).await()
                }
            } catch (e: Exception) {
                Log.e("CommunityHelpers", "Error updating helper location", e)
            }
        }
    }
} 