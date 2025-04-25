package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.SafetySeverity

class SafetySeverityConverter {
    @TypeConverter
    fun fromSafetySeverity(severity: SafetySeverity?): String? {
        return severity?.name
    }

    @TypeConverter
    fun toSafetySeverity(severityString: String?): SafetySeverity? {
        return severityString?.let { SafetySeverity.valueOf(it) }
    }
} 