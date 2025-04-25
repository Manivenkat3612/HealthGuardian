package com.example.urban_safety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urban_safety.data.model.IncidentStatus
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.SafetyIncident
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the SafetyIncident entity
 */
@Dao
interface SafetyIncidentDao {
    
    /**
     * Insert a safety incident
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncident(incident: SafetyIncident)
    
    /**
     * Insert multiple safety incidents
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncidents(incidents: List<SafetyIncident>)
    
    /**
     * Update an existing safety incident
     */
    @Update
    suspend fun updateIncident(incident: SafetyIncident)
    
    /**
     * Delete a safety incident
     */
    @Delete
    suspend fun deleteIncident(incident: SafetyIncident)
    
    /**
     * Get all safety incidents as a Flow
     */
    @Query("SELECT * FROM safety_incidents ORDER BY timestamp DESC")
    fun getAllIncidents(): Flow<List<SafetyIncident>>
    
    /**
     * Get a specific incident by ID
     */
    @Query("SELECT * FROM safety_incidents WHERE id = :incidentId")
    suspend fun getIncidentById(incidentId: String): SafetyIncident?
    
    /**
     * Get all incidents for a specific user
     */
    @Query("SELECT * FROM safety_incidents WHERE userId = :userId ORDER BY timestamp DESC")
    fun getIncidentsByUser(userId: String): Flow<List<SafetyIncident>>
    
    /**
     * Get active incidents (incidents that haven't been resolved)
     */
    @Query("SELECT * FROM safety_incidents WHERE status = 'ACTIVE' ORDER BY timestamp DESC")
    fun getActiveIncidents(): Flow<List<SafetyIncident>>
    
    /**
     * Get active incident for a specific user
     */
    @Query("SELECT * FROM safety_incidents WHERE userId = :userId AND status = 'ACTIVE' LIMIT 1")
    suspend fun getActiveIncidentByUser(userId: String): SafetyIncident?
    
    /**
     * Get incidents by type
     */
    @Query("SELECT * FROM safety_incidents WHERE type = :type ORDER BY timestamp DESC")
    fun getIncidentsByType(type: IncidentType): Flow<List<SafetyIncident>>
    
    /**
     * Get incidents by status
     */
    @Query("SELECT * FROM safety_incidents WHERE status = :status ORDER BY timestamp DESC")
    fun getIncidentsByStatus(status: IncidentStatus): Flow<List<SafetyIncident>>
    
    /**
     * Get incidents within a timeframe
     */
    @Query("SELECT * FROM safety_incidents WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getIncidentsInTimeRange(startTime: Long, endTime: Long): Flow<List<SafetyIncident>>
    
    /**
     * Get count of incidents by type
     */
    @Query("SELECT COUNT(*) FROM safety_incidents WHERE type = :type")
    suspend fun getIncidentCountByType(type: IncidentType): Int
    
    /**
     * Update the status of an incident
     */
    @Query("UPDATE safety_incidents SET status = :newStatus, resolvedTimestamp = :resolvedTimestamp, resolvedBy = :resolvedBy WHERE id = :incidentId")
    suspend fun updateIncidentStatus(
        incidentId: String,
        newStatus: IncidentStatus,
        resolvedTimestamp: Long?,
        resolvedBy: String?
    )
    
    /**
     * Delete all incidents (useful for testing or when user logs out)
     */
    @Query("DELETE FROM safety_incidents")
    suspend fun deleteAllIncidents()
} 