package com.logact.peereminder2.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * AlarmService handles scheduling and cancellation of exact alarms using AlarmManager.
 * 
 * This service is responsible for:
 * - Scheduling exact alarms for 2-hour reminders using setExactAndAllowWhileIdle()
 * - Scheduling exact alarms for 15-minute retry reminders
 * - Canceling alarms when needed (sleep mode, acknowledgment, etc.)
 * - Handling SCHEDULE_EXACT_ALARM permission checks (Android 12+)
 * 
 * This is the critical component that ensures reminders trigger reliably even when
 * the app is closed or the device is in deep sleep mode.
 * 
 * For Android 11+ (API 30+), we use setExactAndAllowWhileIdle() which provides
 * the best reliability for exact alarms.
 */
class AlarmService(private val context: Context) {
    
    companion object {
        /**
         * Request code for reminder alarm PendingIntent
         */
        private const val REMINDER_ALARM_REQUEST_CODE = 1001
        
        /**
         * Request code for retry alarm PendingIntent
         */
        private const val RETRY_ALARM_REQUEST_CODE = 1002
    }
    
    private val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * Schedules an exact alarm for the 2-hour reminder at the specified trigger time.
     * 
     * Uses setExactAndAllowWhileIdle() for maximum reliability (Android 6.0+).
     * This method will wake the device from sleep if needed.
     * 
     * @param triggerTime The exact time (in milliseconds since epoch) when the alarm should trigger
     * @return true if alarm was scheduled successfully, false otherwise (e.g., permission denied)
     */
    fun scheduleReminderAlarm(triggerTime: Long): Boolean {
        android.util.Log.d("AlarmService", "scheduleReminderAlarm: Attempting to schedule alarm for triggerTime=$triggerTime (${java.util.Date(triggerTime)})")
        
        // Check if we have permission to schedule exact alarms (Android 12+)
        val hasPermission = isExactAlarmPermissionGranted()
        android.util.Log.d("AlarmService", "scheduleReminderAlarm: Exact alarm permission granted=$hasPermission")
        if (!hasPermission) {
            android.util.Log.w("AlarmService", "scheduleReminderAlarm: BLOCKED - Exact alarm permission not granted")
            return false
        }
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        
        android.util.Log.d("AlarmService", "scheduleReminderAlarm: Intent created - action=${intent.action}, component=${intent.component}")
        
        val pendingIntent = createPendingIntent(intent, REMINDER_ALARM_REQUEST_CODE)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ (API 23+): Use setExactAndAllowWhileIdle for best reliability
                android.util.Log.d("AlarmService", "scheduleReminderAlarm: Using setExactAndAllowWhileIdle (Android 6.0+)")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Android 5.1 and below: Use setExact (but MVP targets Android 11+, so this is fallback)
                android.util.Log.d("AlarmService", "scheduleReminderAlarm: Using setExact (Android 5.1 and below)")
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            android.util.Log.d("AlarmService", "scheduleReminderAlarm: ✅ Alarm scheduled successfully for ${java.util.Date(triggerTime)}")
            return true
        } catch (e: SecurityException) {
            // Permission denied - user needs to grant SCHEDULE_EXACT_ALARM permission
            android.util.Log.e("AlarmService", "scheduleReminderAlarm: ❌ SecurityException - ${e.message}", e)
            e.printStackTrace()
            return false
        } catch (e: Exception) {
            android.util.Log.e("AlarmService", "scheduleReminderAlarm: ❌ Exception - ${e.message}", e)
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Schedules an exact alarm for the 15-minute retry reminder at the specified trigger time.
     * 
     * Uses setExactAndAllowWhileIdle() for maximum reliability (Android 6.0+).
     * This method will wake the device from sleep if needed.
     * 
     * @param triggerTime The exact time (in milliseconds since epoch) when the retry alarm should trigger
     * @return true if alarm was scheduled successfully, false otherwise (e.g., permission denied)
     */
    fun scheduleRetryAlarm(triggerTime: Long): Boolean {
        // Check if we have permission to schedule exact alarms (Android 12+)
        if (!isExactAlarmPermissionGranted()) {
            return false
        }
        
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_RETRY
        }
        
        val pendingIntent = createPendingIntent(intent, RETRY_ALARM_REQUEST_CODE)
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ (API 23+): Use setExactAndAllowWhileIdle for best reliability
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Android 5.1 and below: Use setExact (but MVP targets Android 11+, so this is fallback)
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            return true
        } catch (e: SecurityException) {
            // Permission denied - user needs to grant SCHEDULE_EXACT_ALARM permission
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Cancels the reminder alarm.
     * 
     * This should be called when:
     * - Sleep mode is enabled
     * - Reminder is acknowledged (to reschedule with new time)
     * - App is being uninstalled or reset
     */
    fun cancelReminderAlarm() {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        
        val pendingIntent = createPendingIntent(intent, REMINDER_ALARM_REQUEST_CODE)
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Cancels the retry alarm.
     * 
     * This should be called when:
     * - Reminder is acknowledged (retry no longer needed)
     * - Sleep mode is enabled
     * - App is being uninstalled or reset
     */
    fun cancelRetryAlarm() {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_RETRY
        }
        
        val pendingIntent = createPendingIntent(intent, RETRY_ALARM_REQUEST_CODE)
        alarmManager.cancel(pendingIntent)
    }
    
    /**
     * Cancels both reminder and retry alarms.
     * 
     * This is useful when:
     * - Sleep mode is enabled
     * - App is being reset or uninstalled
     */
    fun cancelAllAlarms() {
        cancelReminderAlarm()
        cancelRetryAlarm()
    }
    
    /**
     * Checks if the app has permission to schedule exact alarms.
     * 
     * On Android 12+ (API 31+), apps need explicit permission to schedule exact alarms.
     * This permission cannot be requested via runtime dialog - user must enable it in system settings.
     * 
     * @return true if permission is granted, false otherwise
     */
    fun isExactAlarmPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): Check using AlarmManager.canScheduleExactAlarms()
            alarmManager.canScheduleExactAlarms()
        } else {
            // Android 11 and below: Permission is granted by default
            true
        }
    }
    
    /**
     * Checks if the device supports exact alarms.
     * 
     * @return true if device supports exact alarms, false otherwise
     */
    fun hasExactAlarmCapability(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
    }
    
    /**
     * Creates a PendingIntent for alarm scheduling.
     * 
     * Uses FLAG_UPDATE_CURRENT to update existing PendingIntent if it exists,
     * and FLAG_IMMUTABLE for Android 12+ (required for security).
     * 
     * @param intent The Intent to wrap in PendingIntent
     * @param requestCode Unique request code for this PendingIntent
     * @return PendingIntent ready for use with AlarmManager
     */
    private fun createPendingIntent(intent: Intent, requestCode: Int): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )
    }
}

