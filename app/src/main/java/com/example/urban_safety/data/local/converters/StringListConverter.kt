package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StringListConverter {
    private val gson = Gson()
    private val type = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it, type) }
    }

    @TypeConverter
    fun toStringList(string: String?): List<String>? {
        return string?.let { gson.fromJson(it, type) }
    }
} 