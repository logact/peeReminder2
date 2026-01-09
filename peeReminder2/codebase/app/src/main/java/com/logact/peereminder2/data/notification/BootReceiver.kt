package com.logact.peereminder2.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.logact.peereminder2.data.model.TimerState
import com.logact.peereminder2.data.storage.StorageService
import com.logact.peereminder2.domain.TimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BootReceiver handles device boot completion and reschedules alarms.
 * 
 * When the device reboots, all scheduled alarms are cleared by the system.
 * This receiver is triggered after boot completion to:
 * - Load saved timer state from storage
 * - Recalculate next reminder time based on saved state
 * - Reschedule alarms using AlarmService
 * 
 * This ensures that reminders continue to work correctly even after device reboots.
 * 
 * Note: This receiver must be registered in AndroidManifest.xml with
 * RECEIVE_BOOT_COMPLETED permission and BOOT_COMPLETED intent filter.
 */
class BootReceiver : BroadcastReceiver() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            handleBootCompleted(context)
        }
    }
    
    /**
     * Handles boot completion by rescheduling alarms based on saved timer state.
     * 
     * This method:
     * 1. Initializes StorageService, AlarmService, and TimerManager
     * 2. Loads saved timer state
     * 3. Recalculates next reminder time
     * 4. Reschedules alarms if needed (if not in sleep mode)
     */
    private fun handleBootCompleted(context: Context) {
        scope.launch {
            // Initialize services
            val storageService = StorageService.getInstance(context)
            val alarmService = AlarmService(context)
            val timerManager = TimerManager(storageService, alarmService)
            
            // Initialize TimerManager (loads saved state)
            timerManager.initialize()
            
            // Get current timer state
            val timerData = timerManager.getCurrentTimerData()
            
            // Load settings to check sleep time range in real-time
            val settings = storageService.loadAppSettings()
            
            // Only reschedule alarms if reminders should not be paused
            // This checks both manual pause AND sleep time range in real-time
            if (!timerManager.shouldPauseReminders(settings) && timerData.nextReminderTime > 0L) {
                val currentTime = System.currentTimeMillis()
                
                // Schedule reminder alarm if next reminder time is in the future
                if (timerData.nextReminderTime > currentTime) {
                    alarmService.scheduleReminderAlarm(timerData.nextReminderTime)
                } else {
                    // If next reminder time is in the past, recalculate and schedule
                    timerManager.recalculateNextReminderTime()
                    val updatedData = timerManager.getCurrentTimerData()
                    if (updatedData.nextReminderTime > currentTime) {
                        alarmService.scheduleReminderAlarm(updatedData.nextReminderTime)
                    }
                }
                
                // Schedule retry alarm if reminder is active and retry time is in the future
                if (timerData.currentState == TimerState.REMINDER_ACTIVE) {
                    val retryTime = timerManager.calculateRetryTime()
                    if (retryTime > currentTime) {
                        alarmService.scheduleRetryAlarm(retryTime)
                    }
                }
            }
        }
    }
}

