package com.example.urban_safety.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.urban_safety.data.local.dao.SafetyIncidentDao
import com.example.urban_safety.data.local.dao.SafetyReportDao
import com.example.urban_safety.data.local.dao.TravelCheckInDao
import com.example.urban_safety.data.local.converters.*
import com.example.urban_safety.data.model.*

/**
 * Room Database for the Urban Safety app
 */
@Database(
    entities = [
        SafetyIncident::class,
        SafetyReport::class,
        AreaSafetyScore::class,
        TravelCheckIn::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(
    DateConverter::class,
    LocationDataConverter::class,
    EmergencyContactConverter::class,
    IncidentTypeConverter::class,
    IncidentStatusConverter::class,
    StringListConverter::class,
    TravelCheckpointConverter::class,
    SafetyCategoryConverter::class,
    SafetySeverityConverter::class,
    TravelStatusConverter::class,
    TransportModeConverter::class,
    TemporarySafetyFactorConverter::class,
    DataSourceQualityConverter::class,
    TemporaryFactorTypeConverter::class
)
abstract class UrbanSafetyDatabase : RoomDatabase() {

    abstract fun safetyIncidentDao(): SafetyIncidentDao
    abstract fun safetyReportDao(): SafetyReportDao
    abstract fun travelCheckInDao(): TravelCheckInDao

    companion object {
        @Volatile
        private var INSTANCE: UrbanSafetyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE safety_incidents ADD COLUMN resolvedTimestamp INTEGER")
                database.execSQL("ALTER TABLE safety_incidents ADD COLUMN resolvedBy TEXT")
            }
        }

        fun getDatabase(context: Context): UrbanSafetyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UrbanSafetyDatabase::class.java,
                    "urban_safety_database"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 