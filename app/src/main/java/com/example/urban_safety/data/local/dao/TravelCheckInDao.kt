package com.example.urban_safety.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.urban_safety.data.model.TransportMode
import com.example.urban_safety.data.model.TravelCheckIn
import com.example.urban_safety.data.model.TravelStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for travel check-ins
 */
@Dao
interface TravelCheckInDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravelCheckIn(travelCheckIn: TravelCheckIn)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravelCheckIns(travelCheckIns: List<TravelCheckIn>)
    
    @Update
    suspend fun updateTravelCheckIn(travelCheckIn: TravelCheckIn)
    
    @Delete
    suspend fun deleteTravelCheckIn(travelCheckIn: TravelCheckIn)
    
    @Query("SELECT * FROM travel_checkins ORDER BY startTime DESC")
    fun getAllTravelCheckIns(): Flow<List<TravelCheckIn>>
    
    @Query("SELECT * FROM travel_checkins WHERE id = :checkInId")
    suspend fun getTravelCheckInById(checkInId: String): TravelCheckIn?
    
    @Query("SELECT * FROM travel_checkins WHERE userId = :userId ORDER BY startTime DESC")
    fun getTravelCheckInsByUser(userId: String): Flow<List<TravelCheckIn>>
    
    @Query("SELECT * FROM travel_checkins WHERE status = 'ACTIVE' ORDER BY startTime DESC")
    fun getActiveTravelCheckIns(): Flow<List<TravelCheckIn>>
    
    @Query("SELECT * FROM travel_checkins WHERE userId = :userId AND status = 'ACTIVE' ORDER BY startTime DESC")
    fun getActiveUserTravelCheckIns(userId: String): Flow<List<TravelCheckIn>>
    
    @Query("SELECT * FROM travel_checkins WHERE status = :status ORDER BY startTime DESC")
    fun getTravelCheckInsByStatus(status: TravelStatus): Flow<List<TravelCheckIn>>
    
    @Query("SELECT * FROM travel_checkins WHERE transportMode = :transportMode ORDER BY startTime DESC")
    fun getTravelCheckInsByTransportMode(transportMode: TransportMode): Flow<List<TravelCheckIn>>
    
    @Query("SELECT * FROM travel_checkins WHERE estimatedArrivalTime BETWEEN :startTime AND :endTime ORDER BY estimatedArrivalTime ASC")
    fun getUpcomingArrivals(startTime: Long, endTime: Long): Flow<List<TravelCheckIn>>
    
    @Query("UPDATE travel_checkins SET status = :newStatus, actualArrivalTime = :arrivalTime WHERE id = :checkInId")
    suspend fun updateTravelCheckInStatus(checkInId: String, newStatus: TravelStatus, arrivalTime: Long?)
    
    @Query("DELETE FROM travel_checkins WHERE status IN ('COMPLETED', 'CANCELLED') AND startTime < :cutoffTime")
    suspend fun deletePastTravelCheckIns(cutoffTime: Long)
    
    @Query("DELETE FROM travel_checkins")
    suspend fun deleteAllTravelCheckIns()
} 