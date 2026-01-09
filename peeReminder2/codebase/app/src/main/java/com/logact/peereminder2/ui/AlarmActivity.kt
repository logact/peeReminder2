package com.logact.peereminder2.ui

import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.logact.peereminder2.data.notification.AlarmService
import com.logact.peereminder2.data.notification.NotificationService
import com.logact.peereminder2.data.storage.StorageService
import com.logact.peereminder2.domain.TimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen alarm activity that displays when a reminder triggers.
 * 
 * This activity:
 * - Shows a full-screen alarm with large text for visibility
 * - Plays alarm sound continuously
 * - Provides an acknowledge button to dismiss the alarm
 * - Handles both regular and retry reminders
 * - Automatically closes when reminder is acknowledged
 * 
 * This activity is launched via full-screen intent from NotificationService
 * when a reminder alarm triggers.
 */
class AlarmActivity : ComponentActivity() {
    
    companion object {
        /**
         * Intent extra key for reminder type (regular or retry)
         */
        const val EXTRA_IS_RETRY = "is_retry"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var timerManager: TimerManager
    private lateinit var notificationService: NotificationService
    private var isInitialized = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        android.util.Log.d("AlarmActivity", "═══════════════════════════════════════")
        android.util.Log.d("AlarmActivity", "onCreate: 🚨 ALARM ACTIVITY CREATED! 🚨")
        android.util.Log.d("AlarmActivity", "onCreate: Activity should be visible now")
        
        // Make activity full-screen and wake up device
        // Do this FIRST before anything else to ensure it appears
        setupFullScreen()
        
        // Initialize services
        val storageService = StorageService.getInstance(this)
        val alarmService = AlarmService(this)
        timerManager = TimerManager(storageService, alarmService)
        notificationService = NotificationService(this)
        
        // Initialize timer manager and wait for it to complete
        scope.launch {
            timerManager.initialize()
            isInitialized = true
            android.util.Log.d("AlarmActivity", "onCreate: TimerManager initialized successfully")
        }
        
        // Get reminder type from intent
        val isRetry = intent.getBooleanExtra(EXTRA_IS_RETRY, false)
        android.util.Log.d("AlarmActivity", "onCreate: isRetry=$isRetry")
        
        // Start playing alarm sound
        startAlarmSound()
        
        // Set up UI
        setContent {
            MaterialTheme {
                AlarmScreen(
                    isRetry = isRetry,
                    onAcknowledge = {
                        acknowledgeAndFinish()
                    },
                    onDismiss = {
                        finish()
                    }
                )
            }
        }
        
        android.util.Log.d("AlarmActivity", "onCreate: ✅ UI setup complete - full-screen alarm should be visible")
        android.util.Log.d("AlarmActivity", "═══════════════════════════════════════")
    }
    
    /**
     * Configure activity to be full-screen and wake up device
     * 
     * This ensures the activity appears even when:
     * - Device is locked
     * - Screen is off
     * - App is in background
     * - Other apps are in foreground
     */
    private fun setupFullScreen() {
        android.util.Log.d("AlarmActivity", "setupFullScreen: Configuring activity for full-screen display")
        
        try {
            // Turn on screen and unlock if locked (Android 8.1+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
                android.util.Log.d("AlarmActivity", "setupFullScreen: Using setShowWhenLocked/setTurnScreenOn (Android 8.1+)")
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
                android.util.Log.d("AlarmActivity", "setupFullScreen: Using window flags (Android < 8.1)")
            }
            
            // Keep screen on while activity is visible
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            
            // Dismiss keyguard if locked (Android 8.1+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
            }
            
            // Additional flags for maximum visibility
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            
            // Make activity appear above everything
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
            }
            
            android.util.Log.d("AlarmActivity", "setupFullScreen: ✅ Full-screen configuration complete")
        } catch (e: Exception) {
            android.util.Log.e("AlarmActivity", "setupFullScreen: ❌ Error configuring full-screen", e)
            e.printStackTrace()
            // Continue anyway - try to show activity even if some flags fail
        }
    }
    
    /**
     * Start playing alarm sound
     */
    private fun startAlarmSound() {
        try {
            // Use alarm sound instead of notification sound for better visibility
            val alarmUri = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_ALARM
            )
            
            // If no alarm sound, fall back to notification sound
            val soundUri = alarmUri ?: android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            
            mediaPlayer = MediaPlayer.create(this, soundUri).apply {
                isLooping = true // Loop the sound until acknowledged
                start()
            }
        } catch (e: Exception) {
            // If sound fails, continue without sound
            e.printStackTrace()
        }
    }
    
    /**
     * Stop alarm sound
     */
    private fun stopAlarmSound() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Acknowledge reminder and finish activity
     */
    private fun acknowledgeAndFinish() {
        scope.launch {
            // Wait for initialization to complete if not already done
            // This ensures we have the latest state from storage
            var waitCount = 0
            while (!isInitialized && waitCount < 50) { // Wait up to 5 seconds (50 * 100ms)
                kotlinx.coroutines.delay(100)
                waitCount++
            }
            
            if (!isInitialized) {
                android.util.Log.w("AlarmActivity", "TimerManager not initialized after waiting, proceeding anyway")
            }
            
            // Reload state from storage to ensure we have the latest state
            // This is important because ReminderReceiver might have updated the state
            timerManager.reloadStateFromStorage()
            
            val currentState = timerManager.getCurrentState()
            android.util.Log.d("AlarmActivity", "acknowledgeAndFinish: Current state before acknowledgment=$currentState")
            
            // Acknowledge the reminder
            timerManager.acknowledgeReminder()
            
            // Verify the alarm was scheduled
            val timerData = timerManager.getCurrentTimerData()
            android.util.Log.d("AlarmActivity", "acknowledgeAndFinish: After acknowledgment - nextReminderTime=${timerData.nextReminderTime}, state=${timerData.currentState}")
            
            // Verify alarm scheduling
            val currentTime = System.currentTimeMillis()
            if (timerData.nextReminderTime > currentTime) {
                android.util.Log.d("AlarmActivity", "acknowledgeAndFinish: ✅ Next reminder scheduled for ${java.util.Date(timerData.nextReminderTime)}")
            } else {
                android.util.Log.w("AlarmActivity", "acknowledgeAndFinish: ⚠️ Next reminder time is in the past or invalid: ${timerData.nextReminderTime}")
            }
            
            // Cancel notification
            val isRetry = intent.getBooleanExtra(EXTRA_IS_RETRY, false)
            notificationService.cancelReminderNotification(isRetry = isRetry)
            
            // Stop sound
            stopAlarmSound()
            
            // Finish activity
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSound()
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent back button from dismissing alarm
        // User must acknowledge or dismiss explicitly
        // Note: This is deprecated but still works for older Android versions
        // For Android 13+, we would use OnBackPressedDispatcher, but for compatibility
        // we keep this for now
    }
}

/**
 * Full-screen alarm UI composable
 */
@Composable
private fun AlarmScreen(
    isRetry: Boolean,
    onAcknowledge: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = if (isRetry) {
        "Time to Pee! (Reminder)"
    } else {
        "Time to Pee!"
    }
    
    val message = if (isRetry) {
        "It's been 15 minutes since your last reminder.\nDon't forget to pee!"
    } else {
        "It's been 2 hours since your last reminder.\nTime to pee!"
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                fontSize = 48.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center,
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Acknowledge Button
            Button(
                onClick = onAcknowledge,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "I've Peed",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Dismiss Button (smaller, less prominent)
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    text = "Dismiss",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

