package com.example.urban_safety.di

import android.content.Context
import com.example.urban_safety.data.api.SafetyScoreApi
import com.example.urban_safety.data.local.dao.SafetyIncidentDao
import com.example.urban_safety.data.repository.AuthRepository
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.data.repository.SafetyRepository
import com.example.urban_safety.repositories.EmergencyContactsRepository
import com.example.urban_safety.repositories.FirebaseEmergencyContactsRepository
import com.example.urban_safety.util.PreferencesManager
import com.example.urban_safety.util.SmsService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideSafetyRepository(
        @ApplicationContext context: Context,
        safetyIncidentDao: SafetyIncidentDao,
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): SafetyRepository {
        return SafetyRepository(context, safetyIncidentDao, firestore, auth)
    }
    
    @Provides
    @Singleton
    fun provideEmergencyContactsRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth
    ): EmergencyContactsRepository {
        return FirebaseEmergencyContactsRepository(firestore, auth)
    }
    
    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }
    
    @Provides
    @Singleton
    fun provideSmsService(@ApplicationContext context: Context): SmsService {
        return SmsService(context)
    }
    
    // Add more providers as needed
    // Examples:
    // - API services 
    // - Repositories
    // - Shared preferences
} 