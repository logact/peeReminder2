package com.logact.peereminder2.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.logact.peereminder2.ui.AlarmActivity

/**
 * NotificationService handles all notification-related operations for the pee reminder app.
 * 
 * This service is responsible for:
 * - Creating and managing the notification channel
 * - Displaying reminder notifications with sound, vibration, and visual alerts
 * - Handling notification permissions
 * - Canceling notifications when needed
 * 
 * For MVP, this uses standard notifications (no DND override).
 * AlarmManager integration will be added in Hour 5.
 */
class NotificationService(private val context: Context) {
    
    companion object {
        /**
         * Notification channel ID for reminder notifications
         */
        const val REMINDER_CHANNEL_ID = "reminder_channel"
        
        /**
         * Notification ID for regular reminders
         */
        const val REMINDER_NOTIFICATION_ID = 1001
        
        /**
         * Notification ID for retry reminders
         */
        const val RETRY_NOTIFICATION_ID = 1002
        
        /**
         * Channel name displayed to users
         */
        private const val CHANNEL_NAME = "Medical Reminders"
        
        /**
         * Channel description
         */
        private const val CHANNEL_DESCRIPTION = "Reminders for medical bladder exercises"
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    /**
     * Creates the notification channel for reminder notifications.
     * 
     * This must be called before showing any notifications (typically on app startup).
     * On Android 8.0 (API 26) and above, all notifications must be assigned to a channel.
     * 
     * Channel configuration:
     * - Importance: MAX (required for full-screen intents to work reliably)
     * - Vibration: Enabled with default pattern
     * - Lights: Enabled
     * - Sound: Enabled with alarm sound for better visibility
     * - Bypass DND: Enabled (allows notifications even in Do Not Disturb mode)
     */
    fun createNotificationChannel() {
        // Notification channels are only required for Android 8.0 (API 26) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_MAX // Use MAX for full-screen intents
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                // Use alarm sound for better visibility (required for full-screen intents)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
                )
                // Allow bypassing Do Not Disturb mode (Android 8.0+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setBypassDnd(true)
                }
            }
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            systemNotificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Shows a reminder notification with sound, vibration, and visual alert.
     * 
     * This notification includes:
     * - Full-screen intent to show AlarmActivity when device is locked
     * - Content intent to open AlarmActivity when notification is clicked
     * - Acknowledge action button to acknowledge reminder from notification
     * - Alarm sound for better visibility
     * 
     * @param isRetry If true, shows a retry notification (15-minute reminder).
     *                If false, shows the regular 2-hour reminder notification.
     */
    fun showReminderNotification(isRetry: Boolean = false) {
        // Check if notifications are enabled
        if (!isNotificationPermissionGranted()) {
            android.util.Log.w("NotificationService", "Notification permission not granted - cannot show notification")
            return
        }
        
        // Ensure notification channel exists (required for Android 8.0+)
        // This is a safety check - channel should already be created in MainActivity,
        // but this ensures it exists even if called from ReminderReceiver or other contexts
        createNotificationChannel()
        
        // Verify channel is enabled (user might have disabled it in settings)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = systemNotificationManager.getNotificationChannel(REMINDER_CHANNEL_ID)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                android.util.Log.w("NotificationService", "Notification channel is disabled by user - cannot show notification")
                return
            }
        }
        
        val notificationId = if (isRetry) RETRY_NOTIFICATION_ID else REMINDER_NOTIFICATION_ID
        val title = if (isRetry) {
            "Time to Pee! (Reminder)"
        } else {
            "Time to Pee!"
        }
        val contentText = if (isRetry) {
            "It's been 15 minutes since your last reminder. Don't forget to pee!"
        } else {
            "It's been 2 hours since your last reminder"
        }
        
        // Create intent for full-screen alarm activity
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_IS_RETRY, isRetry)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        // Create pending intent for full-screen alarm (high priority)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create intent for content click (opens alarm activity)
        val contentIntent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_IS_RETRY, isRetry)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 1000, // Different request code to avoid conflicts
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create intent for acknowledge action
        val acknowledgeIntent = Intent(AcknowledgeReceiver.ACTION_ACKNOWLEDGE).apply {
            putExtra(AcknowledgeReceiver.EXTRA_IS_RETRY, isRetry)
            setClass(context, AcknowledgeReceiver::class.java)
        }
        
        val acknowledgePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 2000, // Different request code
            acknowledgeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Use alarm sound for better visibility (fallback to notification sound if not available)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        // Build notification with full-screen intent and acknowledge action
        val notification = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use default icon for MVP
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_MAX) // Use MAX priority for full-screen intents
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Use ALARM category for better visibility
            .setAutoCancel(true) // Notification disappears when user taps it
            .setSound(alarmSound) // Use alarm sound instead of notification sound
            .setVibrate(longArrayOf(0, 500, 250, 500)) // Vibrate pattern: wait 0ms, vibrate 500ms, wait 250ms, vibrate 500ms
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setFullScreenIntent(fullScreenPendingIntent, true) // Show full-screen alarm when device is locked
            .setContentIntent(contentPendingIntent) // Open alarm activity when notification is clicked
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel, // Icon for acknowledge button
                "I've Peed", // Button text
                acknowledgePendingIntent // Action to trigger when button is clicked
            )
            .build()
        
        // Show notification
        try {
            notificationManager.notify(notificationId, notification)
            android.util.Log.d("NotificationService", "Notification shown successfully: $title")
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationService", "Failed to show notification - security exception", e)
        } catch (e: Exception) {
            android.util.Log.e("NotificationService", "Failed to show notification", e)
        }
    }
    
    /**
     * Cancels the reminder notification.
     * 
     * @param isRetry If true, cancels the retry notification.
     *                If false, cancels the regular reminder notification.
     */
    fun cancelReminderNotification(isRetry: Boolean = false) {
        val notificationId = if (isRetry) RETRY_NOTIFICATION_ID else REMINDER_NOTIFICATION_ID
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Cancels all reminder notifications (both regular and retry).
     */
    fun cancelAllReminderNotifications() {
        notificationManager.cancel(REMINDER_NOTIFICATION_ID)
        notificationManager.cancel(RETRY_NOTIFICATION_ID)
    }
    
    /**
     * Checks if notification permission is granted.
     * 
     * On Android 13 (API 33) and above, apps need explicit permission to post notifications.
     * On earlier versions, notifications are enabled by default.
     * 
     * @return true if notifications are enabled, false otherwise
     */
    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires explicit permission
            val granted = notificationManager.areNotificationsEnabled()
            android.util.Log.d("NotificationService", "Notification permission granted: $granted (Android 13+)")
            granted
        } else {
            // Android 12 and below: notifications are enabled by default
            android.util.Log.d("NotificationService", "Notification permission granted: true (Android 12 or below)")
            true
        }
    }
    
    /**
     * Checks if the notification channel is enabled and properly configured.
     * 
     * @return true if channel exists and is enabled, false otherwise
     */
    fun isNotificationChannelEnabled(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = systemNotificationManager.getNotificationChannel(REMINDER_CHANNEL_ID)
            if (channel == null) {
                android.util.Log.w("NotificationService", "Notification channel does not exist")
                return false
            }
            val isEnabled = channel.importance != NotificationManager.IMPORTANCE_NONE
            android.util.Log.d("NotificationService", "Notification channel enabled: $isEnabled, importance: ${channel.importance}")
            return isEnabled
        }
        return true // Android 7.1 and below don't use channels
    }
    
    /**
     * Gets detailed notification status for debugging.
     * 
     * @return A string describing the current notification status
     */
    fun getNotificationStatus(): String {
        val permissionGranted = isNotificationPermissionGranted()
        val channelEnabled = isNotificationChannelEnabled()
        
        return buildString {
            append("Notification Status:\n")
            append("  Permission granted: $permissionGranted\n")
            append("  Channel enabled: $channelEnabled\n")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = systemNotificationManager.getNotificationChannel(REMINDER_CHANNEL_ID)
                if (channel != null) {
                    append("  Channel importance: ${channel.importance}\n")
                    append("  Channel can bypass DND: ${channel.canBypassDnd()}\n")
                }
            }
        }
    }
}

