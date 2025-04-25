package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.TravelStatus

class TravelStatusConverter {
    @TypeConverter
    fun fromTravelStatus(status: TravelStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun toTravelStatus(statusString: String?): TravelStatus? {
        return statusString?.let { TravelStatus.valueOf(it) }
    }
} 