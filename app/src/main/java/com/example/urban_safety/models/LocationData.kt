package com.example.urban_safety.models

/**
 * UI model for location data
 */
data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float? = null,
    val address: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDataModel(location: com.example.urban_safety.data.model.LocationData?): LocationData? {
            return location?.let {
                LocationData(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracy = it.accuracy,
                    address = it.address,
                    timestamp = it.timestamp.time
                )
            }
        }
    }
    
    fun toDataModel(): com.example.urban_safety.data.model.LocationData {
        return com.example.urban_safety.data.model.LocationData(
            latitude = this.latitude,
            longitude = this.longitude,
            accuracy = this.accuracy ?: 0f,
            address = this.address,
            timestamp = java.util.Date(this.timestamp)
        )
    }
} 