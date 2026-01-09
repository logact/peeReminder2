package com.logact.peereminder2.domain

import com.logact.peereminder2.data.model.TimerData
import com.logact.peereminder2.data.model.TimerState
import com.logact.peereminder2.data.storage.StorageService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Unit tests for TimerManager
 * Tests timer calculation logic and basic state management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerManagerTest {
    
    private lateinit var mockStorageService: StorageService
    private lateinit var timerManager: TimerManager
    
    @Before
    fun setup() {
        runBlocking {
            mockStorageService = mock()
            timerManager = TimerManager(mockStorageService)
            
            // Mock default storage response for suspend functions
            // Use doAnswer for suspend functions in mockito
            doAnswer { TimerData() }.whenever(mockStorageService).loadTimerData()
            doAnswer { Result.success(Unit) }.whenever(mockStorageService).saveTimerData(any())
        }
    }
    
    @Test
    fun testCalculateNextReminderTime() = runTest {
        // Given
        val referenceTime = 1000000L
        val reminderIntervalMs = 2 * 60 * 60 * 1000L // 2 hours
        
        // When
        val nextReminderTime = timerManager.calculateNextReminderTime(referenceTime)
        
        // Then
        val expectedTime = referenceTime + reminderIntervalMs
        assertEquals(expectedTime, nextReminderTime)
        // Verify it's exactly 2 hours later
        assertEquals(reminderIntervalMs, nextReminderTime - referenceTime)
    }
    
    @Test
    fun testStartTimer() = runTest {
        // Given
        val beforeTime = System.currentTimeMillis()
        
        // When
        timerManager.startTimer()
        val timerData = timerManager.timerData.first()
        
        // Then
        assertTrue(timerData.nextReminderTime > beforeTime)
        assertEquals(TimerState.REMINDER_PENDING, timerData.currentState)
        assertEquals(0L, timerData.lastReminderTime)
        assertEquals(0L, timerData.lastAcknowledgmentTime)
        verify(mockStorageService).saveTimerData(any())
    }
    
    @Test
    fun testGetTimeRemainingUntilNextReminder() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val futureTime = currentTime + 1000000L // 1 hour from now
        val timerData = TimerData(nextReminderTime = futureTime)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val timeRemaining = timerManager.getTimeRemainingUntilNextReminder()
        
        // Then
        assertTrue(timeRemaining > 0)
        assertTrue(timeRemaining <= 1000000L) // Should be approximately 1 hour
    }
    
    @Test
    fun testIsReminderDue_NotDue() = runTest {
        // Given
        val futureTime = System.currentTimeMillis() + 1000000L
        val timerData = TimerData(nextReminderTime = futureTime)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val isDue = timerManager.isReminderDue()
        
        // Then
        assertFalse(isDue)
    }
    
    @Test
    fun testIsReminderDue_Due() = runTest {
        // Given
        val pastTime = System.currentTimeMillis() - 1000L
        val timerData = TimerData(nextReminderTime = pastTime)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val isDue = timerManager.isReminderDue()
        
        // Then
        assertTrue(isDue)
    }
    
    @Test
    fun testMarkReminderActive() = runTest {
        // Given
        val timerData = TimerData(
            nextReminderTime = System.currentTimeMillis() - 1000L,
            currentState = TimerState.REMINDER_PENDING
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.markReminderActive()
        val updatedData = timerManager.timerData.first()
        
        // Then
        assertEquals(TimerState.REMINDER_ACTIVE, updatedData.currentState)
        assertTrue(updatedData.lastReminderTime > 0L)
        verify(mockStorageService).saveTimerData(any())
    }
    
    @Test
    fun testMarkReminderActive_OnlyFromPending() = runTest {
        // Given
        val timerData = TimerData(currentState = TimerState.IDLE)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.markReminderActive()
        val updatedData = timerManager.timerData.first()
        
        // Then - state should not change if not in REMINDER_PENDING
        assertEquals(TimerState.IDLE, updatedData.currentState)
    }
    
    @Test
    fun testRecalculateNextReminderTime_FromAcknowledgment() = runTest {
        // Given
        val acknowledgmentTime = System.currentTimeMillis() - 3600000L // 1 hour ago
        val timerData = TimerData(
            lastAcknowledgmentTime = acknowledgmentTime,
            isSleepModeOn = false
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.recalculateNextReminderTime()
        val updatedData = timerManager.timerData.first()
        
        // Then
        val reminderIntervalMs = 2 * 60 * 60 * 1000L // 2 hours
        val expectedNextTime = acknowledgmentTime + reminderIntervalMs
        assertEquals(expectedNextTime, updatedData.nextReminderTime)
    }
    
    @Test
    fun testRecalculateNextReminderTime_SleepModeOn() = runTest {
        // Given
        val timerData = TimerData(
            lastAcknowledgmentTime = System.currentTimeMillis() - 3600000L,
            isSleepModeOn = true,
            nextReminderTime = 1000L
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.recalculateNextReminderTime()
        val updatedData = timerManager.timerData.first()
        
        // Then - should not recalculate if sleep mode is on
        assertEquals(1000L, updatedData.nextReminderTime)
    }
    
    @Test
    fun testGetCurrentState() = runTest {
        // Given
        val timerData = TimerData(currentState = TimerState.REMINDER_ACTIVE)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val state = timerManager.getCurrentState()
        
        // Then
        assertEquals(TimerState.REMINDER_ACTIVE, state)
    }
    
    @Test
    fun testAcknowledgeReminder_FromReminderActive() = runTest {
        // Given
        val reminderTime = System.currentTimeMillis() - 1000L
        val timerData = TimerData(
            lastReminderTime = reminderTime,
            currentState = TimerState.REMINDER_ACTIVE
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val acknowledgmentTime = System.currentTimeMillis()
        timerManager.acknowledgeReminder(acknowledgmentTime)
        val updatedData = timerManager.timerData.first()
        
        // Then
        assertEquals(acknowledgmentTime, updatedData.lastAcknowledgmentTime)
        assertEquals(0L, updatedData.lastReminderTime) // Reset after acknowledgment
        assertEquals(TimerState.REMINDER_PENDING, updatedData.currentState)
        val expectedNextTime = acknowledgmentTime + (2 * 60 * 60 * 1000L) // 2 hours
        assertEquals(expectedNextTime, updatedData.nextReminderTime)
        verify(mockStorageService, atLeastOnce()).saveTimerData(any())
    }
    
    @Test
    fun testAcknowledgeReminder_FromRetryActive() = runTest {
        // Given
        val reminderTime = System.currentTimeMillis() - 1000L
        val timerData = TimerData(
            lastReminderTime = reminderTime,
            currentState = TimerState.RETRY_ACTIVE
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val acknowledgmentTime = System.currentTimeMillis()
        timerManager.acknowledgeReminder(acknowledgmentTime)
        val updatedData = timerManager.timerData.first()
        
        // Then
        assertEquals(acknowledgmentTime, updatedData.lastAcknowledgmentTime)
        assertEquals(0L, updatedData.lastReminderTime) // Reset after acknowledgment
        assertEquals(TimerState.REMINDER_PENDING, updatedData.currentState)
        verify(mockStorageService, atLeastOnce()).saveTimerData(any())
    }
    
    @Test
    fun testAcknowledgeReminder_CalculatesFromAcknowledgmentTime() = runTest {
        // Given
        val reminderTime = System.currentTimeMillis() - 3600000L // 1 hour ago
        val timerData = TimerData(
            lastReminderTime = reminderTime,
            currentState = TimerState.REMINDER_ACTIVE
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val acknowledgmentTime = System.currentTimeMillis()
        timerManager.acknowledgeReminder(acknowledgmentTime)
        val updatedData = timerManager.timerData.first()
        
        // Then - next reminder should be 2 hours from acknowledgment, not from reminder
        val reminderIntervalMs = 2 * 60 * 60 * 1000L // 2 hours
        val expectedNextTime = acknowledgmentTime + reminderIntervalMs
        assertEquals(expectedNextTime, updatedData.nextReminderTime)
        // Verify it's NOT calculated from reminder time
        assertNotEquals(reminderTime + reminderIntervalMs, updatedData.nextReminderTime)
    }
    
    @Test
    fun testAcknowledgeReminder_IgnoresWhenNotActive() = runTest {
        // Given
        val timerData = TimerData(currentState = TimerState.IDLE)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.acknowledgeReminder()
        val updatedData = timerManager.timerData.first()
        
        // Then - state should not change
        assertEquals(TimerState.IDLE, updatedData.currentState)
        assertEquals(0L, updatedData.lastAcknowledgmentTime)
    }
    
    @Test
    fun testSetSleepMode_Enable_DismissesActiveReminder() = runTest {
        // Given
        val timerData = TimerData(
            currentState = TimerState.REMINDER_ACTIVE,
            isSleepModeOn = false
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.setSleepMode(true)
        val updatedData = timerManager.timerData.first()
        
        // Then
        assertTrue(updatedData.isSleepModeOn)
        assertEquals(TimerState.IDLE, updatedData.currentState) // Dismissed
        verify(mockStorageService).saveTimerData(any())
    }
    
    @Test
    fun testSetSleepMode_Enable_DismissesRetryActive() = runTest {
        // Given
        val timerData = TimerData(
            currentState = TimerState.RETRY_ACTIVE,
            isSleepModeOn = false
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.setSleepMode(true)
        val updatedData = timerManager.timerData.first()
        
        // Then
        assertTrue(updatedData.isSleepModeOn)
        assertEquals(TimerState.IDLE, updatedData.currentState) // Dismissed
    }
    
    @Test
    fun testSetSleepMode_Disable_RecalculatesReminder() = runTest {
        // Given
        val acknowledgmentTime = System.currentTimeMillis() - 3600000L // 1 hour ago
        val timerData = TimerData(
            lastAcknowledgmentTime = acknowledgmentTime,
            isSleepModeOn = true,
            currentState = TimerState.IDLE
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.setSleepMode(false)
        val updatedData = timerManager.timerData.first()
        
        // Then
        assertFalse(updatedData.isSleepModeOn)
        val reminderIntervalMs = 2 * 60 * 60 * 1000L // 2 hours
        val expectedNextTime = acknowledgmentTime + reminderIntervalMs
        assertEquals(expectedNextTime, updatedData.nextReminderTime)
        verify(mockStorageService, atLeastOnce()).saveTimerData(any())
    }
    
    @Test
    fun testSetSleepMode_NoChangeWhenSameState() = runTest {
        // Given
        val timerData = TimerData(isSleepModeOn = true)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // Clear any saves from initialize
        clearInvocations(mockStorageService)
        
        // When
        timerManager.setSleepMode(true) // Already enabled
        val updatedData = timerManager.timerData.first()
        
        // Then - should not change and should not save
        assertTrue(updatedData.isSleepModeOn)
        verify(mockStorageService, never()).saveTimerData(any())
    }
    
    @Test
    fun testMarkRetryActive_PreservesLastReminderTime() = runTest {
        // Given
        val reminderTime = System.currentTimeMillis() - 1000L
        val timerData = TimerData(
            lastReminderTime = reminderTime,
            currentState = TimerState.REMINDER_ACTIVE
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.markRetryActive()
        val updatedData = timerManager.timerData.first()
        
        // Then - lastReminderTime should be preserved (CRITICAL!)
        assertEquals(reminderTime, updatedData.lastReminderTime)
        assertEquals(TimerState.RETRY_ACTIVE, updatedData.currentState)
        verify(mockStorageService, atLeastOnce()).saveTimerData(any())
    }
    
    @Test
    fun testMarkRetryActive_OnlyFromReminderActive() = runTest {
        // Given
        val timerData = TimerData(currentState = TimerState.IDLE)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        timerManager.markRetryActive()
        val updatedData = timerManager.timerData.first()
        
        // Then - state should not change
        assertEquals(TimerState.IDLE, updatedData.currentState)
    }
    
    @Test
    fun testCalculateRetryTime() = runTest {
        // Given
        val reminderTime = System.currentTimeMillis() - 1000L
        val timerData = TimerData(lastReminderTime = reminderTime)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val retryTime = timerManager.calculateRetryTime()
        
        // Then
        val expectedRetryTime = reminderTime + (15 * 60 * 1000L) // 15 minutes
        assertEquals(expectedRetryTime, retryTime)
    }
    
    @Test
    fun testCalculateRetryTime_ReturnsZeroWhenNoReminderTime() = runTest {
        // Given
        val timerData = TimerData(lastReminderTime = 0L)
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val retryTime = timerManager.calculateRetryTime()
        
        // Then
        assertEquals(0L, retryTime)
    }
    
    @Test
    fun testIsRetryDue_True() = runTest {
        // Given
        val reminderTime = System.currentTimeMillis() - (16 * 60 * 1000L) // 16 minutes ago
        val timerData = TimerData(
            lastReminderTime = reminderTime,
            currentState = TimerState.REMINDER_ACTIVE
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val isDue = timerManager.isRetryDue()
        
        // Then
        assertTrue(isDue) // 16 minutes > 15 minutes
    }
    
    @Test
    fun testIsRetryDue_False_NotEnoughTime() = runTest {
        // Given
        val reminderTime = System.currentTimeMillis() - (10 * 60 * 1000L) // 10 minutes ago
        val timerData = TimerData(
            lastReminderTime = reminderTime,
            currentState = TimerState.REMINDER_ACTIVE
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val isDue = timerManager.isRetryDue()
        
        // Then
        assertFalse(isDue) // 10 minutes < 15 minutes
    }
    
    @Test
    fun testIsRetryDue_False_WrongState() = runTest {
        // Given
        val reminderTime = System.currentTimeMillis() - (16 * 60 * 1000L) // 16 minutes ago
        val timerData = TimerData(
            lastReminderTime = reminderTime,
            currentState = TimerState.IDLE // Wrong state
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When
        val isDue = timerManager.isRetryDue()
        
        // Then
        assertFalse(isDue) // Wrong state
    }
    
    @Test
    fun testRetryMechanism_NoCascade() = runTest {
        // Given - Reminder shown, retry shown, but not acknowledged
        val reminderTime = System.currentTimeMillis() - (20 * 60 * 1000L) // 20 minutes ago
        val timerData = TimerData(
            lastReminderTime = reminderTime,
            currentState = TimerState.RETRY_ACTIVE,
            lastAcknowledgmentTime = 0L // Not acknowledged
        )
        doAnswer { timerData }.whenever(mockStorageService).loadTimerData()
        timerManager.initialize()
        
        // When - Recalculate next reminder
        timerManager.recalculateNextReminderTime()
        val updatedData = timerManager.timerData.first()
        
        // Then - Next reminder should be calculated from ORIGINAL reminder time, not retry time
        val reminderIntervalMs = 2 * 60 * 60 * 1000L // 2 hours
        val expectedNextTime = reminderTime + reminderIntervalMs
        assertEquals(expectedNextTime, updatedData.nextReminderTime)
        // Verify it's NOT calculated from retry time (reminderTime + 15 min)
        assertNotEquals(reminderTime + (15 * 60 * 1000L) + reminderIntervalMs, updatedData.nextReminderTime)
    }
    
    @Test
    fun testStateTransitions_Complete() = runTest {
        // Test IDLE → REMINDER_PENDING
        timerManager.startTimer()
        var data = timerManager.timerData.first()
        assertEquals(TimerState.REMINDER_PENDING, data.currentState)
        
        // Test REMINDER_PENDING → REMINDER_ACTIVE
        timerManager.markReminderActive()
        data = timerManager.timerData.first()
        assertEquals(TimerState.REMINDER_ACTIVE, data.currentState)
        
        // Test REMINDER_ACTIVE → RETRY_ACTIVE
        timerManager.markRetryActive()
        data = timerManager.timerData.first()
        assertEquals(TimerState.RETRY_ACTIVE, data.currentState)
        
        // Test RETRY_ACTIVE → REMINDER_PENDING (via acknowledgment)
        timerManager.acknowledgeReminder()
        data = timerManager.timerData.first()
        assertEquals(TimerState.REMINDER_PENDING, data.currentState)
        
        // Test REMINDER_ACTIVE → REMINDER_PENDING (via acknowledgment)
        timerManager.markReminderActive()
        timerManager.acknowledgeReminder()
        data = timerManager.timerData.first()
        assertEquals(TimerState.REMINDER_PENDING, data.currentState)
        
        // Test Any state → IDLE (via sleep mode)
        timerManager.setSleepMode(true)
        data = timerManager.timerData.first()
        assertEquals(TimerState.IDLE, data.currentState)
    }
}

