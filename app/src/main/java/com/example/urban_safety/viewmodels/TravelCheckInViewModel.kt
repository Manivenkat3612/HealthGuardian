package com.example.urban_safety.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.TravelCheckIn
import com.example.urban_safety.data.model.TravelStatus
import com.example.urban_safety.data.model.TransportMode
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.repositories.EmergencyContactsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TravelCheckInViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val emergencyContactsRepository: EmergencyContactsRepository
) : ViewModel() {
    private val checkInsCollection = firestore.collection("travel_checkins")
    
    private val _activeTravelCheckIn = MutableStateFlow<TravelCheckIn?>(null)
    val activeTravelCheckIn: StateFlow<TravelCheckIn?> = _activeTravelCheckIn.asStateFlow()
    
    private val _pastCheckIns = MutableStateFlow<List<TravelCheckIn>>(emptyList())
    val pastCheckIns: StateFlow<List<TravelCheckIn>> = _pastCheckIns.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _selectedContacts = MutableStateFlow<List<String>>(emptyList())
    val selectedContacts: StateFlow<List<String>> = _selectedContacts.asStateFlow()

    init {
        loadActiveTravelCheckIn()
        loadPastTravelCheckIns()
    }
    
    private fun loadActiveTravelCheckIn() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = checkInsCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("status", TravelStatus.ACTIVE.name)
                    .limit(1)
                    .get()
                    .await()
                
                if (snapshot.isEmpty) {
                    _activeTravelCheckIn.value = null
                } else {
                    val checkIn = snapshot.documents[0].toObject(TravelCheckIn::class.java)
                    _activeTravelCheckIn.value = checkIn
                }
            } catch (e: Exception) {
                _error.value = "Failed to load active travel: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun loadPastTravelCheckIns() {
        val currentUser = auth.currentUser ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val snapshot = checkInsCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .whereNotEqualTo("status", TravelStatus.ACTIVE.name)
                    .orderBy("status")
                    .orderBy("startTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()
                
                val checkIns = snapshot.documents.mapNotNull {
                    it.toObject(TravelCheckIn::class.java)
                }
                _pastCheckIns.value = checkIns
            } catch (e: Exception) {
                _error.value = "Failed to load past travels: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Create a new travel check-in
     */
    fun createTravelCheckIn(destination: String, estimatedArrivalTime: Date, transportMode: TransportMode) {
        if (_activeTravelCheckIn.value != null) {
            _error.value = "You already have an active travel check-in"
            return
        }
        
        val currentUser = auth.currentUser ?: run {
            _error.value = "You must be logged in to create a travel check-in"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Create a new travel check-in
                val travelCheckIn = TravelCheckIn(
                    userId = currentUser.uid,
                    userName = currentUser.displayName ?: "User",
                    destination = destination,
                    startLocation = null, // In a real app, this would be the current location
                    endLocation = null,
                    startTime = Date(),
                    estimatedArrivalTime = estimatedArrivalTime,
                    transportMode = transportMode,
                    status = TravelStatus.ACTIVE,
                    notifiedContacts = _selectedContacts.value
                )
                
                // Save to Firestore
                val documentRef = checkInsCollection.document()
                val checkInWithId = travelCheckIn.copy(id = documentRef.id)
                documentRef.set(checkInWithId).await()
                
                // Update local state
                _activeTravelCheckIn.value = checkInWithId
                
                // Notify emergency contacts
                notifyEmergencyContacts(checkInWithId)
            } catch (e: Exception) {
                _error.value = "Failed to create travel check-in: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Mark travel as complete
     */
    fun completeTravel() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentTravel = _activeTravelCheckIn.value ?: throw Exception("No active travel")
                
                // Update travel check-in
                val updatedTravel = currentTravel.copy(
                    status = TravelStatus.COMPLETED,
                    actualArrivalTime = Date(),
                    endTime = Date()
                )
                
                // Save to Firestore
                checkInsCollection
                    .document(updatedTravel.id)
                    .set(updatedTravel)
                    .await()
                
                // Update local state
                _activeTravelCheckIn.value = null
                loadPastTravelCheckIns() // Refresh past travels
                
                // Notify emergency contacts of arrival
                notifyContactsOfArrival(updatedTravel)
            } catch (e: Exception) {
                _error.value = "Failed to complete travel: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Cancel the active travel
     */
    fun cancelTravel() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentTravel = _activeTravelCheckIn.value ?: throw Exception("No active travel")
                
                // Update travel check-in
                val updatedTravel = currentTravel.copy(
                    status = TravelStatus.CANCELLED,
                    endTime = Date()
                )
                
                // Save to Firestore
                checkInsCollection
                    .document(updatedTravel.id)
                    .set(updatedTravel)
                    .await()
                
                // Update local state
                _activeTravelCheckIn.value = null
                loadPastTravelCheckIns() // Refresh past travels
                
                // Notify emergency contacts of cancellation
                notifyContactsOfCancellation(updatedTravel)
            } catch (e: Exception) {
                _error.value = "Failed to cancel travel: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update the selected contacts for notification
     */
    fun updateSelectedContacts(contacts: List<String>) {
        _selectedContacts.value = contacts
    }
    
    /**
     * Notify emergency contacts about new travel
     */
    private suspend fun notifyEmergencyContacts(travelCheckIn: TravelCheckIn) {
        try {
            // In a real app, this would send SMS messages or push notifications
            // For the demo, we'll just log the notifications
            println("Notifying contacts about new travel to ${travelCheckIn.destination}")
            
            // Get actual emergency contacts
            val emergencyContacts = emergencyContactsRepository.getEmergencyContacts().first()
            
            // Filter to only selected contacts if specified
            val contactsToNotify = if (travelCheckIn.notifiedContacts.isNotEmpty()) {
                emergencyContacts.filter { travelCheckIn.notifiedContacts.contains(it.name) }
            } else {
                emergencyContacts
            }
            
            contactsToNotify.forEach { contact ->
                println("Would send message to ${contact.name} (${contact.phoneNumber})")
                
                // In a real app, this would send an SMS or notification:
                // val message = "${travelCheckIn.userName} is traveling to ${travelCheckIn.destination} and expects to arrive by ${formatDateTime(travelCheckIn.expectedArrivalTime)}"
                // smsService.sendSms(contact.phoneNumber, message)
            }
        } catch (e: Exception) {
            println("Failed to notify contacts: ${e.message}")
        }
    }
    
    /**
     * Notify emergency contacts about arrival
     */
    private suspend fun notifyContactsOfArrival(travelCheckIn: TravelCheckIn) {
        try {
            // In a real app, this would send SMS messages or push notifications
            println("Notifying contacts about arrival at ${travelCheckIn.destination}")
            
            // Get actual emergency contacts
            val emergencyContacts = emergencyContactsRepository.getEmergencyContacts().first()
            
            // Filter to only notified contacts
            val contactsToNotify = emergencyContacts.filter { 
                travelCheckIn.notifiedContacts.contains(it.name) 
            }
            
            contactsToNotify.forEach { contact ->
                println("Would send arrival message to ${contact.name} (${contact.phoneNumber})")
                
                // In a real app, this would send an SMS or notification:
                // val message = "${travelCheckIn.userName} has arrived safely at ${travelCheckIn.destination}"
                // smsService.sendSms(contact.phoneNumber, message)
            }
        } catch (e: Exception) {
            println("Failed to notify contacts of arrival: ${e.message}")
        }
    }
    
    /**
     * Notify emergency contacts about cancellation
     */
    private suspend fun notifyContactsOfCancellation(travelCheckIn: TravelCheckIn) {
        try {
            // In a real app, this would send SMS messages or push notifications
            println("Notifying contacts about travel cancellation")
            
            // Get actual emergency contacts
            val emergencyContacts = emergencyContactsRepository.getEmergencyContacts().first()
            
            // Filter to only notified contacts
            val contactsToNotify = emergencyContacts.filter { 
                travelCheckIn.notifiedContacts.contains(it.name) 
            }
            
            contactsToNotify.forEach { contact ->
                println("Would send cancellation message to ${contact.name} (${contact.phoneNumber})")
                
                // In a real app, this would send an SMS or notification:
                // val message = "${travelCheckIn.userName} has cancelled their travel to ${travelCheckIn.destination}"
                // smsService.sendSms(contact.phoneNumber, message)
            }
        } catch (e: Exception) {
            println("Failed to notify contacts of cancellation: ${e.message}")
        }
    }
    
    /**
     * Check for overdue travels
     * This would typically be called by a background worker
     */
    fun checkForOverdueTravels() {
        viewModelScope.launch {
            try {
                val currentTime = Date()
                
                val snapshot = checkInsCollection
                    .whereEqualTo("status", TravelStatus.ACTIVE.name)
                    .whereLessThan("estimatedArrivalTime", currentTime)
                    .get()
                    .await()
                
                snapshot.documents.forEach { doc ->
                    val travel = doc.toObject(TravelCheckIn::class.java) ?: return@forEach
                    
                    // Mark as overdue
                    val updatedTravel = travel.copy(status = TravelStatus.OVERDUE)
                    checkInsCollection.document(travel.id).set(updatedTravel).await()
                    
                    // Send alerts for overdue travel
                    alertContactsAboutOverdueTravel(updatedTravel)
                }
            } catch (e: Exception) {
                _error.value = "Failed to check for overdue travels: ${e.message}"
            }
        }
    }
    
    /**
     * Alert emergency contacts about overdue travel
     */
    private suspend fun alertContactsAboutOverdueTravel(travelCheckIn: TravelCheckIn) {
        try {
            // In a real app, this would send urgent SMS messages or push notifications
            println("ALERT: Notifying contacts about overdue travel to ${travelCheckIn.destination}")
            
            // Get actual emergency contacts
            val emergencyContacts = emergencyContactsRepository.getEmergencyContacts().first()
            
            // Filter to only notified contacts
            val contactsToNotify = emergencyContacts.filter { 
                travelCheckIn.notifiedContacts.contains(it.name) 
            }
            
            contactsToNotify.forEach { contact ->
                println("Would send URGENT alert to ${contact.name} (${contact.phoneNumber})")
                
                // In a real app, this would send an SMS or notification:
                // val message = "ALERT: ${travelCheckIn.userName} has not checked in after traveling to ${travelCheckIn.destination}. Expected arrival was ${formatDateTime(travelCheckIn.expectedArrivalTime)}."
                // smsService.sendUrgentSms(contact.phoneNumber, message)
            }
        } catch (e: Exception) {
            println("Failed to alert contacts about overdue travel: ${e.message}")
        }
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _error.value = null
    }
} 