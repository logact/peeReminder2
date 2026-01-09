package com.logact.peereminder2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Main screen composable displaying:
 * - Countdown timer (large, prominent)
 * - Next reminder time
 * - Pause/Start button (large button)
 * - Acknowledgment button (conditionally visible)
 * - Settings button
 * - Permission warning (if notification permission is missing)
 */
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onSettingsClick: () -> Unit = {},
    showPermissionWarning: Boolean = false,
    onOpenSettings: (() -> Unit)? = null,
    onTestTriggerReminder: (() -> Unit)? = null,
    onTestScheduleReminder: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Header with title and settings button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(48.dp)) // Balance for settings button
            Text(
                text = "Pee Reminder",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }
        
        // Permission warning banner (if permission is missing)
        if (showPermissionWarning && onOpenSettings != null) {
            PermissionWarningBanner(
                onOpenSettings = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Countdown Timer (Large, Prominent)
        CountdownDisplay(
            countdownText = uiState.countdownText,
            modifier = Modifier.fillMaxWidth()
        )
        
        // Next Reminder Time
        NextReminderTimeDisplay(
            nextReminderTimeText = uiState.nextReminderTimeText,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Pause/Start Button (Large Button)
        PauseStartButton(
            isPaused = uiState.isPaused,
            onPause = { viewModel.pauseTimer() },
            onStart = { viewModel.startTimer() },
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        )
        
        // Acknowledgment Button (Conditionally Visible)
        if (uiState.showAcknowledgmentButton) {
            AcknowledgmentButton(
                onClick = { viewModel.acknowledgeReminder() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )
        }
        
        // Test Buttons (for debugging/verification)
        if (onTestTriggerReminder != null || onTestScheduleReminder != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Test Functions",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onTestTriggerReminder != null) {
                    TestButton(
                        text = "Test Full Screen",
                        onClick = onTestTriggerReminder,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                if (onTestScheduleReminder != null) {
                    TestButton(
                        text = "Test Schedule (10s)",
                        onClick = onTestScheduleReminder,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Countdown timer display - large and prominent
 */
@Composable
private fun CountdownDisplay(
    countdownText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Time Remaining",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = countdownText,
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Next reminder time display
 */
@Composable
private fun NextReminderTimeDisplay(
    nextReminderTimeText: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Next Reminder",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = nextReminderTimeText,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Pause/Start button - large, accessible button
 * 
 * When paused: shows "Start" button to resume timer
 * When running: shows "Pause" button to pause timer
 */
@Composable
private fun PauseStartButton(
    isPaused: Boolean,
    onPause: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = if (isPaused) onStart else onPause,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPaused) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isPaused) "Start" else "Pause",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isPaused) "Tap to start reminders" else "Tap to pause reminders",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Acknowledgment button - shown when reminder is active and enabled in settings
 */
@Composable
private fun AcknowledgmentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text(
            text = "I've Peed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Test button for debugging/verification
 */
@Composable
private fun TestButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Permission warning banner - shown when notification permission is missing
 */
@Composable
private fun PermissionWarningBanner(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Notifications Disabled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enable notifications to receive reminders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onOpenSettings) {
                Text(
                    text = "Settings",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

