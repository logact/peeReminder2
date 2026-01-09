package com.logact.peereminder2.data.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.logact.peereminder2.data.model.AppSettings
import com.logact.peereminder2.data.model.TimerData
import com.logact.peereminder2.data.model.TimerState
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for StorageService
 * Tests basic save/load functionality for timer data and app settings
 */
class StorageServiceTest {
    
    private lateinit var storageService: StorageService
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageService = StorageService.getInstance(context)
        // Clear all data before each test
        runBlocking {
            storageService.clearAll()
        }
    }
    
    @Test
    fun testSaveAndLoadTimerData() = runBlocking {
        // Given
        val timerData = TimerData(
            lastReminderTime = 1000L,
            lastAcknowledgmentTime = 2000L,
            nextReminderTime = 3000L,
            isSleepModeOn = true,
            currentState = TimerState.REMINDER_ACTIVE
        )
        
        // When
        val saveResult = storageService.saveTimerData(timerData)
        val loadedData = storageService.loadTimerData()
        
        // Then
        assertTrue(saveResult.isSuccess)
        assertEquals(timerData.lastReminderTime, loadedData.lastReminderTime)
        assertEquals(timerData.lastAcknowledgmentTime, loadedData.lastAcknowledgmentTime)
        assertEquals(timerData.nextReminderTime, loadedData.nextReminderTime)
        assertEquals(timerData.isSleepModeOn, loadedData.isSleepModeOn)
        assertEquals(timerData.currentState, loadedData.currentState)
    }
    
    @Test
    fun testSaveAndLoadAppSettings() = runBlocking {
        // Given
        val settings = AppSettings(
            showAcknowledgmentButton = true,
            setupCompleted = true
        )
        
        // When
        val saveResult = storageService.saveAppSettings(settings)
        val loadedSettings = storageService.loadAppSettings()
        
        // Then
        assertTrue(saveResult.isSuccess)
        assertEquals(settings.showAcknowledgmentButton, loadedSettings.showAcknowledgmentButton)
        assertEquals(settings.setupCompleted, loadedSettings.setupCompleted)
    }
    
    @Test
    fun testSaveAndLoadSleepMode() = runBlocking {
        // Given
        val isSleepModeOn = true
        
        // When
        val saveResult = storageService.saveSleepMode(isSleepModeOn)
        val loadedData = storageService.loadTimerData()
        
        // Then
        assertTrue(saveResult.isSuccess)
        assertEquals(isSleepModeOn, loadedData.isSleepModeOn)
    }
    
    @Test
    fun testLoadDefaultTimerData() = runBlocking {
        // When - load data without saving first
        val loadedData = storageService.loadTimerData()
        
        // Then - should return default values
        assertEquals(0L, loadedData.lastReminderTime)
        assertEquals(0L, loadedData.lastAcknowledgmentTime)
        assertEquals(0L, loadedData.nextReminderTime)
        assertEquals(false, loadedData.isSleepModeOn)
        assertEquals(TimerState.IDLE, loadedData.currentState)
    }
    
    @Test
    fun testLoadDefaultAppSettings() = runBlocking {
        // When - load settings without saving first
        val loadedSettings = storageService.loadAppSettings()
        
        // Then - should return default values
        assertEquals(false, loadedSettings.showAcknowledgmentButton)
        assertEquals(false, loadedSettings.setupCompleted)
    }
    
    @Test
    fun testClearAll() = runBlocking {
        // Given - save some data
        val timerData = TimerData(
            lastReminderTime = 1000L,
            isSleepModeOn = true,
            currentState = TimerState.REMINDER_ACTIVE
        )
        val settings = AppSettings(showAcknowledgmentButton = true, setupCompleted = true)
        storageService.saveTimerData(timerData)
        storageService.saveAppSettings(settings)
        
        // When - clear all
        val clearResult = storageService.clearAll()
        val loadedData = storageService.loadTimerData()
        val loadedSettings = storageService.loadAppSettings()
        
        // Then - should be reset to defaults
        assertTrue(clearResult.isSuccess)
        assertEquals(0L, loadedData.lastReminderTime)
        assertEquals(false, loadedData.isSleepModeOn)
        assertEquals(TimerState.IDLE, loadedData.currentState)
        assertEquals(false, loadedSettings.showAcknowledgmentButton)
        assertEquals(false, loadedSettings.setupCompleted)
    }
}

