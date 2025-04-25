package com.example.urban_safety.di

import android.content.Context
import com.example.urban_safety.data.local.UrbanSafetyDatabase
import com.example.urban_safety.data.local.dao.SafetyIncidentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): UrbanSafetyDatabase {
        return UrbanSafetyDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideSafetyIncidentDao(
        database: UrbanSafetyDatabase
    ): SafetyIncidentDao {
        return database.safetyIncidentDao()
    }
} 