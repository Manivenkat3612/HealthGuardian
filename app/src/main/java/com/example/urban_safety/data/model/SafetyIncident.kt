package com.example.urban_safety.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.urban_safety.data.local.converters.DateConverter
import com.example.urban_safety.data.local.converters.EmergencyContactConverter
import com.example.urban_safety.data.local.converters.IncidentStatusConverter
import com.example.urban_safety.data.local.converters.IncidentTypeConverter
import com.example.urban_safety.data.local.converters.LocationDataConverter
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import java.util.Date

/**
 * Model class representing a safety incident
 */
@Entity(tableName = "safety_incidents")
@TypeConverters(
    DateConverter::class,
    LocationDataConverter::class,
    EmergencyContactConverter::class,
    IncidentTypeConverter::class,
    IncidentStatusConverter::class
)
data class SafetyIncident(
    @PrimaryKey
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val type: IncidentType = IncidentType.OTHER,
    val description: String = "",
    val location: LocationData? = null,
    val timestamp: Date = Date(),
    val status: IncidentStatus = IncidentStatus.ACTIVE,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val resolvedTimestamp: Date? = null,
    val resolvedBy: String? = null,
    
    @Exclude
    @get:Exclude
    var isSavedToFirestore: Boolean = false
)

/**
 * Represents the status of a safety incident
 */
enum class IncidentStatus {
    ACTIVE,
    RESOLVED,
    FALSE_ALARM
} 