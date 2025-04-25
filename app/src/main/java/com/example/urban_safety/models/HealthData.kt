package com.example.urban_safety.models

/**
 * UI model for health data
 */
data class HealthData(
    val heartRate: Int = 0,
    val stepCount: Int = 0,
    val bloodOxygen: Int? = null,
    val bloodPressureSystolic: Int? = null,
    val bloodPressureDiastolic: Int? = null,
    val bodyTemperature: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isAbnormal: Boolean = false,
    // Fields for ML-driven monitoring
    val ecgReadings: List<Float>? = null,
    val heartRateVariability: Float? = null,
    val accelerometerData: List<Float>? = null,
    val gyroscopeData: List<Float>? = null,
    val respirationRate: Int? = null,
    val stressLevel: Int? = null,  // 0-100 scale
    val sleepQuality: Int? = null, // 0-100 scale
    val fallDetected: Boolean = false,
    val arrhythmiaDetected: Boolean = false,
    val predictionResults: Map<String, Float>? = null // Map of condition to probability
) 