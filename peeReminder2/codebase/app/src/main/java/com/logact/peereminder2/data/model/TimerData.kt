package com.logact.peereminder2.data.model

/**
 * Timer data model containing all timer-related state
 * 
 * @param lastReminderTime Timestamp of the last reminder (milliseconds since epoch)
 * @param lastAcknowledgmentTime Timestamp of the last acknowledgment (milliseconds since epoch)
 * @param nextReminderTime Timestamp of the next scheduled reminder (milliseconds since epoch)
 * @param isSleepModeOn Whether sleep mode is currently active
 * @param currentState Current state of the timer
 */
data class TimerData(
    val lastReminderTime: Long = 0L,
    val lastAcknowledgmentTime: Long = 0L,
    val nextReminderTime: Long = 0L,
    val isSleepModeOn: Boolean = false,
    val currentState: TimerState = TimerState.IDLE
)

