package com.example.urban_safety.ui.screens.community

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.urban_safety.MainActivity
import com.example.urban_safety.data.model.HelpRequest
import com.example.urban_safety.data.model.LocationData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import com.example.urban_safety.viewmodels.SosResponseViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class SosResponseActivity : ComponentActivity() {

    @Inject
    lateinit var firestore: FirebaseFirestore
    
    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userId = intent.getStringExtra("USER_ID") ?: ""
        val userName = intent.getStringExtra("USER_NAME") ?: "Someone"
        val latitude = intent.getStringExtra("LATITUDE")?.toDoubleOrNull() ?: 0.0
        val longitude = intent.getStringExtra("LONGITUDE")?.toDoubleOrNull() ?: 0.0
        val timestamp = intent.getStringExtra("TIMESTAMP")?.toLongOrNull() ?: System.currentTimeMillis()
        
        setContent {
            MaterialTheme {
                SosResponseScreen(
                    onNavigateBack = { finish() },
                    viewModel = hiltViewModel()
                )
            }
        }
    }
    
    private fun acceptHelpRequest(
        userId: String,
        userName: String,
        latitude: Double,
        longitude: Double,
        message: String
    ) {
        lifecycleScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // Create a help response record
                val helpRequest = HelpRequest(
                    id = UUID.randomUUID().toString(),
                    requesterId = userId,
                    requesterName = userName,
                    helperId = currentUser.uid,
                    helperName = currentUser.displayName ?: "Helper",
                    message = message,
                    requesterLocation = LocationData(
                        latitude = latitude,
                        longitude = longitude,
                        timestamp = Date()
                    ),
                    status = HelpRequest.RequestStatus.ACCEPTED,
                    createdAt = Date(),
                    acceptedAt = Date()
                )
                
                // Save to Firestore
                firestore.collection("help_requests")
                    .document(helpRequest.id)
                    .set(helpRequest)
                    .await()
                
                // Navigate to maps
                val mapIntent = Intent(Intent.ACTION_VIEW, 
                    Uri.parse("geo:0,0?q=$latitude,$longitude($userName)"))
                startActivity(mapIntent)
                finish()
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SosResponseScreen(
    onNavigateBack: () -> Unit,
    viewModel: SosResponseViewModel = hiltViewModel()
) {
    val sosRequest by viewModel.sosRequest.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SOS Response") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (sosRequest != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // SOS Request Details
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "SOS Request Details",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            DetailRow(
                                icon = Icons.Default.Person,
                                label = "Requester",
                                value = sosRequest?.requesterName ?: "Unknown"
                            )
                            
                            DetailRow(
                                icon = Icons.Default.LocationOn,
                                label = "Location",
                                value = sosRequest?.location?.let { 
                                    "${it.latitude}, ${it.longitude}" 
                                } ?: "Unknown"
                            )
                            
                            DetailRow(
                                icon = Icons.Default.Info,
                                label = "Type",
                                value = sosRequest?.type?.name ?: "Unknown"
                            )
                            
                            DetailRow(
                                icon = Icons.Default.Message,
                                label = "Message",
                                value = sosRequest?.message ?: "No message provided"
                            )
                        }
                    }
                    
                    // Response Actions
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.acceptRequest() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Accept Request")
                        }
                        
                        Button(
                            onClick = { viewModel.rejectRequest() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reject Request")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No active SOS request found",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Please check back later",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Error snackbar
            if (error != null) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
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
}

@Composable
fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
} 