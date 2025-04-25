package com.example.urban_safety.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
            
            SOSButton(onClick = onNavigateToManualSOS)
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
fun SOSButton(onClick: () -> Unit) {
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
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = "SOS",
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "SOS",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
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