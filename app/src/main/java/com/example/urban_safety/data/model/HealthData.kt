package com.example.urban_safety.data.model

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Model class representing health monitoring data
 */
data class HealthData(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val heartRate: Int = 0,
    val stepCount: Int = 0,
    val bloodOxygen: Int? = null,
    val bloodPressureSystolic: Int? = null,
    val bloodPressureDiastolic: Int? = null,
    val bodyTemperature: Float? = null,
    val timestamp: Date = Date(),
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
) {
    companion object {
        fun fromModelHealthData(healthData: com.example.urban_safety.models.HealthData): HealthData {
            return HealthData(
                heartRate = healthData.heartRate ?: 0,
                stepCount = healthData.stepCount ?: 0,
                bloodOxygen = healthData.bloodOxygen,
                bloodPressureSystolic = healthData.bloodPressureSystolic,
                bloodPressureDiastolic = healthData.bloodPressureDiastolic,
                bodyTemperature = healthData.bodyTemperature,
                timestamp = Date(healthData.timestamp),
                isAbnormal = healthData.isAbnormal,
                ecgReadings = healthData.ecgReadings,
                heartRateVariability = healthData.heartRateVariability,
                accelerometerData = healthData.accelerometerData,
                gyroscopeData = healthData.gyroscopeData,
                respirationRate = healthData.respirationRate,
                stressLevel = healthData.stressLevel,
                sleepQuality = healthData.sleepQuality,
                fallDetected = healthData.fallDetected,
                arrhythmiaDetected = healthData.arrhythmiaDetected,
                predictionResults = healthData.predictionResults
            )
        }
    }
    
    fun toModelHealthData(): com.example.urban_safety.models.HealthData {
        return com.example.urban_safety.models.HealthData(
            heartRate = this.heartRate,
            stepCount = this.stepCount,
            bloodOxygen = this.bloodOxygen,
            bloodPressureSystolic = this.bloodPressureSystolic,
            bloodPressureDiastolic = this.bloodPressureDiastolic,
            bodyTemperature = this.bodyTemperature,
            timestamp = this.timestamp.time,
            isAbnormal = this.isAbnormal,
            ecgReadings = this.ecgReadings,
            heartRateVariability = this.heartRateVariability,
            accelerometerData = this.accelerometerData,
            gyroscopeData = this.gyroscopeData,
            respirationRate = this.respirationRate,
            stressLevel = this.stressLevel,
            sleepQuality = this.sleepQuality,
            fallDetected = this.fallDetected,
            arrhythmiaDetected = this.arrhythmiaDetected,
            predictionResults = this.predictionResults
        )
    }
} 