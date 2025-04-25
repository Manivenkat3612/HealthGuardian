package com.example.urban_safety.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urban_safety.R
import com.example.urban_safety.viewmodels.AuthViewModel
import com.example.urban_safety.viewmodels.SOSViewModel
import com.example.urban_safety.viewmodels.TestCenterViewModel
import com.example.urban_safety.ui.screens.testing.TestResult
import com.example.urban_safety.ui.screens.testing.TestResultSeverity
import kotlinx.coroutines.delay

data class Feature(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isPrimary: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSafeRoutes: () -> Unit,
    onNavigateToManualSOS: () -> Unit,
    onNavigateToEmergencyContacts: () -> Unit,
    onNavigateToWearableMonitoring: () -> Unit,
    onNavigateToSafetyScore: () -> Unit,
    onNavigateToTravelCheckIn: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToCommunityHelpers: () -> Unit,
    onNavigateToTestCenter: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val features = listOf(
        Feature(
            title = "Health Monitoring",
            icon = Icons.Default.Watch,
            onClick = onNavigateToWearableMonitoring,
            isPrimary = true
        ),
        Feature(
            title = "Health Analytics",
            icon = Icons.Default.HealthAndSafety,
            onClick = onNavigateToSafetyScore,
            isPrimary = true
        ),
        Feature(
            title = "Manual SOS",
            icon = Icons.Default.Warning,
            onClick = onNavigateToManualSOS
        ),
        Feature(
            title = "Emergency Contacts",
            icon = Icons.Default.Contacts,
            onClick = onNavigateToEmergencyContacts
        ),
        Feature(
            title = "My Health History",
            icon = Icons.Default.Timeline,
            onClick = { /* Implement later */ },
            isPrimary = true
        ),
        Feature(
            title = "Health Prediction",
            icon = Icons.Default.TrendingUp,
            onClick = { /* Implement later */ },
            isPrimary = true
        ),
        Feature(
            title = "Community Support",
            icon = Icons.Rounded.People,
            onClick = onNavigateToCommunityHelpers
        ),
        Feature(
            title = "Testing Center",
            icon = Icons.Default.Science,
            onClick = onNavigateToTestCenter
        )
    )

    var showSOSConfirmation by remember { mutableStateOf(false) }
    val sosViewModel: SOSViewModel = hiltViewModel()
    val isSOSActive by sosViewModel.isSOSActive.collectAsState()
    val isLoading by sosViewModel.isLoading.collectAsState()
    val error by sosViewModel.error.collectAsState()

    // Listen for emergency situations from TestCenter
    val testCenterViewModel: TestCenterViewModel = hiltViewModel()
    val testResults by testCenterViewModel.uiState.collectAsState()
    
    // Check for critical conditions
    LaunchedEffect(testResults) {
        val criticalResults = testResults.testResults.filter { it.severity == TestResultSeverity.CRITICAL }
        if (criticalResults.isNotEmpty()) {
            // Automatically trigger SOS for critical conditions
            sosViewModel.activateSOS()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("HealthGuardian") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Show emergency alert if there are critical conditions
            val criticalResults = testResults.testResults.filter { it.severity == TestResultSeverity.CRITICAL }
            if (criticalResults.isNotEmpty()) {
                EmergencyAlertCard(criticalResults)
                Spacer(modifier = Modifier.height(16.dp))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(features) { feature ->
                    FeatureCard(feature = feature)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            SOSButton(
                onClick = { showSOSConfirmation = true },
                isActive = isSOSActive,
                isLoading = isLoading
            )
        }
    }

    if (showSOSConfirmation) {
        AlertDialog(
            onDismissRequest = { showSOSConfirmation = false },
            title = { Text("Trigger Emergency SOS?") },
            text = { 
                Text(
                    "This will immediately notify your emergency contacts with your current location. " +
                    "Are you sure you want to proceed?"
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSOSConfirmation = false
                        sosViewModel.activateSOS()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Yes, Trigger SOS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSOSConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (error != null) {
        LaunchedEffect(error) {
            delay(5000)
            sosViewModel.clearError()
        }
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { sosViewModel.clearError() }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(error ?: "")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureCard(feature: Feature) {
    ElevatedCard(
        onClick = feature.onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = if (feature.isPrimary) 
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) 
            else CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = feature.icon,
                contentDescription = feature.title,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SOSButton(
    onClick: () -> Unit,
    isActive: Boolean,
    isLoading: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        OutlinedCard(
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
            modifier = Modifier.size(150.dp),
            shape = CircleShape
        ) {
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                enabled = !isLoading
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = "SOS",
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (isActive) "SOS Active" else "SOS",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyAlertCard(criticalResults: List<TestResult>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Emergency",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EMERGENCY ALERT",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            criticalResults.forEach { result ->
                Text(
                    text = "• ${result.title}: ${result.description}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

data class FeatureItem(
    val title: String,
    val icon: ImageVector,
    val backgroundColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit
) 