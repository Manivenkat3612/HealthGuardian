package com.example.urban_safety.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadUserProfile()
    }
    
    /**
     * Load user profile data
     */
    private fun loadUserProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val currentUser = auth.currentUser
                
                if (currentUser != null) {
                    // Try to get profile from Firestore
                    val userDocRef = firestore.collection("users").document(currentUser.uid)
                    val userDoc = userDocRef.get().await()
                    
                    if (userDoc.exists()) {
                        // Profile exists in Firestore
                        val profile = userDoc.toObject(UserProfile::class.java)
                        _userProfile.value = profile
                    } else {
                        // Create a default profile from auth data
                        val defaultProfile = UserProfile(
                            id = currentUser.uid,
                            name = currentUser.displayName ?: "User",
                            email = currentUser.email ?: "",
                            phoneNumber = currentUser.phoneNumber
                        )
                        _userProfile.value = defaultProfile
                    }
                } else {
                    _error.value = "User not authenticated"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update user profile information
     */
    fun updateProfile(name: String, email: String, phoneNumber: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
                
                // Update profile in Firestore
                val profile = UserProfile(
                    id = currentUser.uid,
                    name = name,
                    email = email,
                    phoneNumber = phoneNumber
                )
                
                firestore.collection("users")
                    .document(currentUser.uid)
                    .set(profile)
                    .await()
                
                _userProfile.value = profile
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to update profile: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    fun signOut() {
        auth.signOut()
        // In a real app, you would navigate to login screen here
    }
    
    /**
     * Clear any error message
     */
    fun clearError() {
        _error.value = null
    }
} 