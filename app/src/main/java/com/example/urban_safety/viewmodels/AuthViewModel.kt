package com.example.urban_safety.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class User(
    val id: String,
    val name: String,
    val email: String,
    val profileImageUrl: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()

    init {
        // Check if user is already logged in
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Check Firebase Auth for current user
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    _currentUser.value = User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: ""
                    )
                }
            } catch (e: Exception) {
                _authError.value = "Failed to load user: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null

            try {
                // Real Firebase login
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                
                if (firebaseUser != null) {
                    _currentUser.value = User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "User",
                        email = firebaseUser.email ?: ""
                    )
                } else {
                    _authError.value = "Login failed. Please try again."
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Authentication failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun signup(name: String, email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null

            try {
                // Real Firebase signup
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                
                if (firebaseUser != null) {
                    // Update the display name
                    firebaseUser.updateProfile(com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()).await()
                    
                    _currentUser.value = User(
                        id = firebaseUser.uid,
                        name = name,
                        email = firebaseUser.email ?: ""
                    )
                } else {
                    _authError.value = "Registration failed. Please try again."
                }
            } catch (e: Exception) {
                _authError.value = e.message ?: "Registration failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Real Firebase logout
                auth.signOut()
                _currentUser.value = null
            } catch (e: Exception) {
                _authError.value = e.message ?: "Logout failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }
} 