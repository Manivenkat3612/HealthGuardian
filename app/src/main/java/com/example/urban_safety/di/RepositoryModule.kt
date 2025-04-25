package com.example.urban_safety.di

import android.content.Context
import com.example.urban_safety.data.HealthRepository
import com.example.urban_safety.data.HealthRepositoryImpl
import com.example.urban_safety.data.LocationRepository
import com.example.urban_safety.data.SafetyRepository
import com.example.urban_safety.data.SafetyRepositoryImpl
import com.example.urban_safety.data.repository.SafetyScoreRepository
import com.example.urban_safety.data.repository.SafetyScoreRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindHealthRepository(
        healthRepositoryImpl: HealthRepositoryImpl
    ): HealthRepository
    
    @Binds
    @Singleton
    abstract fun bindSafetyRepository(
        safetyRepositoryImpl: SafetyRepositoryImpl
    ): SafetyRepository
    
    @Binds
    @Singleton
    abstract fun bindSafetyScoreRepository(
        safetyScoreRepositoryImpl: SafetyScoreRepositoryImpl
    ): SafetyScoreRepository
}

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {
    
    @Provides
    @Singleton
    fun provideLocationRepository(
        @ApplicationContext context: Context
    ): LocationRepository {
        return LocationRepository(context)
    }
} 