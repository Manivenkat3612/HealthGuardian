package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.SafetyCategory

class SafetyCategoryConverter {
    @TypeConverter
    fun fromSafetyCategory(category: SafetyCategory?): String? {
        return category?.name
    }

    @TypeConverter
    fun toSafetyCategory(categoryString: String?): SafetyCategory? {
        return categoryString?.let { SafetyCategory.valueOf(it) }
    }
} 