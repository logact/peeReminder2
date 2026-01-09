package com.logact.peereminder2

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.logact.peereminder2.data.model.TimerState
import com.logact.peereminder2.data.notification.AlarmService
import com.logact.peereminder2.data.notification.NotificationService
import com.logact.peereminder2.data.storage.StorageService
import com.logact.peereminder2.domain.TimerManager
import com.logact.peereminder2.ui.MainScreen
import com.logact.peereminder2.ui.MainViewModel
import com.logact.peereminder2.ui.SettingsScreen
import com.logact.peereminder2.ui.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Activity - Entry point of the application
 * 
 * This activity:
 * - Handles notification permission requests (Android 13+)
 * - Handles exact alarm permission checks (Android 12+)
 * - Initializes NotificationService and creates notification channel
 * - Initializes AlarmService for reliable background reminders
 * - Initializes TimerManager and observes state changes
 * - Shows/cancels notifications based on timer state
 * - Schedules/cancels alarms based on timer state
 * - Displays main screen UI with countdown, sleep mode toggle, and acknowledgment button
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var notificationService: NotificationService
    private lateinit var alarmService: AlarmService
    private lateinit var timerManager: TimerManager
    private lateinit var storageService: StorageService
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // State for showing permission denied dialog
    private var showPermissionDeniedDialog by mutableStateOf(false)
    
    // Permission launcher for notification permission (Android 13+)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, initialize notification service
            initializeNotificationService()
            showPermissionDeniedDialog = false
        } else {
            // Permission denied - show dialog to guide user to settings
            showPermissionDeniedDialog = true
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize services
        initializeServices()
        
        // Request notification permission if needed (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!notificationService.isNotificationPermissionGranted()) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Check exact alarm permission (Android 12+)
        // Note: This permission cannot be requested via runtime dialog
        // User must enable it in system settings
        checkExactAlarmPermission()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Reload state from storage when activity resumes
        // This ensures we have the latest state after AlarmActivity updates it
        scope.launch {
            android.util.Log.d("MainActivity", "onResume: Reloading state from storage")
            timerManager.reloadStateFromStorage()
            
            // After reloading, ensure alarms are scheduled if needed
            scheduleAlarmsIfNeeded()
        }
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Navigation state
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
                    
                    // Create ViewModels
                    val mainViewModel: MainViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                return MainViewModel(timerManager, storageService) as T
                            }
                        }
                    )
                    
                    val settingsViewModel: SettingsViewModel = viewModel(
                        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                            @Suppress("UNCHECKED_CAST")
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                return SettingsViewModel(storageService) as T
                            }
                        }
                    )
                    
                    // Observe timer state for notification handling
                    TimerStateObserver()
                    
                    // Check for overdue reminders when state changes (more efficient than periodic checks)
                    OverdueReminderChecker(timerManager)
                    
                    // Check onboarding status and mark as completed after permissions
                    OnboardingHandler(settingsViewModel)
                    
                    // Observe settings changes and reload interval when it changes
                    IntervalChangeObserver(settingsViewModel)
                    
                    // Show permission denied dialog if needed
                    PermissionDeniedDialog(
                        showDialog = showPermissionDeniedDialog,
                        onDismiss = { showPermissionDeniedDialog = false },
                        onOpenSettings = { openNotificationSettings() }
                    )
                    
                    // Check and show alert when reminder is due but permission is missing
                    PermissionMissingAlert(
                        notificationService = notificationService,
                        timerManager = timerManager
                    )
                    
                    // Show exact alarm permission dialog if needed
                    ExactAlarmPermissionDialog(
                        showDialog = showExactAlarmPermissionDialog,
                        onDismiss = { showExactAlarmPermissionDialog = false },
                        onOpenSettings = { openExactAlarmSettings() }
                    )
                    
                    // Display current screen
                    when (currentScreen) {
                        Screen.Main -> {
                            MainScreen(
                                viewModel = mainViewModel,
                                onSettingsClick = { currentScreen = Screen.Settings },
                                showPermissionWarning = !notificationService.isNotificationPermissionGranted(),
                                onOpenSettings = { openNotificationSettings() },
                                onTestTriggerReminder = { testTriggerReminder() },
                                onTestScheduleReminder = { testScheduleReminderIn10Seconds() }
                            )
                        }
                        Screen.Settings -> {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBackClick = { currentScreen = Screen.Main }
                            )
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Initialize all services (NotificationService, AlarmService, TimerManager, StorageService)
     */
    private fun initializeServices() {
        // Initialize NotificationService and create channel
        notificationService = NotificationService(this)
        notificationService.createNotificationChannel()
        
        // Initialize AlarmService for reliable background reminders
        alarmService = AlarmService(this)
        
        // Initialize StorageService and TimerManager with AlarmService
        storageService = StorageService.getInstance(this)
        timerManager = TimerManager(storageService, alarmService)
        
        // Initialize TimerManager (loads saved state and schedules alarms)
        scope.launch {
            timerManager.initialize()
            
            // Load settings for real-time sleep time range checks
            val settings = storageService.loadAppSettings()
            
            // If timer hasn't been started (no next reminder time), start it now
            // Only start if reminders should not be paused (check in real-time)
            val timerData = timerManager.getCurrentTimerData()
            if (timerData.nextReminderTime <= 0L && !timerManager.shouldPauseReminders(settings)) {
                timerManager.startTimer()
            }
            
            // After initialization, ensure alarms are scheduled if needed
            scheduleAlarmsIfNeeded()
        }
    }
    
    /**
     * Schedules alarms if needed based on current timer state.
     * 
     * This is called after TimerManager initialization to ensure alarms are scheduled
     * even if the app was restarted or device was rebooted.
     */
    private suspend fun scheduleAlarmsIfNeeded() {
        val timerData = timerManager.getCurrentTimerData()
        val settings = storageService.loadAppSettings()
        
        android.util.Log.d("MainActivity", "scheduleAlarmsIfNeeded: Checking if alarms need to be scheduled")
        android.util.Log.d("MainActivity", "scheduleAlarmsIfNeeded: shouldPauseReminders=${timerManager.shouldPauseReminders(settings)}, nextReminderTime=${timerData.nextReminderTime}")
        
        // Only schedule if reminders should not be paused (check in real-time)
        // This checks both manual pause AND sleep time range
        if (!timerManager.shouldPauseReminders(settings) && timerData.nextReminderTime > 0L) {
            val currentTime = System.currentTimeMillis()
            
            // Schedule reminder alarm if next reminder time is in the future
            if (timerData.nextReminderTime > currentTime) {
                android.util.Log.d("MainActivity", "scheduleAlarmsIfNeeded: Scheduling reminder alarm for ${java.util.Date(timerData.nextReminderTime)}")
                val scheduled = alarmService.scheduleReminderAlarm(timerData.nextReminderTime)
                android.util.Log.d("MainActivity", "scheduleAlarmsIfNeeded: Alarm scheduling result=$scheduled")
                
                // If scheduling failed due to permission, check and show dialog
                if (!scheduled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!alarmService.isExactAlarmPermissionGranted()) {
                        android.util.Log.w("MainActivity", "scheduleAlarmsIfNeeded: Alarm scheduling failed - exact alarm permission not granted")
                        // Show dialog on main thread
                        scope.launch(Dispatchers.Main) {
                            showExactAlarmPermissionDialog = true
                        }
                    }
                }
            } else {
                android.util.Log.w("MainActivity", "scheduleAlarmsIfNeeded: nextReminderTime ($timerData.nextReminderTime) is in the past, not scheduling")
            }
            
            // Schedule retry alarm if reminder is active and retry time is in the future
            if (timerData.currentState == TimerState.REMINDER_ACTIVE) {
                val retryTime = timerManager.calculateRetryTime()
                if (retryTime > currentTime) {
                    android.util.Log.d("MainActivity", "scheduleAlarmsIfNeeded: Scheduling retry alarm for ${java.util.Date(retryTime)}")
                    alarmService.scheduleRetryAlarm(retryTime)
                }
            }
        } else {
            android.util.Log.d("MainActivity", "scheduleAlarmsIfNeeded: Skipping alarm scheduling - paused=${timerManager.shouldPauseReminders(settings)}, nextReminderTime=${timerData.nextReminderTime}")
        }
    }
    
    // State for showing exact alarm permission dialog
    private var showExactAlarmPermissionDialog by mutableStateOf(false)
    
    /**
     * Checks if exact alarm permission is granted and guides user to settings if needed.
     * 
     * On Android 12+ (API 31+), apps need explicit permission to schedule exact alarms.
     * This permission cannot be requested via runtime dialog - user must enable it in system settings.
     * 
     * If permission is not granted, we'll show a dialog guiding user to settings.
     */
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmService.isExactAlarmPermissionGranted()) {
                android.util.Log.w("MainActivity", "checkExactAlarmPermission: Exact alarm permission not granted")
                // Show dialog to guide user to settings
                showExactAlarmPermissionDialog = true
            } else {
                android.util.Log.d("MainActivity", "checkExactAlarmPermission: Exact alarm permission granted")
                showExactAlarmPermissionDialog = false
            }
        }
    }
    
    /**
     * Opens the system settings screen for exact alarm permission.
     * 
     * This is called when exact alarm permission is not granted.
     * User can enable the permission from the settings screen.
     * 
     * On some devices, the exact alarm setting might be:
     * - Directly accessible via ACTION_REQUEST_SCHEDULE_EXACT_ALARM
     * - Under "Special app access" in app settings
     * - Under "Alarms & reminders" in app settings
     * - In the main app settings page
     */
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Try to open the direct exact alarm settings first
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    setPackage(packageName)
                }
                startActivity(intent)
                android.util.Log.d("MainActivity", "openExactAlarmSettings: Opened exact alarm settings via ACTION_REQUEST_SCHEDULE_EXACT_ALARM")
                return
            } catch (e: Exception) {
                android.util.Log.d("MainActivity", "openExactAlarmSettings: ACTION_REQUEST_SCHEDULE_EXACT_ALARM not available, using app settings", e)
            }
            
            // Fallback: Open app settings where user can find "Schedule exact alarms"
            // This is more reliable across different device manufacturers
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
                android.util.Log.d("MainActivity", "openExactAlarmSettings: Opened app settings")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "openExactAlarmSettings: Failed to open settings", e)
            }
        } else {
            // Android 11 and below don't need this permission
            android.util.Log.d("MainActivity", "openExactAlarmSettings: Android 11 or below - permission not needed")
        }
    }
    
    /**
     * Initialize notification service (called after permission is granted)
     */
    private fun initializeNotificationService() {
        // Notification channel is already created in initializeServices()
        // This method is here for future use if needed
    }
    
    /**
     * Opens the app settings screen where user can grant notification permission.
     * This is called from composables, so it needs to be accessible.
     */
    fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
    
    /**
     * Reload reminder interval in TimerManager when settings change
     */
    fun reloadTimerInterval() {
        scope.launch {
            timerManager.reloadInterval()
        }
    }
    
    /**
     * Checks if reminder is overdue and transitions state to REMINDER_ACTIVE if needed.
     * 
     * This method:
     * 1. First reloads state from storage to sync with any changes made by ReminderReceiver
     * 2. Checks if reminder is overdue and still in PENDING state
     * 3. Transitions to ACTIVE if needed, which triggers notifications and AlarmActivity
     * 
     * This is called when timer state changes, making it more efficient than periodic checks.
     * It ensures state consistency between ReminderReceiver and MainActivity instances.
     */
    fun checkAndHandleOverdueReminder() {
        scope.launch {
            android.util.Log.d("MainActivity", "checkAndHandleOverdueReminder: Called")
            
            // First, reload state from storage to sync with ReminderReceiver changes
            // This ensures we see state updates made by ReminderReceiver's TimerManager instance
            timerManager.reloadStateFromStorage()
            
            val timerData = timerManager.getCurrentTimerData()
            val settings = storageService.loadAppSettings()
            
            android.util.Log.d("MainActivity", "checkAndHandleOverdueReminder: currentState=${timerData.currentState}, nextReminderTime=${timerData.nextReminderTime}, currentTime=${System.currentTimeMillis()}")
            
            // Only check if timer is in REMINDER_PENDING state
            if (timerData.currentState != TimerState.REMINDER_PENDING) {
                android.util.Log.d("MainActivity", "checkAndHandleOverdueReminder: State is ${timerData.currentState}, not PENDING - skipping")
                return@launch
            }
            
            // Check if reminders should be paused (manual pause or sleep time range)
            if (timerManager.shouldPauseReminders(settings)) {
                android.util.Log.d("MainActivity", "checkAndHandleOverdueReminder: Reminders are paused - skipping")
                return@launch
            }
            
            // Check if reminder is due (time has passed)
            val isDue = timerManager.isReminderDue()
            android.util.Log.d("MainActivity", "checkAndHandleOverdueReminder: isReminderDue=$isDue")
            
            if (isDue) {
                android.util.Log.d("MainActivity", "Overdue reminder detected - transitioning to REMINDER_ACTIVE")
                
                // Mark reminder as active - this will trigger state change and observer
                timerManager.markReminderActive()
                
                android.util.Log.d("MainActivity", "checkAndHandleOverdueReminder: markReminderActive() called")
                
                // Note: The state change will trigger TimerStateObserver which will:
                // - Show notification
                // - Launch AlarmActivity
                // - Schedule retry alarm
            }
        }
    }
    
    /**
     * Composable that observes TimerManager state and shows/cancels notifications
     */
    @Composable
    private fun TimerStateObserver() {
        val timerData by timerManager.timerData.collectAsState()
        
        LaunchedEffect(timerData.currentState) {
            android.util.Log.d("MainActivity", "TimerStateObserver: State changed to ${timerData.currentState}")
            android.util.Log.d("MainActivity", "TimerStateObserver: nextReminderTime=${timerData.nextReminderTime}, currentTime=${System.currentTimeMillis()}")
            handleTimerStateChange(timerData.currentState)
        }
    }
    
    /**
     * Handle timer state changes and show/cancel notifications accordingly.
     * Also reschedules alarms when needed.
     * 
     * When reminder becomes active, this method:
     * - Shows reminder notification
     * - Launches AlarmActivity to display the reminder page
     * - Ensures user can acknowledge the reminder from the full-screen alarm
     */
    private fun handleTimerStateChange(state: TimerState) {
        scope.launch {
            val timerData = timerManager.getCurrentTimerData()
            val settings = storageService.loadAppSettings()
            
            when (state) {
                TimerState.REMINDER_ACTIVE -> {
                    // Log notification status for debugging
                    android.util.Log.d("MainActivity", "═══════════════════════════════════════")
                    android.util.Log.d("MainActivity", "REMINDER_ACTIVE: ⏰ Reminder is now active!")
                    android.util.Log.d("MainActivity", notificationService.getNotificationStatus())
                    
                    // Show reminder notification
                    if (notificationService.isNotificationPermissionGranted()) {
                        if (notificationService.isNotificationChannelEnabled()) {
                            notificationService.showReminderNotification(isRetry = false)
                            android.util.Log.d("MainActivity", "REMINDER_ACTIVE: Reminder notification shown")
                        } else {
                            android.util.Log.w("MainActivity", "REMINDER_ACTIVE: Notification channel is disabled - cannot show notification")
                        }
                    } else {
                        android.util.Log.w("MainActivity", "REMINDER_ACTIVE: Notification permission not granted - cannot show notification")
                        // Permission missing - will be handled by PermissionMissingAlert composable
                        // This ensures user is notified even if permission was denied
                    }
                    
                    // Launch AlarmActivity to show reminder page
                    // This ensures the reminder page is displayed even when app is in foreground
                    android.util.Log.d("MainActivity", "REMINDER_ACTIVE: 🚀 Launching AlarmActivity for full-screen reminder...")
                    launchAlarmActivity(isRetry = false)
                    android.util.Log.d("MainActivity", "REMINDER_ACTIVE: AlarmActivity launch command sent")
                    
                    // Alarm scheduling for retry is handled by TimerManager.markReminderActive()
                    android.util.Log.d("MainActivity", "═══════════════════════════════════════")
                }
                TimerState.RETRY_ACTIVE -> {
                    // Log notification status for debugging
                    android.util.Log.d("MainActivity", "═══════════════════════════════════════")
                    android.util.Log.d("MainActivity", "RETRY_ACTIVE: ⏰ Retry reminder is now active!")
                    android.util.Log.d("MainActivity", notificationService.getNotificationStatus())
                    
                    // Show retry notification
                    if (notificationService.isNotificationPermissionGranted()) {
                        if (notificationService.isNotificationChannelEnabled()) {
                            notificationService.showReminderNotification(isRetry = true)
                            android.util.Log.d("MainActivity", "RETRY_ACTIVE: Retry notification shown")
                        } else {
                            android.util.Log.w("MainActivity", "RETRY_ACTIVE: Notification channel is disabled - cannot show notification")
                        }
                    } else {
                        android.util.Log.w("MainActivity", "RETRY_ACTIVE: Notification permission not granted - cannot show notification")
                        // Permission missing - will be handled by PermissionMissingAlert composable
                    }
                    
                    // Launch AlarmActivity to show retry reminder page
                    android.util.Log.d("MainActivity", "RETRY_ACTIVE: 🚀 Launching AlarmActivity for full-screen retry reminder...")
                    launchAlarmActivity(isRetry = true)
                    android.util.Log.d("MainActivity", "RETRY_ACTIVE: AlarmActivity launch command sent")
                    android.util.Log.d("MainActivity", "═══════════════════════════════════════")
                }
                TimerState.IDLE -> {
                    // Sleep mode enabled or timer stopped - cancel all notifications and alarms
                    notificationService.cancelAllReminderNotifications()
                    // Alarm cancellation is handled by TimerManager.setSleepMode(true)
                }
                TimerState.REMINDER_PENDING -> {
                    // Timer reset or acknowledged - cancel active notifications
                    notificationService.cancelAllReminderNotifications()
                    // Alarm scheduling is handled by TimerManager (acknowledgeReminder, startTimer, etc.)
                    // Ensure reminder alarm is scheduled if we have a valid next reminder time
                    // Check in real-time if reminders should be paused
                    if (timerData.nextReminderTime > 0L && !timerManager.shouldPauseReminders(settings)) {
                        val currentTime = System.currentTimeMillis()
                        if (timerData.nextReminderTime > currentTime) {
                            alarmService.scheduleReminderAlarm(timerData.nextReminderTime)
                        }
                    }
                }
                TimerState.RETRY_PENDING -> {
                    // Retry scheduled but not active - no action needed
                }
            }
        }
    }
    
    /**
     * Launches AlarmActivity to display the reminder page.
     * 
     * This method is called when a reminder becomes active to ensure the user
     * sees the full-screen alarm page with acknowledgment button.
     * 
     * @param isRetry If true, indicates this is a retry reminder (15-minute reminder).
     *                If false, indicates this is a regular 2-hour reminder.
     */
    private fun launchAlarmActivity(isRetry: Boolean) {
        // Launch activity on main thread (required for UI operations)
        scope.launch {
            withContext(Dispatchers.Main) {
                try {
                    android.util.Log.d("MainActivity", "launchAlarmActivity: Launching AlarmActivity (isRetry=$isRetry)")
                    
                    val intent = Intent(this@MainActivity, com.logact.peereminder2.ui.AlarmActivity::class.java).apply {
                        putExtra(com.logact.peereminder2.ui.AlarmActivity.EXTRA_IS_RETRY, isRetry)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    
                    startActivity(intent)
                    android.util.Log.d("MainActivity", "launchAlarmActivity: ✅ AlarmActivity launched successfully")
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "launchAlarmActivity: ❌ Failed to launch AlarmActivity", e)
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Test function: Manually trigger the full-screen reminder (for testing/debugging)
     * This launches AlarmActivity directly to test the full-screen display
     */
    fun testTriggerReminder() {
        android.util.Log.d("MainActivity", "testTriggerReminder: Manually triggering reminder for testing")
        // Mark reminder as active first
        scope.launch {
            timerManager.markReminderActive()
            // Then launch AlarmActivity
            launchAlarmActivity(isRetry = false)
        }
    }
    
    /**
     * Test function: Schedule a test reminder in 10 seconds (for testing/debugging)
     * This helps verify that alarm scheduling works correctly
     */
    fun testScheduleReminderIn10Seconds() {
        scope.launch {
            val currentTime = System.currentTimeMillis()
            val testReminderTime = currentTime + 10_000L // 10 seconds from now
            
            android.util.Log.d("MainActivity", "testScheduleReminderIn10Seconds: Scheduling test reminder for ${java.util.Date(testReminderTime)}")
            
            // Get the reminder interval from settings
            val settings = storageService.loadAppSettings()
            val interval = settings.reminderIntervalMs
            
            // Calculate acknowledgment time that will result in next reminder in 10 seconds
            // If we acknowledge at (testReminderTime - interval), the next reminder will be at testReminderTime
            val testAckTime = testReminderTime - interval
            
            android.util.Log.d("MainActivity", "testScheduleReminderIn10Seconds: Will acknowledge at ${java.util.Date(testAckTime)} to schedule reminder at ${java.util.Date(testReminderTime)}")
            
            // First, ensure we're in a state that allows acknowledgment
            // If not in REMINDER_ACTIVE or RETRY_ACTIVE, mark as active first
            val currentData = timerManager.getCurrentTimerData()
            if (currentData.currentState != com.logact.peereminder2.data.model.TimerState.REMINDER_ACTIVE &&
                currentData.currentState != com.logact.peereminder2.data.model.TimerState.RETRY_ACTIVE) {
                // Mark as active first
                timerManager.markReminderActive()
                kotlinx.coroutines.delay(100) // Small delay to ensure state is updated
            }
            
            // Now acknowledge with the calculated time
            timerManager.acknowledgeReminder(testAckTime)
            
            // Verify the alarm was scheduled
            val updatedData = timerManager.getCurrentTimerData()
            android.util.Log.d("MainActivity", "testScheduleReminderIn10Seconds: Updated state - nextReminderTime=${updatedData.nextReminderTime} (${java.util.Date(updatedData.nextReminderTime)})")
            
            if (updatedData.nextReminderTime == testReminderTime || 
                kotlin.math.abs(updatedData.nextReminderTime - testReminderTime) < 1000) {
                android.util.Log.d("MainActivity", "testScheduleReminderIn10Seconds: ✅ Test reminder scheduled successfully for 10 seconds from now")
            } else {
                android.util.Log.w("MainActivity", "testScheduleReminderIn10Seconds: ⚠️ Next reminder time doesn't match expected test time")
            }
        }
    }
}

/**
 * Sealed class representing app screens
 */
private sealed class Screen {
    object Main : Screen()
    object Settings : Screen()
}

/**
 * Composable that handles onboarding completion
 */
@Composable
private fun OnboardingHandler(settingsViewModel: SettingsViewModel) {
    val uiState by settingsViewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        // Onboarding is considered complete after first permission request
        // This is handled in the permission launcher callback
    }
}

/**
 * Composable that observes settings changes and reloads interval in TimerManager when it changes
 */
@Composable
private fun IntervalChangeObserver(settingsViewModel: SettingsViewModel) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
    
    LaunchedEffect(uiState.reminderIntervalMs) {
        // Reload interval in TimerManager when it changes
        activity?.reloadTimerInterval()
    }
}

/**
 * Composable that checks for overdue reminders when timer state changes.
 * 
 * This is more efficient than periodic checks because:
 * 1. It only checks when the timer state changes (via StateFlow)
 * 2. It first reloads state from storage to sync with ReminderReceiver changes
 * 3. If reminder is overdue and still PENDING, it transitions to ACTIVE
 * 
 * This ensures:
 * - State changes from ReminderReceiver are detected
 * - Overdue reminders are handled even if alarm doesn't fire
 * - No unnecessary periodic polling
 */
@Composable
private fun OverdueReminderChecker(timerManager: TimerManager) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
    val timerData by timerManager.timerData.collectAsState()
    
    LaunchedEffect(timerData.currentState, timerData.nextReminderTime) {
        android.util.Log.d("MainActivity", "OverdueReminderChecker: State or time changed - state=${timerData.currentState}, nextReminderTime=${timerData.nextReminderTime}")
        // Check when state or next reminder time changes
        activity?.checkAndHandleOverdueReminder()
    }
}

