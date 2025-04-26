package com.example.urban_safety.ui.components

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urban_safety.R
import com.example.urban_safety.viewmodels.FallDetectionViewModel

@Composable
fun FallDetectionControl(
    onRequestPermissions: (Array<String>) -> Unit,
    viewModel: FallDetectionViewModel = hiltViewModel()
) {
    val isActive by viewModel.isFallDetectionActive.collectAsState()
    val hasPermissions by viewModel.permissionsGranted.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showBackgroundLocationDialog by remember { mutableStateOf(false) }
    
    // Check permissions when composable is first created and whenever it recomposes
    LaunchedEffect(Unit) {
        val permissionsGranted = viewModel.checkPermissions()
        Log.d("FallDetectionControl", "Permissions check result: $permissionsGranted")
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) 
                MaterialTheme.colorScheme.primaryContainer 
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Falling person icon (simulated by rotating the running icon)
                Icon(
                    imageVector = Icons.Default.DirectionsRun,
                    contentDescription = "Fall Detection",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(90f), // Rotate to simulate a falling person
                    tint = if (isActive) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = "Fall Detection",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isActive) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = if (isActive) "Active - Monitoring for falls" else "Disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isActive) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Switch(
                checked = isActive,
                onCheckedChange = {
                    Log.d("FallDetectionControl", "Switch toggled to: $it, current permissions: $hasPermissions")
                    // Always allow toggling for testing
                    viewModel.toggleFallDetection()
                    
                    // If permissions not granted, show dialog for future reference
                    if (it && !hasPermissions) {
                        showPermissionDialog = true
                    }
                }
            )
        }
        
        // Add a debug row with direct control buttons
        if (hasPermissions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { viewModel.startFallDetection() }) {
                    Text("Force Start")
                }
                Button(onClick = { viewModel.stopFallDetection() }) {
                    Text("Force Stop")
                }
            }
            
            // Add test fall detection button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { viewModel.testFallDetection() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text("Test Fall Detection Alert")
                }
            }
        }
    }
    
    // First permissions dialog for basic permissions
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissions Required") },
            text = { 
                Text(
                    "Fall detection requires location and SMS permissions to function. " +
                    "Please grant these permissions to enable fall detection."
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        // Request basic permissions first
                        onRequestPermissions(viewModel.getRequiredPermissions().toTypedArray())
                        
                        // Check if we need to show background permission dialog
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            showBackgroundLocationDialog = true
                        }
                    }
                ) {
                    Text("Grant Permissions")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Second dialog specifically for background location (Android 10+)
    if (showBackgroundLocationDialog) {
        AlertDialog(
            onDismissRequest = { showBackgroundLocationDialog = false },
            title = { Text("Background Location Access") },
            text = { 
                Text(
                    "Fall detection needs to monitor your location in the background to " +
                    "detect falls and send your location to emergency contacts.\n\n" +
                    "Please select \"Allow all the time\" on the next screen."
                ) 
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBackgroundLocationDialog = false
                        viewModel.getBackgroundLocationPermission()?.let { permission ->
                            onRequestPermissions(arrayOf(permission))
                        }
                    }
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBackgroundLocationDialog = false }
                ) {
                    Text("Skip")
                }
            }
        )
    }
} 