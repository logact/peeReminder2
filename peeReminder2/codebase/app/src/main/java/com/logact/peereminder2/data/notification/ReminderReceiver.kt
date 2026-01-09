package com.logact.peereminder2.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver.PendingResult
import com.logact.peereminder2.data.storage.StorageService
import com.logact.peereminder2.domain.TimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ReminderReceiver handles alarm triggers from AlarmManager.
 * 
 * This BroadcastReceiver is registered to receive alarm intents scheduled by AlarmManager.
 * When an alarm fires (scheduled for reminder time), this receiver:
 * - Shows the appropriate notification (regular or retry)
 * - Updates TimerManager state
 * - Handles retry logic
 * 
 * This is the foundation for Hour 5 (AlarmManager Integration).
 * For Hour 4, this receiver is set up but will be fully integrated in Hour 5.
 * 
 * Note: This receiver needs to be registered in AndroidManifest.xml
 */
class ReminderReceiver : BroadcastReceiver() {
    
    companion object {
        /**
         * Action string for regular reminder alarms
         */
        const val ACTION_REMINDER = "com.logact.peereminder2.ACTION_REMINDER"
        
        /**
         * Action string for retry reminder alarms
         */
        const val ACTION_RETRY = "com.logact.peereminder2.ACTION_RETRY"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.d("ReminderReceiver", "═══════════════════════════════════════")
        android.util.Log.d("ReminderReceiver", "🔔🔔🔔 REMINDER RECEIVER TRIGGERED! 🔔🔔🔔")
        android.util.Log.d("ReminderReceiver", "onReceive: ✅ BroadcastReceiver triggered!")
        android.util.Log.d("ReminderReceiver", "onReceive: Action=${intent.action}")
        android.util.Log.d("ReminderReceiver", "onReceive: Component=${intent.component}")
        android.util.Log.d("ReminderReceiver", "onReceive: Extras=${intent.extras}")
        android.util.Log.d("ReminderReceiver", "onReceive: Current time=${System.currentTimeMillis()} (${java.util.Date()})")
        
        // Use goAsync() to keep the BroadcastReceiver alive while we do async work
        val pendingResult = goAsync()
        
        when (intent.action) {
            ACTION_REMINDER -> {
                android.util.Log.d("ReminderReceiver", "onReceive: ✅ Handling ACTION_REMINDER (regular reminder)")
                handleReminderAlarm(context, pendingResult)
            }
            ACTION_RETRY -> {
                android.util.Log.d("ReminderReceiver", "onReceive: ✅ Handling ACTION_RETRY (retry reminder)")
                handleRetryAlarm(context, pendingResult)
            }
            else -> {
                android.util.Log.w("ReminderReceiver", "onReceive: ⚠️ Unknown action=${intent.action}")
                pendingResult.finish()
            }
        }
    }
    
