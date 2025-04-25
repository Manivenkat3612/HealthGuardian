package com.example.urban_safety.data.repository

import com.example.urban_safety.data.model.SafetyScore

/**
 * Repository for safety score data
 */
interface SafetyScoreRepository {
    /**
     * Get the safety score information
     */
    suspend fun getSafetyScore(): SafetyScore
} 