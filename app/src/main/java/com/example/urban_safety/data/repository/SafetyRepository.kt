package com.example.urban_safety.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import com.example.urban_safety.data.local.UrbanSafetyDatabase
import com.example.urban_safety.data.local.dao.SafetyIncidentDao
import com.example.urban_safety.data.model.EmergencyContact
import com.example.urban_safety.data.model.LocationData
import com.example.urban_safety.data.model.HealthData
import com.example.urban_safety.data.model.IncidentStatus
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.SafetyIncident
import com.example.urban_safety.data.model.User
import com.example.urban_safety.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for safety incident and emergency operations
 */
@Singleton
class SafetyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyIncidentDao: SafetyIncidentDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    
    private val realtime = FirebaseDatabase.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    
    private val usersCollection = firestore.collection(Constants.USERS_COLLECTION)
    private val incidentsRef = realtime.getReference("safety_incidents")
    private val emergencyContactsCollection = firestore.collection(Constants.EMERGENCY_CONTACTS_COLLECTION)
    private val incidentsCollection = firestore.collection(Constants.INCIDENTS_COLLECTION)
    
    /**
     * Gets the current user ID
     */
    private fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: ""
    }
    
    /**
     * Create a new safety incident (SOS)
     */
    suspend fun createSafetyIncident(
        type: IncidentType,
        locationData: LocationData?,
        description: String
    ): Result<SafetyIncident> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        try {
            // Create incident object
            val incidentId = UUID.randomUUID().toString()
            val incident = SafetyIncident(
                id = incidentId,
                userId = user.uid,
                timestamp = Date(System.currentTimeMillis()),
                type = type,
                status = IncidentStatus.ACTIVE,
                location = locationData,
                description = description
            )
            
            // First try Firestore
            try {
                incidentsCollection.document(incidentId).set(incident).await()
            } catch (e: Exception) {
                // Log error but continue with Realtime Database
                println("Firestore write failed: ${e.message}")
            }
            
            // Then try Realtime Database
            try {
                incidentsRef.child(incidentId).setValue(incident).await()
            } catch (e: Exception) {
                // If both databases fail, throw error
                if (!incident.isSavedToFirestore) {
                    throw Exception("Failed to save incident to both databases")
                }
            }
            
            // Save to local Room database
            try {
                safetyIncidentDao.insertIncident(incident)
            } catch (e: Exception) {
                // Log error but don't fail the operation
                println("Local database write failed: ${e.message}")
            }
            
            // Notify emergency contacts
            try {
                notifyEmergencyContacts(incident, user.uid)
            } catch (e: Exception) {
                // Log error but don't fail the operation
                println("Emergency contact notification failed: ${e.message}")
            }
            
            return Result.success(incident)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Resolve a safety incident
     */
    suspend fun resolveSafetyIncident(incidentId: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        try {
            val resolvedTimestamp = Date(System.currentTimeMillis())
            
            // Update in Firebase
            incidentsRef.child(incidentId).child("status").setValue(IncidentStatus.RESOLVED.name).await()
            incidentsRef.child(incidentId).child("resolvedTimestamp").setValue(resolvedTimestamp).await()
            incidentsRef.child(incidentId).child("resolvedBy").setValue(user.uid).await()
            
            // Update in Room
            safetyIncidentDao.updateIncidentStatus(
                incidentId = incidentId,
                newStatus = IncidentStatus.RESOLVED,
                resolvedTimestamp = resolvedTimestamp.time,
                resolvedBy = user.uid
            )
            
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Mark a safety incident as a false alarm
     */
    suspend fun markIncidentAsFalseAlarm(incidentId: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not authenticated"))
        
        try {
            val resolvedTimestamp = Date(System.currentTimeMillis())
            
            // Update in Firebase
            incidentsRef.child(incidentId).child("status").setValue(IncidentStatus.FALSE_ALARM.name).await()
            incidentsRef.child(incidentId).child("resolvedTimestamp").setValue(resolvedTimestamp).await()
            incidentsRef.child(incidentId).child("resolvedBy").setValue(user.uid).await()
            
            // Update in Room
            safetyIncidentDao.updateIncidentStatus(
                incidentId = incidentId,
                newStatus = IncidentStatus.FALSE_ALARM,
                resolvedTimestamp = resolvedTimestamp.time,
                resolvedBy = user.uid
            )
            
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Gets all incidents for the current user
     */
    suspend fun getUserIncidents(): Result<List<SafetyIncident>> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            val incidents = incidentsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(SafetyIncident::class.java) }
                
            Result.success(incidents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets a user by their ID
     */
    suspend fun getUser(userId: String): Result<User> {
        return try {
            val userDoc = usersCollection
                .document(userId)
                .get()
                .await()
                
            val user = userDoc.toObject(User::class.java)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets all emergency contacts for a user
     */
    suspend fun getEmergencyContacts(userId: String): Result<List<EmergencyContact>> {
        return try {
            val contacts = emergencyContactsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(EmergencyContact::class.java) }
                
            Result.success(contacts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Adds an emergency contact
     */
    suspend fun addEmergencyContact(contact: EmergencyContact): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            usersCollection
                .document(userId)
                .collection("emergencyContacts")
                .add(contact)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Updates an emergency contact
     */
    suspend fun updateEmergencyContact(updatedContact: EmergencyContact): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            // Need to find the contact by name or another field since we don't store IDs in our simplified model
            val contactsQuery = usersCollection
                .document(userId)
                .collection("emergencyContacts")
                .whereEqualTo("name", updatedContact.name)
                .get()
                .await()
                
            val contactDoc = contactsQuery.documents.firstOrNull()
            if (contactDoc != null) {
                contactDoc.reference.set(updatedContact).await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Contact not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Removes an emergency contact
     */
    suspend fun removeEmergencyContact(contactName: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
            if (userId.isEmpty()) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            // Need to find the contact by name since we don't store IDs in our simplified model
            val contactsQuery = usersCollection
                .document(userId)
                .collection("emergencyContacts")
                .whereEqualTo("name", contactName)
                .get()
                .await()
                
            val contactDoc = contactsQuery.documents.firstOrNull()
            if (contactDoc != null) {
                contactDoc.reference.delete().await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Contact not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sends SMS to emergency contacts
     */
    fun sendEmergencySMS(contacts: List<EmergencyContact>, message: String, location: LocationData?): Result<Unit> {
        return try {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.SEND_SMS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return Result.failure(Exception("SMS permission not granted"))
            }
            
            val smsManager = SmsManager.getDefault()
            val locationText = if (location != null) {
                "\nLocation: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else ""
            
            val fullMessage = "$message$locationText"
            
            contacts.forEach { contact ->
                smsManager.sendTextMessage(
                    contact.phoneNumber,
                    null,
                    fullMessage,
                    null,
                    null
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Notifies emergency contacts of an incident
     */
    private suspend fun notifyEmergencyContacts(incident: SafetyIncident, userId: String) {
        try {
            val contactsResult = getEmergencyContacts(userId)
            if (contactsResult.isSuccess) {
                val contacts = contactsResult.getOrNull() ?: emptyList()
                val message = "EMERGENCY: ${incident.type} incident reported by your contact. Please respond immediately."
                
                // Use the incident.location directly instead of creating a new one
                sendEmergencySMS(contacts, message, incident.location)
            }
        } catch (e: Exception) {
            // Log the error but don't fail the operation
            println("Failed to notify emergency contacts: ${e.message}")
        }
    }
    
    /**
     * Get active safety incidents for the current user
     */
    fun getActiveUserIncidents(): Flow<List<SafetyIncident>> {
        val user = auth.currentUser
        return if (user != null) {
            safetyIncidentDao.getIncidentsByUser(user.uid)
        } else {
            safetyIncidentDao.getActiveIncidents()
        }
    }
    
    /**
     * Get a specific incident by ID
     */
    suspend fun getIncidentById(incidentId: String): Result<SafetyIncident> {
        try {
            // First try to get from local database
            val localIncident = safetyIncidentDao.getIncidentById(incidentId)
            
            if (localIncident != null) {
                return Result.success(localIncident)
            }
            
            // If not in local database, try to get from Firebase
            val snapshot = incidentsRef.child(incidentId).get().await()
            val incident = snapshot.getValue<SafetyIncident>()
            
            return if (incident != null) {
                // Save to local database for future queries
                safetyIncidentDao.insertIncident(incident)
                Result.success(incident)
            } else {
                Result.failure(Exception("Incident not found"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Get the active incident for a user
     */
    suspend fun getActiveIncident(userId: String): SafetyIncident? {
        try {
            // Check for active incidents in local database
            val localActiveIncident = safetyIncidentDao.getActiveIncidentByUser(userId)
            
            if (localActiveIncident != null) {
                return localActiveIncident
            }
            
            // Check in Firebase
            val snapshot = incidentsRef
                .orderByChild("userId")
                .equalTo(userId)
                .get()
                .await()
                
            for (child in snapshot.children) {
                val incident = child.getValue<SafetyIncident>()
                if (incident != null && incident.status == IncidentStatus.ACTIVE) {
                    // Save to local database
                    safetyIncidentDao.insertIncident(incident)
                    return incident
                }
            }
            
            return null
        } catch (e: Exception) {
            println("Error getting active incident: ${e.message}")
            return null
        }
    }
    
    /**
     * Resolve the active incident for a user
     */
    suspend fun resolveActiveIncident(userId: String): Result<Unit> {
        try {
            val activeIncident = getActiveIncident(userId) ?: 
                return Result.failure(Exception("No active incident found"))
                
            return resolveSafetyIncident(activeIncident.id)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
    
    /**
     * Trigger SOS with optional health data
     */
    suspend fun triggerSOS(
        message: String,
        latitude: Double?,
        longitude: Double?,
        healthData: HealthData? = null
    ): Result<SafetyIncident> {
        try {
            val location = if (latitude != null && longitude != null) {
                LocationData(
                    latitude = latitude,
                    longitude = longitude,
                    accuracy = 0f,
                    timestamp = Date(System.currentTimeMillis())
                )
            } else null
            
            return createSafetyIncident(
                type = IncidentType.MANUAL_SOS,
                locationData = location,
                description = message
            )
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
} 