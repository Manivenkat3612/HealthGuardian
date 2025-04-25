package com.example.urban_safety.ui.screens.health

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urban_safety.viewmodels.HealthMonitoringViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIHealthMonitoringScreen(
    onNavigateBack: () -> Unit,
    viewModel: HealthMonitoringViewModel = hiltViewModel()
) {
    val healthData by viewModel.healthData.collectAsState()
    val isMonitoring by viewModel.isMonitoring.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val thresholds by viewModel.thresholds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Health Monitoring") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Health Guardian",
                                style = MaterialTheme.typography.titleLarge
                            )
                            
                            Switch(
                                checked = isMonitoring,
                                onCheckedChange = { 
                                    if (it) viewModel.startMonitoring() else viewModel.stopMonitoring() 
                                },
                                enabled = !isLoading
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (isMonitoring) "AI Monitoring Active" else "AI Monitoring Inactive",
                            color = if (isMonitoring) Color.Green else Color.Gray
                        )
                        
                        if (isLoading) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            
            if (error != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            // ECG Graph Section
            healthData?.ecgReadings?.let { ecgData ->
                if (ecgData.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .height(200.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "ECG Readings",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                ) {
                                    ECGGraph(data = ecgData, isAbnormal = healthData?.arrhythmiaDetected ?: false)
                                }
                            }
                        }
                    }
                }
            }
            
            // AI Prediction Results
            healthData?.predictionResults?.let { predictions ->
                if (predictions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "AI Health Predictions",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                predictions.forEach { (condition, probability) ->
                                    PredictionItem(
                                        condition = condition,
                                        probability = probability
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Fall Detection Alert
            if (healthData?.fallDetected == true) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Fall Detected",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fall Detected - Emergency contacts notified",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            if (healthData != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Current Health Metrics",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            // Heart Rate
                            HealthMetricItem(
                                label = "Heart Rate",
                                value = "${healthData?.heartRate ?: "--"} BPM",
                                isAbnormal = healthData?.heartRate?.let { 
                                    it > (thresholds.heartRateMax ?: 100) || it < (thresholds.heartRateMin ?: 50) 
                                } ?: false,
                                icon = Icons.Default.Favorite
                            )
                            
                            // Heart Rate Variability (HRV)
                            healthData?.heartRateVariability?.let {
                                HealthMetricItem(
                                    label = "Heart Rate Variability",
                                    value = "$it ms",
                                    isAbnormal = it < 20, // Low HRV is concerning
                                    icon = Icons.Default.Timeline
                                )
                            }
                            
                            // Step Count
                            HealthMetricItem(
                                label = "Step Count",
                                value = "${healthData?.stepCount ?: "--"} steps",
                                isAbnormal = false,
                                icon = Icons.Default.DirectionsWalk
                            )
                            
                            // Respiration Rate
                            healthData?.respirationRate?.let {
                                HealthMetricItem(
                                    label = "Respiration Rate",
                                    value = "$it breaths/min",
                                    isAbnormal = it > 20 || it < 12,
                                    icon = Icons.Default.Air
                                )
                            }
                            
                            // Blood Oxygen
                            healthData?.bloodOxygen?.let {
                                HealthMetricItem(
                                    label = "Blood Oxygen",
                                    value = "$it%",
                                    isAbnormal = it < (thresholds.bloodOxygenMin ?: 95),
                                    icon = Icons.Default.Opacity
                                )
                            }
                            
                            // Blood Pressure
                            if (healthData?.bloodPressureSystolic != null && healthData?.bloodPressureDiastolic != null) {
                                HealthMetricItem(
                                    label = "Blood Pressure",
                                    value = "${healthData?.bloodPressureSystolic}/${healthData?.bloodPressureDiastolic} mmHg",
                                    isAbnormal = (healthData?.bloodPressureSystolic ?: 0) > (thresholds.bpSystolicMax ?: 140) || 
                                               (healthData?.bloodPressureDiastolic ?: 0) > (thresholds.bpDiastolicMax ?: 90),
                                    icon = Icons.Default.Speed
                                )
                            }
                            
                            // Body Temperature
                            healthData?.bodyTemperature?.let {
                                HealthMetricItem(
                                    label = "Body Temperature",
                                    value = "$it°C",
                                    isAbnormal = it > (thresholds.tempMax ?: 38.0f) || it < (thresholds.tempMin ?: 35.5f),
                                    icon = Icons.Default.Thermostat
                                )
                            }
                            
                            // Stress Level
                            healthData?.stressLevel?.let {
                                HealthMetricItem(
                                    label = "Stress Level",
                                    value = "$it%",
                                    isAbnormal = it > 70,
                                    icon = Icons.Default.Psychology
                                )
                            }
                            
                            // Sleep Quality
                            healthData?.sleepQuality?.let {
                                HealthMetricItem(
                                    label = "Sleep Quality",
                                    value = "$it%",
                                    isAbnormal = it < 40,
                                    icon = Icons.Default.Bedtime
                                )
                            }
                            
                            // Last Updated
                            Text(
                                text = "Last updated: ${
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(
                                        Date(healthData?.timestamp ?: System.currentTimeMillis())
                                    )
                                }",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Health Alert Thresholds",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        ThresholdItem(
                            label = "Heart Rate",
                            minValue = thresholds.heartRateMin?.toString() ?: "--",
                            maxValue = thresholds.heartRateMax?.toString() ?: "--",
                            unit = "BPM"
                        )
                        
                        ThresholdItem(
                            label = "Blood Oxygen",
                            minValue = thresholds.bloodOxygenMin?.toString() ?: "--",
                            maxValue = "100",
                            unit = "%"
                        )
                        
                        ThresholdItem(
                            label = "Blood Pressure",
                            minValue = "--",
                            maxValue = "${thresholds.bpSystolicMax ?: "--"}/${thresholds.bpDiastolicMax ?: "--"}",
                            unit = "mmHg"
                        )
                        
                        ThresholdItem(
                            label = "Temperature",
                            minValue = thresholds.tempMin?.toString() ?: "--",
                            maxValue = thresholds.tempMax?.toString() ?: "--",
                            unit = "°C"
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = { viewModel.triggerManualSOS() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Emergency",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Trigger Emergency SOS")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HealthMetricItem(
    label: String,
    value: String,
    isAbnormal: Boolean,
    icon: ImageVector = Icons.Default.Monitor
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isAbnormal) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isAbnormal) MaterialTheme.colorScheme.error else Color.Unspecified
        )
    }
}

@Composable
fun ThresholdItem(
    label: String,
    minValue: String,
    maxValue: String,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = "$minValue - $maxValue $unit",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun PredictionItem(
    condition: String,
    probability: Float
) {
    val progress = probability.coerceIn(0f, 1f)
    val color = when {
        probability > 0.7f -> MaterialTheme.colorScheme.error
        probability > 0.4f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = condition,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "${(probability * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier.fillMaxWidth(),
            color = color
        )
    }
}

@Composable
fun ECGGraph(data: List<Float>, isAbnormal: Boolean) {
    val strokeColor = if (isAbnormal) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (data.isEmpty()) return@Canvas
        
        val width = size.width
        val height = size.height
        val xStep = width / (data.size - 1).coerceAtLeast(1)
        
        // Find min and max for scaling
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        val range = (max - min).coerceAtLeast(1f)
        
        // Create a path for the ECG line
        val path = Path()
        
        // Move to the first point
        val startX = 0f
        val startY = height - ((data[0] - min) / range) * height
        path.moveTo(startX, startY)
        
        // Draw lines to each point
        for (i in 1 until data.size) {
            val x = i * xStep
            val y = height - ((data[i] - min) / range) * height
            path.lineTo(x, y)
        }
        
        // Draw the path
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )
    }
} 