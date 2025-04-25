package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.IncidentType

class IncidentTypeConverter {
    @TypeConverter
    fun fromIncidentType(type: IncidentType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toIncidentType(typeString: String?): IncidentType? {
        return typeString?.let { IncidentType.valueOf(it) }
    }
} 