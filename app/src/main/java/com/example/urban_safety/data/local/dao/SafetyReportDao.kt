package com.example.urban_safety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urban_safety.data.model.AreaSafetyScore
import com.example.urban_safety.data.model.SafetyCategory
import com.example.urban_safety.data.model.SafetyReport
import com.example.urban_safety.data.model.SafetySeverity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the safety reports and safety scores
 */
@Dao
interface SafetyReportDao {

    /**
     * Insert a safety report
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafetyReport(report: SafetyReport)
    
    /**
     * Insert multiple safety reports
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafetyReports(reports: List<SafetyReport>)
    
    /**
     * Update a safety report
     */
    @Update
    suspend fun updateSafetyReport(report: SafetyReport)
    
    /**
     * Delete a safety report
     */
    @Delete
    suspend fun deleteSafetyReport(report: SafetyReport)
    
    /**
     * Get all safety reports
     */
    @Query("SELECT * FROM safety_reports ORDER BY timestamp DESC")
    fun getAllSafetyReports(): Flow<List<SafetyReport>>
    
    /**
     * Get a specific safety report by ID
     */
    @Query("SELECT * FROM safety_reports WHERE id = :reportId")
    suspend fun getSafetyReportById(reportId: String): SafetyReport?
    
    /**
     * Get safety reports submitted by a specific user
     */
    @Query("SELECT * FROM safety_reports WHERE userId = :userId ORDER BY timestamp DESC")
    fun getSafetyReportsByUser(userId: String): Flow<List<SafetyReport>>
    
    /**
     * Get safety reports by category
     */
    @Query("SELECT * FROM safety_reports WHERE category = :category ORDER BY timestamp DESC")
    fun getSafetyReportsByCategory(category: SafetyCategory): Flow<List<SafetyReport>>
    
    /**
     * Get safety reports by severity
     */
    @Query("SELECT * FROM safety_reports WHERE severity = :severity ORDER BY timestamp DESC")
    fun getSafetyReportsBySeverity(severity: SafetySeverity): Flow<List<SafetyReport>>
    
    /**
     * Get verified safety reports
     */
    @Query("SELECT * FROM safety_reports WHERE verified = 1 ORDER BY timestamp DESC")
    fun getVerifiedSafetyReports(): Flow<List<SafetyReport>>
    
    /**
     * Get unverified safety reports
     */
    @Query("SELECT * FROM safety_reports WHERE verified = 0 ORDER BY timestamp DESC")
    fun getUnverifiedSafetyReports(): Flow<List<SafetyReport>>
    
    /**
     * Get recent safety reports within a specific timeframe
     */
    @Query("SELECT * FROM safety_reports WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getRecentSafetyReports(startTime: Long): Flow<List<SafetyReport>>
    
    /**
     * Update verification status of a report
     */
    @Query("UPDATE safety_reports SET verified = :isVerified WHERE id = :reportId")
    suspend fun updateReportVerificationStatus(reportId: String, isVerified: Boolean)
    
    /**
     * Delete all safety reports
     */
    @Query("DELETE FROM safety_reports")
    suspend fun deleteAllSafetyReports()
    
    // Area Safety Score methods
    
    /**
     * Insert a safety score for an area
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAreaSafetyScore(areaSafetyScore: AreaSafetyScore)
    
    /**
     * Update a safety score
     */
    @Update
    suspend fun updateAreaSafetyScore(areaSafetyScore: AreaSafetyScore)
    
    /**
     * Get all area safety scores
     */
    @Query("SELECT * FROM area_safety_scores ORDER BY overallScore ASC")
    fun getAllAreaSafetyScores(): Flow<List<AreaSafetyScore>>
    
    /**
     * Get a specific area safety score by ID
     */
    @Query("SELECT * FROM area_safety_scores WHERE id = :areaId")
    suspend fun getAreaSafetyScoreById(areaId: String): AreaSafetyScore?
    
    /**
     * Get areas with safety scores below a threshold
     */
    @Query("SELECT * FROM area_safety_scores WHERE overallScore < :threshold ORDER BY overallScore ASC")
    fun getUnsafeAreas(threshold: Int): Flow<List<AreaSafetyScore>>
    
    /**
     * Get areas sorted by safety score (safest first)
     */
    @Query("SELECT * FROM area_safety_scores ORDER BY overallScore DESC")
    fun getAreasSortedBySafety(): Flow<List<AreaSafetyScore>>
    
    /**
     * Get recently updated safety scores
     */
    @Query("SELECT * FROM area_safety_scores WHERE lastUpdated >= :updateTime")
    fun getRecentlyUpdatedAreas(updateTime: Long): Flow<List<AreaSafetyScore>>
} 