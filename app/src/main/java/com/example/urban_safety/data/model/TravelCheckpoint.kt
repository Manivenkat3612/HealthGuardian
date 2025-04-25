package com.example.urban_safety.data.model

import java.util.Date

/**
 * Checkpoint during travel
 */
data class TravelCheckpoint(
    val id: String = "",
    val location: LocationData? = null,
    val timestamp: Date = Date(),
    val name: String? = null,
    val isManual: Boolean = false
) 