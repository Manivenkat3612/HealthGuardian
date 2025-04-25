package com.example.urban_safety.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.urban_safety.data.local.converters.DateConverter
import com.example.urban_safety.data.local.converters.LocationDataConverter
import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Categories of safety reports
 */
enum class SafetyCategory {
    THEFT,
    ASSAULT,
    HARASSMENT,
    SUSPICIOUS_ACTIVITY,
    LIGHTING_ISSUE,
    ROAD_HAZARD,
    PUBLIC_DISTURBANCE,
    INFRASTRUCTURE,
    TRAFFIC_HAZARD,
    OTHER
}

/**
 * Severity levels for safety reports
 */
enum class SafetySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Entity for community safety reports
 */
@Entity(tableName = "safety_reports")
data class SafetyReport(
    @PrimaryKey
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val category: SafetyCategory = SafetyCategory.OTHER,
    val severity: SafetySeverity = SafetySeverity.MEDIUM,
    val description: String = "",
    val location: LocationData? = null,
    val timestamp: Date = Date(),
    val isAnonymous: Boolean = false,
    val imageUrl: String? = null,
    val verified: Boolean = false,
    val address: String? = null,
    val photoUrls: List<String> = emptyList(),
    val verifiedBy: String? = null,
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val isResolved: Boolean = false,
    val resolvedTimestamp: Date? = null
)

/**
 * Entity for area safety scores
 */
@Entity(tableName = "area_safety_scores")
data class AreaSafetyScore(
    @PrimaryKey
    @DocumentId
    val id: String = "",
    val locationData: LocationData? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Double = 0.0,
    val areaName: String? = null,
    val overallScore: Float = 0.0f,
    val crimeScore: Float = 0.0f,
    val lightingScore: Float = 0.0f,
    val publicTransportScore: Float = 0.0f,
    val emergencyServiceScore: Float = 0.0f,
    val communityAwarenessScore: Float = 0.0f,
    val publicFootfallScore: Float = 0.0f,
    val incidentCount: Int = 0,
    val reportCount: Int = 0,
    val lastUpdated: Date = Date(),
    val safetyTips: List<String> = emptyList(),
    val dataSourceQuality: DataSourceQuality = DataSourceQuality.MEDIUM,
    val temporaryFactors: List<TemporarySafetyFactor> = emptyList()
)

/**
 * Comprehensive safety score model for an area
 */
data class SafetyScore(
    val overallScore: Double,
    val crimeRate: Double,
    val lightingScore: Double,
    val populationDensity: Double,
    val emergencyResponseTime: Double,
    val publicTransportScore: Double,
    val safetyTips: List<String> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Enum representing the quality of data sources used for safety scores
 */
enum class DataSourceQuality {
    LOW,
    MEDIUM,
    HIGH,
    VERIFIED
}

/**
 * Data class representing temporary factors affecting safety scores
 */
data class TemporarySafetyFactor(
    val id: String = "",
    val factorType: TemporaryFactorType = TemporaryFactorType.OTHER,
    val description: String? = null,
    val impactScore: Float = 0.0f,  // -10 to +10 scale, negative means reduces safety
    val startTime: Date = Date(),
    val endTime: Date? = null,
    val isActive: Boolean = true
)

/**
 * Enum representing types of temporary safety factors
 */
enum class TemporaryFactorType {
    EVENT,
    CONSTRUCTION,
    WEATHER,
    POLICE_ACTIVITY,
    TRAFFIC,
    PUBLIC_GATHERING,
    OTHER
} 