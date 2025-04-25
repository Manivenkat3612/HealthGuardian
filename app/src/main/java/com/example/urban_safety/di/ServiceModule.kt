package com.example.urban_safety.di

import android.content.Context
import com.example.urban_safety.data.repository.LocationRepository
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    // We can add other service-related providers here in the future
} 