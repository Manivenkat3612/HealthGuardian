package com.example.urban_safety.ui.viewmodels

import com.google.firebase.auth.FirebaseUser

/**
 * Sealed class representing different states of the authentication process
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class Error(val message: String) : AuthState()
} 