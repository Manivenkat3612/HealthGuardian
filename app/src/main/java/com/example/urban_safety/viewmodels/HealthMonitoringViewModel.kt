package com.example.urban_safety.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.repository.HealthRepository
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.data.repository.SafetyRepository
import com.example.urban_safety.models.HealthData
import com.example.urban_safety.services.HealthMonitoringService
import com.example.urban_safety.services.MLModelService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Date
import java.util.Random
import kotlin.math.max
import kotlin.math.min

/**
 * Data class to hold health threshold values
 */
data class HealthThresholds(
    val heartRateMin: Int? = 50,
    val heartRateMax: Int? = 150,
    val bloodOxygenMin: Int? = 95,
    val bpSystolicMax: Int? = 140,
    val bpDiastolicMax: Int? = 90,
    val tempMin: Float? = 35.5f,
    val tempMax: Float? = 38.0f
)

@HiltViewModel
class HealthMonitoringViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthRepository: HealthRepository,
    private val locationRepository: LocationRepository,
    private val safetyRepository: SafetyRepository,
    private val mlModelService: MLModelService
) : ViewModel() {
    
    private val _healthData = MutableStateFlow<HealthData?>(null)
    val healthData: StateFlow<HealthData?> = _healthData.asStateFlow()
    
    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _thresholds = MutableStateFlow(HealthThresholds())
    val thresholds: StateFlow<HealthThresholds> = _thresholds.asStateFlow()
    
    private var monitoringJob: kotlinx.coroutines.Job? = null
    
    private val random = Random()
    
    // Counters for simulation patterns
    private var monitoringCycles = 0
    private var abnormalityCounter = 0
    private var fallDetectionCounter = 0
    private var arrhythmiaCounter = 0
    
    init {
        // Check if monitoring service is already running
        checkServiceStatus()
        // Load initial health data
        loadLastHealthData()
        // Load user's custom thresholds if they exist
        loadUserThresholds()
    }
    
    private fun checkServiceStatus() {
        // In a real app, this would check if HealthMonitoringService is running
        _isMonitoring.value = healthRepository.isMonitoringActive()
    }
    
    private fun loadLastHealthData() {
        viewModelScope.launch {
            try {
                val data = healthRepository.getLastHealthData()?.toModelHealthData()
                _healthData.value = data
            } catch (e: Exception) {
                _error.value = "Failed to load health data: ${e.message}"
            }
        }
    }
    
    private fun loadUserThresholds() {
        viewModelScope.launch {
            try {
                // In a real app, this would load user-specific thresholds from a repository
                // For now, we'll use default values
                _thresholds.value = HealthThresholds(
                    heartRateMin = 50,
                    heartRateMax = 150,
                    bloodOxygenMin = 95,
                    bpSystolicMax = 140,
                    bpDiastolicMax = 90,
                    tempMin = 35.5f,
                    tempMax = 38.0f
                )
            } catch (e: Exception) {
                _error.value = "Failed to load thresholds: ${e.message}"
            }
        }
    }
    
    fun startMonitoring() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Start the health monitoring service
                val serviceIntent = Intent(context, HealthMonitoringService::class.java).apply {
                    action = HealthMonitoringService.ACTION_START_MONITORING
                    putExtra(HealthMonitoringService.EXTRA_HEART_RATE_THRESHOLD, _thresholds.value.heartRateMax)
                    putExtra(HealthMonitoringService.EXTRA_CHECK_INTERVAL_MS, 30000L) // 30 seconds
                }
                context.startForegroundService(serviceIntent)
                
                // Start local polling for UI updates
                startPollingHealthData()
                
                _isMonitoring.value = true
            } catch (e: Exception) {
                _error.value = "Failed to start monitoring: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun stopMonitoring() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Stop the health monitoring service
                val serviceIntent = Intent(context, HealthMonitoringService::class.java).apply {
                    action = HealthMonitoringService.ACTION_STOP_MONITORING
                }
                context.startService(serviceIntent)
                
                // Stop local polling
                stopPollingHealthData()
                
                _isMonitoring.value = false
            } catch (e: Exception) {
                _error.value = "Failed to stop monitoring: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Trigger a manual SOS
     */
    fun triggerManualSOS() {
        viewModelScope.launch {
            try {
                val location = locationRepository.getLastLocation().getOrNull()
                
                // Trigger SOS with health data
                safetyRepository.triggerSOS(
                    message = "Health emergency! Vital signs indicate a medical emergency.",
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    healthData = _healthData.value?.let { 
                        com.example.urban_safety.data.model.HealthData.fromModelHealthData(it)
                    }
                )
                
                // Show success message
                _error.value = "Emergency SOS triggered successfully."
                
                // Clear error after a few seconds
                delay(3000)
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to trigger SOS: ${e.message}"
            }
        }
    }
    
    /**
     * Simulates polling health data for UI updates
     * In a real app, this would connect to a wearable device or health API
     */
    private fun startPollingHealthData() {
        // Cancel existing job if any
        monitoringJob?.cancel()
        
        monitoringJob = viewModelScope.launch {
            while (true) {
                try {
                    // Generate simulated health data with AI predictions
                    simulateHealthData()
                    
                    // Update cycle counter
                    monitoringCycles++
                    
                    // Check for abnormal values and trigger SOS if needed
                    _healthData.value?.let { healthData ->
                        if (healthData.isAbnormal || 
                            healthData.fallDetected || 
                            healthData.arrhythmiaDetected) {
                            handleAbnormalHealthData(healthData)
                        }
                    }
                    
                    delay(5000) // Poll every 5 seconds
                } catch (e: Exception) {
                    _error.value = "Error reading health data: ${e.message}"
                    delay(10000) // Wait longer on error
                }
            }
        }
    }
    
    private fun stopPollingHealthData() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    private fun simulateHealthData() {
        // For demo purposes, generate realistic values with small random variations
        val currentData = _healthData.value
        
        // Simulate regular vital signs
        val heartRate = currentData?.heartRate?.let {
            val variation = -5 + random.nextInt(11) // -5 to 5
            min(180, max(45, it + variation))
        } ?: (60 + random.nextInt(41)) // 60-100
        
        val stepCount = currentData?.stepCount?.let {
            it + random.nextInt(11) // 0-10 steps
        } ?: (100 + random.nextInt(401)) // 100-500
        
        val bloodOxygen = currentData?.bloodOxygen ?: (94 + random.nextInt(6)) // 94-99
        
        val bloodPressureSystolic = currentData?.bloodPressureSystolic ?: (110 + random.nextInt(31)) // 110-140
        val bloodPressureDiastolic = currentData?.bloodPressureDiastolic ?: (70 + random.nextInt(21)) // 70-90
        
        val bodyTemperature = currentData?.bodyTemperature ?: (36.5f + (random.nextFloat() - 0.5f)) // 36.0-37.0
        
        // New vital signs for AI-driven monitoring
        val respirationRate = currentData?.respirationRate ?: (12 + random.nextInt(9)) // 12-20
        val heartRateVariability = currentData?.heartRateVariability ?: (30f + random.nextFloat() * 50f) // 30-80ms
        
        // Generate stress level that has some correlation with heart rate
        val baseStressLevel = if (heartRate > 90) 60 else 30
        val stressLevel = currentData?.stressLevel ?: min(100, baseStressLevel + random.nextInt(31)) // 0-100
        
        // Generate sleep quality that relates to stress level
        val baseSleepQuality = if (stressLevel > 70) 50 else 80
        val sleepQuality = currentData?.sleepQuality ?: max(0, baseSleepQuality - random.nextInt(31)) // 0-100
        
        // Determine if we should generate an abnormal scenario (gradually increase probability)
        val shouldGenerateAbnormality = when {
            abnormalityCounter > 0 -> {
                // Continue abnormality for a few cycles
                abnormalityCounter--
                true
            }
            random.nextInt(60) < monitoringCycles / 20 -> {
                // Start a new abnormality sequence
                abnormalityCounter = 3 + random.nextInt(3) // 3-5 cycles
                true
            }
            else -> false
        }
        
        // Determine fall detection (rare event)
        val fallDetected = when {
            fallDetectionCounter > 0 -> {
                // Continue fall alert for a few cycles
                fallDetectionCounter--
                true
            }
            random.nextInt(100) == 0 -> {
                // Very rare fall detection
                fallDetectionCounter = 5 // Alert for 5 cycles
                true
            }
            else -> false
        }
        
        // Generate accelerometer data for fall detection
        val accelerometerData = mlModelService.generateSimulatedAccelerometerData(
            duration = 5,
            hasFall = fallDetected
        )
        
        // Generate gyroscope data (empty for now)
        val gyroscopeData = listOf<Float>()
        
        // Determine arrhythmia detection (uncommon event)
        val arrhythmiaDetected = when {
            arrhythmiaCounter > 0 -> {
                // Continue arrhythmia for a few cycles
                arrhythmiaCounter--
                true
            }
            random.nextInt(40) == 0 -> {
                // Uncommon arrhythmia detection
                arrhythmiaCounter = 4 // Alert for 4 cycles
                true
            }
            else -> false
        }
        
        // Generate ECG data
        val ecgReadings = mlModelService.generateSimulatedECG(
            duration = 5,
            isAbnormal = arrhythmiaDetected
        )
        
        // Create abnormal values for simulation
        val abnormalHeartRate = if (shouldGenerateAbnormality) (160 + random.nextInt(21)) else heartRate
        val abnormalOxygen = if (shouldGenerateAbnormality) (89 + random.nextInt(6)) else bloodOxygen
        
        // Create health data with the generated values
        val healthData = HealthData(
            heartRate = if (shouldGenerateAbnormality) abnormalHeartRate else heartRate,
            stepCount = stepCount,
            bloodOxygen = if (shouldGenerateAbnormality) abnormalOxygen else bloodOxygen,
            bloodPressureSystolic = bloodPressureSystolic,
            bloodPressureDiastolic = bloodPressureDiastolic,
            bodyTemperature = bodyTemperature,
            timestamp = System.currentTimeMillis(),
            isAbnormal = shouldGenerateAbnormality,
            ecgReadings = ecgReadings,
            heartRateVariability = heartRateVariability,
            accelerometerData = accelerometerData,
            gyroscopeData = gyroscopeData,
            respirationRate = respirationRate,
            stressLevel = stressLevel,
            sleepQuality = sleepQuality,
            fallDetected = fallDetected,
            arrhythmiaDetected = arrhythmiaDetected
        )
        
        // Generate AI predictions
        val predictionResults = mlModelService.predictHealthConditions(healthData)
        
        // Update health data with predictions
        _healthData.value = healthData.copy(predictionResults = predictionResults)
    }
    
    /**
     * Handle abnormal health data
     */
    private fun handleAbnormalHealthData(healthData: HealthData) {
        viewModelScope.launch {
            // Save the abnormal health data to the repository
            healthRepository.saveHealthData(
                com.example.urban_safety.data.model.HealthData.fromModelHealthData(healthData)
            )
            
            // Check for critical conditions that require immediate SOS
            if (healthData.fallDetected || 
                (healthData.heartRate > 180 || healthData.heartRate < 40) || 
                (healthData.bloodOxygen != null && healthData.bloodOxygen < 90)) {
                
                // For demo, we'll just show a message rather than triggering actual SOS
                _error.value = "CRITICAL HEALTH ALERT: ${getEmergencyMessage(healthData)}"
                
                // In a real app, you might trigger SOS here:
                // triggerManualSOS()
            }
        }
    }
    
    /**
     * Generate an emergency message based on the abnormal data
     */
    private fun getEmergencyMessage(healthData: HealthData): String {
        return when {
            healthData.fallDetected -> 
                "Fall detected! Contacting emergency services."
            healthData.arrhythmiaDetected -> 
                "Cardiac arrhythmia detected! Abnormal heart rhythm detected."
            healthData.heartRate > 180 -> 
                "Extreme tachycardia detected! Heart rate: ${healthData.heartRate} BPM."
            healthData.heartRate < 40 -> 
                "Extreme bradycardia detected! Heart rate: ${healthData.heartRate} BPM."
            healthData.bloodOxygen != null && healthData.bloodOxygen < 90 -> 
                "Hypoxemia detected! Blood oxygen: ${healthData.bloodOxygen}%."
            else -> 
                "Abnormal vital signs detected! Immediate medical attention recommended."
        }
    }
} 