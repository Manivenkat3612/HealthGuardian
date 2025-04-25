package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.IncidentStatus

class IncidentStatusConverter {
    @TypeConverter
    fun fromIncidentStatus(status: IncidentStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toIncidentStatus(statusString: String?): IncidentStatus? {
        return statusString?.let { IncidentStatus.valueOf(it) }
    }
} 