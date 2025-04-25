package com.example.urban_safety.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Model class representing a community helper
 */
data class CommunityHelper(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val profileImage: String? = null,
    val helperType: HelperType = HelperType.GENERAL,
    val isVerified: Boolean = false,
    val isAvailable: Boolean = false,
    val location: LocationData? = null,
    val distanceKm: Double = 0.0,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val skills: List<String> = emptyList(),
    val specialties: List<String> = emptyList(),
    val certifications: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val joinDate: Date = Date(),
    val lastActive: Date = Date(),
    val helpRequestsCompleted: Int = 0
) {
    /**
     * Type of community helper
     */
    enum class HelperType {
        MEDICAL,    // Medical professional
        SECURITY,   // Security professional
        GENERAL     // General community helper
    }
    
    companion object {
        // Helper method to calculate distance based on location
        fun calculateDistanceKm(helper: CommunityHelper, userLocation: LocationData): Double {
            if (helper.location == null) return Double.MAX_VALUE
            
            val earthRadius = 6371.0 // Earth's radius in kilometers
            
            val lat1 = Math.toRadians(helper.location.latitude)
            val lon1 = Math.toRadians(helper.location.longitude)
            val lat2 = Math.toRadians(userLocation.latitude)
            val lon2 = Math.toRadians(userLocation.longitude)
            
            val dLat = lat2 - lat1
            val dLon = lon2 - lon1
            
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(lat1) * Math.cos(lat2) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
            
            return earthRadius * c
        }
    }
}

/**
 * Model class for a help request sent to a community helper
 */
data class HelpRequest(
    @DocumentId
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val helperId: String = "",
    val helperName: String = "",
    val message: String = "",
    val requesterLocation: LocationData? = null,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Date = Date(),
    val acceptedAt: Date? = null,
    val completedAt: Date? = null,
    val cancelledAt: Date? = null,
    val notes: String? = null
) {
    /**
     * Status of a help request
     */
    enum class RequestStatus {
        PENDING,
        ACCEPTED,
        COMPLETED,
        CANCELLED,
        REJECTED
    }
} 