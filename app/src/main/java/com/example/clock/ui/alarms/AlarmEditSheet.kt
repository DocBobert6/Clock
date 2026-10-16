package com.example.clock.ui.alarms

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clock.data.model.Alarm
import com.example.clock.ui.components.WheelPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditSheet(
    alarm: Alarm?,
    is24Hour: Boolean,
    onSave: (Alarm) -> Unit,
    onDelete: (Alarm) -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = alarm != null
    val initialAlarm = alarm ?: Alarm(
        hour = 7,
        minute = 0,
        label = "Alarm",
        isEnabled = true,
        daysOfWeek = 0
    )

    var hour by remember { mutableIntStateOf(initialAlarm.hour) }
    var minute by remember { mutableIntStateOf(initialAlarm.minute) }
    var label by remember { mutableStateOf(initialAlarm.label) }
    var daysMask by remember { mutableIntStateOf(initialAlarm.daysOfWeek) }
    var soundUri by remember { mutableStateOf(initialAlarm.soundUri) }
    var soundName by remember { mutableStateOf(initialAlarm.soundName) }
    var vibrationPattern by remember { mutableStateOf(initialAlarm.vibrationPattern) }
    var snoozeEnabled by remember { mutableStateOf(initialAlarm.snoozeLengthMinutes > 0) }
    var snoozeMinutes by remember { mutableIntStateOf(initialAlarm.snoozeLengthMinutes.coerceAtLeast(9)) }
    var deleteAfterRings by remember { mutableStateOf(initialAlarm.deleteAfterRings) }
    var volume by remember { mutableFloatStateOf(initialAlarm.volume) }
    var isGradualVolume by remember { mutableStateOf(initialAlarm.isGradualVolume) }
    var gradualVolumeSeconds by remember { mutableIntStateOf(initialAlarm.gradualVolumeSeconds) }

    var showSoundPicker by remember { mutableStateOf(false) }

    val hoursList = remember(is24Hour) {
        if (is24Hour) (0..23).map { String.format("%02d", it) }
        else (1..12).map { it.toString() }
    }
    val minutesList = remember { (0..59).map { String.format("%02d", it) } }
    val amPmList = listOf("AM", "PM")

    // Determine initial index for wheel pickers
    val initialHourIndex = if (is24Hour) hour else {
        val rem = hour % 12
        if (rem == 0) 11 else rem - 1
    }
    val initialMinuteIndex = minute
    val initialAmPmIndex = if (hour < 12) 0 else 1

    var amPmIndex by remember { mutableIntStateOf(initialAmPmIndex) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top Nav: Cancel / Title / Save
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("alarm_edit_cancel_button")) {
                    Text("Cancel", color = Color(0xFF0A84FF), fontSize = 17.sp)
                }

                Text(
                    text = if (isEditing) "Edit Alarm" else "Add Alarm",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                TextButton(
                    onClick = {
                        val computedHour = if (is24Hour) hour else {
                            val base12 = (hour % 12)
                            if (amPmIndex == 1) base12 + 12 else base12
                        }
                        onSave(
                            initialAlarm.copy(
                                hour = computedHour,
                                minute = minute,
                                label = label.ifBlank { "Alarm" },
                                daysOfWeek = daysMask,
                                soundUri = soundUri,
                                soundName = soundName,
                                vibrationPattern = vibrationPattern,
                                snoozeLengthMinutes = if (snoozeEnabled) snoozeMinutes else 0,
                                deleteAfterRings = deleteAfterRings,
                                volume = volume,
                                isGradualVolume = isGradualVolume,
                                gradualVolumeSeconds = gradualVolumeSeconds,
                                isEnabled = true
                            )
                        )
                    },
                    modifier = Modifier.testTag("alarm_edit_save_button")
                ) {
                    Text("Save", color = Color(0xFF0A84FF), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Wheel Pickers Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        items = hoursList,
                        initialIndex = initialHourIndex,
                        onItemSelected = { idx ->
                            hour = if (is24Hour) idx else (idx + 1)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Text(":", fontSize = 28.sp, fontWeight = FontWeight.Light, color = Color.White)

                    WheelPicker(
                        items = minutesList,
                        initialIndex = initialMinuteIndex,
                        onItemSelected = { idx -> minute = idx },
                        modifier = Modifier.weight(1f)
                    )

                    if (!is24Hour) {
                        WheelPicker(
                            items = amPmList,
                            initialIndex = initialAmPmIndex,
                            onItemSelected = { idx -> amPmIndex = idx },
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Container for Properties
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Repeat row with Day Chips
                        Text(
                            text = "Repeat",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                            for (day in 1..7) {
                                val isSelected = (daysMask and (1 shl day)) != 0
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        daysMask = if (isSelected) {
                                            daysMask and (1 shl day).inv()
                                        } else {
                                            daysMask or (1 shl day)
                                        }
                                    },
                                    label = { Text(dayLabels[day - 1], fontSize = 13.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF0A84FF),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF38383A),
                                        labelColor = Color(0xFF8E8E93)
                                    ),
                                    modifier = Modifier.size(width = 44.dp, height = 36.dp)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF38383A), thickness = 0.5.dp)

                        // Label row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Label", fontSize = 17.sp, color = Color.White)
                            OutlinedTextField(
                                value = label,
                                onValueChange = { label = it },
                                placeholder = { Text("Alarm", color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier
                                    .width(180.dp)
                                    .testTag("alarm_label_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0A84FF),
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF38383A), thickness = 0.5.dp)

                        // Sound Picker Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSoundPicker = true }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sound", fontSize = 17.sp, color = Color.White)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(soundName, fontSize = 17.sp, color = Color(0xFF8E8E93))
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8E93))
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF38383A), thickness = 0.5.dp)

                        // Snooze Toggle & Length
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Snooze", fontSize = 17.sp, color = Color.White)
                            Switch(
                                checked = snoozeEnabled,
                                onCheckedChange = { snoozeEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF30D158)
                                )
                            )
                        }

                        if (snoozeEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Snooze Duration", fontSize = 14.sp, color = Color(0xFF8E8E93))
                                Text("$snoozeMinutes min", fontSize = 14.sp, color = Color(0xFF0A84FF))
                            }
                            Slider(
                                value = snoozeMinutes.toFloat(),
                                onValueChange = { snoozeMinutes = it.toInt() },
                                valueRange = 1f..30f,
                                steps = 28,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF0A84FF)
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF38383A), thickness = 0.5.dp)

                        // Delete After Rings
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Delete After Rings", fontSize = 17.sp, color = Color.White)
                                Text("Deletes alarm when dismissed", fontSize = 13.sp, color = Color(0xFF8E8E93))
                            }
                            Switch(
                                checked = deleteAfterRings,
                                onCheckedChange = { deleteAfterRings = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF30D158)
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF38383A), thickness = 0.5.dp)

                        // Volume Slider & Gradual Volume
                        Text("Volume", fontSize = 17.sp, color = Color.White)
                        Slider(
                            value = volume,
                            onValueChange = { volume = it },
                            valueRange = 0.1f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color(0xFF0A84FF)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Gradual Volume Increase", fontSize = 15.sp, color = Color.White)
                            Switch(
                                checked = isGradualVolume,
                                onCheckedChange = { isGradualVolume = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF30D158)
                                )
                            )
                        }
                    }
                }

                // Delete Alarm Button (if editing)
                if (isEditing) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onDelete(initialAlarm) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("delete_alarm_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2C2C2E),
                            contentColor = Color(0xFFFF453A)
                        )
                    ) {
                        Text("Delete Alarm", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        if (showSoundPicker) {
            SoundPickerSheet(
                selectedSoundUri = soundUri,
                onSoundSelected = { uri, name ->
                    soundUri = uri
                    soundName = name
                    showSoundPicker = false
                },
                onDismiss = { showSoundPicker = false }
            )
        }
    }
}
