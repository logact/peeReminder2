package com.logact.peereminder2.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.logact.peereminder2.data.model.AppSettings
import com.logact.peereminder2.data.storage.StorageService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the settings screen
 * 
 * Manages app settings including:
 * - Acknowledgment button visibility toggle
 * - Setup completion status
 */
class SettingsViewModel(
    private val storageService: StorageService
) : ViewModel() {
    
    /**
     * UI state containing current settings
     */
    data class SettingsUiState(
        val showAcknowledgmentButton: Boolean = false,
        val setupCompleted: Boolean = false,
        val reminderIntervalMs: Long = 2 * 60 * 60 * 1000L, // Default 2 hours
        val sleepModeStartTime: String? = null, // HH:mm format
        val sleepModeEndTime: String? = null, // HH:mm format
        val isLoading: Boolean = true
    )
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Load settings from storage
     */
    private fun loadSettings() {
        viewModelScope.launch {
            val settings = storageService.loadAppSettings()
            _uiState.value = SettingsUiState(
                showAcknowledgmentButton = settings.showAcknowledgmentButton,
                setupCompleted = settings.setupCompleted,
                reminderIntervalMs = settings.reminderIntervalMs,
                sleepModeStartTime = settings.sleepModeStartTime,
                sleepModeEndTime = settings.sleepModeEndTime,
                isLoading = false
            )
        }
    }
    
    /**
     * Toggle acknowledgment button visibility
     */
    fun toggleAcknowledgmentButtonVisibility(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = storageService.loadAppSettings()
            val updatedSettings = currentSettings.copy(
                showAcknowledgmentButton = enabled
            )
            storageService.saveAppSettings(updatedSettings)
            
            // Update UI state
            _uiState.value = _uiState.value.copy(
                showAcknowledgmentButton = enabled
            )
        }
    }
    
    /**
     * Mark setup as completed
     */
    fun markSetupCompleted() {
        viewModelScope.launch {
            val currentSettings = storageService.loadAppSettings()
            val updatedSettings = currentSettings.copy(
                setupCompleted = true
            )
            storageService.saveAppSettings(updatedSettings)
            
            // Update UI state
            _uiState.value = _uiState.value.copy(
                setupCompleted = true
            )
        }
    }
    
    /**
     * Update reminder interval
     * 
     * @param intervalMs The new interval in milliseconds
     */
    fun updateReminderInterval(intervalMs: Long) {
        viewModelScope.launch {
            val currentSettings = storageService.loadAppSettings()
            val updatedSettings = currentSettings.copy(
                reminderIntervalMs = intervalMs
            )
            storageService.saveAppSettings(updatedSettings)
            
            // Update UI state
            _uiState.value = _uiState.value.copy(
                reminderIntervalMs = intervalMs
            )
        }
    }
    
    /**
     * Update sleep mode time range
     * 
     * @param startTime Sleep mode start time in HH:mm format (e.g., "22:00"), or null to disable
     * @param endTime Sleep mode end time in HH:mm format (e.g., "07:00"), or null to disable
     */
    fun updateSleepModeTimeRange(startTime: String?, endTime: String?) {
        viewModelScope.launch {
            val currentSettings = storageService.loadAppSettings()
            val updatedSettings = currentSettings.copy(
                sleepModeStartTime = startTime,
                sleepModeEndTime = endTime
            )
            storageService.saveAppSettings(updatedSettings)
            
            // Update UI state
            _uiState.value = _uiState.value.copy(
                sleepModeStartTime = startTime,
                sleepModeEndTime = endTime
            )
        }
    }
}

