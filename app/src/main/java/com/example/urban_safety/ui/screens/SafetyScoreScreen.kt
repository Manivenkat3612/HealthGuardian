package com.example.urban_safety.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urban_safety.data.model.SafetyScore
import com.example.urban_safety.ui.viewmodels.SafetyScoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SafetyScoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: SafetyScoreViewModel = hiltViewModel()
) {
    val safetyScore by viewModel.safetyScore.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.refreshSafetyScore() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Area Safety Score",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                SafetyScoreCard(safetyScore)

                if (safetyScore?.safetyTips?.isNotEmpty() == true) {
                    SafetyTipsCard(safetyScore!!.safetyTips)
                }
            }
        }
    }
}

@Composable
private fun SafetyScoreCard(safetyScore: SafetyScore?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScoreRow("Overall Score", "${safetyScore?.overallScore ?: "N/A"}/100")
            ScoreRow("Crime Rate", "${safetyScore?.crimeRate?.let { String.format("%.2f", it) } ?: "N/A"}%")
            ScoreRow("Lighting Score", "${safetyScore?.lightingScore ?: "N/A"}/100")
            ScoreRow("Population Density", "${safetyScore?.populationDensity?.let { String.format("%.2f", it) } ?: "N/A"}")
            ScoreRow("Emergency Response Time", "${safetyScore?.emergencyResponseTime?.let { String.format("%.1f", it) } ?: "N/A"} min")
            
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            val lastUpdated = safetyScore?.lastUpdated?.let { dateFormat.format(Date(it)) } ?: "N/A"
            Text(
                text = "Last Updated: $lastUpdated",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SafetyTipsCard(tips: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Safety Tips",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            tips.forEach { tip ->
                Text(
                    text = "• $tip",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
    }
} 