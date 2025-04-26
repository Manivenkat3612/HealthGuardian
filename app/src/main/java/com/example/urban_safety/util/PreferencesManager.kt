package com.example.urban_safety.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local preferences and settings
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefsFileName = "health_guardian_prefs"
    private val preferences: SharedPreferences = context.getSharedPreferences(prefsFileName, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // First Run
    var isFirstRun: Boolean
        get() = preferences.getBoolean(Constants.KEY_FIRST_RUN, true)
        set(value) = preferences.edit { putBoolean(Constants.KEY_FIRST_RUN, value) }
    
    // User Profile
    var userId: String
        get() = preferences.getString(Constants.KEY_USER_ID, "") ?: ""
        set(value) = preferences.edit { putString(Constants.KEY_USER_ID, value) }

    var userName: String
        get() = preferences.getString(Constants.KEY_USER_NAME, "") ?: ""
        set(value) = preferences.edit { putString(Constants.KEY_USER_NAME, value) }

    var userEmail: String
        get() = preferences.getString(Constants.KEY_USER_EMAIL, "") ?: ""
        set(value) = preferences.edit { putString(Constants.KEY_USER_EMAIL, value) }

    var userPhone: String
        get() = preferences.getString(Constants.KEY_USER_PHONE, "") ?: ""
        set(value) = preferences.edit { putString(Constants.KEY_USER_PHONE, value) }

    // Settings - Replaced properties with methods to avoid JVM signature clashes
    fun getLocationTracking(): Boolean {
        return preferences.getBoolean(Constants.KEY_LOCATION_TRACKING_ENABLED, false)
    }
    
    fun enableLocationTracking(enabled: Boolean) {
        preferences.edit { putBoolean(Constants.KEY_LOCATION_TRACKING_ENABLED, enabled) }
    }
    
    fun getVoiceRecognition(): Boolean {
        return preferences.getBoolean(Constants.KEY_VOICE_SOS_ENABLED, false)
    }
    
    fun enableVoiceRecognition(enabled: Boolean) {
        preferences.edit { putBoolean(Constants.KEY_VOICE_SOS_ENABLED, enabled) }
    }
    
    fun getHealthMonitoring(): Boolean {
        return preferences.getBoolean(Constants.KEY_HEALTH_MONITORING_ENABLED, false)
    }
    
    fun enableHealthMonitoring(enabled: Boolean) {
        preferences.edit { putBoolean(Constants.KEY_HEALTH_MONITORING_ENABLED, enabled) }
    }
    
    fun getFallDetection(): Boolean {
        return preferences.getBoolean(Constants.KEY_FALL_DETECTION_ENABLED, false)
    }
    
    fun enableFallDetection(enabled: Boolean) {
        preferences.edit { putBoolean(Constants.KEY_FALL_DETECTION_ENABLED, enabled) }
    }
    
    // Health Monitoring
    fun getHeartRateThreshold(): Int {
        return preferences.getInt("heart_rate_threshold", 120) // Default 120 BPM
    }
    
    fun setHeartRateThreshold(threshold: Int) {
        preferences.edit { putInt("heart_rate_threshold", threshold) }
    }
    
    fun getHealthCheckIntervalMs(): Long {
        return preferences.getLong("health_check_interval_ms", 60000) // Default 1 minute
    }
    
    fun setHealthCheckIntervalMs(intervalMs: Long) {
        preferences.edit { putLong("health_check_interval_ms", intervalMs) }
    }
    
    // Voice Activation
    fun getVoiceActivationKeyword(): String {
        return preferences.getString("voice_activation_keyword", "help") ?: "help"
    }
    
    fun setVoiceActivationKeyword(keyword: String) {
        preferences.edit { putString("voice_activation_keyword", keyword) }
    }

    // Emergency Contacts
    fun getEmergencyContacts(): List<String> {
        val json = preferences.getString(Constants.KEY_EMERGENCY_CONTACTS, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun setEmergencyContacts(contacts: List<String>) {
        preferences.edit {
            putString(Constants.KEY_EMERGENCY_CONTACTS, gson.toJson(contacts))
        }
    }

    // Last Known Location
    fun getLastKnownLocation(): com.example.urban_safety.data.model.LocationData? {
        val json = preferences.getString(Constants.KEY_LAST_KNOWN_LOCATION, null)
        return json?.let { gson.fromJson(it, com.example.urban_safety.data.model.LocationData::class.java) }
    }

    fun setLastKnownLocation(location: com.example.urban_safety.data.model.LocationData) {
        preferences.edit {
            putString(Constants.KEY_LAST_KNOWN_LOCATION, gson.toJson(location))
        }
    }

    // Clear all preferences
    fun clearAll() {
        preferences.edit().clear().apply()
    }
} 