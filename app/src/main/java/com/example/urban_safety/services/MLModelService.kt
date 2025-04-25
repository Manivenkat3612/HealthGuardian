package com.example.urban_safety.services

import android.content.Context
import android.util.Log
import com.example.urban_safety.models.HealthData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * Service for ML-based health predictions and analysis
 */
@Singleton
class MLModelService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MLModelService"
        private const val ECG_MODEL_NAME = "ecg_arrhythmia_model"
        private const val FALL_DETECTION_MODEL_NAME = "fall_detection_model"
        private const val HEALTH_PREDICTION_MODEL_NAME = "health_prediction_model"
    }

    private var ecgInterpreter: Interpreter? = null
    private var fallDetectionInterpreter: Interpreter? = null
    private var healthPredictionInterpreter: Interpreter? = null

    private val random = Random()
    private var isInitialized = false
    
    init {
        // We'll simulate model loading and initialization for the hackathon
        // In a production app, you'd download the models from Firebase ML or assets
        Log.d(TAG, "Initializing ML models")
        isInitialized = true
    }

    /**
     * Analyze ECG data for arrhythmia detection
     * 
     * @param ecgData List of ECG readings
     * @return True if arrhythmia detected, false otherwise
     */
    fun analyzeECG(ecgData: List<Float>?): Boolean {
        if (ecgData == null || ecgData.isEmpty()) return false
        
        // For the hackathon, we'll simulate ECG analysis
        // In a real app, you'd use the TFLite interpreter
        
        // Simple simulation: detect if there are any large spikes or irregularities
        // This is NOT accurate for real ECG analysis
        if (ecgData.size < 10) return false
        
        var irregularities = 0
        val threshold = 0.5f
        
        // Count "irregularities" as large jumps between consecutive points
        for (i in 1 until ecgData.size) {
            if (Math.abs(ecgData[i] - ecgData[i-1]) > threshold) {
                irregularities++
            }
        }
        
        // If more than 15% of points have irregularities, consider it abnormal
        return irregularities.toFloat() / ecgData.size > 0.15f
    }
    
    /**
     * Detect falls based on accelerometer and gyroscope data
     * 
     * @param accelerometerData List of accelerometer readings [x, y, z, x, y, z, ...]
     * @param gyroscopeData List of gyroscope readings [x, y, z, x, y, z, ...]
     * @return True if fall detected, false otherwise
     */
    fun detectFall(accelerometerData: List<Float>?, gyroscopeData: List<Float>?): Boolean {
        if (accelerometerData == null || accelerometerData.isEmpty()) return false
        
        // For the hackathon, we'll simulate fall detection
        // In a real app, you'd use the TFLite interpreter
        
        // Simple simulation: detect if there's a large acceleration spike followed by a static period
        // This is NOT accurate for real fall detection
        if (accelerometerData.size < 30) return false
        
        // Look for large downward acceleration (simulating fall) followed by period of low movement
        var maxAcceleration = 0f
        var lowMovementPeriods = 0
        
        for (i in 0 until accelerometerData.size step 3) {
            if (i + 2 >= accelerometerData.size) break
            
            // Calculate magnitude of acceleration
            val x = accelerometerData[i]
            val y = accelerometerData[i+1]
            val z = accelerometerData[i+2]
            val magnitude = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            
            maxAcceleration = max(maxAcceleration, magnitude)
            
            // Count periods of low movement
            if (magnitude < 0.2f) {
                lowMovementPeriods++
            }
        }
        
        // If there was a large acceleration spike and then periods of low movement, consider it a fall
        return maxAcceleration > 3.0f && lowMovementPeriods > 5
    }
    
    /**
     * Predict health conditions based on vital signs
     * 
     * @param healthData Health data for prediction
     * @return Map of health conditions to probability (0-1)
     */
    fun predictHealthConditions(healthData: HealthData): Map<String, Float> {
        // For the hackathon, we'll simulate health predictions
        // In a real app, you'd use the TFLite interpreter
        
        val predictions = mutableMapOf<String, Float>()
        
        // Heart disease risk based on heart rate, blood pressure
        val heartRate = healthData.heartRate
        val systolic = healthData.bloodPressureSystolic ?: 120
        val diastolic = healthData.bloodPressureDiastolic ?: 80
        
        if (heartRate > 100 || systolic > 140 || diastolic > 90) {
            val heartDiseaseRisk = calculateRisk(
                heartRate.toFloat() / 220f, 
                systolic.toFloat() / 180f,
                diastolic.toFloat() / 120f
            )
            predictions["Heart Disease Risk"] = heartDiseaseRisk
        }
        
        // Stress level estimation based on heart rate variability
        healthData.heartRateVariability?.let { hrv ->
            if (hrv < 30) {
                val stressRisk = (30 - hrv) / 30f
                predictions["High Stress"] = stressRisk
            }
        }
        
        // Respiratory irregularity based on blood oxygen and respiration rate
        val spo2 = healthData.bloodOxygen ?: 98
        val respRate = healthData.respirationRate ?: 16
        
        if (spo2 < 95 || respRate > 20 || respRate < 12) {
            val respiratoryRisk = calculateRisk(
                (100 - spo2).toFloat() / 10f,
                Math.abs(respRate - 16).toFloat() / 8f
            )
            predictions["Respiratory Issue"] = respiratoryRisk
        }
        
        // Atrial fibrillation risk if arrhythmia detected
        if (healthData.arrhythmiaDetected) {
            predictions["Atrial Fibrillation"] = 0.7f + random.nextFloat() * 0.2f
        }
        
        return predictions
    }
    
    /**
     * Calculate weighted risk score from multiple inputs
     */
    private fun calculateRisk(vararg factors: Float): Float {
        var risk = 0f
        var weight = 0f
        
        for (factor in factors) {
            risk += factor
            weight += 1
        }
        
        return (risk / weight).coerceIn(0f, 1f)
    }
    
    /**
     * Generate simulated ECG data for testing
     * 
     * @param duration Duration in seconds
     * @param isAbnormal Whether to generate abnormal ECG
     * @return List of ECG values
     */
    fun generateSimulatedECG(duration: Int = 5, isAbnormal: Boolean = false): List<Float> {
        // Sampling rate: 250Hz (typical for ECG)
        val samplingRate = 250
        val totalSamples = duration * samplingRate
        val result = ArrayList<Float>(totalSamples)
        
        // Heart rate between 60-100 bpm
        val heartRate = 60 + random.nextInt(41)
        val secondsPerBeat = 60.0 / heartRate
        
        // Basic ECG waveform parameters
        val pAmplitude = 0.1f
        val qAmplitude = -0.1f
        val rAmplitude = 1.0f
        val sAmplitude = -0.2f
        val tAmplitude = 0.2f
        
        val pDuration = 0.08
        val prInterval = 0.16
        val qrsDuration = 0.08
        val stInterval = 0.12
        val tDuration = 0.16
        
        for (i in 0 until totalSamples) {
            val time = i.toDouble() / samplingRate
            val beatTime = time % secondsPerBeat
            
            // Generate normal ECG waveform
            val value = when {
                // P wave
                beatTime < pDuration -> pAmplitude * sin(PI * beatTime / pDuration).toFloat()
                
                // PR interval (flat)
                beatTime < prInterval -> 0f
                
                // QRS complex
                beatTime < prInterval + qrsDuration / 3 -> qAmplitude
                beatTime < prInterval + qrsDuration * 2/3 -> rAmplitude
                beatTime < prInterval + qrsDuration -> sAmplitude
                
                // ST segment (flat)
                beatTime < prInterval + qrsDuration + stInterval -> 0f
                
                // T wave
                beatTime < prInterval + qrsDuration + stInterval + tDuration -> 
                    tAmplitude * sin(PI * (beatTime - prInterval - qrsDuration - stInterval) / tDuration).toFloat()
                
                // Rest of the cycle (flat)
                else -> 0f
            }
            
            // Add noise
            val noise = (random.nextFloat() - 0.5f) * 0.05f
            
            // Add arrhythmia simulation for abnormal ECG
            val arrhythmia = if (isAbnormal && random.nextFloat() < 0.2f) {
                // Simulate premature ventricular contraction or other arrhythmias
                (random.nextFloat() - 0.5f) * 0.7f
            } else 0f
            
            result.add(value + noise + arrhythmia)
        }
        
        return result
    }
    
    /**
     * Generate simulated accelerometer data for testing
     * 
     * @param duration Duration in seconds
     * @param hasFall Whether to simulate a fall
     * @return List of accelerometer values [x, y, z, x, y, z, ...]
     */
    fun generateSimulatedAccelerometerData(duration: Int = 5, hasFall: Boolean = false): List<Float> {
        // Sampling rate: 50Hz (typical for phone accelerometer)
        val samplingRate = 50
        val totalSamples = duration * samplingRate
        val result = ArrayList<Float>(totalSamples * 3) // x, y, z for each sample
        
        // Default gravity vector (when phone is still)
        val gravityX = 0f
        val gravityY = 0f
        val gravityZ = 9.8f
        
        // Fall simulation parameters
        val fallStart = if (hasFall) samplingRate else totalSamples * 2
        val fallDuration = samplingRate / 2  // 0.5 seconds
        
        for (i in 0 until totalSamples) {
            // Add basic noise to simulate minor movements
            val noiseX = (random.nextFloat() - 0.5f) * 0.2f
            val noiseY = (random.nextFloat() - 0.5f) * 0.2f
            val noiseZ = (random.nextFloat() - 0.5f) * 0.2f
            
            if (hasFall && i >= fallStart && i < fallStart + fallDuration) {
                // Simulate fall: large acceleration followed by impact and then stillness
                val fallProgress = (i - fallStart).toFloat() / fallDuration
                
                when {
                    // Initial acceleration phase
                    fallProgress < 0.3f -> {
                        result.add(gravityX + noiseX + (random.nextFloat() - 0.5f) * 3f)
                        result.add(gravityY + noiseY + (random.nextFloat() - 0.5f) * 3f)
                        result.add(gravityZ + noiseZ - fallProgress * 15f) // Reducing Z as falling
                    }
                    // Impact phase
                    fallProgress < 0.4f -> {
                        result.add(gravityX + noiseX + (random.nextFloat() - 0.5f) * 10f)
                        result.add(gravityY + noiseY + (random.nextFloat() - 0.5f) * 10f)
                        result.add(gravityZ + noiseZ + (random.nextFloat() - 0.5f) * 10f)
                    }
                    // Stillness after fall
                    else -> {
                        // Different gravity vector after fall (person lying down)
                        result.add(gravityZ + noiseX) // Person might be lying on side
                        result.add(gravityY + noiseY)
                        result.add(gravityX + noiseZ)
                    }
                }
            } else {
                // Normal activity
                result.add(gravityX + noiseX)
                result.add(gravityY + noiseY)
                result.add(gravityZ + noiseZ)
            }
        }
        
        return result
    }
} 