package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.TransportMode

class TransportModeConverter {
    @TypeConverter
    fun fromTransportMode(mode: TransportMode?): String? {
        return mode?.name
    }

    @TypeConverter
    fun toTransportMode(modeString: String?): TransportMode? {
        return modeString?.let { TransportMode.valueOf(it) }
    }
} 