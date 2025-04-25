package com.example.urban_safety.data.model

import android.location.Location
import java.util.Date

/**
 * Model class representing location data
 */
data class LocationData(
    val id: String = "",
    val userId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0f,
    val address: String? = null,
    val timestamp: Date = Date()
) {
    companion object {
        fun fromLocation(location: Location): LocationData {
            return LocationData(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                timestamp = Date(location.time)
            )
        }

        fun fromModelLocationData(locationData: com.example.urban_safety.models.LocationData): LocationData {
            return LocationData(
                latitude = locationData.latitude,
                longitude = locationData.longitude,
                accuracy = locationData.accuracy ?: 0f,
                address = locationData.address,
                timestamp = Date(locationData.timestamp)
            )
        }
    }

    fun toModelLocationData(): com.example.urban_safety.models.LocationData {
        return com.example.urban_safety.models.LocationData(
            latitude = this.latitude,
            longitude = this.longitude,
            accuracy = this.accuracy,
            address = this.address,
            timestamp = this.timestamp.time
        )
    }

    fun toLocation(): Location {
        val location = Location("")
        location.latitude = this.latitude
        location.longitude = this.longitude
        location.accuracy = this.accuracy
        location.time = this.timestamp.time
        return location
    }
} 