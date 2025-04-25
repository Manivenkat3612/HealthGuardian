package com.example.urban_safety.data.model

import com.google.firebase.firestore.DocumentId

/**
 * Model class representing an emergency contact
 */
data class EmergencyContact(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val isDefault: Boolean = false
) {
    companion object {
        fun fromModelEmergencyContact(contact: com.example.urban_safety.models.EmergencyContact): EmergencyContact {
            return EmergencyContact(
                name = contact.name,
                phoneNumber = contact.phoneNumber
            )
        }
    }
    
    fun toModelEmergencyContact(): com.example.urban_safety.models.EmergencyContact {
        return com.example.urban_safety.models.EmergencyContact(
            name = this.name,
            phoneNumber = this.phoneNumber
        )
    }
} 