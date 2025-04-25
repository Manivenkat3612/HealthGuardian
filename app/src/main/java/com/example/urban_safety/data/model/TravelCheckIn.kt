package com.example.urban_safety.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.urban_safety.data.local.converters.DateConverter
import com.example.urban_safety.data.local.converters.LocationDataConverter
import com.example.urban_safety.data.local.converters.StringListConverter
import com.example.urban_safety.data.local.converters.TravelCheckpointConverter
import com.example.urban_safety.data.local.converters.TravelStatusConverter
import com.example.urban_safety.data.local.converters.TransportModeConverter
import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Entity for travel check-ins
 */
@Entity(tableName = "travel_checkins")
data class TravelCheckIn(
    @PrimaryKey
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val destination: String = "",
    val startLocation: LocationData? = null,
    val endLocation: LocationData? = null,
    val startTime: Date = Date(),
    val estimatedArrivalTime: Date = Date(),
    val actualArrivalTime: Date? = null,
    val endTime: Date? = null,
    val transportMode: TransportMode = TransportMode.OTHER,
    val status: TravelStatus = TravelStatus.ACTIVE,
    val notifiedContacts: List<String> = emptyList(),
    val checkpoints: List<TravelCheckpoint> = emptyList(),
    val notes: String? = null
)

/**
 * Status of a travel check-in
 */
enum class TravelStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED,
    OVERDUE
}


