package com.example.urban_safety.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urban_safety.data.HealthRepository
import com.example.urban_safety.data.SafetyRepository
import com.example.urban_safety.data.model.HealthData
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.repositories.EmergencyContactsRepository
import com.example.urban_safety.ui.screens.testing.TestResult
import com.example.urban_safety.ui.screens.testing.TestResultSeverity
import com.example.urban_safety.ui.screens.testing.TestScenario
import com.example.urban_safety.util.SmsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Random
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class TestCenterUiState(
    val selectedScenario: TestScenario = TestScenario.NORMAL_HEALTH,
    val heartRate: Float = 72f,
    val bloodOxygen: Int = 98,
    val bloodPressureSystolic: Int = 120,
    val bloodPressureDiastolic: Int = 80,
    val bodyTemperature: Float = 36.6f,
    val respirationRate: Int = 16,
    val heartRateVariability: Float = 65f,
    val stressLevel: Int = 25,
    val sleepQuality: Int = 85,
    val ecgAbnormalityEnabled: Boolean = false,
    val fallDetectionEnabled: Boolean = false,
    val emergencyNotificationsEnabled: Boolean = false,
    val isMonitoring: Boolean = false,
    val testResults: List<TestResult> = emptyList()
)

@HiltViewModel
class TestCenterViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val safetyRepository: SafetyRepository,
    private val locationRepository: LocationRepository,
    private val emergencyContactsRepository: EmergencyContactsRepository,
    private val smsService: SmsService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestCenterUiState())
    val uiState: StateFlow<TestCenterUiState> = _uiState.asStateFlow()
    
    private var monitoringJob: Job? = null
    private val random = Random()
    
    // Track current location
    private val _currentLocation = MutableStateFlow<LocationData?>(null)
    
    init {
        // Start location updates
        viewModelScope.launch {
            try {
                locationRepository.getLocationUpdates().collect { location ->
                    if (location != null) {
                        _currentLocation.value = LocationData(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            accuracy = location.accuracy,
                            timestamp = Date(location.time)
                        )
                        println("DEBUG: Location updated in TestCenterViewModel")
                    }
                }
            } catch (e: Exception) {
                println("Failed to start location updates: ${e.message}")
            }
        }
    }
    
    fun selectScenario(scenario: TestScenario) {
        when (scenario) {
            TestScenario.NORMAL_HEALTH -> {
                _uiState.update { 
                    it.copy(
                        selectedScenario = scenario,
                        heartRate = 72f,
                        bloodOxygen = 98,
                        bloodPressureSystolic = 120,
                        bloodPressureDiastolic = 80,
                        bodyTemperature = 36.6f,
                        respirationRate = 16,
                        heartRateVariability = 65f,
                        stressLevel = 25,
                        sleepQuality = 85,
                        ecgAbnormalityEnabled = false,
                        fallDetectionEnabled = false
                    )
                }
            }
            TestScenario.HEART_DISEASE_RISK -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        heartRate = 88f,
                        bloodPressureSystolic = 145,
                        bloodPressureDiastolic = 95,
                        heartRateVariability = 30f,
                        stressLevel = 65,
                        ecgAbnormalityEnabled = true
                    )
                }
            }
            TestScenario.ARRHYTHMIA_DETECTION -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        heartRate = 95f,
                        heartRateVariability = 20f,
                        ecgAbnormalityEnabled = true
                    )
                }
            }
            TestScenario.FALL_DETECTION -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        fallDetectionEnabled = true
                    )
                }
            }
            TestScenario.SLEEP_APNEA -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        bloodOxygen = 88,
                        respirationRate = 22,
                        sleepQuality = 40
                    )
                }
            }
            TestScenario.HIGH_STRESS -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        heartRate = 90f,
                        bloodPressureSystolic = 135,
                        bloodPressureDiastolic = 88,
                        stressLevel = 85,
                        sleepQuality = 55
                    )
                }
            }
            TestScenario.RESPIRATORY_ISSUE -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        respirationRate = 28,
                        bloodOxygen = 90
                    )
                }
            }
            TestScenario.HYPERTENSION -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        bloodPressureSystolic = 165,
                        bloodPressureDiastolic = 105
                    )
                }
            }
            TestScenario.HYPOTHERMIA -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        bodyTemperature = 35.1f,
                        heartRate = 58f
                    )
                }
            }
            TestScenario.HYPERTHERMIA -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        bodyTemperature = 39.2f,
                        heartRate = 105f
                    )
                }
            }
            TestScenario.CARDIAC_EMERGENCY -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        heartRate = 145f,
                        bloodPressureSystolic = 185,
                        bloodPressureDiastolic = 110,
                        ecgAbnormalityEnabled = true
                    )
                }
            }
            TestScenario.STROKE_RISK -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        bloodPressureSystolic = 175,
                        bloodPressureDiastolic = 115,
                        heartRate = 88f
                    )
                }
            }
            TestScenario.RURAL_EMERGENCY -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        heartRate = 125f,
                        bloodPressureSystolic = 155,
                        bloodPressureDiastolic = 95,
                        respirationRate = 24,
                        bodyTemperature = 38.5f
                    )
                }
            }
            TestScenario.EMERGENCY_NOTIFICATION_TEST -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario,
                        heartRate = 155f,
                        bloodPressureSystolic = 180,
                        bloodPressureDiastolic = 115,
                        bloodOxygen = 85,
                        bodyTemperature = 39.5f,
                        ecgAbnormalityEnabled = true,
                        emergencyNotificationsEnabled = true,
                        fallDetectionEnabled = true,
                        testResults = emptyList()
                    )
                }
                
                // Test emergency notifications using both methods
                viewModelScope.launch {
                    // Short delay to ensure UI state is updated
                    delay(500)
                    println("DEBUG: Testing emergency notifications for EMERGENCY_NOTIFICATION_TEST scenario")
                    
                    // Method 1: Apply test case which should trigger notifications
                    println("DEBUG: Auto-applying test case for EMERGENCY_NOTIFICATION_TEST")
                    applyTestCase()
                    
                    // Method 2: Directly test emergency notifications (as a backup)
                    delay(1000) // Wait a bit between attempts to avoid SMS flood
                    println("DEBUG: Directly testing emergency notifications")
                    testEmergencyNotifications()
                }
            }
            TestScenario.CUSTOM -> {
                _uiState.update {
                    it.copy(
                        selectedScenario = scenario
                        // Keep existing values for custom scenario
                    )
                }
            }
        }
    }
    
    // Vital sign update functions
    fun updateHeartRate(value: Float) {
        _uiState.update { it.copy(heartRate = value) }
    }
    
    fun updateBloodOxygen(value: Int) {
        _uiState.update { it.copy(bloodOxygen = value) }
    }
    
    fun updateBloodPressureSystolic(value: Int) {
        _uiState.update { it.copy(bloodPressureSystolic = value) }
    }
    
    fun updateBloodPressureDiastolic(value: Int) {
        _uiState.update { it.copy(bloodPressureDiastolic = value) }
    }
    
    fun updateBodyTemperature(value: Float) {
        _uiState.update { it.copy(bodyTemperature = value) }
    }
    
    fun updateRespirationRate(value: Int) {
        _uiState.update { it.copy(respirationRate = value) }
    }
    
    fun updateHeartRateVariability(value: Float) {
        _uiState.update { it.copy(heartRateVariability = value) }
    }
    
    fun updateStressLevel(value: Int) {
        _uiState.update { it.copy(stressLevel = value) }
    }
    
    fun updateSleepQuality(value: Int) {
        _uiState.update { it.copy(sleepQuality = value) }
    }
    
    // Special feature toggles
    fun toggleEcgAbnormality(enabled: Boolean) {
        _uiState.update { it.copy(ecgAbnormalityEnabled = enabled) }
    }
    
    fun toggleFallDetection(enabled: Boolean) {
        _uiState.update { it.copy(fallDetectionEnabled = enabled) }
    }
    
    fun toggleEmergencyNotifications(enabled: Boolean) {
        _uiState.update { it.copy(emergencyNotificationsEnabled = enabled) }
    }
    
    // Monitoring controls
    fun toggleMonitoring(enabled: Boolean) {
        if (enabled && monitoringJob == null) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }
    
    private fun startMonitoring() {
        monitoringJob = viewModelScope.launch {
            _uiState.update { it.copy(isMonitoring = true) }
            
            while (true) {
                applyTestCase()
                delay(5000) // Apply test case every 5 seconds
            }
        }
    }
    
    private fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _uiState.update { it.copy(isMonitoring = false) }
    }
    
    fun resetToNormal() {
        selectScenario(TestScenario.NORMAL_HEALTH)
        _uiState.update { it.copy(testResults = emptyList()) }
    }
    
    fun applyTestCase() {
        val currentState = _uiState.value
        val results = mutableListOf<TestResult>()
        
        // Analyze vital signs
        analyzeHeartRate(currentState.heartRate)?.let { results.add(it) }
        analyzeBloodOxygen(currentState.bloodOxygen)?.let { results.add(it) }
        analyzeBloodPressure(currentState.bloodPressureSystolic, currentState.bloodPressureDiastolic)?.let { results.add(it) }
        analyzeBodyTemperature(currentState.bodyTemperature)?.let { results.add(it) }
        analyzeRespirationRate(currentState.respirationRate)?.let { results.add(it) }
        
        // Analyze heart rate variability and stress
        analyzeHeartRateVariability(currentState.heartRateVariability)?.let { results.add(it) }
        analyzeStressLevel(currentState.stressLevel)?.let { results.add(it) }
        analyzeSleepQuality(currentState.sleepQuality)?.let { results.add(it) }
        
        // Check special tests
        if (currentState.ecgAbnormalityEnabled) {
            detectEcgAbnormality()?.let { results.add(it) }
        }
        
        if (currentState.fallDetectionEnabled) {
            detectFall()?.let { results.add(it) }
        }
        
        // Force emergency test if enabled
        if (currentState.selectedScenario == TestScenario.EMERGENCY_NOTIFICATION_TEST) {
            println("DEBUG: EMERGENCY_NOTIFICATION_TEST scenario detected - ensuring critical conditions")
            
            // Clear existing results and add forced critical results
            results.clear()
            
            // Add multiple critical conditions to ensure notification triggers
            results.add(TestResult(
                "Emergency Notification Test",
                "This is a test for emergency contact notifications",
                TestResultSeverity.CRITICAL
            ))
            
            results.add(TestResult(
                "Fall Detected",
                "Sudden motion consistent with falling detected",
                TestResultSeverity.CRITICAL
            ))
            
            results.add(TestResult(
                "Cardiac Emergency",
                "Critical heart rate abnormality detected",
                TestResultSeverity.CRITICAL
            ))
            
            results.add(TestResult(
                "Hypertensive Crisis",
                "Blood pressure at ${currentState.bloodPressureSystolic}/${currentState.bloodPressureDiastolic} requires immediate attention",
                TestResultSeverity.CRITICAL
            ))
            
            println("DEBUG: Added ${results.size} forced critical conditions for emergency test")
        }
        
        // Calculate combined risk assessments
        checkHeartDiseaseRisk(currentState)?.let { results.add(it) }
        checkStrokeRisk(currentState)?.let { results.add(it) }
        checkRespiratoryIssueRisk(currentState)?.let { results.add(it) }
        
        // First update the UI with results so that the state contains the new test results
        _uiState.update { it.copy(testResults = results) }
        
        println("DEBUG: Generated ${results.size} test results")
        println("DEBUG: Critical results: ${results.filter { it.severity == TestResultSeverity.CRITICAL }.map { it.title }}")
        
        // Always generate a health report for disease prediction and monitoring
        generateHealthReport(currentState.copy(testResults = results), results)
        
        // Submit monitoring data to repositories with the updated state that includes test results
        submitHealthData(currentState.copy(testResults = results))
    }
    
    private fun analyzeHeartRate(heartRate: Float): TestResult? {
        return when {
            heartRate < 50 -> TestResult(
                "Bradycardia Detected",
                "Heart rate is abnormally low at ${heartRate.roundToInt()} BPM",
                TestResultSeverity.WARNING
            )
            heartRate > 100 -> TestResult(
                "Tachycardia Detected",
                "Heart rate is elevated at ${heartRate.roundToInt()} BPM",
                if (heartRate > 130) TestResultSeverity.CRITICAL else TestResultSeverity.WARNING
            )
            else -> null // Normal heart rate
        }
    }
    
    private fun analyzeBloodOxygen(bloodOxygen: Int): TestResult? {
        return when {
            bloodOxygen < 90 -> TestResult(
                "Low Blood Oxygen",
                "Blood oxygen saturation at $bloodOxygen% indicates possible hypoxemia",
                if (bloodOxygen < 85) TestResultSeverity.CRITICAL else TestResultSeverity.WARNING
            )
            bloodOxygen in 90..94 -> TestResult(
                "Borderline Blood Oxygen",
                "Blood oxygen saturation at $bloodOxygen% is slightly below normal range",
                TestResultSeverity.WARNING
            )
            else -> null // Normal oxygen levels
        }
    }
    
    private fun analyzeBloodPressure(systolic: Int, diastolic: Int): TestResult? {
        return when {
            systolic >= 180 || diastolic >= 120 -> TestResult(
                "Hypertensive Crisis",
                "Blood pressure at $systolic/$diastolic mmHg requires immediate medical attention",
                TestResultSeverity.CRITICAL
            )
            systolic >= 140 || diastolic >= 90 -> TestResult(
                "Hypertension Detected",
                "Blood pressure at $systolic/$diastolic mmHg is above normal range",
                TestResultSeverity.WARNING
            )
            systolic < 90 || diastolic < 60 -> TestResult(
                "Hypotension Detected",
                "Blood pressure at $systolic/$diastolic mmHg is below normal range",
                TestResultSeverity.WARNING
            )
            else -> null // Normal blood pressure
        }
    }
    
    private fun analyzeBodyTemperature(temperature: Float): TestResult? {
        return when {
            temperature < 35.0 -> TestResult(
                "Severe Hypothermia",
                "Body temperature at $temperature°C is dangerously low",
                TestResultSeverity.CRITICAL
            )
            temperature < 36.0 -> TestResult(
                "Mild Hypothermia",
                "Body temperature at $temperature°C is below normal range",
                TestResultSeverity.WARNING
            )
            temperature > 39.0 -> TestResult(
                "High Fever",
                "Body temperature at $temperature°C indicates significant fever",
                TestResultSeverity.CRITICAL
            )
            temperature > 37.5 -> TestResult(
                "Elevated Temperature",
                "Body temperature at $temperature°C is above normal range",
                TestResultSeverity.WARNING
            )
            else -> null // Normal temperature
        }
    }
    
    private fun analyzeRespirationRate(rate: Int): TestResult? {
        return when {
            rate > 24 -> TestResult(
                "Tachypnea Detected",
                "Respiration rate of $rate breaths/min is abnormally high",
                TestResultSeverity.WARNING
            )
            rate < 12 -> TestResult(
                "Bradypnea Detected",
                "Respiration rate of $rate breaths/min is abnormally low",
                TestResultSeverity.WARNING
            )
            else -> null // Normal respiration
        }
    }
    
    private fun analyzeHeartRateVariability(hrv: Float): TestResult? {
        return when {
            hrv < 30 -> TestResult(
                "Low Heart Rate Variability",
                "HRV of ${hrv.roundToInt()} ms indicates potential autonomic nervous system imbalance",
                TestResultSeverity.WARNING
            )
            else -> null
        }
    }
    
    private fun analyzeStressLevel(stress: Int): TestResult? {
        return when {
            stress > 75 -> TestResult(
                "High Stress Detected",
                "Stress level at $stress% indicates excessive stress exposure",
                TestResultSeverity.WARNING
            )
            else -> null
        }
    }
    
    private fun analyzeSleepQuality(quality: Int): TestResult? {
        return when {
            quality < 50 -> TestResult(
                "Poor Sleep Quality",
                "Sleep quality score of $quality% indicates insufficient restorative sleep",
                TestResultSeverity.WARNING
            )
            else -> null
        }
    }
    
    private fun detectEcgAbnormality(): TestResult? {
        val currentState = _uiState.value
        
        // Simulate ECG analysis with some randomness
        val severity = when {
            currentState.selectedScenario == TestScenario.CARDIAC_EMERGENCY -> TestResultSeverity.CRITICAL
            currentState.selectedScenario == TestScenario.ARRHYTHMIA_DETECTION -> TestResultSeverity.WARNING
            else -> if (random.nextBoolean()) TestResultSeverity.WARNING else TestResultSeverity.CRITICAL
        }
        
        val abnormalityType = when {
            currentState.selectedScenario == TestScenario.ARRHYTHMIA_DETECTION -> "Atrial Fibrillation"
            currentState.heartRate > 120 -> "Ventricular Tachycardia"
            currentState.heartRate < 50 -> "AV Block"
            else -> listOf("Premature Ventricular Contractions", "Atrial Fibrillation", "Bundle Branch Block").random()
        }
        
        return TestResult(
            "ECG Abnormality Detected",
            "Possible $abnormalityType detected in ECG rhythm analysis",
            severity
        )
    }
    
    private fun detectFall(): TestResult {
        // Simulate sudden acceleration/orientation changes in accelerometer data
        val fallSeverity = when {
            random.nextBoolean() -> TestResultSeverity.CRITICAL
            else -> TestResultSeverity.WARNING
        }
        
        val location = listOf("GPS Location: Home", "GPS Location: Outdoors", "GPS Location: Unknown").random()
        
        return TestResult(
            "Fall Detected",
            "Sudden motion detected consistent with a fall. $location",
            fallSeverity
        )
    }
    
    private fun checkHeartDiseaseRisk(state: TestCenterUiState): TestResult? {
        // Risk factors: high BP, high HR, low HRV, ECG abnormalities
        var riskScore = 0
        
        if (state.bloodPressureSystolic > 140 || state.bloodPressureDiastolic > 90) riskScore += 2
        if (state.heartRate > 85) riskScore += 1
        if (state.heartRateVariability < 35) riskScore += 2
        if (state.ecgAbnormalityEnabled) riskScore += 3
        if (state.stressLevel > 70) riskScore += 1
        
        return when {
            riskScore >= 5 -> TestResult(
                "High Cardiovascular Risk",
                "Multiple risk factors indicate elevated risk of cardiovascular disease",
                TestResultSeverity.CRITICAL
            )
            riskScore >= 3 -> TestResult(
                "Moderate Cardiovascular Risk",
                "Several factors suggest moderate risk of cardiovascular disease",
                TestResultSeverity.WARNING
            )
            else -> null
        }
    }
    
    private fun checkStrokeRisk(state: TestCenterUiState): TestResult? {
        // Stroke risk factors: very high BP, irregular heart rhythm
        if (state.bloodPressureSystolic >= 170 || state.bloodPressureDiastolic >= 110 || 
            (state.ecgAbnormalityEnabled && state.selectedScenario == TestScenario.ARRHYTHMIA_DETECTION)) {
            
            return TestResult(
                "Stroke Risk Alert",
                "Blood pressure and/or heart rhythm abnormalities indicate elevated stroke risk",
                TestResultSeverity.CRITICAL
            )
        }
        return null
    }
    
    private fun checkRespiratoryIssueRisk(state: TestCenterUiState): TestResult? {
        // Respiratory risk: low O2, high respiration, abnormal temperature
        if (state.bloodOxygen < 92 && state.respirationRate > 20) {
            val severity = if (state.bloodOxygen < 88) TestResultSeverity.CRITICAL else TestResultSeverity.WARNING
            
            return TestResult(
                "Respiratory Distress Detected",
                "Low oxygen saturation with elevated respiration rate indicates potential respiratory issue",
                severity
            )
        }
        return null
    }
    
    private fun submitHealthData(state: TestCenterUiState) {
        // In a real implementation, this would submit data to the repositories
        viewModelScope.launch {
            // Always submit vital signs to the health repository for tracking/reports
            healthRepository.submitVitalSigns(
                heartRate = state.heartRate.roundToInt(),
                bloodOxygen = state.bloodOxygen,
                systolicBP = state.bloodPressureSystolic,
                diastolicBP = state.bloodPressureDiastolic,
                temperature = state.bodyTemperature,
                respirationRate = state.respirationRate
            )
            
            // If any critical conditions detected, handle them based on severity
            val criticalResults = state.testResults.filter { it.severity == TestResultSeverity.CRITICAL }
            
            println("DEBUG: Processing ${criticalResults.size} critical results in submitHealthData")
            criticalResults.forEach { 
                println("DEBUG: Critical result - ${it.title}: ${it.description}") 
            }
            
            // Special handling for EMERGENCY_NOTIFICATION_TEST scenario - always process as critical
            if (state.selectedScenario == TestScenario.EMERGENCY_NOTIFICATION_TEST) {
                println("DEBUG: Emergency notification test scenario detected in submitHealthData")
                if (criticalResults.isEmpty()) {
                    println("DEBUG: No critical results found for test scenario - this shouldn't happen")
                }
            }
            
            if (criticalResults.isNotEmpty() || state.selectedScenario == TestScenario.EMERGENCY_NOTIFICATION_TEST) {
                // Get the critical condition descriptions
                val criticalConditions = criticalResults.joinToString(", ") { it.title }
                
                // More detailed vital signs
                val detailedVitals = buildDetailedVitalSigns(state)
                
                println("DEBUG: Triggering health alert for: $criticalConditions")
                
                // Always generate health alert/report in the system
                safetyRepository.triggerHealthAlert(
                    message = "HEALTH ALERT: $criticalConditions detected. Assessment required.",
                    location = "Test Location",
                    vitalSigns = detailedVitals
                )
                
                // Determine if we should notify emergency contacts - only for true emergencies
                val shouldNotify = shouldNotifyContacts(criticalResults)
                println("DEBUG: Should notify emergency contacts: $shouldNotify")
                
                if (shouldNotify) {
                    val emergencyMessage = "EMERGENCY ALERT: $criticalConditions detected. Immediate assistance may be required."
                    println("DEBUG: Will notify emergency contacts with message: $emergencyMessage")
                    notifyEmergencyContacts(emergencyMessage, criticalResults)
                }
                
                // Full SOS - only for the most serious emergencies that require immediate response
                val shouldTriggerSOS = shouldTriggerEmergencySOS(criticalResults)
                println("DEBUG: Should trigger SOS: $shouldTriggerSOS")
                
                if (shouldTriggerSOS) {
                    println("DEBUG: Will trigger emergency SOS")
                    triggerEmergencySOS(state, criticalResults)
                }
            }
        }
    }
    
    /**
     * Build a detailed vital signs string for emergency notifications
     */
    private fun buildDetailedVitalSigns(state: TestCenterUiState): String {
        return """
            Heart Rate: ${state.heartRate.roundToInt()} BPM
            Blood Pressure: ${state.bloodPressureSystolic}/${state.bloodPressureDiastolic} mmHg
            Blood Oxygen: ${state.bloodOxygen}%
            Body Temperature: ${state.bodyTemperature}°C
            Respiration Rate: ${state.respirationRate} breaths/min
            ${if (state.ecgAbnormalityEnabled) "ECG Abnormality Detected" else ""}
            ${if (state.fallDetectionEnabled) "Fall Detected" else ""}
        """.trimIndent()
    }
    
    /**
     * Determine if an emergency SOS should be triggered based on the critical results
     */
    private fun shouldTriggerEmergencySOS(criticalResults: List<TestResult>): Boolean {
        val currentState = _uiState.value
        
        // For testing purposes
        if (currentState.emergencyNotificationsEnabled && 
            currentState.selectedScenario == TestScenario.EMERGENCY_NOTIFICATION_TEST) {
            return true
        }
        
        // Only trigger SOS for immediately life-threatening conditions
        return criticalResults.any { result ->
            // Most severe cardiac events
            result.title.contains("Cardiac Emergency") ||
            
            // Severe oxygen deprivation
            (result.title.contains("Blood Oxygen") && currentState.bloodOxygen < 82) ||
            
            // Detected falls
            result.title.contains("Fall Detected") ||
            
            // Extreme temperature abnormalities
            (result.title.contains("Severe Hypothermia") && currentState.bodyTemperature < 34.5) ||
            (result.title.contains("High Fever") && currentState.bodyTemperature > 40.0) ||
            
            // Stroke indicators with critical severity
            (result.title.contains("Stroke") && result.severity == TestResultSeverity.CRITICAL)
        }
    }
    
    /**
     * Trigger an emergency SOS for severe health conditions
     */
    private fun triggerEmergencySOS(state: TestCenterUiState, criticalResults: List<TestResult>) {
        viewModelScope.launch {
            try {
                val emergencyMessage = "URGENT MEDICAL EMERGENCY: " + 
                    criticalResults.joinToString(", ") { it.title } +
                    ". Vital signs indicate critical condition."
                
                // First try to use cached location, then fall back to repository if needed
                var locationData = _currentLocation.value
                if (locationData == null) {
                    try {
                        val locationResult = locationRepository.getLastLocation()
                        locationData = locationResult.getOrNull()
                        println("DEBUG: Location data retrieved from repository for SOS: ${locationData != null}")
                    } catch (e: Exception) {
                        println("DEBUG: Failed to get location for SOS: ${e.message}")
                        // Proceed without location data
                    }
                } else {
                    println("DEBUG: Using cached location data for SOS")
                }
                
                // Create health data object for the emergency
                val healthData = buildHealthData(state)
                
                // First, use the safetyRepository to record the incident in the system
                println("DEBUG: Triggering SOS via SafetyRepository")
                val sosResult = safetyRepository.triggerSOS(
                    message = emergencyMessage,
                    latitude = locationData?.latitude ?: 0.0,
                    longitude = locationData?.longitude ?: 0.0,
                    healthData = healthData
                )
                
                // Process result using isSuccess
                if (sosResult.isSuccess) {
                    println("DEBUG: SOS triggered successfully")
                } else {
                    println("DEBUG: Failed to trigger SOS: ${sosResult.exceptionOrNull()?.message}")
                }
                
                // Then, directly notify emergency contacts with SMS - this is the key part that should work
                println("DEBUG: Notifying emergency contacts directly")
                notifyEmergencyContacts(emergencyMessage, criticalResults)
                
            } catch (e: Exception) {
                println("Failed to trigger emergency SOS: ${e.message}")
            }
        }
    }
    
    /**
     * Build health data object from current state for emergency reporting
     */
    private fun buildHealthData(state: TestCenterUiState): HealthData {
        return HealthData(
            heartRate = state.heartRate.roundToInt(),
            bloodOxygen = state.bloodOxygen,
            bloodPressureSystolic = state.bloodPressureSystolic,
            bloodPressureDiastolic = state.bloodPressureDiastolic,
            bodyTemperature = state.bodyTemperature,
            respirationRate = state.respirationRate,
            timestamp = Date(),
            isAbnormal = true,
            heartRateVariability = state.heartRateVariability,
            stressLevel = state.stressLevel,
            sleepQuality = state.sleepQuality,
            fallDetected = state.fallDetectionEnabled,
            arrhythmiaDetected = state.ecgAbnormalityEnabled
        )
    }
    
    /**
     * Notifies all emergency contacts about the emergency situation by directly using SmsService
     * (based on the working SOSViewModel implementation)
     */
    private suspend fun notifyEmergencyContacts(emergencyMessage: String, criticalResults: List<TestResult>) {
        try {
            println("DEBUG: Attempting to notify emergency contacts using direct SmsService approach")
            println("DEBUG: Message: $emergencyMessage")
            println("DEBUG: Critical conditions: ${criticalResults.map { it.title }}")
            
            // First try to use cached location, then fall back to repository if needed
            var locationData = _currentLocation.value
            if (locationData == null) {
                try {
                    val locationResult = locationRepository.getLastLocation()
                    locationData = locationResult.getOrNull()
                    println("DEBUG: Location data retrieved: ${locationData != null}")
                } catch (e: Exception) {
                    println("DEBUG: Failed to get location: ${e.message}")
                    // Proceed without location data
                }
            } else {
                println("DEBUG: Using cached location data")
            }
            
            // Check if SMS permission is granted
            if (!smsService.hasSmsPermission()) {
                println("DEBUG: SMS permission not granted. Emergency contacts will not be notified.")
                return
            }
            
            // Get emergency contacts directly
            val contacts = emergencyContactsRepository.getEmergencyContacts().first()
            println("DEBUG: Found ${contacts.size} emergency contacts")
            
            if (contacts.isEmpty()) {
                println("DEBUG: No emergency contacts found to notify")
                return
            }
            
            // Create a simple emergency message with vital signs
            val currentState = _uiState.value
            val vitalSigns = """
                Heart Rate: ${currentState.heartRate.roundToInt()} BPM
                Blood Pressure: ${currentState.bloodPressureSystolic}/${currentState.bloodPressureDiastolic} mmHg
                Blood Oxygen: ${currentState.bloodOxygen}%
                Body Temperature: ${currentState.bodyTemperature}°C
            """.trimIndent()
            
            // Add location information if available
            val locationText = if (locationData != null) {
                "\n\nMy location: https://maps.google.com/?q=${locationData.latitude},${locationData.longitude}"
            } else ""
            
            // Combine all parts into a complete message
            val fullMessage = "$emergencyMessage\n\n$vitalSigns$locationText"
            println("DEBUG: Full message to send: $fullMessage")
            
            // Send SMS to all contacts DIRECTLY using SmsService
            var successCount = 0
            contacts.forEach { contact ->
                try {
                    // Send emergency SMS directly
                    val result = smsService.sendEmergencySMS(contact.phoneNumber, fullMessage)
                    
                    // Use isSuccess to handle the result
                    if (result.isSuccess) {
                        successCount++
                        println("DEBUG: Successfully sent SMS to ${contact.name}")
                    } else {
                        println("DEBUG: Failed to send SMS to ${contact.name}: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    println("DEBUG: Exception sending SMS to ${contact.name}: ${e.message}")
                }
            }
            
            println("DEBUG: Emergency notifications sent to $successCount out of ${contacts.size} contacts")
        } catch (e: Exception) {
            println("DEBUG: Failed to notify emergency contacts: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Determine if emergency contacts should be notified about health alerts
     */
    private fun shouldNotifyContacts(criticalResults: List<TestResult>): Boolean {
        val currentState = _uiState.value
        
        // ALWAYS notify if emergency notification test is enabled - this ensures test works
        if (currentState.selectedScenario == TestScenario.EMERGENCY_NOTIFICATION_TEST) {
            println("DEBUG: Emergency notification test scenario active - ALWAYS notify contacts")
            return true
        }
        
        // For regular testing - also notify if notifications are enabled
        if (currentState.emergencyNotificationsEnabled) {
            println("DEBUG: Emergency notifications enabled - will notify contacts")
            return true
        }
        
        var shouldNotify = false
        
        // Only notify for true life-threatening emergencies that require immediate attention
        criticalResults.forEach { result ->
            // Acute cardiac events
            if (result.title.contains("Cardiac Emergency") ||
                (result.title.contains("ECG Abnormality") && result.severity == TestResultSeverity.CRITICAL)) {
                println("DEBUG: Cardiac emergency detected - will notify contacts")
                shouldNotify = true
            }
            
            // Severe vital sign abnormalities that indicate immediate danger
            if (result.title.contains("Bradycardia") && currentState.heartRate < 40) {
                println("DEBUG: Severe bradycardia detected - will notify contacts")
                shouldNotify = true
            }
            if (result.title.contains("Tachycardia") && currentState.heartRate > 140) {
                println("DEBUG: Severe tachycardia detected - will notify contacts")
                shouldNotify = true
            }
            if (result.title.contains("Blood Oxygen") && currentState.bloodOxygen < 85) {
                println("DEBUG: Low blood oxygen detected - will notify contacts")
                shouldNotify = true
            }
            if (result.title.contains("Hypertensive Crisis")) {
                println("DEBUG: Hypertensive crisis detected - will notify contacts")
                shouldNotify = true
            }
            
            // Immediate threats - severe temperature issues
            if (result.title.contains("Hypothermia") && currentState.bodyTemperature < 35.0) {
                println("DEBUG: Severe hypothermia detected - will notify contacts")
                shouldNotify = true
            }
            if (result.title.contains("High Fever") && currentState.bodyTemperature > 39.5) {
                println("DEBUG: High fever detected - will notify contacts")
                shouldNotify = true
            }
            
            // Fall detection - always an emergency
            if (result.title.contains("Fall Detected")) {
                println("DEBUG: Fall detected - will notify contacts")
                shouldNotify = true
            }
            
            // Stroke indicators
            if (result.title.contains("Stroke") && result.severity == TestResultSeverity.CRITICAL) {
                println("DEBUG: Stroke risk detected - will notify contacts")
                shouldNotify = true
            }
        }
        
        println("DEBUG: Should notify contacts: $shouldNotify")
        return shouldNotify
    }
    
    /**
     * Generates a comprehensive health report based on the vital signs and test results
     */
    private fun generateHealthReport(state: TestCenterUiState, results: List<TestResult>) {
        viewModelScope.launch {
            try {
                // Categorize results by severity
                val criticalResults = results.filter { it.severity == TestResultSeverity.CRITICAL }
                val warningResults = results.filter { it.severity == TestResultSeverity.WARNING }
                val normalResults = results.filter { it.severity == TestResultSeverity.NORMAL }
                
                // Generate report title based on overall condition
                val reportTitle = when {
                    criticalResults.isNotEmpty() -> "Critical Health Assessment"
                    warningResults.isNotEmpty() -> "Health Warning Assessment"
                    else -> "Normal Health Assessment"
                }
                
                // Build a structured report with predictions and recommendations
                val reportContent = buildString {
                    append("# $reportTitle\n\n")
                    append("## Vital Signs Summary\n")
                    append("- Heart Rate: ${state.heartRate.roundToInt()} BPM\n")
                    append("- Blood Pressure: ${state.bloodPressureSystolic}/${state.bloodPressureDiastolic} mmHg\n")
                    append("- Blood Oxygen: ${state.bloodOxygen}%\n")
                    append("- Body Temperature: ${state.bodyTemperature}°C\n")
                    append("- Respiration Rate: ${state.respirationRate} breaths/min\n")
                    append("- Heart Rate Variability: ${state.heartRateVariability.roundToInt()} ms\n")
                    append("- Stress Level: ${state.stressLevel}%\n")
                    append("- Sleep Quality: ${state.sleepQuality}%\n\n")
                    
                    if (criticalResults.isNotEmpty()) {
                        append("## Critical Findings\n")
                        criticalResults.forEach {
                            append("- ${it.title}: ${it.description}\n")
                        }
                        append("\n")
                    }
                    
                    if (warningResults.isNotEmpty()) {
                        append("## Warning Signs\n")
                        warningResults.forEach {
                            append("- ${it.title}: ${it.description}\n")
                        }
                        append("\n")
                    }
                    
                    append("## Health Predictions\n")
                    
                    // Add disease predictions based on results
                    val predictions = generateDiseasePredictions(state, results)
                    predictions.forEach { (disease, risk) ->
                        append("- $disease Risk: $risk%\n")
                    }
                    
                    append("\n## Recommendations\n")
                    if (criticalResults.isNotEmpty()) {
                        append("- Seek immediate medical attention for critical issues\n")
                    }
                    
                    if (warningResults.isNotEmpty()) {
                        append("- Consult with a healthcare provider about warning signs\n")
                    }
                    
                    // Add lifestyle recommendations
                    if (state.stressLevel > 60) {
                        append("- Consider stress reduction techniques\n")
                    }
                    
                    if (state.sleepQuality < 60) {
                        append("- Improve sleep hygiene for better rest\n")
                    }
                    
                    if (state.bloodPressureSystolic > 130 || state.bloodPressureDiastolic > 85) {
                        append("- Monitor blood pressure regularly\n")
                    }
                    
                    // Timestamp the report
                    append("\nReport generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}\n")
                }
                
                // Save the report to the health repository
                healthRepository.saveHealthReport(
                    title = reportTitle,
                    content = reportContent,
                    timestamp = System.currentTimeMillis(),
                    severity = if (criticalResults.isNotEmpty()) "critical" 
                        else if (warningResults.isNotEmpty()) "warning" else "normal",
                    associatedConditions = results.map { it.title }
                )
                
                println("Health report generated successfully")
            } catch (e: Exception) {
                println("Failed to generate health report: ${e.message}")
            }
        }
    }
    
    /**
     * Generates disease risk predictions based on vital signs and test results
     */
    private fun generateDiseasePredictions(state: TestCenterUiState, results: List<TestResult>): Map<String, Int> {
        val predictions = mutableMapOf<String, Int>()
        
        // Calculate cardiovascular disease risk
        var cvdRisk = 0
        if (state.bloodPressureSystolic > 140) cvdRisk += 15
        if (state.bloodPressureSystolic > 160) cvdRisk += 15
        if (state.bloodPressureDiastolic > 90) cvdRisk += 10
        if (state.heartRate > 90) cvdRisk += 5
        if (state.heartRateVariability < 30) cvdRisk += 10
        if (state.ecgAbnormalityEnabled) cvdRisk += 25
        cvdRisk = cvdRisk.coerceAtMost(95)
        predictions["Cardiovascular Disease"] = cvdRisk
        
        // Calculate stroke risk
        var strokeRisk = 0
        if (state.bloodPressureSystolic > 160) strokeRisk += 20
        if (state.bloodPressureDiastolic > 100) strokeRisk += 15
        if (state.heartRate > 100) strokeRisk += 5
        if (state.ecgAbnormalityEnabled) strokeRisk += 15
        strokeRisk = strokeRisk.coerceAtMost(90)
        predictions["Stroke"] = strokeRisk
        
        // Calculate respiratory disease risk
        var respRisk = 0
        if (state.respirationRate > 20) respRisk += 10
        if (state.respirationRate > 25) respRisk += 15
        if (state.bloodOxygen < 95) respRisk += 10
        if (state.bloodOxygen < 90) respRisk += 20
        respRisk = respRisk.coerceAtMost(85)
        predictions["Respiratory Disease"] = respRisk
        
        // Calculate hypertension risk
        var htRisk = 0
        if (state.bloodPressureSystolic > 130) htRisk += 15
        if (state.bloodPressureSystolic > 140) htRisk += 20
        if (state.bloodPressureDiastolic > 85) htRisk += 15
        if (state.bloodPressureDiastolic > 90) htRisk += 20
        htRisk = htRisk.coerceAtMost(90)
        predictions["Hypertension"] = htRisk
        
        // Calculate stress-related disorder risk
        var stressRisk = 0
        if (state.stressLevel > 60) stressRisk += 15
        if (state.stressLevel > 80) stressRisk += 20
        if (state.heartRate > 85) stressRisk += 5
        if (state.sleepQuality < 60) stressRisk += 10
        stressRisk = stressRisk.coerceAtMost(80)
        predictions["Stress-Related Disorder"] = stressRisk
        
        return predictions
    }
    
    /**
     * Directly test emergency notifications without waiting for monitoring
     * This can be called from UI to force a notification test
     */
    fun testEmergencyNotifications() {
        println("DEBUG: Manual emergency notification test triggered")
        viewModelScope.launch {
            try {
                // Create a test message
                val testMessage = "EMERGENCY TEST: This is a test of the emergency notification system."
                
                // Create test results
                val testResults = listOf(
                    TestResult(
                        "Emergency Notification Test",
                        "This is a manual test of the emergency notification system",
                        TestResultSeverity.CRITICAL
                    ),
                    TestResult(
                        "Fall Detected",
                        "Test fall detection event",
                        TestResultSeverity.CRITICAL
                    )
                )
                
                println("DEBUG: Manually triggering emergency notification")
                
                // Force notification using the direct SMS method
                notifyEmergencyContacts(testMessage, testResults)
                
                // As a fallback, also try sending to a single hardcoded number (careful with this!)
                // Uncomment these lines if you need a direct test to a specific number
                try {
                    val result = smsService.sendEmergencySMS(
                        "YOUR_TEST_PHONE_NUMBER", // Replace with a test phone number
                        "TEST EMERGENCY MESSAGE: This is a fallback test."
                    )
                    if (result.isSuccess) {
                        println("DEBUG: Direct test SMS sent successfully")
                    } else {
                        println("DEBUG: Direct test SMS failed: ${result.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    println("DEBUG: Failed to send direct test SMS: ${e.message}")
                }
                
                println("DEBUG: Manual notification test completed")
            } catch (e: Exception) {
                println("DEBUG: Error in manual notification test: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Manual emergency test accessible from UI button
     * This combines the SOSViewModel approach with our approach
     */
    fun triggerManualEmergencyTest() {
        println("DEBUG: Manual emergency test button pressed")
        viewModelScope.launch {
            try {
                // Get current location
                val locationData = _currentLocation.value
                
                // Create a test message
                val message = "URGENT TEST: Manual emergency test from Health Guardian app."
                
                // Check SMS permission
                if (!smsService.hasSmsPermission()) {
                    println("DEBUG: SMS permission not granted. Cannot send test message.")
                    return@launch
                }
                
                // Get emergency contacts directly
                val contacts = emergencyContactsRepository.getEmergencyContacts().first()
                
                if (contacts.isEmpty()) {
                    println("DEBUG: No emergency contacts found. Add contacts in settings.")
                    return@launch
                }
                
                println("DEBUG: Found ${contacts.size} contacts, sending test messages")
                
                // Send SMS to all contacts directly
                var successCount = 0
                contacts.forEach { contact ->
                    // Create location part of message
                    val locationText = if (locationData != null) {
                        "\n\nMy location: https://maps.google.com/?q=${locationData.latitude},${locationData.longitude}"
                    } else ""
                    
                    // Full message with location
                    val fullMessage = "$message$locationText"
                    
                    // Send direct emergency SMS 
                    try {
                        val result = smsService.sendEmergencySMS(contact.phoneNumber, fullMessage)
                        
                        // Use isSuccess to handle the result
                        if (result.isSuccess) {
                            successCount++
                            println("DEBUG: Successfully sent test SMS to ${contact.name}")
                        } else {
                            println("DEBUG: Failed to send test SMS to ${contact.name}: ${result.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        println("DEBUG: Exception sending test SMS to ${contact.name}: ${e.message}")
                    }
                }
                
                println("DEBUG: Successfully sent $successCount out of ${contacts.size} test messages")
                
            } catch (e: Exception) {
                println("DEBUG: Error in manual emergency test: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        monitoringJob?.cancel()
    }
} 