    /**
     * Handles regular reminder alarm trigger.
     * 
     * When a reminder alarm fires, this method:
     * - Validates that the alarm is still valid (not stale from interval change)
     * - Checks if reminder is due and not in sleep mode
     * - Shows reminder notification
     * - Updates TimerManager state to REMINDER_ACTIVE
     * - Launches AlarmActivity for full-screen display
     * 
     * **Stale Alarm Protection:**
     * If the interval was changed, old alarms may still fire. This method validates:
     * 1. The alarm time matches the expected nextReminderTime (within tolerance)
     * 2. The timer state is REMINDER_PENDING (not already active)
     * 3. Sleep mode is not active
     * 
     * Stale alarms are silently ignored to prevent duplicate notifications.
     */
    private fun handleReminderAlarm(context: Context, pendingResult: PendingResult) {
        scope.launch {
            android.util.Log.d("ReminderReceiver", "═══════════════════════════════════════")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: ⏰ ALARM RECEIVED!")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Starting validation checks")
            
            // Initialize services
            val storageService = StorageService.getInstance(context)
            val alarmService = AlarmService(context) // Add AlarmService for retry scheduling
            val timerManager = TimerManager(storageService, alarmService) // Pass AlarmService
            timerManager.initialize()
            
            val currentTime = System.currentTimeMillis()
            val timerData = timerManager.getCurrentTimerData()
            
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Timer initialized")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: currentState=${timerData.currentState}")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: nextReminderTime=${timerData.nextReminderTime} (${java.util.Date(timerData.nextReminderTime)})")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: currentTime=$currentTime (${java.util.Date(currentTime)})")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: timeDifference=${currentTime - timerData.nextReminderTime}ms")
            
            // Validation 1: Always check in real-time if reminders should be paused
            // This checks both manual pause AND sleep time range
            val settings = storageService.loadAppSettings()
            val shouldPause = timerManager.shouldPauseReminders(settings)
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 1 - shouldPauseReminders=$shouldPause")
            if (shouldPause) {
                // Reminders should be paused (either manually or in sleep time range)
                // Always check in real-time, don't rely on stored state
                android.util.Log.w("ReminderReceiver", "handleReminderAlarm: BLOCKED by Validation 1 - Reminders are paused")
                pendingResult.finish()
                return@launch
            }
            
            // Validation 2: Check if reminder is due (current time >= nextReminderTime)
            // IMPORTANT: If an alarm fires, it means it was scheduled for that time (or close to it).
            // We should process it even if the stored nextReminderTime doesn't exactly match,
            // because state might have been updated by MainActivity's periodic reloads.
            // Only block if the alarm is way too early (more than 5 minutes early).
            val isDue = timerManager.isReminderDue()
            val timeDiffSigned = currentTime - timerData.nextReminderTime // Signed difference (negative if early, positive if late)
            val isWayTooEarly = timeDiffSigned < -5 * 60 * 1000L // More than 5 minutes early
            
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 2 - isReminderDue=$isDue")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 2 - nextReminderTime=${timerData.nextReminderTime} (${java.util.Date(timerData.nextReminderTime)})")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 2 - currentTime=$currentTime (${java.util.Date(currentTime)})")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 2 - timeDiffSigned=${timeDiffSigned}ms (${timeDiffSigned / 1000 / 60} minutes)")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 2 - isWayTooEarly=$isWayTooEarly")
            
            if (!isDue && isWayTooEarly) {
                // Alarm fired way too early - this is likely a stale alarm
                android.util.Log.w("ReminderReceiver", "handleReminderAlarm: BLOCKED by Validation 2 - Alarm fired ${-timeDiffSigned / 1000 / 60} minutes too early (stale alarm)")
                pendingResult.finish()
                return@launch
            } else if (!isDue) {
                // Alarm fired slightly early or stored state doesn't match - process it anyway
                // This handles cases where MainActivity's periodic reloads updated the stored state
                android.util.Log.w("ReminderReceiver", "handleReminderAlarm: ⚠️ Validation 2 - Alarm fired ${-timeDiffSigned / 1000 / 60} minutes early, but processing anyway")
                android.util.Log.w("ReminderReceiver", "handleReminderAlarm: ⚠️ This might be due to state synchronization - stored nextReminderTime may not match actual alarm time")
            } else {
                android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 2 - ✅ Reminder is due (or past due)")
            }
            
            // Validation 3: Check timer state - should be REMINDER_PENDING
            // If state is already REMINDER_ACTIVE or RETRY_ACTIVE, this alarm might be a duplicate
            // But if the reminder is actually due, we should still process it (might be a race condition)
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 3 - currentState=${timerData.currentState}, expected=REMINDER_PENDING")
            
            val isStateValid = timerData.currentState == com.logact.peereminder2.data.model.TimerState.REMINDER_PENDING
            val isReminderActuallyDue = currentTime >= timerData.nextReminderTime
            
            if (!isStateValid) {
                if (isReminderActuallyDue) {
                    // State is not PENDING but reminder is actually due - might be a race condition
                    // Process it anyway to ensure user gets the reminder
                    android.util.Log.w("ReminderReceiver", "handleReminderAlarm: ⚠️ Validation 3 - State is ${timerData.currentState} (not REMINDER_PENDING)")
                    android.util.Log.w("ReminderReceiver", "handleReminderAlarm: ⚠️ But reminder is actually due - processing anyway (possible race condition)")
                } else {
                    // State is not PENDING and reminder is not due - this is likely a stale alarm
                    android.util.Log.w("ReminderReceiver", "handleReminderAlarm: BLOCKED by Validation 3 - State is ${timerData.currentState}, not REMINDER_PENDING, and reminder not due")
                    pendingResult.finish()
                    return@launch
                }
            } else {
                android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 3 - ✅ State is REMINDER_PENDING (valid)")
            }
            
            // Validation 4: Verify alarm time is close to expected nextReminderTime
            // Allow tolerance of 10 minutes (600000 ms) for system delays and clock drift
            // Increased tolerance to handle cases where alarm fires slightly early/late
            val timeDifference = kotlin.math.abs(currentTime - timerData.nextReminderTime)
            val toleranceMs = 10 * 60 * 1000L // 10 minutes tolerance (increased from 5 minutes)
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Validation 4 - timeDifference=${timeDifference}ms (${timeDifference / 1000 / 60} minutes), tolerance=${toleranceMs}ms (${toleranceMs / 1000 / 60} minutes)")
            
            // If alarm fires before the scheduled time (more than 1 minute early), it might be stale
            // But if it fires after the scheduled time (even hours later), it's still valid
            val isEarly = currentTime < timerData.nextReminderTime
            val isTooEarly = isEarly && timeDifference > 1 * 60 * 1000L // More than 1 minute early
            
            if (isTooEarly) {
                android.util.Log.w("ReminderReceiver", "handleReminderAlarm: BLOCKED by Validation 4 - Alarm fired too early (${timeDifference}ms before scheduled time)")
                android.util.Log.w("ReminderReceiver", "handleReminderAlarm: This might be a stale alarm, ignoring it")
                pendingResult.finish()
                return@launch
            }
            
            // If alarm fires way too late (more than tolerance), log warning but still process it
            // This handles cases where device was off or in deep sleep
            if (timeDifference > toleranceMs && !isEarly) {
                android.util.Log.w("ReminderReceiver", "handleReminderAlarm: ⚠️ Alarm fired ${timeDifference / 1000 / 60} minutes late (beyond ${toleranceMs / 1000 / 60} minute tolerance)")
                android.util.Log.w("ReminderReceiver", "handleReminderAlarm: Processing anyway - device might have been in deep sleep")
            }
            
            // All validations passed - this is a valid alarm
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: ✅✅✅ ALL VALIDATIONS PASSED ✅✅✅")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Proceeding to mark reminder as active and show full-screen alarm")
            
            // Mark reminder as active
            timerManager.markReminderActive()
            
            // Verify state was updated
            val updatedData = timerManager.getCurrentTimerData()
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: markReminderActive() called")
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: New state=${updatedData.currentState} (should be REMINDER_ACTIVE)")
            
            if (updatedData.currentState != com.logact.peereminder2.data.model.TimerState.REMINDER_ACTIVE) {
                android.util.Log.e("ReminderReceiver", "handleReminderAlarm: ❌ ERROR - State is ${updatedData.currentState}, expected REMINDER_ACTIVE")
            }
            
            // Show notification FIRST (before launching activity)
            // IMPORTANT: Create notification channel before showing notification
            // The channel must exist before notifications can be displayed (Android 8.0+)
            val notificationService = NotificationService(context)
            notificationService.createNotificationChannel() // Ensure channel exists
            
            // Show notification and verify it was shown
            try {
                notificationService.showReminderNotification(isRetry = false)
                android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Notification shown successfully")
            } catch (e: Exception) {
                android.util.Log.e("ReminderReceiver", "handleReminderAlarm: Failed to show notification", e)
                // Continue anyway - try to launch activity even if notification fails
            }
            
            // Launch AlarmActivity immediately to show full-screen reminder
            // This ensures the full-screen alarm appears even when device is unlocked
            // Do this on the main thread to ensure it happens immediately
            withContext(Dispatchers.Main) {
                try {
                    android.util.Log.d("ReminderReceiver", "handleReminderAlarm: 🚀 Launching AlarmActivity for full-screen reminder...")
                    launchAlarmActivity(context, isRetry = false)
                    android.util.Log.d("ReminderReceiver", "handleReminderAlarm: ✅ AlarmActivity launch attempted successfully")
                } catch (e: Exception) {
                    android.util.Log.e("ReminderReceiver", "handleReminderAlarm: ❌ FAILED to launch AlarmActivity", e)
                    e.printStackTrace()
                    // Even if activity launch fails, we've marked the reminder as active
                    // The notification should still be visible
                }
            }
            
            // Finish the pending result
            android.util.Log.d("ReminderReceiver", "handleReminderAlarm: Finishing pending result")
            pendingResult.finish()
            android.util.Log.d("ReminderReceiver", "═══════════════════════════════════════")
        }
    }
    
    /**
     * Handles retry reminder alarm trigger.
     * 
     * When a retry alarm fires, this method:
     * - Validates that the retry is still valid (not stale)
     * - Checks if retry is due and not in sleep mode
     * - Shows retry notification
     * - Updates TimerManager state to RETRY_ACTIVE
     * - Launches AlarmActivity for full-screen display
     * 
     * **Stale Alarm Protection:**
     * Retry alarms are based on the original reminder time, not the interval.
     * However, if the reminder was already acknowledged, the retry should be ignored.
     * 
     * Stale retry alarms are silently ignored to prevent duplicate notifications.
     */
    private fun handleRetryAlarm(context: Context, pendingResult: PendingResult) {
        scope.launch {
            android.util.Log.d("ReminderReceiver", "═══════════════════════════════════════")
            android.util.Log.d("ReminderReceiver", "handleRetryAlarm: ⏰ RETRY ALARM RECEIVED!")
            
            // Initialize services
            val storageService = StorageService.getInstance(context)
            val alarmService = AlarmService(context) // Add AlarmService
            val timerManager = TimerManager(storageService, alarmService) // Pass AlarmService
            timerManager.initialize()
            
            val timerData = timerManager.getCurrentTimerData()
            
            // Validation 1: Always check in real-time if reminders should be paused
            // This checks both manual pause AND sleep time range
            val settings = storageService.loadAppSettings()
            if (timerManager.shouldPauseReminders(settings)) {
                // Reminders should be paused (either manually or in sleep time range)
                // Always check in real-time, don't rely on stored state
                pendingResult.finish()
                return@launch
            }
            
            // Validation 2: Check if retry is due
            if (!timerManager.isRetryDue()) {
                // Retry is not due - this might be a stale alarm
                // Silently ignore it
                pendingResult.finish()
                return@launch
            }
            
            // Validation 3: Check timer state - should be REMINDER_ACTIVE
            // If state is not REMINDER_ACTIVE, the reminder was already acknowledged
            // and this retry alarm is stale
            if (timerData.currentState != com.logact.peereminder2.data.model.TimerState.REMINDER_ACTIVE) {
                // Timer is not in REMINDER_ACTIVE state - retry is stale or no longer needed
                // Silently ignore it
                pendingResult.finish()
                return@launch
            }
            
            // All validations passed - this is a valid retry alarm
            // Mark retry as active
            timerManager.markRetryActive()
            
            // Show retry notification FIRST (before launching activity)
            // IMPORTANT: Create notification channel before showing notification
            // The channel must exist before notifications can be displayed (Android 8.0+)
            val notificationService = NotificationService(context)
            notificationService.createNotificationChannel() // Ensure channel exists
            
            // Show notification and verify it was shown
            try {
                notificationService.showReminderNotification(isRetry = true)
                android.util.Log.d("ReminderReceiver", "handleRetryAlarm: Retry notification shown successfully")
            } catch (e: Exception) {
                android.util.Log.e("ReminderReceiver", "handleRetryAlarm: Failed to show retry notification", e)
                // Continue anyway - try to launch activity even if notification fails
            }
            
            // Launch AlarmActivity immediately to show full-screen retry reminder
            // This ensures the full-screen alarm appears even when device is unlocked
            // Do this on the main thread to ensure it happens immediately
            withContext(Dispatchers.Main) {
                try {
                    launchAlarmActivity(context, isRetry = true)
                    android.util.Log.d("ReminderReceiver", "handleRetryAlarm: AlarmActivity launch attempted")
                } catch (e: Exception) {
                    android.util.Log.e("ReminderReceiver", "handleRetryAlarm: Failed to launch AlarmActivity", e)
                    e.printStackTrace()
                }
            }
            
            // Finish the pending result
            pendingResult.finish()
        }
    }
    
    /**
     * Launches AlarmActivity to display the full-screen reminder.
     * 
     * This method launches the activity immediately when the reminder triggers,
     * ensuring the full-screen alarm appears even when the device is locked or the app is in background.
     * 
     * Note: This must be called from the main thread (Dispatchers.Main).
     * 
     * @param context The context to launch the activity from
     * @param isRetry If true, indicates this is a retry reminder (15-minute reminder).
     *                If false, indicates this is a regular 2-hour reminder.
     */
    private fun launchAlarmActivity(context: Context, isRetry: Boolean) {
        try {
            android.util.Log.d("ReminderReceiver", "launchAlarmActivity: Preparing to launch AlarmActivity (isRetry=$isRetry)")
            
            val intent = Intent(context, com.logact.peereminder2.ui.AlarmActivity::class.java).apply {
                putExtra(com.logact.peereminder2.ui.AlarmActivity.EXTRA_IS_RETRY, isRetry)
                
                // Critical flags to ensure activity appears in all conditions:
                // - NEW_TASK: Required when launching from BroadcastReceiver
                // - CLEAR_TOP: Clear any existing instances
                // - SINGLE_TOP: Don't create multiple instances
                // - SHOW_WHEN_LOCKED: Show even when device is locked (Android 8.0+)
                // - DISMISS_KEYGUARD: Dismiss keyguard if locked
                // - TURN_SCREEN_ON: Turn screen on if off
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                
                // For Android 8.0+ (API 26+), add flags to show over lock screen
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            
            android.util.Log.d("ReminderReceiver", "launchAlarmActivity: Intent created with flags=${intent.flags}")
            android.util.Log.d("ReminderReceiver", "launchAlarmActivity: Starting activity...")
            
            context.startActivity(intent)
            
            android.util.Log.d("ReminderReceiver", "launchAlarmActivity: ✅ AlarmActivity launch command sent successfully (isRetry=$isRetry)")
            android.util.Log.d("ReminderReceiver", "launchAlarmActivity: Activity should appear now - check screen")
            
        } catch (e: SecurityException) {
            android.util.Log.e("ReminderReceiver", "launchAlarmActivity: ❌ SecurityException - ${e.message}", e)
            e.printStackTrace()
        } catch (e: Exception) {
            android.util.Log.e("ReminderReceiver", "launchAlarmActivity: ❌ Exception - ${e.message}", e)
            e.printStackTrace()
        }
    }
}

