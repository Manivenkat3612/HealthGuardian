package com.example.urban_safety.util

/**
 * Constants used throughout the application
 */
object Constants {
    // SOS Trigger Types
    const val MANUAL_SOS = "MANUAL_SOS"
    const val AUTO_HEALTH_TRIGGERED = "AUTO_HEALTH_TRIGGERED"
    const val VOICE_ACTIVATED = "VOICE_ACTIVATED"
    const val GEOFENCE_TRIGGERED = "GEOFENCE_TRIGGERED"

    // Shared Preferences Keys
    const val PREF_NAME = "urban_safety_preferences"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_PHONE = "user_phone"
    const val KEY_EMERGENCY_CONTACTS = "emergency_contacts"
    const val KEY_SOS_ACTIVE = "sos_active"
    const val KEY_LAST_KNOWN_LOCATION = "last_known_location"
    const val KEY_HEALTH_MONITORING_ENABLED = "health_monitoring_enabled"
    const val KEY_VOICE_SOS_ENABLED = "voice_sos_enabled"
    const val KEY_GEOFENCE_ENABLED = "geofence_enabled"
    const val KEY_LOCATION_TRACKING_ENABLED = "location_tracking_enabled"
    const val KEY_VOICE_RECOGNITION_ENABLED = "voice_recognition_enabled"

    // Notification Channels
    const val SOS_NOTIFICATION_CHANNEL_ID = "sos_notifications"
    const val HEALTH_NOTIFICATION_CHANNEL_ID = "health_notifications"
    const val LOCATION_NOTIFICATION_CHANNEL_ID = "location_notifications"
    const val GENERAL_NOTIFICATION_CHANNEL_ID = "general_notifications"

    // Request Codes
    const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    const val CAMERA_PERMISSION_REQUEST_CODE = 1002
    const val MICROPHONE_PERMISSION_REQUEST_CODE = 1003
    const val CONTACTS_PERMISSION_REQUEST_CODE = 1004
    const val ACTIVITY_RECOGNITION_REQUEST_CODE = 1005

    // Time Intervals (in milliseconds)
    const val LOCATION_UPDATE_INTERVAL = 30000L // 30 seconds
    const val HEALTH_CHECK_INTERVAL = 60000L // 1 minute
    const val SOS_TIMEOUT = 300000L // 5 minutes
    const val GEOFENCE_LOITERING_DELAY = 300000L // 5 minutes

    // Firebase Collections
    const val USERS_COLLECTION = "users"
    const val INCIDENTS_COLLECTION = "incidents"
    const val EMERGENCY_CONTACTS_COLLECTION = "emergency_contacts"
    const val SAFETY_REPORTS_COLLECTION = "safety_reports"
    const val TRAVEL_CHECKINS_COLLECTION = "travel_checkins"
    const val SOS_REQUESTS_COLLECTION = "sos_requests"
    const val COMMUNITY_HELPERS_COLLECTION = "community_helpers"
    const val HELP_REQUESTS_COLLECTION = "help_requests"
} 