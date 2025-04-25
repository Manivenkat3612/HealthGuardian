package com.example.urban_safety.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Data class representing an SOS request in the system
 */
data class SosRequest(
    @DocumentId
    val id: String = "",
    val requesterId: String = "",
    val requesterName: String = "",
    val location: LocationData? = null,
    val type: IncidentType = IncidentType.OTHER,
    val message: String = "",
    val timestamp: Date = Date(),
    val status: String = "PENDING",
    val responderId: String? = null,
    val responderName: String? = null,
    val responseTime: Date? = null
) 