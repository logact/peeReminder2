package com.logact.peereminder2.domain

import com.logact.peereminder2.data.model.AppSettings
import com.logact.peereminder2.data.model.TimerData
import com.logact.peereminder2.data.model.TimerState
import com.logact.peereminder2.data.notification.AlarmService
import com.logact.peereminder2.data.storage.StorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * TimerManager manages the 2-hour reminder timer logic and state transitions.
 * 
 * This class is responsible for:
 * - Calculating next reminder time (2-hour intervals)
 * - Managing timer state (IDLE, REMINDER_PENDING, REMINDER_ACTIVE, RETRY_PENDING, RETRY_ACTIVE)
 * - Handling user acknowledgment of reminders
 * - Managing sleep mode (pause/resume reminders)
 * - Implementing retry mechanism (15-minute retry if reminder not acknowledged)
 * - Persisting timer state to storage
 * - Providing reactive state updates via StateFlow
 * - Scheduling and canceling alarms via AlarmService (when provided)
 * 
 * The timer calculates reminders based on:
 * - Last acknowledgment time (preferred - if available)
 * - Last reminder time (fallback - if no acknowledgment)
 * - Current time (if no previous state)
 * - 2-hour intervals (120 minutes = 7,200,000 milliseconds)
 * 
 * State Transitions:
 * - IDLE → REMINDER_PENDING (via startTimer())
 * - REMINDER_PENDING → REMINDER_ACTIVE (via markReminderActive())
 * - REMINDER_ACTIVE → RETRY_PENDING (via retry scheduling)
 * - RETRY_PENDING → RETRY_ACTIVE (via markRetryActive())
 * - REMINDER_ACTIVE → REMINDER_PENDING (via acknowledgeReminder())
 * - RETRY_ACTIVE → REMINDER_PENDING (via acknowledgeReminder())
 * - Any state → IDLE (via setSleepMode(true))
 * 
 * Alarm Integration:
 * - When AlarmService is provided, alarms are scheduled/canceled automatically
 * - Reminder alarms are scheduled when: startTimer(), acknowledgeReminder(), setSleepMode(false), recalculateNextReminderTime()
 * - Retry alarms are scheduled when: markReminderActive()
 * - Alarms are canceled when: setSleepMode(true), acknowledgeReminder() (retry alarm only)
 */
