package com.example.urban_safety.data.api

import com.example.urban_safety.data.model.SafetyScore
import retrofit2.http.GET
import retrofit2.http.Query

interface SafetyScoreApi {
    @GET("safety-score")
    suspend fun getSafetyScore(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double
    ): SafetyScore
} 