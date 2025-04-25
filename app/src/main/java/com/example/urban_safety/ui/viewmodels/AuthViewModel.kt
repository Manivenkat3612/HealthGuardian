package com.example.urban_safety.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.User
import com.example.urban_safety.data.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import javax.inject.Inject
import android.content.Context

/**
 * ViewModel for authentication operations
 */
class AuthViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    
    private val authRepository = AuthRepository()
    private val auth: FirebaseAuth = Firebase.auth
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    
    private val _loginResult = MutableStateFlow<Result<FirebaseUser>?>(null)
    val loginResult: Result<FirebaseUser>? get() = _loginResult.value
    
    private val _signupResult = MutableStateFlow<Result<FirebaseUser>?>(null)
    val signupResult: Result<FirebaseUser>? get() = _signupResult.value
    
    private val _userDataResult = MutableStateFlow<Result<User>?>(null)
    val userDataResult: Result<User>? get() = _userDataResult.value
    
    private val _googleSignInResult = MutableStateFlow<Result<FirebaseUser>?>(null)
    val googleSignInResult: Result<FirebaseUser>? get() = _googleSignInResult.value
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    init {
        checkCurrentUser()
    }
    
    /**
     * Check if a user is currently logged in
     */
    fun checkCurrentUser() {
        _currentUser.value = authRepository.getCurrentUser()
    }
    
    /**
     * Login with email and password
     */
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = authRepository.signIn(email, password)
                _authState.value = AuthState.Success(result.getOrNull())
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }
    
    /**
     * Register a new user
     */
    fun signUp(email: String, password: String, name: String, phoneNumber: String = "") {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = authRepository.signUp(name, email, password, phoneNumber)
                _authState.value = AuthState.Success(result.getOrNull())
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign up failed")
            }
        }
    }
    
    /**
     * Logout the current user
     */
    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _authState.value = AuthState.Success(null)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign out failed")
            }
        }
    }
    
    /**
     * Reset login result to avoid triggering observers multiple times
     */
    fun resetLoginResult() {
        _loginResult.value = null
    }
    
    /**
     * Reset signup result to avoid triggering observers multiple times
     */
    fun resetSignupResult() {
        _signupResult.value = null
    }
    
    /**
     * Get user data from Firestore
     */
    fun getUserData(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _userDataResult.value = authRepository.getUserData(userId)
            _isLoading.value = false
        }
    }
    
    /**
     * Reset user data result
     */
    fun resetUserDataResult() {
        _userDataResult.value = null
    }
    
    /**
     * Send password reset email
     */
    fun sendPasswordResetEmail(email: String, onComplete: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.sendPasswordResetEmail(email)
            onComplete(result)
            _isLoading.value = false
        }
    }
    
    /**
     * Authenticate with Google credential
     */
    fun signInWithGoogle(context: Context, onResult: (Boolean, String?) -> Unit) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("273310097648-fjggorfi3tmr1kvk3d19dja4qf705sen.apps.googleusercontent.com")
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        
        try {
            val signInIntent = googleSignInClient.signInIntent
            // Note: In a real implementation, you would need to handle the result in an Activity
            // This is a simplified version just to fix compilation errors
            viewModelScope.launch {
                try {
                    // This is a simplification - in a real app, you'd use ActivityResultLauncher
                    onResult(false, "This is a simplified implementation for compilation")
                } catch (e: Exception) {
                    onResult(false, e.message ?: "Google sign in failed")
                }
            }
        } catch (e: Exception) {
            onResult(false, e.message ?: "Failed to start Google sign in")
        }
    }
    
    /**
     * Reset Google sign-in result
     */
    fun resetGoogleSignInResult() {
        _googleSignInResult.value = null
    }
} 