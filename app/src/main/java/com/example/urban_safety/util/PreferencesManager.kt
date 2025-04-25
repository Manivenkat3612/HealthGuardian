package com.example.urban_safety.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PreferencesManager(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        Constants.PREF_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    // User Preferences
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

    // Feature Toggles
    var isSosActive: Boolean
        get() = preferences.getBoolean(Constants.KEY_SOS_ACTIVE, false)
        set(value) = preferences.edit { putBoolean(Constants.KEY_SOS_ACTIVE, value) }

    var isHealthMonitoringEnabled: Boolean
        get() = preferences.getBoolean(Constants.KEY_HEALTH_MONITORING_ENABLED, true)
        set(value) = preferences.edit { putBoolean(Constants.KEY_HEALTH_MONITORING_ENABLED, value) }

    var isVoiceSosEnabled: Boolean
        get() = preferences.getBoolean(Constants.KEY_VOICE_SOS_ENABLED, true)
        set(value) = preferences.edit { putBoolean(Constants.KEY_VOICE_SOS_ENABLED, value) }

    var isGeofenceEnabled: Boolean
        get() = preferences.getBoolean(Constants.KEY_GEOFENCE_ENABLED, true)
        set(value) = preferences.edit { putBoolean(Constants.KEY_GEOFENCE_ENABLED, value) }

    // Location tracking
    fun getLocationTrackingEnabled(): Boolean {
        return preferences.getBoolean(Constants.KEY_LOCATION_TRACKING_ENABLED, true)
    }
    
    fun setLocationTrackingEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(Constants.KEY_LOCATION_TRACKING_ENABLED, enabled) }
    }
    
    // Voice recognition
    fun getVoiceRecognitionEnabled(): Boolean {
        return preferences.getBoolean(Constants.KEY_VOICE_RECOGNITION_ENABLED, true)
    }
    
    fun setVoiceRecognitionEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(Constants.KEY_VOICE_RECOGNITION_ENABLED, enabled) }
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