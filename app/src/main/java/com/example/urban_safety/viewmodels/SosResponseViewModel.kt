package com.example.urban_safety.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.model.SosRequest
import com.example.urban_safety.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SosResponseViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _sosRequest = MutableStateFlow<SosRequest?>(null)
    val sosRequest: StateFlow<SosRequest?> = _sosRequest.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val sosRequestsCollection
        get() = firestore.collection(Constants.SOS_REQUESTS_COLLECTION)
    
    init {
        loadActiveSosRequest()
    }
    
    private fun loadActiveSosRequest() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                // Get the most recent active SOS request
                val snapshot = sosRequestsCollection
                    .whereEqualTo("status", "PENDING")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                
                if (snapshot.isEmpty) {
                    _sosRequest.value = null
                } else {
                    val doc = snapshot.documents[0]
                    val request = SosRequest(
                        id = doc.id,
                        requesterId = doc.getString("requesterId") ?: "",
                        requesterName = doc.getString("requesterName") ?: "Unknown",
                        location = doc.get("location") as? LocationData,
                        type = doc.getString("type")?.let { IncidentType.valueOf(it) } ?: IncidentType.OTHER,
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getDate("timestamp") ?: Date(),
                        status = doc.getString("status") ?: "PENDING"
                    )
                    _sosRequest.value = request
                }
            } catch (e: Exception) {
                _error.value = "Failed to load SOS request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun acceptRequest() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentRequest = _sosRequest.value ?: throw Exception("No active SOS request")
                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                // Update request status
                sosRequestsCollection
                    .document(currentRequest.id)
                    .update(
                        mapOf(
                            "status" to "ACCEPTED",
                            "responderId" to currentUser.uid,
                            "responderName" to (currentUser.displayName ?: "Unknown"),
                            "responseTime" to Date()
                        )
                    )
                    .await()
                
                // Update local state
                _sosRequest.value = currentRequest.copy(status = "ACCEPTED")
            } catch (e: Exception) {
                _error.value = "Failed to accept request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun rejectRequest() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentRequest = _sosRequest.value ?: throw Exception("No active SOS request")
                
                // Update request status
                sosRequestsCollection
                    .document(currentRequest.id)
                    .update(
                        mapOf(
                            "status" to "REJECTED",
                            "responseTime" to Date()
                        )
                    )
                    .await()
                
                // Update local state
                _sosRequest.value = currentRequest.copy(status = "REJECTED")
            } catch (e: Exception) {
                _error.value = "Failed to reject request: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
} 