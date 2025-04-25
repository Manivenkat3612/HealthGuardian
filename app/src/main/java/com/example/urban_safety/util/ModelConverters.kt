package com.example.urban_safety.util

import android.location.Location
import com.example.urban_safety.data.model.EmergencyContact
import com.example.urban_safety.data.model.LocationData
import java.util.Date

/**
 * Utility class to handle conversion between different model types
 * This helps to solve type mismatch issues between UI models and data models
 */
object ModelConverters {
    
    /**
     * Convert an Android Location to our LocationData model
     */
    fun fromLocation(location: Location?): LocationData? {
        if (location == null) return null
        
        return LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = Date(location.time)
        )
    }
    
    /**
     * Convert a UI model EmergencyContact to data model EmergencyContact
     */
    fun toDataEmergencyContact(contact: com.example.urban_safety.models.EmergencyContact): EmergencyContact {
        return EmergencyContact(
            name = contact.name,
            phoneNumber = contact.phoneNumber
        )
    }
    
    /**
     * Convert a data model EmergencyContact to UI model EmergencyContact
     */
    fun toUiEmergencyContact(contact: EmergencyContact): com.example.urban_safety.models.EmergencyContact {
        return com.example.urban_safety.models.EmergencyContact(
            name = contact.name,
            phoneNumber = contact.phoneNumber
        )
    }
    
    /**
     * Convert a UI model LocationData to data model LocationData
     */
    fun toDataLocationData(location: com.example.urban_safety.models.LocationData?): LocationData? {
        if (location == null) return null
        
        return LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy ?: 0f,
            timestamp = Date(System.currentTimeMillis())
        )
    }
    
    /**
     * Convert a data model LocationData to UI model LocationData
     */
    fun toUiLocationData(location: LocationData?): com.example.urban_safety.models.LocationData? {
        if (location == null) return null
        
        return com.example.urban_safety.models.LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = location.timestamp.time
        )
    }
} 