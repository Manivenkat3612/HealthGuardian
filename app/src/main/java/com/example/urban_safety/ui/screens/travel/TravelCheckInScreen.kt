package com.example.urban_safety.ui.screens.travel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.urban_safety.data.model.TravelStatus
import com.example.urban_safety.data.model.TransportMode
import com.example.urban_safety.viewmodels.TravelCheckInViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelCheckInScreen(
    onNavigateBack: () -> Unit,
    viewModel: TravelCheckInViewModel = hiltViewModel()
) {
    val activeTravelCheckIn by viewModel.activeTravelCheckIn.collectAsState()
    val pastCheckIns by viewModel.pastCheckIns.collectAsState()
    val selectedContacts by viewModel.selectedContacts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var showNewTravelDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Travel Check-In") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (activeTravelCheckIn == null) {
                FloatingActionButton(
                    onClick = { showNewTravelDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Travel")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Show active travel check-in if any
            activeTravelCheckIn?.let { checkIn ->
                ActiveTravelCard(
                    checkIn = checkIn,
                    onArrived = { viewModel.completeTravel() },
                    onCancel = { viewModel.cancelTravel() }
                )
            }
            
            // Show past check-ins
            if (pastCheckIns.isNotEmpty()) {
                Text(
                    text = "Past Travel Check-Ins",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                LazyColumn {
                    items(pastCheckIns) { checkIn ->
                        PastTravelCard(checkIn = checkIn)
                    }
                }
            } else if (activeTravelCheckIn == null) {
                // Show empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "No travel check-ins yet",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Tap the + button to start a new travel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
        
        // New travel dialog
        if (showNewTravelDialog) {
            NewTravelDialog(
                onDismiss = { showNewTravelDialog = false },
                onConfirm = { destination, estimatedArrivalTime, transportMode ->
                    viewModel.createTravelCheckIn(destination, estimatedArrivalTime, transportMode)
                    showNewTravelDialog = false
                },
                selectedContacts = selectedContacts,
                onContactsChanged = { viewModel.updateSelectedContacts(it) }
            )
        }
        
        // Error snackbar
        if (error != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(error!!)
            }
        }
    }
}

@Composable
fun ActiveTravelCard(
    checkIn: com.example.urban_safety.data.model.TravelCheckIn,
    onArrived: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Active Travel",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = "Destination",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = checkIn.destination,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = "Expected Arrival",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = formatDateTime(checkIn.estimatedArrivalTime),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Status: ${formatStatus(checkIn.status)}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel Travel")
                }
                
                Button(onClick = onArrived) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("I've Arrived")
                }
            }
        }
    }
}

@Composable
fun PastTravelCard(checkIn: com.example.urban_safety.data.model.TravelCheckIn) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = checkIn.destination,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                StatusChip(status = checkIn.status)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Started: ${formatDateTime(checkIn.startTime)}",
                style = MaterialTheme.typography.bodySmall
            )
            
            if (checkIn.actualArrivalTime != null) {
                Text(
                    text = "Arrived: ${formatDateTime(checkIn.actualArrivalTime)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: TravelStatus) {
    val (backgroundColor, textColor) = when (status) {
        TravelStatus.ACTIVE -> Pair(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.primary)
        TravelStatus.COMPLETED -> Pair(Color.Green.copy(alpha = 0.2f), Color.Green.copy(alpha = 0.8f))
        TravelStatus.CANCELLED -> Pair(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), MaterialTheme.colorScheme.error)
        TravelStatus.OVERDUE -> Pair(Color.Red.copy(alpha = 0.2f), Color.Red)
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = formatStatus(status),
            color = textColor,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTravelDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Date, TransportMode) -> Unit,
    selectedContacts: List<String>,
    onContactsChanged: (List<String>) -> Unit
) {
    var destination by remember { mutableStateOf("") }
    var expectedArrivalHours by remember { mutableStateOf("1") }
    var showContactsDialog by remember { mutableStateOf(false) }
    var transportMode by remember { mutableStateOf(TransportMode.WALKING) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Travel Check-In") },
        text = {
            Column {
                Text(
                    text = "Let your emergency contacts know where you're going and when you should arrive",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Expected arrival time",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Slider(
                    value = expectedArrivalHours.toFloatOrNull() ?: 1f,
                    onValueChange = { expectedArrivalHours = it.toInt().toString() },
                    valueRange = 1f..24f,
                    steps = 23,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                Text(
                    text = if (expectedArrivalHours.toIntOrNull() == 1) "Arriving in 1 hour" else "Arriving in $expectedArrivalHours hours",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Transport mode selection
                Text(
                    text = "Transport Mode",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                var transportModeExpanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = transportModeExpanded,
                    onExpandedChange = { transportModeExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = transportMode.name.lowercase().capitalize(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = transportModeExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = transportModeExpanded,
                        onDismissRequest = { transportModeExpanded = false }
                    ) {
                        TransportMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(mode.name.lowercase().capitalize()) },
                                onClick = {
                                    transportMode = mode
                                    transportModeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { showContactsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedContacts.isEmpty()) {
                            "Select contacts to notify"
                        } else {
                            "${selectedContacts.size} contacts selected"
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val arrivalTime = Calendar.getInstance().apply {
                        add(Calendar.HOUR, expectedArrivalHours.toIntOrNull() ?: 1)
                    }.time
                    onConfirm(destination, arrivalTime, transportMode)
                },
                enabled = destination.isNotBlank() && selectedContacts.isNotEmpty()
            ) {
                Text("Start Travel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Contacts selection dialog
    if (showContactsDialog) {
        ContactSelectionDialog(
            selectedContacts = selectedContacts,
            onContactsSelected = { 
                onContactsChanged(it)
                showContactsDialog = false
            },
            onDismiss = { showContactsDialog = false }
        )
    }
}

@Composable
fun ContactSelectionDialog(
    selectedContacts: List<String>,
    onContactsSelected: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    // In a real app, this would fetch actual contacts
    // For demo, we'll use mock data
    val availableContacts = remember {
        listOf("John Doe", "Jane Smith", "Alice Johnson", "Bob Brown", "Charlie Davis")
    }
    
    val selected = remember { selectedContacts.toMutableStateList() }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Emergency Contacts") },
        text = {
            LazyColumn {
                items(availableContacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected.contains(contact),
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    selected.add(contact)
                                } else {
                                    selected.remove(contact)
                                }
                            }
                        )
                        
                        Text(
                            text = contact,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onContactsSelected(selected) }
            ) {
                Text("Select ${selected.size} Contacts")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions
private fun formatDateTime(date: Date): String {
    val format = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return format.format(date)
}

private fun formatStatus(status: TravelStatus): String {
    return when (status) {
        TravelStatus.ACTIVE -> "In Progress"
        TravelStatus.COMPLETED -> "Completed"
        TravelStatus.CANCELLED -> "Cancelled"
        TravelStatus.OVERDUE -> "Overdue"
    }
}

// Extension function to capitalize the first letter of a string
private fun String.capitalize(): String {
    return this.replaceFirstChar { 
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
    }
} 