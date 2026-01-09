package com.logact.peereminder2.data.model

/**
 * Application settings data model
 * 
 * @param showAcknowledgmentButton Whether to show the acknowledgment button (default: false)
 * @param setupCompleted Whether the first-time setup has been completed
 * @param reminderIntervalMs Reminder interval in milliseconds (default: 2 hours = 7,200,000 ms)
 * @param sleepModeStartTime Sleep mode start time in HH:mm format (e.g., "22:00"), null to disable
 * @param sleepModeEndTime Sleep mode end time in HH:mm format (e.g., "07:00"), null to disable
 */
data class AppSettings(
    val showAcknowledgmentButton: Boolean = false,
    val setupCompleted: Boolean = false,
    val reminderIntervalMs: Long = 2 * 60 * 60 * 1000L, // 2 hours in milliseconds
    val sleepModeStartTime: String? = null, // HH:mm format, e.g., "22:00"
    val sleepModeEndTime: String? = null // HH:mm format, e.g., "07:00"
)

