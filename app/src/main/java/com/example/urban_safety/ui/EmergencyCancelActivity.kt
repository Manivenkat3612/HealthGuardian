package com.example.urban_safety.ui

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.urban_safety.R
import com.example.urban_safety.services.FallDetectionService

/**
 * A fullscreen activity that shows a 5-second countdown timer with a cancel button
 * for emergency alerts. Used by the fall detection service to give users time to cancel
 * false alarms before notifying emergency contacts.
 */
class EmergencyCancelActivity : AppCompatActivity() {

    private lateinit var timerTextView: TextView
    private lateinit var cancelButton: Button
    private lateinit var subtitleTextView: TextView
    private var countDownTimer: CountDownTimer? = null
    private var secondsRemaining = 5
    
    private val TAG = "EmergencyCancelActivity"

    companion object {
        const val EXTRA_EMERGENCY_TYPE = "emergency_type"
        const val TYPE_FALL_DETECTION = "fall_detection"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_cancel)

        // Make sure this activity shows on top of lock screen and turns screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or  
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        // Hide system UI for full immersion
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        // Initialize views
        timerTextView = findViewById(R.id.timerTextView)
        cancelButton = findViewById(R.id.cancelButton)
        subtitleTextView = findViewById(R.id.subtitleTextView)

        // Set up the cancel button
        cancelButton.setOnClickListener {
            cancelEmergency()
        }

        // Get emergency type from intent
        val emergencyType = intent.getStringExtra(EXTRA_EMERGENCY_TYPE) ?: TYPE_FALL_DETECTION
        
        // Set appropriate subtitle based on emergency type
        if (emergencyType == TYPE_FALL_DETECTION) {
            subtitleTextView.text = "Preparing to notify emergency contacts"
        }

        // Start countdown timer
        startCountdownTimer()
    }

    private fun startCountdownTimer() {
        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = (millisUntilFinished / 1000).toInt() + 1
                timerTextView.text = "${secondsRemaining}s"
            }

            override fun onFinish() {
                // Time's up - proceed with emergency notification
                proceedWithEmergency()
            }
        }.start()
    }

    private fun cancelEmergency() {
        Log.d(TAG, "Cancel button clicked - cancelling emergency alert")
        
        // Cancel the timer
        countDownTimer?.cancel()
        
        try {
            // Get the emergency type from the intent
            val emergencyType = intent.getStringExtra(EXTRA_EMERGENCY_TYPE)
            Log.d(TAG, "Emergency type: $emergencyType")
            
            // Create cancel intent based on emergency type
            val cancelIntent = when (emergencyType) {
                TYPE_FALL_DETECTION -> {
                    Log.d(TAG, "Creating Fall Detection cancel intent")
                    FallDetectionService.getCancelIntent(this)
                }
                else -> {
                    Log.d(TAG, "Unknown emergency type: $emergencyType, using default cancel intent")
                    null
                }
            }
            
            // Send the cancel intent to the service
            if (cancelIntent != null) {
                Log.d(TAG, "Sending cancel intent to service: ${cancelIntent.action}")
                startService(cancelIntent)
                Log.d(TAG, "Cancel intent sent successfully")
            } else {
                Log.e(TAG, "Cancel intent is null, could not send")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending cancel intent: ${e.message}", e)
        }
        
        // Close the activity
        Log.d(TAG, "Finishing EmergencyCancelActivity")
        finish()
    }

    private fun proceedWithEmergency() {
        // Nothing to do here - the service will handle notification after timeout
        Log.d(TAG, "Countdown finished, proceeding with emergency")
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
} 