package com.example.urban_safety.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.urban_safety.MainActivity
import com.example.urban_safety.R
import com.example.urban_safety.HealthGuardianApp
import com.example.urban_safety.data.model.IncidentType
import com.example.urban_safety.data.model.User
import com.example.urban_safety.data.repository.LocationRepository
import com.example.urban_safety.data.repository.SafetyRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject
import com.example.urban_safety.util.Constants
import com.example.urban_safety.util.PreferencesManager
import android.content.Context
import android.os.Build
import android.app.NotificationManager
import android.graphics.Color
import android.app.NotificationChannel

/**
 * Service for voice-activated SOS
 */
@AndroidEntryPoint
class VoiceRecognitionService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    
    private lateinit var speechRecognizer: SpeechRecognizer
    
    @Inject
    lateinit var safetyRepository: SafetyRepository
    
    @Inject
    lateinit var locationRepository: LocationRepository
    
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    
    private var isListening = false
    private var userPreferences: User? = null
    private var activationKeyword = "help" // Default keyword
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    
    companion object {
        const val NOTIFICATION_ID = 98765
        
        // Service actions
        const val ACTION_START_LISTENING = "com.example.urban_safety.START_VOICE_RECOGNITION"
        const val ACTION_STOP_LISTENING = "com.example.urban_safety.STOP_VOICE_RECOGNITION"
        
        // Extra parameters
        const val EXTRA_ACTIVATION_KEYWORD = "activation_keyword"
        
        // Required permissions
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Check if we have all required permissions
        if (!hasRequiredPermissions()) {
            stopSelf()
            return
        }
        
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        // Set up speech recognizer
        speechRecognizer.setRecognitionListener(createRecognitionListener())
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_START_LISTENING -> {
                    // Check permissions again before starting
                    if (!hasRequiredPermissions()) {
                        stopSelf()
                        return START_NOT_STICKY
                    }
                    
                    val keyword = it.getStringExtra(EXTRA_ACTIVATION_KEYWORD)
                    if (!keyword.isNullOrBlank()) {
                        activationKeyword = keyword
                    }
                    startVoiceRecognition()
                }
                ACTION_STOP_LISTENING -> {
                    stopVoiceRecognition()
                    stopSelf()
                }
            }
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // No binding supported
    }
    
    override fun onDestroy() {
        stopVoiceRecognition()
        speechRecognizer.destroy()
        super.onDestroy()
    }
    
    /**
     * Start voice recognition
     */
    private fun startVoiceRecognition() {
        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Check if voice activation is enabled
        if (preferencesManager.getVoiceRecognition()) {
            // Update activation keyword if specified in user preferences
            val keyword = "help" // Default fallback
            activationKeyword = keyword
            startListening()
        } else {
            stopSelf()
        }
    }
    
    /**
     * Stop voice recognition
     */
    private fun stopVoiceRecognition() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
    
    /**
     * Start speech recognition listening
     */
    private fun startListening() {
        if (!isListening) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                }
                
                speechRecognizer.startListening(intent)
                isListening = true
            } catch (e: SecurityException) {
                // Handle permission denied
                e.printStackTrace()
                stopSelf()
            }
        }
    }
    
    /**
     * Create speech recognition listener
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            
            override fun onBeginningOfSpeech() {}
            
            override fun onRmsChanged(rmsdB: Float) {}
            
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                // Start listening again after a short delay
                serviceScope.launch {
                    kotlinx.coroutines.delay(1000)
                    startListening()
                }
            }
            
            override fun onError(error: Int) {
                // Restart listening after error
                isListening = false
                serviceScope.launch {
                    kotlinx.coroutines.delay(1000)
                    startListening()
                }
            }
            
            override fun onResults(results: Bundle?) {
                processVoiceResults(results)
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Process partial results for faster response
                processVoiceResults(partialResults)
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }
    
    /**
     * Process voice recognition results
     */
    private fun processVoiceResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        
        if (matches != null && matches.isNotEmpty()) {
            for (result in matches) {
                // Check if the result contains the activation keyword
                if (result.lowercase(Locale.getDefault()).contains(activationKeyword.lowercase(Locale.getDefault()))) {
                    // Trigger SOS
                    triggerEmergencyAlert()
                    break
                }
            }
        }
    }
    
    /**
     * Stop speech recognition
     */
    private fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer.stopListening()
                isListening = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Trigger emergency alert when voice activation detected
     */
    private fun triggerEmergencyAlert() {
        // Only trigger once
        stopListening()
        
        // Create notification sound and vibration
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM)
        val originalVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_ALARM)
        
        // Set volume to max for alert
        audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, maxVolume, 0)
        
        // Vibrate the device
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
        
        // Vibration pattern for SOS: ... --- ...
        val pattern = longArrayOf(
            0, 200, 200, 200, 200, 200,     // ...
            600,                            // pause
            600, 200, 600, 200, 600,        // ---
            600,                            // pause
            200, 200, 200, 200, 200         // ...
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
        
        // Play alert sound
        try {
            val notification = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, notification)
            ringtone.play()
            
            // Stop the ringtone after 3 seconds
            android.os.Handler(mainLooper).postDelayed({
                ringtone.stop()
                // Restore original volume
                audioManager.setStreamVolume(android.media.AudioManager.STREAM_ALARM, originalVolume, 0)
            }, 3000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Create and launch notification
        val notificationBuilder = NotificationCompat.Builder(this, HealthGuardianApp.EMERGENCY_CHANNEL_ID)
            .setContentTitle("SOS Emergency Activated")
            .setContentText("Emergency triggered by voice activation")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(pattern)
            .setLights(Color.RED, 1000, 500)
            .setAutoCancel(true)
        
        // Intent to open the app
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SOS_TRIGGERED", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.setContentIntent(pendingIntent)
        
        // Show notification
        notificationManager.notify(123, notificationBuilder.build())
        
        // Trigger SOS in repository
        serviceScope.launch {
            try {
                // Get last known location
                val locationResult = locationRepository.getLastLocation()
                val locationData = locationResult.getOrNull()
                
                // Create emergency incident
                safetyRepository.createSafetyIncident(
                    type = IncidentType.VOICE_ACTIVATED,
                    locationData = locationData,
                    description = "Voice-activated emergency: " + Constants.VOICE_ACTIVATED
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        val notificationChannelId = "voice_recognition_channel"
        
        // Create the notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                notificationChannelId,
                "Voice Recognition Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationChannel.description = "Voice recognition service notification channel"
            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)
        }
        
        // Create the notification
        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("HealthGuardianApp Voice Recognition")
            .setContentText("Listening for voice commands...")
            .setSmallIcon(R.drawable.ic_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        
        // Create PendingIntent for notification click
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        notificationBuilder.setContentIntent(pendingIntent)
        
        return notificationBuilder.build()
    }
} 