package com.example.urban_safety.repositories

import com.example.urban_safety.data.model.EmergencyContact
import kotlinx.coroutines.flow.Flow

interface EmergencyContactsRepository {
    fun getEmergencyContacts(): Flow<List<EmergencyContact>>
    suspend fun addEmergencyContact(contact: EmergencyContact)
    suspend fun removeEmergencyContact(contactId: String)
} 