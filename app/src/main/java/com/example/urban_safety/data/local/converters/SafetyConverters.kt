package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.DataSourceQuality
import com.example.urban_safety.data.model.TemporaryFactorType
import com.example.urban_safety.data.model.TemporarySafetyFactor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room converter for TemporarySafetyFactor and other safety model types
 */
class TemporarySafetyFactorConverter {
    private val gson = Gson()
    private val type = object : TypeToken<List<TemporarySafetyFactor>>() {}.type

    @TypeConverter
    fun fromTemporarySafetyFactors(factors: List<TemporarySafetyFactor>?): String? {
        return factors?.let { gson.toJson(it, type) }
    }

    @TypeConverter
    fun toTemporarySafetyFactors(json: String?): List<TemporarySafetyFactor>? {
        return json?.let { gson.fromJson(it, type) }
    }
}

/**
 * Room converter for DataSourceQuality enum
 */
class DataSourceQualityConverter {
    @TypeConverter
    fun fromDataSourceQuality(quality: DataSourceQuality?): String? {
        return quality?.name
    }

    @TypeConverter
    fun toDataSourceQuality(qualityString: String?): DataSourceQuality? {
        return qualityString?.let { DataSourceQuality.valueOf(it) }
    }
}

/**
 * Room converter for TemporaryFactorType enum
 */
class TemporaryFactorTypeConverter {
    @TypeConverter
    fun fromTemporaryFactorType(type: TemporaryFactorType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toTemporaryFactorType(typeString: String?): TemporaryFactorType? {
        return typeString?.let { TemporaryFactorType.valueOf(it) }
    }
} 