/**
 * Dialog shown when notification permission is denied.
 * Guides user to enable notification permission in app settings.
 */
@Composable
private fun PermissionDeniedDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Notification Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This app needs notification permission to show reminders. " +
                            "Please enable notifications in app settings to receive reminders.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(onClick = {
                    onOpenSettings()
                    onDismiss()
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        )
    }
}

/**
 * Dialog shown when exact alarm permission is not granted.
 * Guides user to enable exact alarm permission in system settings.
 */
@Composable
private fun ExactAlarmPermissionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Exact Alarm Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "This app needs exact alarm permission to show reminders reliably.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "How to enable:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Tap 'Open App Settings' below",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "2. Look for 'Schedule exact alarms' or 'Alarms & reminders'",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "3. Enable the toggle",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note: The setting name may vary by device. If you don't see it, check under 'Special app access' or 'App permissions'.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Without this permission, reminders may not work when the app is closed or the device is in sleep mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onOpenSettings()
                    onDismiss()
                }) {
                    Text("Open App Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Later")
                }
            }
        )
    }
}

/**
 * Alert shown when reminder is due but notification permission is missing.
 * This ensures user is notified even if permission was denied.
 */
@Composable
private fun PermissionMissingAlert(
    notificationService: NotificationService,
    timerManager: TimerManager
) {
    val timerData by timerManager.timerData.collectAsState()
    var showAlert by remember { mutableStateOf(false) }
    val activity = androidx.compose.ui.platform.LocalContext.current as? MainActivity
    
    // Check if reminder is active but permission is missing
    LaunchedEffect(timerData.currentState) {
        val isReminderActive = timerData.currentState == TimerState.REMINDER_ACTIVE ||
                timerData.currentState == TimerState.RETRY_ACTIVE
        
        if (isReminderActive && !notificationService.isNotificationPermissionGranted()) {
            showAlert = true
        } else {
            showAlert = false
        }
    }
    
    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = {
                Text(
                    text = "Reminder Active - Permission Missing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Your reminder is due, but notifications are disabled.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enable notification permission to receive alerts.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    activity?.openNotificationSettings()
                    showAlert = false
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAlert = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

