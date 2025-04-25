package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.LocationData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class LocationDataConverter {
    private val gson = Gson()
    private val type = object : TypeToken<LocationData>() {}.type

    @TypeConverter
    fun fromLocationData(location: LocationData?): String? {
        return location?.let { gson.toJson(it, type) }
    }

    @TypeConverter
    fun toLocationData(locationString: String?): LocationData? {
        return locationString?.let { gson.fromJson(it, type) }
    }
} 