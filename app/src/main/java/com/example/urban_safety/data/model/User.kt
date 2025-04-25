package com.example.urban_safety.data.model

import com.example.urban_safety.models.EmergencyContact

/**
 * Model class representing a user in the Urban Safety app
 */
data class User(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * User's safety preferences
 */
data class SafetyPreferences(
    val autoSOSEnabled: Boolean = false,  // Auto SOS via health triggers
    val voiceActivatedSOSEnabled: Boolean = true,  // "Help" voice command 
    val voiceActivationKeyword: String = "help",
    val automaticLocationSharingEnabled: Boolean = true,
    val locationSharingDuration: Int = 60, // minutes
    val healthDataMonitoringEnabled: Boolean = false,
    val heartRateThreshold: Int = 120, // bpm
    val fallDetectionEnabled: Boolean = true,
    val notifyNearbyUsersEnabled: Boolean = true,
    val notifyNearbyRadius: Int = 500 // meters
) 