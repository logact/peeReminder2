package com.logact.peereminder2.data.model

/**
 * Represents the state of the reminder timer
 */
enum class TimerState {
    IDLE,                    // Timer is idle, waiting for next reminder
    REMINDER_PENDING,        // Reminder is scheduled and pending
    REMINDER_ACTIVE,         // Reminder is currently active (notification shown)
    RETRY_PENDING,           // Retry reminder is scheduled (15 minutes after initial)
    RETRY_ACTIVE             // Retry reminder is currently active
}

