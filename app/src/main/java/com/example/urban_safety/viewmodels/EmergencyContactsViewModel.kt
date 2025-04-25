package com.example.urban_safety.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.model.EmergencyContact
import com.example.urban_safety.repositories.EmergencyContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmergencyContactsViewModel @Inject constructor(
    private val repository: EmergencyContactsRepository
) : ViewModel() {
    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            repository.getEmergencyContacts()
                .catch { e ->
                    _error.value = "Failed to load contacts: ${e.message}"
                }
                .collect { contacts ->
                    _contacts.value = contacts
                }
        }
    }

    fun addContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                repository.addEmergencyContact(contact)
                loadContacts() // Reload to get updated list
            } catch (e: Exception) {
                _error.value = "Failed to add contact: ${e.message}"
            }
        }
    }

    fun removeContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                repository.removeEmergencyContact(contact.id)
                loadContacts() // Reload to get updated list
            } catch (e: Exception) {
                _error.value = "Failed to remove contact: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
} 