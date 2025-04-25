package com.example.urban_safety.data.repository

import android.content.Context
import android.util.Log
import com.example.urban_safety.data.local.UrbanSafetyDatabase
import com.example.urban_safety.data.model.HealthData
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.request.OnDataPointListener
import com.google.android.gms.fitness.request.SensorRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val database: UrbanSafetyDatabase
) {
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .build()

    private val _healthData = MutableStateFlow<HealthData?>(null)
    val healthData: StateFlow<HealthData?> = _healthData

    private var dataPointListener: OnDataPointListener? = null
    private var isMonitoring = false
    private var lastHealthData: HealthData? = null

    private val random = Random()

    companion object {
        private const val COLLECTION_HEALTH_DATA = "health_data"
        private const val COLLECTION_HEALTH_ALERTS = "health_alerts"
    }

    /**
     * Get the last recorded health data
     */
    suspend fun getLastHealthData(): HealthData? {
        val currentUser = auth.currentUser ?: return null
        
        try {
            val snapshot = firestore.collection(COLLECTION_HEALTH_DATA)
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
            
            return if (!snapshot.isEmpty) {
                snapshot.documents[0].toObject(HealthData::class.java)?.also {
                    lastHealthData = it
                }
            } else {
                // Return simulated data if no real data exists yet
                createSimulatedHealthData().also {
                    lastHealthData = it
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Return cached data or simulated data on error
            return lastHealthData ?: createSimulatedHealthData()
        }
    }
    
    /**
     * Save new health data
     */
    suspend fun saveHealthData(healthData: HealthData): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        val dataWithUserId = healthData.copy(userId = currentUser.uid)
        lastHealthData = dataWithUserId
        
        return try {
            firestore.collection(COLLECTION_HEALTH_DATA)
                .add(dataWithUserId)
                .await()
            
            // If data is abnormal, save an alert
            if (dataWithUserId.isAbnormal) {
                saveHealthAlert(dataWithUserId)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Save a health alert for abnormal values
     */
    private suspend fun saveHealthAlert(healthData: HealthData) {
        try {
            val alert = mapOf(
                "userId" to healthData.userId,
                "healthData" to healthData,
                "timestamp" to Date(),
                "resolved" to false
            )
            
            firestore.collection(COLLECTION_HEALTH_ALERTS)
                .add(alert)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Get health data stream
     * In a real app, this would connect to a wearable device or health API
     */
    fun getHealthDataStream(): Flow<HealthData> = flow {
        // This would be implemented with a real device connection
        // For demo purposes, we'll just emit some simulated data
        val baseData = lastHealthData ?: createSimulatedHealthData()
        emit(baseData)
    }
    
    /**
     * Start health monitoring
     */
    suspend fun startHealthMonitoring() {
        if (isMonitoring) return

        try {
            // For demonstration purposes, we'll simulate health data
            // In a real app, you would need to handle Google Sign-In and permissions
            simulateHealthData()
            isMonitoring = true
            Log.d("HealthRepository", "Health monitoring started")
        } catch (e: Exception) {
            Log.e("HealthRepository", "Failed to start health monitoring", e)
        }
    }

    suspend fun stopHealthMonitoring() {
        if (!isMonitoring) return

        try {
            isMonitoring = false
            Log.d("HealthRepository", "Health monitoring stopped")
        } catch (e: Exception) {
            Log.e("HealthRepository", "Failed to stop health monitoring", e)
        }
    }

    private fun simulateHealthData() {
        // Simulate heart rate between 60-100 bpm
        val heartRate = (60..100).random()
        // Simulate steps between 100-500
        val steps = (100..500).random()
        
        updateHealthData { 
            HealthData(
                heartRate = heartRate,
                stepCount = steps,
                timestamp = java.util.Date(System.currentTimeMillis())
            )
        }
    }

    private fun updateHealthData(update: (HealthData?) -> HealthData) {
        _healthData.value = update(_healthData.value)
    }

    /**
     * Check if monitoring is active
     */
    fun isMonitoringActive(): Boolean {
        return isMonitoring
    }
    
    /**
     * Create simulated health data for demo purposes
     */
    private fun createSimulatedHealthData(): HealthData {
        return HealthData(
            heartRate = 60 + random.nextInt(41), // 60-100
            stepCount = 100 + random.nextInt(9901), // 100-10000
            bloodOxygen = 94 + random.nextInt(6), // 94-99
            bloodPressureSystolic = 110 + random.nextInt(31), // 110-140
            bloodPressureDiastolic = 70 + random.nextInt(21), // 70-90
            bodyTemperature = 36.5f + (random.nextFloat() - 0.5f), // 36.0-37.0
            timestamp = Date(),
            isAbnormal = false
        )
    }
} 