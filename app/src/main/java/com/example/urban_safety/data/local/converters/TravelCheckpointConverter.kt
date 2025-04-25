package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.TravelCheckpoint
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TravelCheckpointConverter {
    private val gson = Gson()
    private val type = object : TypeToken<List<TravelCheckpoint>>() {}.type

    @TypeConverter
    fun fromTravelCheckpoints(checkpoints: List<TravelCheckpoint>?): String? {
        return checkpoints?.let { gson.toJson(it, type) }
    }

    @TypeConverter
    fun toTravelCheckpoints(string: String?): List<TravelCheckpoint>? {
        return string?.let { gson.fromJson(it, type) }
    }
} 