package com.example.urban_safety.data.local.type

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.CheckpointStatus
import com.example.urban_safety.data.model.HealthData
import com.example.urban_safety.data.model.IncidentStatus
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.model.SafetyCategory
import com.example.urban_safety.data.model.SafetySeverity
import com.example.urban_safety.data.model.TransportMode
import com.example.urban_safety.data.model.TravelCheckpoint
import com.example.urban_safety.data.model.TravelStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Type converters for Room to handle complex types
 */
class Converters {
    private val gson = Gson()

    // Convert LocationData object to JSON string
    @TypeConverter
    fun fromLocationData(locationData: LocationData?): String? {
        return locationData?.let { gson.toJson(it) }
    }

    // Convert JSON string to LocationData object
    @TypeConverter
    fun toLocationData(locationDataString: String?): LocationData? {
        return locationDataString?.let {
            gson.fromJson(it, LocationData::class.java)
        }
    }

    // Convert HealthData object to JSON string
    @TypeConverter
    fun fromHealthData(healthData: HealthData?): String? {
        return healthData?.let { gson.toJson(it) }
    }

    // Convert JSON string to HealthData object
    @TypeConverter
    fun toHealthData(healthDataString: String?): HealthData? {
        return healthDataString?.let {
            gson.fromJson(it, HealthData::class.java)
        }
    }

    // Convert list of strings to JSON string
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    // Convert JSON string to list of strings
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        } ?: emptyList()
    }

    // Convert Map<SafetyCategory, Int> to JSON string
    @TypeConverter
    fun fromCategoryMap(map: Map<SafetyCategory, Int>?): String? {
        return map?.let { gson.toJson(it) }
    }

    // Convert JSON string to Map<SafetyCategory, Int>
    @TypeConverter
    fun toCategoryMap(value: String?): Map<SafetyCategory, Int>? {
        return value?.let {
            val type = object : TypeToken<Map<SafetyCategory, Int>>() {}.type
            gson.fromJson(it, type)
        } ?: emptyMap()
    }

    // Convert List<TravelCheckpoint> to JSON string
    @TypeConverter
    fun fromCheckpointList(checkpoints: List<TravelCheckpoint>?): String? {
        return checkpoints?.let { gson.toJson(it) }
    }

    // Convert JSON string to List<TravelCheckpoint>
    @TypeConverter
    fun toCheckpointList(value: String?): List<TravelCheckpoint>? {
        return value?.let {
            val type = object : TypeToken<List<TravelCheckpoint>>() {}.type
            gson.fromJson(it, type)
        } ?: emptyList()
    }

    // Enum converters
    @TypeConverter
    fun fromIncidentType(value: IncidentType?): String? = value?.name

    @TypeConverter
    fun toIncidentType(value: String?): IncidentType? = value?.let { IncidentType.valueOf(it) }

    @TypeConverter
    fun fromIncidentStatus(value: IncidentStatus?): String? = value?.name

    @TypeConverter
    fun toIncidentStatus(value: String?): IncidentStatus? = value?.let { IncidentStatus.valueOf(it) }

    @TypeConverter
    fun fromSafetyCategory(value: SafetyCategory?): String? = value?.name

    @TypeConverter
    fun toSafetyCategory(value: String?): SafetyCategory? = value?.let { SafetyCategory.valueOf(it) }

    @TypeConverter
    fun fromSafetySeverity(value: SafetySeverity?): String? = value?.name

    @TypeConverter
    fun toSafetySeverity(value: String?): SafetySeverity? = value?.let { SafetySeverity.valueOf(it) }

    @TypeConverter
    fun fromTravelStatus(value: TravelStatus?): String? = value?.name

    @TypeConverter
    fun toTravelStatus(value: String?): TravelStatus? = value?.let { TravelStatus.valueOf(it) }

    @TypeConverter
    fun fromTransportMode(value: TransportMode?): String? = value?.name

    @TypeConverter
    fun toTransportMode(value: String?): TransportMode? = value?.let { TransportMode.valueOf(it) }

    @TypeConverter
    fun fromCheckpointStatus(value: CheckpointStatus?): String? = value?.name

    @TypeConverter
    fun toCheckpointStatus(value: String?): CheckpointStatus? = value?.let { CheckpointStatus.valueOf(it) }
} 