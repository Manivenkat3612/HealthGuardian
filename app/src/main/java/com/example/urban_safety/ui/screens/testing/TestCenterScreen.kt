package com.example.urban_safety.ui.screens.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Report
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urban_safety.viewmodels.TestCenterViewModel
import kotlin.math.roundToInt

enum class TestScenario {
    NORMAL_HEALTH,
    HEART_DISEASE_RISK,
    ARRHYTHMIA_DETECTION,
    FALL_DETECTION,
    SLEEP_APNEA,
    HIGH_STRESS,
    RESPIRATORY_ISSUE,
    HYPERTENSION,
    HYPOTHERMIA,
    HYPERTHERMIA,
    CARDIAC_EMERGENCY,
    STROKE_RISK,
    RURAL_EMERGENCY,
    EMERGENCY_NOTIFICATION_TEST,
    CUSTOM
}

enum class TestResultSeverity {
    NORMAL,
    WARNING,
    CRITICAL
}

data class TestResult(
    val title: String,
    val description: String,
    val severity: TestResultSeverity = TestResultSeverity.NORMAL
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestCenterScreen(
    onNavigateBack: () -> Unit,
    viewModel: TestCenterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Testing Center") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Select a test scenario or customize your own",
                    style = MaterialTheme.typography.titleMedium
                )
                
                TestScenarioSelector(
                    selectedScenario = uiState.selectedScenario,
                    onScenarioSelected = viewModel::selectScenario
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.MonitorHeart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Test Vital Signs",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            
                            Text(
                                text = if (uiState.selectedScenario == TestScenario.CUSTOM) "Custom" else uiState.selectedScenario.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Heart Rate
                        VitalSignSlider(
                            icon = Icons.Outlined.Favorite,
                            title = "Heart Rate",
                            value = uiState.heartRate,
                            valueRange = 30f..180f,
                            unitText = "${uiState.heartRate.roundToInt()} BPM",
                            onValueChange = viewModel::updateHeartRate
                        )
                        
                        // Blood Oxygen
                        VitalSignSlider(
                            icon = Icons.Outlined.Biotech,
                            title = "Blood Oxygen",
                            value = uiState.bloodOxygen.toFloat(),
                            valueRange = 70f..100f,
                            unitText = "${uiState.bloodOxygen}%",
                            onValueChange = { viewModel.updateBloodOxygen(it.roundToInt()) }
                        )
                        
                        // Blood Pressure - Systolic
                        VitalSignSlider(
                            icon = Icons.Outlined.QueryStats,
                            title = "Blood Pressure (Systolic)",
                            value = uiState.bloodPressureSystolic.toFloat(),
                            valueRange = 80f..220f,
                            unitText = "${uiState.bloodPressureSystolic} mmHg",
                            onValueChange = { viewModel.updateBloodPressureSystolic(it.roundToInt()) }
                        )
                        
                        // Blood Pressure - Diastolic
                        VitalSignSlider(
                            icon = Icons.Outlined.QueryStats,
                            title = "Blood Pressure (Diastolic)",
                            value = uiState.bloodPressureDiastolic.toFloat(),
                            valueRange = 40f..140f,
                            unitText = "${uiState.bloodPressureDiastolic} mmHg",
                            onValueChange = { viewModel.updateBloodPressureDiastolic(it.roundToInt()) }
                        )
                        
                        // Body Temperature
                        VitalSignSlider(
                            icon = Icons.Outlined.DeviceThermostat,
                            title = "Body Temperature",
                            value = uiState.bodyTemperature,
                            valueRange = 34f..42f,
                            unitText = "${uiState.bodyTemperature} °C",
                            onValueChange = viewModel::updateBodyTemperature
                        )
                        
                        // Respiration Rate
                        VitalSignSlider(
                            icon = Icons.Outlined.Science,
                            title = "Respiration Rate",
                            value = uiState.respirationRate.toFloat(),
                            valueRange = 5f..30f,
                            unitText = "${uiState.respirationRate} breaths/min",
                            onValueChange = { viewModel.updateRespirationRate(it.roundToInt()) }
                        )
                        
                        // Heart Rate Variability
                        VitalSignSlider(
                            icon = Icons.Outlined.MonitorHeart,
                            title = "Heart Rate Variability",
                            value = uiState.heartRateVariability,
                            valueRange = 5f..100f,
                            unitText = "${uiState.heartRateVariability.roundToInt()} ms",
                            onValueChange = viewModel::updateHeartRateVariability
                        )
                        
                        // Stress Level
                        VitalSignSlider(
                            icon = Icons.Outlined.Warning,
                            title = "Stress Level",
                            value = uiState.stressLevel.toFloat(),
                            valueRange = 0f..100f,
                            unitText = "${uiState.stressLevel}%",
                            onValueChange = { viewModel.updateStressLevel(it.roundToInt()) }
                        )
                        
                        // Sleep Quality
                        VitalSignSlider(
                            icon = Icons.Outlined.LocalHospital,
                            title = "Sleep Quality",
                            value = uiState.sleepQuality.toFloat(),
                            valueRange = 0f..100f,
                            unitText = "${uiState.sleepQuality}%",
                            onValueChange = { viewModel.updateSleepQuality(it.roundToInt()) }
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.SettingsSuggest,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Special Test Features",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // ECG Rhythm Abnormality
                        FeatureToggle(
                            title = "ECG Rhythm Abnormality",
                            description = "Simulate irregular heart rhythm detection",
                            checked = uiState.ecgAbnormalityEnabled,
                            onCheckedChange = viewModel::toggleEcgAbnormality
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Fall Detection
                        FeatureToggle(
                            title = "Fall Detection",
                            description = "Simulate accelerometer-detected fall",
                            checked = uiState.fallDetectionEnabled,
                            onCheckedChange = viewModel::toggleFallDetection
                        )

                        // Emergency Notifications
                        FeatureToggle(
                            title = "Emergency Notifications",
                            description = "Enable emergency contact notifications for testing",
                            checked = uiState.emergencyNotificationsEnabled,
                            onCheckedChange = viewModel::toggleEmergencyNotifications
                        )
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.SettingsRemote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monitoring Control",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Control buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { viewModel.applyTestCase() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Text("Run Once")
                            }
                            
                            Button(
                                onClick = { viewModel.toggleMonitoring(!uiState.isMonitoring) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isMonitoring) 
                                        MaterialTheme.colorScheme.errorContainer 
                                    else 
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = if (uiState.isMonitoring) 
                                        MaterialTheme.colorScheme.onErrorContainer 
                                    else 
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = if (uiState.isMonitoring) 
                                        Icons.Filled.Stop 
                                    else 
                                        Icons.Filled.PlayArrow,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (uiState.isMonitoring) "Stop Monitoring" else "Start Monitoring")
                            }
                            
                            Button(
                                onClick = { viewModel.resetToNormal() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("Reset")
                            }
                        }
                    }
                }
            }
            
            item {
                Text(
                    text = "Test Results",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (uiState.testResults.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No test results yet. Run a test to see results here.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.testResults) { result ->
                    TestResultCard(result = result)
                }
            }
        }
    }
}

