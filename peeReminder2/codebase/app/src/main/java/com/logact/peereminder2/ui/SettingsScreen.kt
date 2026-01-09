package com.logact.peereminder2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Settings screen composable
 * 
 * Displays:
 * - Acknowledgment button visibility toggle
 * - Other settings (for future expansion)
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Settings content
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Reminder Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Reminder interval input
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Reminder Interval",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set the time interval between reminders (format: hh-mm-ss)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ReminderIntervalInput(
                            currentIntervalMs = uiState.reminderIntervalMs,
                            onIntervalChange = { intervalMs ->
                                viewModel.updateReminderInterval(intervalMs)
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Sleep mode time range configuration
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Sleep Mode Time Range",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Set automatic sleep mode time range (format: HH:mm). Reminders will be paused during this time.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SleepModeTimeRangeInput(
                            startTime = uiState.sleepModeStartTime,
                            endTime = uiState.sleepModeEndTime,
                            onTimeRangeChange = { startTime, endTime ->
                                viewModel.updateSleepModeTimeRange(startTime, endTime)
                            }
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Acknowledgment button visibility toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Show Acknowledgment Button",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enable to show 'I've Peed' button when reminder is active",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.showAcknowledgmentButton,
                            onCheckedChange = { enabled ->
                                viewModel.toggleAcknowledgmentButtonVisibility(enabled)
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This app helps you remember to pee at regular intervals. " +
                                "Use the sleep mode toggle on the main screen to pause reminders during sleep.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * Composable for reminder interval input with hh-mm-ss format
 * 
 * @param currentIntervalMs Current interval in milliseconds
 * @param onIntervalChange Callback when interval is changed (in milliseconds)
 */
@Composable
private fun ReminderIntervalInput(
    currentIntervalMs: Long,
    onIntervalChange: (Long) -> Unit
) {
    // Convert milliseconds to hh-mm-ss format
    val hours = (currentIntervalMs / (1000 * 60 * 60)).toInt()
    val minutes = ((currentIntervalMs % (1000 * 60 * 60)) / (1000 * 60)).toInt()
    val seconds = ((currentIntervalMs % (1000 * 60)) / 1000).toInt()
    val formattedText = String.format("%02d-%02d-%02d", hours, minutes, seconds)
    
    var textFieldValue by remember(currentIntervalMs) { 
        mutableStateOf(TextFieldValue(formattedText)) 
    }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Update text field when currentIntervalMs changes from outside (but not while user is typing)
    LaunchedEffect(currentIntervalMs) {
        // Only update if the formatted text is different and doesn't match current input
        // This prevents overwriting user input while they're typing
        val currentFormatted = String.format("%02d-%02d-%02d", hours, minutes, seconds)
        if (textFieldValue.text != currentFormatted) {
            // Check if current input is a valid partial input (user might be typing)
            val parts = textFieldValue.text.split("-")
            val isPartialInput = parts.size < 3 || 
                    parts.any { it.isEmpty() } ||
                    !textFieldValue.text.matches(Regex("\\d{1,2}-\\d{1,2}-\\d{1,2}"))
            
            // Only update if it's not a valid partial input
            if (!isPartialInput) {
                textFieldValue = TextFieldValue(currentFormatted)
                isError = false
                errorMessage = ""
            }
        }
    }
    
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            // Only allow digits and hyphens
            val filtered = newValue.text.filter { it.isDigit() || it == '-' }
            if (filtered.length <= 8) { // Max: 99-99-99
                textFieldValue = newValue.copy(text = filtered)
                
                // Validate and parse
                val parts = filtered.split("-")
                if (parts.size == 3) {
                    try {
                        val h = parts[0].toIntOrNull() ?: 0
                        val m = parts[1].toIntOrNull() ?: 0
                        val s = parts[2].toIntOrNull() ?: 0
                        
                        // Validate ranges
                        if (h >= 0 && h < 24 && m >= 0 && m < 60 && s >= 0 && s < 60) {
                            val totalMs = (h * 60 * 60 * 1000L) + (m * 60 * 1000L) + (s * 1000L)
                            if (totalMs > 0) {
                                isError = false
                                errorMessage = ""
                                onIntervalChange(totalMs)
                            } else {
                                isError = true
                                errorMessage = "Interval must be greater than 0"
                            }
                        } else {
                            isError = true
                            errorMessage = "Invalid time: hours (0-23), minutes (0-59), seconds (0-59)"
                        }
                    } catch (e: Exception) {
                        isError = true
                        errorMessage = "Invalid format"
                    }
                } else if (filtered.isNotEmpty() && !filtered.endsWith("-")) {
                    // Still typing, don't show error yet
                    isError = false
                    errorMessage = ""
                } else if (filtered.isEmpty()) {
                    isError = true
                    errorMessage = "Interval cannot be empty"
                }
            }
        },
        label = { Text("Interval (hh-mm-ss)") },
        placeholder = { Text("02-00-00") },
        isError = isError,
        supportingText = {
            if (isError) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
            } else {
                Text("Current: ${formatInterval(currentIntervalMs)}")
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Format interval in milliseconds to hh-mm-ss string
 */
private fun formatInterval(intervalMs: Long): String {
    val hours = (intervalMs / (1000 * 60 * 60)).toInt()
    val minutes = ((intervalMs % (1000 * 60 * 60)) / (1000 * 60)).toInt()
    val seconds = ((intervalMs % (1000 * 60)) / 1000).toInt()
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

/**
 * Composable for sleep mode time range input with HH:mm format
 * 
 * @param startTime Current sleep mode start time in HH:mm format, or null if disabled
 * @param endTime Current sleep mode end time in HH:mm format, or null if disabled
 * @param onTimeRangeChange Callback when time range is changed (startTime, endTime)
 */
@Composable
private fun SleepModeTimeRangeInput(
    startTime: String?,
    endTime: String?,
    onTimeRangeChange: (String?, String?) -> Unit
) {
    var startTimeValue by remember(startTime) { 
        mutableStateOf(TextFieldValue(startTime ?: "")) 
    }
    var endTimeValue by remember(endTime) { 
        mutableStateOf(TextFieldValue(endTime ?: "")) 
    }
    var startTimeError by remember { mutableStateOf(false) }
    var endTimeError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    // Update text fields when values change from outside
    LaunchedEffect(startTime) {
        if (startTimeValue.text != (startTime ?: "")) {
            startTimeValue = TextFieldValue(startTime ?: "")
        }
    }
    LaunchedEffect(endTime) {
        if (endTimeValue.text != (endTime ?: "")) {
            endTimeValue = TextFieldValue(endTime ?: "")
        }
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Start time input
        OutlinedTextField(
            value = startTimeValue,
            onValueChange = { newValue ->
                val filtered = newValue.text.filter { it.isDigit() || it == ':' }
                if (filtered.length <= 5) { // Max: HH:mm
                    startTimeValue = newValue.copy(text = filtered)
                    
                    // Validate format
                    if (filtered.isEmpty()) {
                        startTimeError = false
                        onTimeRangeChange(null, endTime)
                    } else if (isValidTimeFormat(filtered)) {
                        startTimeError = false
                        errorMessage = ""
                        onTimeRangeChange(filtered, endTime)
                    } else if (filtered.length >= 3) {
                        // Only show error if user has typed enough characters
                        startTimeError = true
                        errorMessage = "Invalid format. Use HH:mm (e.g., 22:00)"
                    }
                }
            },
            label = { Text("Start Time") },
            placeholder = { Text("22:00") },
            isError = startTimeError,
            modifier = Modifier.weight(1f),
            supportingText = {
                if (startTimeError) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Text(
            text = "to",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        // End time input
        OutlinedTextField(
            value = endTimeValue,
            onValueChange = { newValue ->
                val filtered = newValue.text.filter { it.isDigit() || it == ':' }
                if (filtered.length <= 5) { // Max: HH:mm
                    endTimeValue = newValue.copy(text = filtered)
                    
                    // Validate format
                    if (filtered.isEmpty()) {
                        endTimeError = false
                        onTimeRangeChange(startTime, null)
                    } else if (isValidTimeFormat(filtered)) {
                        endTimeError = false
                        errorMessage = ""
                        onTimeRangeChange(startTime, filtered)
                    } else if (filtered.length >= 3) {
                        // Only show error if user has typed enough characters
                        endTimeError = true
                        errorMessage = "Invalid format. Use HH:mm (e.g., 07:00)"
                    }
                }
            },
            label = { Text("End Time") },
            placeholder = { Text("07:00") },
            isError = endTimeError,
            modifier = Modifier.weight(1f),
            supportingText = {
                if (endTimeError) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

/**
 * Validate time format HH:mm
 */
private fun isValidTimeFormat(time: String): Boolean {
    val pattern = Regex("^([0-1]?[0-9]|2[0-3]):([0-5][0-9])$")
    return pattern.matches(time)
}

