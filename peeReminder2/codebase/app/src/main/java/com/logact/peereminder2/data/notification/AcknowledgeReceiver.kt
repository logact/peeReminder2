package com.logact.peereminder2.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.logact.peereminder2.data.notification.AlarmService
import com.logact.peereminder2.data.storage.StorageService
import com.logact.peereminder2.domain.TimerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles acknowledgment action from notification.
 * 
 * When user clicks the "I've Peed" button in the notification, this receiver:
 * - Acknowledges the reminder via TimerManager
 * - Cancels the notification
 * - Resets the 2-hour timer from acknowledgment time
 * 
 * This receiver is triggered by a PendingIntent attached to the notification action button.
 */
class AcknowledgeReceiver : BroadcastReceiver() {
    
    companion object {
        /**
         * Action string for acknowledgment
         */
        const val ACTION_ACKNOWLEDGE = "com.logact.peereminder2.ACTION_ACKNOWLEDGE"
        
        /**
         * Intent extra key for reminder type (regular or retry)
         */
        const val EXTRA_IS_RETRY = "is_retry"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ACKNOWLEDGE) {
            handleAcknowledge(context, intent)
        }
    }
    
    /**
     * Handle acknowledgment action
     */
    private fun handleAcknowledge(context: Context, intent: Intent) {
        scope.launch {
            // Initialize services
            val storageService = StorageService.getInstance(context)
            val alarmService = AlarmService(context)
            val timerManager = TimerManager(storageService, alarmService)
            timerManager.initialize()
            
            // Get reminder type from intent
            val isRetry = intent.getBooleanExtra(EXTRA_IS_RETRY, false)
            
            // Acknowledge the reminder (this will reset the timer and schedule the next alarm)
            timerManager.acknowledgeReminder()
            
            // Cancel the notification
            val notificationService = NotificationService(context)
            notificationService.cancelReminderNotification(isRetry = isRetry)
            
            android.util.Log.d("AcknowledgeReceiver", "handleAcknowledge: Reminder acknowledged, timer reset, next alarm scheduled")
        }
    }
}