@Composable
fun VitalSignSlider(
    icon: ImageVector,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    unitText: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Text(
                text = unitText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun FeatureToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun TestScenarioSelector(
    selectedScenario: TestScenario,
    onScenarioSelected: (TestScenario) -> Unit
) {
    val scenarioGroups = listOf(
        "Normal" to listOf(TestScenario.NORMAL_HEALTH),
        "Cardiac Issues" to listOf(
            TestScenario.HEART_DISEASE_RISK,
            TestScenario.ARRHYTHMIA_DETECTION,
            TestScenario.CARDIAC_EMERGENCY
        ),
        "Physical Health" to listOf(
            TestScenario.FALL_DETECTION,
            TestScenario.RESPIRATORY_ISSUE
        ),
        "Sleep & Stress" to listOf(
            TestScenario.SLEEP_APNEA,
            TestScenario.HIGH_STRESS
        ),
        "Temperature" to listOf(
            TestScenario.HYPOTHERMIA,
            TestScenario.HYPERTHERMIA
        ),
        "Blood Pressure" to listOf(
            TestScenario.HYPERTENSION,
            TestScenario.STROKE_RISK
        ),
        "Special Cases" to listOf(
            TestScenario.RURAL_EMERGENCY,
            TestScenario.EMERGENCY_NOTIFICATION_TEST,
            TestScenario.CUSTOM
        )
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        scenarioGroups.forEach { (groupName, scenarios) ->
            Text(
                text = groupName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                scenarios.forEach { scenario ->
                    val isSelected = scenario == selectedScenario
                    val backgroundColor = when {
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val textColor = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = backgroundColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onScenarioSelected(scenario) }
                            )
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getScenarioDisplayName(scenario),
                            color = textColor,
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TestResultCard(result: TestResult) {
    val (backgroundColor, borderColor, icon) = when (result.severity) {
        TestResultSeverity.NORMAL -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.tertiary,
            Icons.Filled.HealthAndSafety
        )
        TestResultSeverity.WARNING -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.secondary,
            Icons.Outlined.Warning
        )
        TestResultSeverity.CRITICAL -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            Icons.Outlined.BatteryAlert
        )
    }
    
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.outlinedCardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = result.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getScenarioDisplayName(scenario: TestScenario): String {
    return when (scenario) {
        TestScenario.NORMAL_HEALTH -> "Normal Health"
        TestScenario.HEART_DISEASE_RISK -> "Heart Disease"
        TestScenario.ARRHYTHMIA_DETECTION -> "Arrhythmia"
        TestScenario.FALL_DETECTION -> "Fall Detection"
        TestScenario.SLEEP_APNEA -> "Sleep Apnea"
        TestScenario.HIGH_STRESS -> "High Stress"
        TestScenario.RESPIRATORY_ISSUE -> "Respiratory"
        TestScenario.HYPERTENSION -> "Hypertension"
        TestScenario.HYPOTHERMIA -> "Low Temp"
        TestScenario.HYPERTHERMIA -> "High Temp"
        TestScenario.CARDIAC_EMERGENCY -> "Cardiac ER"
        TestScenario.STROKE_RISK -> "Stroke Risk"
        TestScenario.RURAL_EMERGENCY -> "Rural ER"
        TestScenario.EMERGENCY_NOTIFICATION_TEST -> "Notify Test"
        TestScenario.CUSTOM -> "Custom"
    }
} 