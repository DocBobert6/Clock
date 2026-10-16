package com.example.clock.ui.alarms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmsScreen(
    viewModel: AlarmsViewModel,
    onOpenSettings: () -> Unit
) {
    val alarms by viewModel.alarmsList.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    val is24Hour by viewModel.is24Hour.collectAsStateWithLifecycle()
    val nextRingCountdown by viewModel.nextRingCountdown.collectAsStateWithLifecycle()
    val showCreateSheet by viewModel.showCreateSheet.collectAsStateWithLifecycle()
    val editingAlarm by viewModel.editingAlarm.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (msg == "Alarm deleted") "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete()
            }
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.toggleEditMode() },
                        modifier = Modifier.testTag("alarms_edit_button")
                    ) {
                        Text(
                            text = if (isEditMode) "Done" else "Edit",
                            fontSize = 17.sp,
                            color = Color(0xFF0A84FF),
                            fontWeight = FontWeight.Normal
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("alarms_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFF0A84FF)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.openCreateAlarm() },
                            modifier = Modifier.testTag("alarms_add_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Alarm",
                                tint = Color(0xFF0A84FF),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                // Large Screen Title
                Text(
                    text = "Alarms",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Subtitle: "Rings in 7 hr 12 min"
                nextRingCountdown?.let { countdown ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = countdown,
                        fontSize = 15.sp,
                        color = Color(0xFF0A84FF),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alarms List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(alarms, key = { it.alarm.id }) { item ->
                        val alarm = item.alarm
                        val alpha = if (alarm.isEnabled) 1.0f else 0.45f

                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.openEditAlarm(alarm) }
                                    .padding(vertical = 14.dp)
                                    .alpha(alpha)
                                    .testTag("alarm_row_${alarm.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Minus button in edit mode + Time and repeat summary
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (isEditMode) {
                                        IconButton(
                                            onClick = { viewModel.deleteAlarm(alarm) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("delete_alarm_${alarm.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RemoveCircle,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFFF453A)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    Column {
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                text = item.timeFormatted,
                                                fontSize = 54.sp,
                                                fontWeight = FontWeight.Light,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                            if (item.amPmFormatted.isNotEmpty()) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = item.amPmFormatted,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Light,
                                                    color = Color(0xFF8E8E93),
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                            }
                                        }

                                        val summary = if (alarm.isRepeating()) {
                                            "${alarm.label}, ${alarm.getRepeatSummary()}"
                                        } else {
                                            alarm.label
                                        }
                                        Text(
                                            text = summary,
                                            fontSize = 14.sp,
                                            color = Color(0xFF8E8E93)
                                        )
                                    }
                                }

                                // Right: Switch
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { viewModel.toggleAlarm(alarm, it) },
                                    modifier = Modifier.testTag("alarm_switch_${alarm.id}"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF30D158)
                                    )
                                )
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }

            // Create / Edit Sheet
            if (showCreateSheet) {
                AlarmEditSheet(
                    alarm = editingAlarm,
                    is24Hour = is24Hour,
                    onSave = { viewModel.saveAlarm(it) },
                    onDelete = { viewModel.deleteAlarm(it) },
                    onDismiss = { viewModel.dismissCreateSheet() }
                )
            }
        }
    }
}
