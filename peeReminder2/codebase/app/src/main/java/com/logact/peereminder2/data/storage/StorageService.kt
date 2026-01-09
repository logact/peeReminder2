package com.logact.peereminder2.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.logact.peereminder2.data.model.AppSettings
import com.logact.peereminder2.data.model.TimerData
import com.logact.peereminder2.data.model.TimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Storage service using SharedPreferences for persisting app state
 * 
 * This service handles all local storage operations for:
 * - Timer state (last reminder time, acknowledgment time, next reminder time)
 * - Sleep mode state
 * - App settings (acknowledgment button visibility, setup completion)
 * 
 * All operations are performed on the IO dispatcher to avoid blocking the main thread.
 */
class StorageService private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "pee_reminder_prefs"
        
        // Keys for timer data
        private const val KEY_LAST_REMINDER_TIME = "last_reminder_time"
        private const val KEY_LAST_ACKNOWLEDGMENT_TIME = "last_acknowledgment_time"
        private const val KEY_NEXT_REMINDER_TIME = "next_reminder_time"
        private const val KEY_IS_SLEEP_MODE_ON = "is_sleep_mode_on"
        private const val KEY_CURRENT_STATE = "current_state"
        
        // Keys for app settings
        private const val KEY_SHOW_ACKNOWLEDGMENT_BUTTON = "show_acknowledgment_button"
        private const val KEY_SETUP_COMPLETED = "setup_completed"
        private const val KEY_REMINDER_INTERVAL_MS = "reminder_interval_ms"
        private const val KEY_SLEEP_MODE_START_TIME = "sleep_mode_start_time"
        private const val KEY_SLEEP_MODE_END_TIME = "sleep_mode_end_time"
        
        @Volatile
        private var INSTANCE: StorageService? = null
        
        fun getInstance(context: Context): StorageService {
            return INSTANCE ?: synchronized(this) {
                val instance = StorageService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    /**
     * Save timer data to SharedPreferences
     */
    suspend fun saveTimerData(timerData: TimerData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().apply {
                putLong(KEY_LAST_REMINDER_TIME, timerData.lastReminderTime)
                putLong(KEY_LAST_ACKNOWLEDGMENT_TIME, timerData.lastAcknowledgmentTime)
                putLong(KEY_NEXT_REMINDER_TIME, timerData.nextReminderTime)
                putBoolean(KEY_IS_SLEEP_MODE_ON, timerData.isSleepModeOn)
                putString(KEY_CURRENT_STATE, timerData.currentState.name)
                apply()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Load timer data from SharedPreferences
     */
    suspend fun loadTimerData(): TimerData = withContext(Dispatchers.IO) {
        val stateName = prefs.getString(KEY_CURRENT_STATE, TimerState.IDLE.name) ?: TimerState.IDLE.name
        TimerData(
            lastReminderTime = prefs.getLong(KEY_LAST_REMINDER_TIME, 0L),
            lastAcknowledgmentTime = prefs.getLong(KEY_LAST_ACKNOWLEDGMENT_TIME, 0L),
            nextReminderTime = prefs.getLong(KEY_NEXT_REMINDER_TIME, 0L),
            isSleepModeOn = prefs.getBoolean(KEY_IS_SLEEP_MODE_ON, false),
            currentState = try {
                TimerState.valueOf(stateName)
            } catch (e: IllegalArgumentException) {
                TimerState.IDLE
            }
        )
    }
    
    /**
     * Save app settings to SharedPreferences
     */
    suspend fun saveAppSettings(settings: AppSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().apply {
                putBoolean(KEY_SHOW_ACKNOWLEDGMENT_BUTTON, settings.showAcknowledgmentButton)
                putBoolean(KEY_SETUP_COMPLETED, settings.setupCompleted)
                putLong(KEY_REMINDER_INTERVAL_MS, settings.reminderIntervalMs)
                if (settings.sleepModeStartTime != null) {
                    putString(KEY_SLEEP_MODE_START_TIME, settings.sleepModeStartTime)
                } else {
                    remove(KEY_SLEEP_MODE_START_TIME)
                }
                if (settings.sleepModeEndTime != null) {
                    putString(KEY_SLEEP_MODE_END_TIME, settings.sleepModeEndTime)
                } else {
                    remove(KEY_SLEEP_MODE_END_TIME)
                }
                apply()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Load app settings from SharedPreferences
     */
    suspend fun loadAppSettings(): AppSettings = withContext(Dispatchers.IO) {
        val defaultInterval = 2 * 60 * 60 * 1000L // 2 hours in milliseconds
        AppSettings(
            showAcknowledgmentButton = prefs.getBoolean(KEY_SHOW_ACKNOWLEDGMENT_BUTTON, false),
            setupCompleted = prefs.getBoolean(KEY_SETUP_COMPLETED, false),
            reminderIntervalMs = prefs.getLong(KEY_REMINDER_INTERVAL_MS, defaultInterval),
            sleepModeStartTime = prefs.getString(KEY_SLEEP_MODE_START_TIME, null),
            sleepModeEndTime = prefs.getString(KEY_SLEEP_MODE_END_TIME, null)
        )
    }
    
    /**
     * Save sleep mode state
     */
    suspend fun saveSleepMode(isSleepModeOn: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().putBoolean(KEY_IS_SLEEP_MODE_ON, isSleepModeOn).apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear all stored data (for testing/reset purposes)
     */
    suspend fun clearAll(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            prefs.edit().clear().apply()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

