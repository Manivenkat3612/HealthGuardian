package com.example.urban_safety.data.model

/**
 * Data class representing a user profile
 */
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val emergencyContacts: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val healthMonitoringEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastActive: Long = System.currentTimeMillis()
) 