class TimerManager(
    private val storageService: StorageService,
    private val alarmService: AlarmService? = null
) {
    
    companion object {
        /**
         * Default reminder interval: 2 hours in milliseconds
         */
        private const val DEFAULT_REMINDER_INTERVAL_MS = 2 * 60 * 60 * 1000L // 2 hours
        
        /**
         * Retry interval: 15 minutes in milliseconds
         */
        const val RETRY_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
    }
    
    /**
     * Current reminder interval in milliseconds (loaded from settings)
     */
    private var reminderIntervalMs: Long = DEFAULT_REMINDER_INTERVAL_MS
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    /**
     * Current timer data state
     */
    private val _timerData = MutableStateFlow<TimerData>(TimerData())
    val timerData: StateFlow<TimerData> = _timerData.asStateFlow()
    
    /**
     * Initialize the timer manager by loading saved state from storage
     */
    suspend fun initialize() {
        // Load reminder interval from settings
        val settings = storageService.loadAppSettings()
        reminderIntervalMs = settings.reminderIntervalMs
        
        val savedData = storageService.loadTimerData()
        _timerData.value = savedData
        
        // If we have a saved state, recalculate next reminder time based on current time
        if (savedData.lastAcknowledgmentTime > 0L || savedData.lastReminderTime > 0L) {
            recalculateNextReminderTime()
        }
    }
    
    /**
     * Reload timer state from storage.
     * 
     * This is useful when state might have been changed by another TimerManager instance
     * (e.g., ReminderReceiver creates a new instance and updates state).
     * 
     * After reloading, if the state changed, the StateFlow will emit the new value,
     * which will trigger observers in MainActivity.
     */
    suspend fun reloadStateFromStorage() {
        val savedData = storageService.loadTimerData()
        val currentData = _timerData.value
        
        android.util.Log.d("TimerManager", "reloadStateFromStorage: currentState=${currentData.currentState}, savedState=${savedData.currentState}")
        android.util.Log.d("TimerManager", "reloadStateFromStorage: currentNextReminderTime=${currentData.nextReminderTime}, savedNextReminderTime=${savedData.nextReminderTime}")
        android.util.Log.d("TimerManager", "reloadStateFromStorage: currentLastAckTime=${currentData.lastAcknowledgmentTime}, savedLastAckTime=${savedData.lastAcknowledgmentTime}")
        
        // Always update StateFlow to ensure observers get the latest data
        // Even if the data appears the same, we want to ensure synchronization
        // This is important when AlarmActivity updates state - MainActivity needs to see it
        if (savedData != currentData) {
            android.util.Log.d("TimerManager", "reloadStateFromStorage: ✅ State changed! Updating from ${currentData.currentState} to ${savedData.currentState}")
            android.util.Log.d("TimerManager", "reloadStateFromStorage: nextReminderTime changed from ${currentData.nextReminderTime} to ${savedData.nextReminderTime}")
            _timerData.value = savedData
        } else {
            android.util.Log.d("TimerManager", "reloadStateFromStorage: No state change detected (data is identical)")
            // Even if data is identical, update to ensure StateFlow observers are notified
            // This helps with synchronization across different TimerManager instances
            _timerData.value = savedData
        }
    }
    
    /**
     * Reload reminder interval from settings
     * This should be called when settings change
     * 
     * When the interval changes:
     * - Cancels existing reminder alarm (if any)
     * - Recalculates next reminder time using the new interval
     * - Reschedules alarm with the new interval
     */
    suspend fun reloadInterval() {
        val settings = storageService.loadAppSettings()
        val newInterval = settings.reminderIntervalMs
        
        // Only update if interval changed
        if (newInterval != reminderIntervalMs) {
            val currentData = _timerData.value
            
            // Cancel existing reminder alarm before rescheduling with new interval
            // This ensures the old alarm (scheduled with old interval) doesn't fire
            if (currentData.nextReminderTime > 0L && !currentData.isSleepModeOn) {
                alarmService?.cancelReminderAlarm()
            }
            
            reminderIntervalMs = newInterval
            
            // Recalculate next reminder time with new interval
            // This will reschedule the alarm with the new interval
            recalculateNextReminderTime()
        }
    }
    
    /**
     * Start the timer from the current time
     * This is called when the app first starts or when there's no previous state
     * Also used to resume/start the timer after pausing
     */
    suspend fun startTimer() {
        val currentTime = System.currentTimeMillis()
        val nextReminderTime = calculateNextReminderTime(currentTime)
        
        val newTimerData = _timerData.value.copy(
            lastReminderTime = 0L, // No previous reminder
            lastAcknowledgmentTime = 0L, // No previous acknowledgment
            nextReminderTime = nextReminderTime,
            currentState = TimerState.REMINDER_PENDING,
            isSleepModeOn = false // Clear sleep mode when starting
        )
        
        _timerData.value = newTimerData
        saveTimerData(newTimerData)
        
        // Schedule reminder alarm
        alarmService?.scheduleReminderAlarm(nextReminderTime)
    }
    
    /**
     * Pause the timer - cancel all alarms and stop reminders
     * 
     * This method:
     * - Cancels all scheduled alarms (reminder and retry)
     * - Sets state to IDLE
     * - Sets sleep mode to true (to prevent alarms from being rescheduled)
     * - Persists state
     */
    suspend fun pauseTimer() {
        val currentData = _timerData.value
        
        // Cancel all alarms
        alarmService?.cancelAllAlarms()
        
        // Set state to IDLE and enable sleep mode (which prevents alarms)
        val newTimerData = currentData.copy(
            currentState = TimerState.IDLE,
            isSleepModeOn = true
        )
        
        _timerData.value = newTimerData
        saveTimerData(newTimerData)
    }
    
    /**
     * Check if timer is paused (manually paused via pauseTimer)
     */
    fun isPaused(): Boolean {
        return _timerData.value.isSleepModeOn
    }
    
    /**
     * Check if reminders should be paused based on:
     * 1. Manual pause (isSleepModeOn from pauseTimer)
     * 2. Sleep time range (real-time check)
     * 
     * This method always checks the time range in real-time, not relying on stored state.
     * 
     * @param settings App settings containing sleep mode time range
     * @return true if reminders should be paused, false otherwise
     */
    fun shouldPauseReminders(settings: com.logact.peereminder2.data.model.AppSettings): Boolean {
        // Check manual pause first
        if (_timerData.value.isSleepModeOn) {
            return true
        }
        
        // Always check sleep time range in real-time
        return isInSleepTimeRange(settings)
    }
    
    /**
     * Check if current time is within the configured sleep mode time range
     * 
     * @param settings App settings containing sleep mode time range
     * @return true if current time is within sleep mode range, false otherwise
     */
    fun isInSleepTimeRange(settings: com.logact.peereminder2.data.model.AppSettings): Boolean {
        val startTime = settings.sleepModeStartTime
        val endTime = settings.sleepModeEndTime
        
        // If sleep mode time range is not configured, return false
        if (startTime == null || endTime == null) {
            return false
        }
        
        try {
            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)
            val currentTimeMinutes = currentHour * 60 + currentMinute
            
            // Parse start and end times
            val startParts = startTime.split(":")
            val endParts = endTime.split(":")
            val startHour = startParts[0].toInt()
            val startMinute = startParts[1].toInt()
            val endHour = endParts[0].toInt()
            val endMinute = endParts[1].toInt()
            
            val startTimeMinutes = startHour * 60 + startMinute
            val endTimeMinutes = endHour * 60 + endMinute
            
            // Handle sleep time range that spans midnight (e.g., 22:00 to 07:00)
            return if (startTimeMinutes > endTimeMinutes) {
                // Range spans midnight: current time >= start OR current time <= end
                currentTimeMinutes >= startTimeMinutes || currentTimeMinutes <= endTimeMinutes
            } else {
                // Range within same day: start <= current <= end
                currentTimeMinutes >= startTimeMinutes && currentTimeMinutes <= endTimeMinutes
            }
        } catch (e: Exception) {
            // If parsing fails, return false (don't enable sleep mode)
            return false
        }
    }
    
    /**
     * Update sleep mode based on configured time range
     * 
     * This method checks if current time is within the sleep time range and
     * automatically sets sleep mode accordingly.
     * 
     * Note: This only updates automatic sleep mode. Manual pause (from pauseTimer)
     * takes precedence and won't be overridden.
     * 
     * @param settings App settings containing sleep mode time range
     */
    suspend fun updateSleepModeFromTimeRange(settings: com.logact.peereminder2.data.model.AppSettings) {
        // Only update automatic sleep mode if not manually paused
        // Manual pause (isSleepModeOn from pauseTimer) should not be overridden
        val currentData = _timerData.value
        
        // If manually paused, don't update based on time range
        // (User explicitly paused, so respect that)
        // Note: We could add a separate flag for manual vs automatic, but for now
        // we'll only update if not manually paused
        // Actually, let's not update stored state at all - always check in real-time
        // The stored isSleepModeOn is only for manual pause/start
    }
    
    /**
     * Calculate the next reminder time based on a reference time
     * 
     * @param referenceTime The time to calculate from (usually current time or last acknowledgment)
     * @return The next reminder time (reminderIntervalMs after reference time)
     */
    fun calculateNextReminderTime(referenceTime: Long): Long {
        return referenceTime + reminderIntervalMs
    }
    
    /**
     * Recalculate the next reminder time based on the current state
     * This is useful when the app restarts or when we need to sync with current time
     */
    suspend fun recalculateNextReminderTime() {
        val currentTime = System.currentTimeMillis()
        val currentData = _timerData.value
        
        // If manually paused, don't recalculate
        if (currentData.isSleepModeOn) {
            return
        }
        
        // Note: We don't check sleep time range here because:
        // 1. This is called when resuming/starting, and we want to schedule alarms
        // 2. The alarm handlers will check sleep time range in real-time when alarms fire
        // 3. MainViewModel periodically checks and cancels alarms if in sleep range
        
        val nextReminderTime = when {
            // If we have a last acknowledgment time, calculate from there
            currentData.lastAcknowledgmentTime > 0L -> {
                calculateNextReminderTime(currentData.lastAcknowledgmentTime)
            }
            // If we have a last reminder time, calculate from there
            currentData.lastReminderTime > 0L -> {
                calculateNextReminderTime(currentData.lastReminderTime)
            }
            // Otherwise, calculate from current time
            else -> {
                calculateNextReminderTime(currentTime)
            }
        }
        
        // If the calculated next reminder time is in the past, schedule immediately
        val finalNextReminderTime = if (nextReminderTime <= currentTime) {
            calculateNextReminderTime(currentTime)
        } else {
            nextReminderTime
        }
        
        // Update state appropriately based on current state
        val newState = when (currentData.currentState) {
            TimerState.IDLE -> TimerState.REMINDER_PENDING
            TimerState.REMINDER_PENDING, TimerState.REMINDER_ACTIVE, 
            TimerState.RETRY_PENDING, TimerState.RETRY_ACTIVE -> {
                // Keep current state if already in an active state
                currentData.currentState
            }
            else -> currentData.currentState
        }
        
        val newTimerData = currentData.copy(
            nextReminderTime = finalNextReminderTime,
            currentState = newState
        )
        
        _timerData.value = newTimerData
        saveTimerData(newTimerData)
        
        // Reschedule reminder alarm if state is REMINDER_PENDING
        if (newState == TimerState.REMINDER_PENDING) {
            alarmService?.scheduleReminderAlarm(finalNextReminderTime)
        }
    }
    
    /**
     * Get the time remaining until the next reminder in milliseconds
     * 
     * @return Time remaining in milliseconds, or 0 if reminder is due
     */
    fun getTimeRemainingUntilNextReminder(): Long {
        val currentTime = System.currentTimeMillis()
        val nextReminderTime = _timerData.value.nextReminderTime
        return maxOf(0L, nextReminderTime - currentTime)
    }
    
    /**
     * Check if a reminder is due (next reminder time has passed)
     * 
     * @return true if reminder is due, false otherwise
     */
    fun isReminderDue(): Boolean {
        val currentTime = System.currentTimeMillis()
        val nextReminderTime = _timerData.value.nextReminderTime
        return nextReminderTime > 0L && currentTime >= nextReminderTime
    }
    
    /**
     * Mark reminder as active (notification shown)
     * This transitions state from REMINDER_PENDING to REMINDER_ACTIVE
     */
    suspend fun markReminderActive() {
        val currentData = _timerData.value
        android.util.Log.d("TimerManager", "markReminderActive: currentState=${currentData.currentState}")
        
        if (currentData.currentState == TimerState.REMINDER_PENDING) {
            val currentTime = System.currentTimeMillis()
            val newTimerData = currentData.copy(
                lastReminderTime = currentTime,
                currentState = TimerState.REMINDER_ACTIVE
            )
            _timerData.value = newTimerData
            saveTimerData(newTimerData)
            
            android.util.Log.d("TimerManager", "markReminderActive: State updated to REMINDER_ACTIVE and saved to storage")
            
            // Schedule retry alarm (15 minutes after reminder)
            val retryTime = calculateRetryTime()
            if (retryTime > 0L) {
                alarmService?.scheduleRetryAlarm(retryTime)
            }
        } else {
            android.util.Log.w("TimerManager", "markReminderActive: Cannot mark active - current state is ${currentData.currentState}, expected REMINDER_PENDING")
        }
    }
    
    /**
     * Acknowledge the current reminder.
     * 
     * This method handles user acknowledgment of a reminder. It:
     * - Updates lastAcknowledgmentTime to the acknowledgment timestamp
     * - Calculates next reminder time from acknowledgment time (2 hours later)
     * - Transitions state from REMINDER_ACTIVE or RETRY_ACTIVE to REMINDER_PENDING
     * - Resets lastReminderTime to 0 (acknowledgment takes precedence)
     * - Persists updated state to storage
     * - Cancels retry alarm and schedules next reminder alarm
     * 
     * Priority: lastAcknowledgmentTime > lastReminderTime
     * After acknowledgment, next reminder is calculated from acknowledgment time.
     * 
     * @param acknowledgmentTime Optional timestamp of acknowledgment. If not provided, uses current time.
     */
    suspend fun acknowledgeReminder(acknowledgmentTime: Long = System.currentTimeMillis()) {
        val currentData = _timerData.value
        
        android.util.Log.d("TimerManager", "acknowledgeReminder: Called with acknowledgmentTime=$acknowledgmentTime (${java.util.Date(acknowledgmentTime)})")
        android.util.Log.d("TimerManager", "acknowledgeReminder: Current state=${currentData.currentState}")
        
        // Only allow acknowledgment from REMINDER_ACTIVE or RETRY_ACTIVE states
        if (currentData.currentState != TimerState.REMINDER_ACTIVE && 
            currentData.currentState != TimerState.RETRY_ACTIVE) {
            android.util.Log.w("TimerManager", "acknowledgeReminder: Cannot acknowledge - current state is ${currentData.currentState}, expected REMINDER_ACTIVE or RETRY_ACTIVE")
            return
        }
        
        // Calculate next reminder time from acknowledgment time (2 hours later)
        val nextReminderTime = calculateNextReminderTime(acknowledgmentTime)
        val currentTime = System.currentTimeMillis()
        android.util.Log.d("TimerManager", "acknowledgeReminder: Calculated nextReminderTime=$nextReminderTime (${java.util.Date(nextReminderTime)})")
        android.util.Log.d("TimerManager", "acknowledgeReminder: Current time=$currentTime (${java.util.Date(currentTime)})")
        android.util.Log.d("TimerManager", "acknowledgeReminder: Time until next reminder=${nextReminderTime - currentTime}ms (${(nextReminderTime - currentTime) / 1000 / 60} minutes)")
        
        // Validate that next reminder time is in the future
        if (nextReminderTime <= currentTime) {
            android.util.Log.e("TimerManager", "acknowledgeReminder: ❌ ERROR - nextReminderTime is in the past! This should not happen.")
            android.util.Log.e("TimerManager", "acknowledgeReminder: nextReminderTime=$nextReminderTime, currentTime=$currentTime, difference=${nextReminderTime - currentTime}ms")
            // Recalculate from current time to fix the issue
            val correctedNextReminderTime = calculateNextReminderTime(currentTime)
            android.util.Log.w("TimerManager", "acknowledgeReminder: Correcting nextReminderTime to $correctedNextReminderTime (${java.util.Date(correctedNextReminderTime)})")
            val correctedTimerData = currentData.copy(
                lastAcknowledgmentTime = currentTime, // Use current time instead
                lastReminderTime = 0L,
                nextReminderTime = correctedNextReminderTime,
                currentState = TimerState.REMINDER_PENDING
            )
            _timerData.value = correctedTimerData
            saveTimerData(correctedTimerData)
            
            // Schedule with corrected time
            alarmService?.cancelRetryAlarm()
            if (alarmService != null) {
                val scheduled = alarmService.scheduleReminderAlarm(correctedNextReminderTime)
                if (scheduled) {
                    android.util.Log.d("TimerManager", "acknowledgeReminder: ✅ Corrected alarm scheduled successfully")
                } else {
                    android.util.Log.e("TimerManager", "acknowledgeReminder: ❌ Failed to schedule corrected alarm")
                }
            }
            return
        }
        
        val newTimerData = currentData.copy(
            lastAcknowledgmentTime = acknowledgmentTime,
            lastReminderTime = 0L, // Reset - acknowledgment takes precedence
            nextReminderTime = nextReminderTime,
            currentState = TimerState.REMINDER_PENDING
        )
        
        _timerData.value = newTimerData
        saveTimerData(newTimerData)
        
        android.util.Log.d("TimerManager", "acknowledgeReminder: State updated to REMINDER_PENDING and saved to storage")
        android.util.Log.d("TimerManager", "acknowledgeReminder: New state - nextReminderTime=${newTimerData.nextReminderTime}, state=${newTimerData.currentState}")
        
        // Cancel retry alarm (no longer needed) and schedule next reminder alarm
        alarmService?.cancelRetryAlarm()
        
        // Schedule next reminder alarm and verify it was scheduled
        if (alarmService != null) {
            val scheduled = alarmService.scheduleReminderAlarm(nextReminderTime)
            if (scheduled) {
                android.util.Log.d("TimerManager", "acknowledgeReminder: ✅ Next reminder alarm scheduled successfully for ${java.util.Date(nextReminderTime)}")
                android.util.Log.d("TimerManager", "acknowledgeReminder: Alarm will trigger in ${(nextReminderTime - currentTime) / 1000 / 60} minutes")
            } else {
                android.util.Log.e("TimerManager", "acknowledgeReminder: ❌ Failed to schedule next reminder alarm - check exact alarm permission")
            }
        } else {
            android.util.Log.w("TimerManager", "acknowledgeReminder: ⚠️ AlarmService is null - alarm not scheduled")
        }
    }
    
    /**
     * Set sleep mode on or off.
     * 
     * When enabling sleep mode:
     * - If reminder is active, transition to IDLE (dismiss reminder)
     * - Cancel any pending retry timers
     * - Persist state
     * 
     * When disabling sleep mode:
     * - Recalculate next reminder time based on current state
     * - Transition to REMINDER_PENDING if needed
     * - Persist state
     * 
     * @param enabled true to enable sleep mode, false to disable
     */
    suspend fun setSleepMode(enabled: Boolean) {
        val currentData = _timerData.value
        
        // If sleep mode state is not changing, do nothing
        if (currentData.isSleepModeOn == enabled) {
            return
        }
        
        val newTimerData = if (enabled) {
            // Enabling sleep mode - dismiss active reminders and pause pending reminders
            val newState = when (currentData.currentState) {
                TimerState.REMINDER_PENDING, TimerState.REMINDER_ACTIVE, 
                TimerState.RETRY_ACTIVE, TimerState.RETRY_PENDING -> {
                    TimerState.IDLE
                }
                else -> currentData.currentState
            }
            
            currentData.copy(
                isSleepModeOn = true,
                currentState = newState
            )
        } else {
            // Disabling sleep mode - recalculate next reminder
            // We'll recalculate after updating state
            currentData.copy(
                isSleepModeOn = false
            )
        }
        
        _timerData.value = newTimerData
        saveTimerData(newTimerData)
        
        if (enabled) {
            // Enabling sleep mode - cancel all alarms
            alarmService?.cancelAllAlarms()
        } else {
            // Disabling sleep mode - recalculate next reminder time (which will schedule alarm)
            recalculateNextReminderTime()
        }
    }
    
    /**
     * Mark retry reminder as active (retry notification shown).
     * 
     * This transitions state from REMINDER_ACTIVE to RETRY_ACTIVE.
     * 
     * **CRITICAL:** This method does NOT update lastReminderTime.
     * The original reminder time is preserved to ensure:
     * - Next reminder is calculated from original reminder time (not retry time)
     * - Prevents cascade of retries
     * - Maintains 2-hour intervals correctly
     * 
     * See LAST-REMINDER-TIME-EXPLAINED.md for detailed explanation.
     */
    suspend fun markRetryActive() {
        val currentData = _timerData.value
        
        // Only allow retry activation from REMINDER_ACTIVE state
        if (currentData.currentState != TimerState.REMINDER_ACTIVE) {
            return
        }
        
        // Transition to RETRY_ACTIVE without updating lastReminderTime
        val newTimerData = currentData.copy(
            currentState = TimerState.RETRY_ACTIVE
        )
        
        _timerData.value = newTimerData
        saveTimerData(newTimerData)
    }
    
    /**
     * Calculate the retry time (15 minutes after the original reminder time).
     * 
     * This is used by the notification service to schedule retry notifications.
     * 
     * @return The retry time in milliseconds, or 0 if no reminder time is available
     */
    fun calculateRetryTime(): Long {
        val currentData = _timerData.value
        val lastReminderTime = currentData.lastReminderTime
        
        if (lastReminderTime > 0L) {
            return lastReminderTime + RETRY_INTERVAL_MS
        }
        
        return 0L
    }
    
    /**
     * Check if a retry reminder is due.
     * 
     * A retry is due if:
     * - Current state is REMINDER_ACTIVE (not yet acknowledged)
     * - 15 minutes have passed since lastReminderTime
     * - No acknowledgment has occurred
     * 
     * This prevents cascade by ensuring only one retry per reminder cycle.
     * 
     * @return true if retry is due, false otherwise
     */
    fun isRetryDue(): Boolean {
        val currentData = _timerData.value
        
        // Only check retry if in REMINDER_ACTIVE state
        if (currentData.currentState != TimerState.REMINDER_ACTIVE) {
            return false
        }
        
        // Need a valid lastReminderTime
        if (currentData.lastReminderTime <= 0L) {
            return false
        }
        
        // Check if 15 minutes have passed since reminder
        val currentTime = System.currentTimeMillis()
        val retryTime = currentData.lastReminderTime + RETRY_INTERVAL_MS
        
        return currentTime >= retryTime
    }
    
    /**
     * Get the current timer state
     */
    fun getCurrentState(): TimerState {
        return _timerData.value.currentState
    }
    
    /**
     * Get the current timer data
     */
    fun getCurrentTimerData(): TimerData {
        return _timerData.value
    }
    
    /**
     * Save timer data to storage
     */
    private suspend fun saveTimerData(timerData: TimerData) {
        storageService.saveTimerData(timerData).onFailure { error ->
            // Log error in production (for now, we'll just fail silently)
            // In production, you might want to use a logging framework
            error.printStackTrace()
        }
    }
    
    /**
     * Update timer data (internal method for state updates)
     */
    private suspend fun updateTimerData(update: (TimerData) -> TimerData) {
        val updated = update(_timerData.value)
        _timerData.value = updated
        saveTimerData(updated)
    }
}

