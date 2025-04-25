package com.example.urban_safety.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.SafetyScore
import com.example.urban_safety.data.repository.SafetyScoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafetyScoreViewModel @Inject constructor(
    private val repository: SafetyScoreRepository
) : ViewModel() {

    private val _safetyScore = MutableStateFlow<SafetyScore?>(null)
    val safetyScore: StateFlow<SafetyScore?> = _safetyScore.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadSafetyScore()
    }

    private fun loadSafetyScore() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _safetyScore.value = repository.getSafetyScore()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load safety score"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshSafetyScore() {
        loadSafetyScore()
    }
} 