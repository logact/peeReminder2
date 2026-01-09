package com.logact.peereminder2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logact.peereminder2.data.model.AppSettings
import com.logact.peereminder2.data.model.TimerState
import com.logact.peereminder2.data.storage.StorageService
import com.logact.peereminder2.domain.TimerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the main screen UI
 * 
 * This ViewModel:
 * - Observes TimerManager state and calculates countdown
 * - Formats next reminder time for display
 * - Manages sleep mode toggle
 * - Handles acknowledgment button visibility based on settings
 * - Provides UI state via StateFlow
 */
class MainViewModel(
    private val timerManager: TimerManager,
    private val storageService: StorageService
) : ViewModel() {
    
    /**
     * UI state containing all display data
     */
    data class UiState(
        val countdownText: String = "--:--:--",
        val nextReminderTimeText: String = "Not scheduled",
        val isPaused: Boolean = false,
        val showAcknowledgmentButton: Boolean = false,
        val isReminderActive: Boolean = false,
        val currentState: TimerState = TimerState.IDLE
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private val _appSettings = MutableStateFlow<AppSettings?>(null)
    
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    
    init {
        // Load app settings once
        viewModelScope.launch {
            _appSettings.value = storageService.loadAppSettings()
        }
        
        // Observe timer data and app settings, combine them for UI state
        viewModelScope.launch {
            combine(
                timerManager.timerData,
                _appSettings
            ) { timerData, appSettings ->
                if (appSettings != null) {
                    updateUiState(timerData, appSettings)
                }
            }.collect { }
        }
        
        // Start countdown timer updates
        startCountdownUpdates()
        
        // Reload settings periodically to catch changes from Settings screen
        // Also check sleep time range and update sleep mode automatically
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000) // Check every second
                val currentSettings = storageService.loadAppSettings()
                
                // Update app settings if acknowledgment button visibility changed
                if (_appSettings.value?.showAcknowledgmentButton != currentSettings.showAcknowledgmentButton) {
                    _appSettings.value = currentSettings
                }
                
                // Note: We no longer update stored sleep mode state based on time range
                // All checks are done in real-time via shouldPauseReminders()
                // This ensures accuracy even if the app hasn't been running
            }
        }
    }
    
    /**
     * Update UI state based on timer data and app settings
     */
    private fun updateUiState(
        timerData: com.logact.peereminder2.data.model.TimerData,
        appSettings: AppSettings
    ) {
        val currentTime = System.currentTimeMillis()
        val countdownText = calculateCountdownText(timerData, currentTime, appSettings)
        val nextReminderTimeText = formatNextReminderTime(timerData.nextReminderTime)
        
        val isReminderActive = timerData.currentState == TimerState.REMINDER_ACTIVE ||
                timerData.currentState == TimerState.RETRY_ACTIVE
        
        val showAcknowledgmentButton = appSettings.showAcknowledgmentButton && isReminderActive
        
        // Check if paused in real-time (manual pause OR sleep time range)
        val isPaused = timerManager.shouldPauseReminders(appSettings)
        
        _uiState.value = UiState(
            countdownText = countdownText,
            nextReminderTimeText = nextReminderTimeText,
            isPaused = isPaused, // Real-time check: manual pause OR sleep time range
            showAcknowledgmentButton = showAcknowledgmentButton,
            isReminderActive = isReminderActive,
            currentState = timerData.currentState
        )
    }
    
    /**
     * Calculate countdown text from current time to next reminder
     */
    private fun calculateCountdownText(
        timerData: com.logact.peereminder2.data.model.TimerData,
        currentTime: Long,
        appSettings: AppSettings
    ): String {
        // Check if paused in real-time (manual pause OR sleep time range)
        if (timerManager.shouldPauseReminders(appSettings)) {
            return "Paused"
        }
        
        // If no next reminder time, show "Not scheduled"
        if (timerData.nextReminderTime <= 0L) {
            return "Not scheduled"
        }
        
        // If reminder is active, show "Reminder Active"
        if (timerData.currentState == TimerState.REMINDER_ACTIVE ||
            timerData.currentState == TimerState.RETRY_ACTIVE) {
            return "Reminder Active"
        }
        
        // Calculate time remaining
        val timeRemaining = timerData.nextReminderTime - currentTime
        
        // If time has passed, show "Overdue"
        if (timeRemaining <= 0) {
            return "Overdue"
        }
        
        // Format as HH:MM:SS
        val hours = timeRemaining / (1000 * 60 * 60)
        val minutes = (timeRemaining % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (timeRemaining % (1000 * 60)) / 1000
        
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    
    /**
     * Format next reminder time for display
     */
    private fun formatNextReminderTime(nextReminderTime: Long): String {
        if (nextReminderTime <= 0L) {
            return "Not scheduled"
        }
        return timeFormatter.format(Date(nextReminderTime))
    }
    
    /**
     * Start countdown updates (updates every second)
     * 
     * This method periodically:
     * - Reloads state from storage to sync with changes from AlarmActivity or ReminderReceiver
     * - Updates UI state with the latest timer data
     */
    private fun startCountdownUpdates() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000) // Update every second
                
                // Reload state from storage periodically to sync with changes from other components
                // (AlarmActivity, ReminderReceiver, etc.)
                timerManager.reloadStateFromStorage()
                
                val timerData = timerManager.getCurrentTimerData()
                val appSettings = _appSettings.value ?: storageService.loadAppSettings()
                if (_appSettings.value == null) {
                    _appSettings.value = appSettings
                }
                // Always check sleep time range in real-time, but don't update stored state
                // The shouldPauseReminders() method will do real-time checks
                updateUiState(timerData, appSettings)
            }
        }
    }
    
    /**
     * Pause the timer - cancel all alarms and stop reminders
     */
    fun pauseTimer() {
        viewModelScope.launch {
            timerManager.pauseTimer()
        }
    }
    
    /**
     * Start/resume the timer - reset all reminders from current time
     */
    fun startTimer() {
        viewModelScope.launch {
            timerManager.startTimer()
        }
    }
    
    /**
     * Acknowledge the current reminder
     */
    fun acknowledgeReminder() {
        viewModelScope.launch {
            timerManager.acknowledgeReminder()
        }
    }
    
    /**
     * Test function: Manually trigger reminder (for testing/debugging)
     * This marks the reminder as active and shows the full-screen alarm
     */
    fun testTriggerReminder() {
        viewModelScope.launch {
            timerManager.markReminderActive()
        }
    }
}

