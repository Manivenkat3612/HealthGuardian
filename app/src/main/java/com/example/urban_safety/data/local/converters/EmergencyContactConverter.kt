package com.example.urban_safety.data.local.converters

import androidx.room.TypeConverter
import com.example.urban_safety.data.model.EmergencyContact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EmergencyContactConverter {
    private val gson = Gson()
    private val type = object : TypeToken<List<EmergencyContact>>() {}.type

    @TypeConverter
    fun fromEmergencyContacts(contacts: List<EmergencyContact>?): String? {
        return contacts?.let { gson.toJson(it, type) }
    }

    @TypeConverter
    fun toEmergencyContacts(contactsString: String?): List<EmergencyContact>? {
        return contactsString?.let { gson.fromJson(it, type) }
    }
} 