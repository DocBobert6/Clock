package com.example.clock.ui.timers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clock.data.model.TimerItem
import com.example.clock.data.model.TimerState
import com.example.clock.ui.alarms.SoundPickerSheet
import com.example.clock.ui.components.CircularActionButton
import com.example.clock.ui.components.WheelPicker

@Composable
fun TimersScreen(
    viewModel: TimersViewModel,
    onOpenSettings: () -> Unit
) {
    val runningTimers by viewModel.runningTimers.collectAsStateWithLifecycle()

    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(5) }
    var seconds by remember { mutableIntStateOf(0) }
    var label by remember { mutableStateOf("Timer") }
    var soundUri by remember { mutableStateOf("procedural:cosmic_pulse") }
    var soundName by remember { mutableStateOf("Cosmic Pulse") }
    var showSoundPicker by remember { mutableStateOf(false) }

    val hoursList = remember { (0..23).map { it.toString() } }
    val minutesList = remember { (0..59).map { it.toString() } }
    val secondsList = remember { (0..59).map { it.toString() } }

    val presets = listOf(
        Pair("1 min", 60),
        Pair("5 min", 300),
        Pair("10 min", 600),
        Pair("15 min", 900),
        Pair("30 min", 1800)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(1.dp))
                IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("timers_settings_button")) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF0A84FF)
                    )
                }
            }

            // Title
            Text(
                text = "Timers",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // If there are running timers, render them first
                if (runningTimers.isNotEmpty()) {
                    item {
                        Text(
                            text = "ACTIVE TIMERS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(runningTimers, key = { it.id }) { timer ->
                        RunningTimerCard(
                            timer = timer,
                            onPause = { viewModel.pauseTimer(timer) },
                            onResume = { viewModel.resumeTimer(timer) },
                            onCancel = { viewModel.cancelTimer(timer) },
                            onAddOneMin = { viewModel.addOneMinute(timer) },
                            formatRemaining = { viewModel.formatRemaining(it) },
                            formatEndsAt = { viewModel.formatEndsAt(it) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SET NEW TIMER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Timer Setup Wheel Pickers
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WheelPicker(
                            items = hoursList,
                            initialIndex = hours,
                            onItemSelected = { hours = it },
                            modifier = Modifier.weight(1f)
                        )
                        Text("hours", fontSize = 14.sp, color = Color(0xFF8E8E93), modifier = Modifier.padding(end = 8.dp))

                        WheelPicker(
                            items = minutesList,
                            initialIndex = minutes,
                            onItemSelected = { minutes = it },
                            modifier = Modifier.weight(1f)
                        )
                        Text("min", fontSize = 14.sp, color = Color(0xFF8E8E93), modifier = Modifier.padding(end = 8.dp))

                        WheelPicker(
                            items = secondsList,
                            initialIndex = seconds,
                            onItemSelected = { seconds = it },
                            modifier = Modifier.weight(1f)
                        )
                        Text("sec", fontSize = 14.sp, color = Color(0xFF8E8E93))
                    }
                }

                // Setup Card for Label and Sound
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Label input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Label", fontSize = 17.sp, color = Color.White)
                                OutlinedTextField(
                                    value = label,
                                    onValueChange = { label = it },
                                    placeholder = { Text("Timer", color = Color.Gray) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(180.dp)
                                        .testTag("timer_label_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0A84FF),
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C2E), thickness = 0.5.dp)

                            // When Timer Ends Sound Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSoundPicker = true }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("When Timer Ends", fontSize = 17.sp, color = Color.White)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(soundName, fontSize = 17.sp, color = Color(0xFF8E8E93))
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8E93))
                                }
                            }
                        }
                    }
                }

                // Dual Action Buttons: Cancel (Gray) & Start (Green)
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularActionButton(
                            text = "Cancel",
                            containerColor = Color(0xFF38383A),
                            contentColor = Color(0xFF8E8E93),
                            onClick = {
                                hours = 0
                                minutes = 5
                                seconds = 0
                            },
                            testTag = "timer_cancel_setup_button"
                        )

                        CircularActionButton(
                            text = "Start",
                            containerColor = Color(0xFF30D158),
                            contentColor = Color(0xFF30D158),
                            onClick = {
                                viewModel.startNewTimer(hours, minutes, seconds, label, soundUri, soundName)
                            },
                            testTag = "timer_start_setup_button"
                        )
                    }
                }

                // Quick Presets Row
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "PRESETS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8E8E93),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presets) { (title, sec) ->
                            OutlinedButton(
                                onClick = {
                                    val h = sec / 3600
                                    val m = (sec % 3600) / 60
                                    val s = sec % 60
                                    viewModel.startNewTimer(h, m, s, title, soundUri, soundName)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0xFF1C1C1E),
                                    contentColor = Color(0xFF0A84FF)
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF38383A))
                                ),
                                modifier = Modifier.testTag("preset_${title.replace(" ", "_")}")
                            ) {
                                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
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

@Composable
fun RunningTimerCard(
    timer: TimerItem,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onAddOneMin: () -> Unit,
    formatRemaining: (Long) -> String,
    formatEndsAt: (Long) -> String
) {
    val progress = if (timer.totalSeconds > 0) {
        (timer.remainingSeconds.toFloat() / timer.totalSeconds).coerceIn(0f, 1f)
    } else 0f

    val accentBlue = Color(0xFF0A84FF)
    val isPaused = timer.state == TimerState.PAUSED

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("running_timer_${timer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Progress Ring
            Box(
                modifier = Modifier.size(76.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 4.dp.toPx()
                    val radius = (size.minDimension - strokeW) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Track
                    drawCircle(
                        color = Color(0xFF2C2C2E),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeW)
                    )

                    // Progress Arc
                    drawArc(
                        color = accentBlue,
                        startAngle = -90f,
                        sweepAngle = progress * 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = formatRemaining(timer.remainingSeconds),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Label and Ends At
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.label,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isPaused) "Paused" else "Ends at ${formatEndsAt(timer.endsAtTimestamp)}",
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93)
                )
            }

            // Buttons: +1:00, Pause/Resume, Cancel
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onAddOneMin,
                    modifier = Modifier.size(width = 54.dp, height = 36.dp)
                ) {
                    Text("+1:00", fontSize = 12.sp, color = Color(0xFF0A84FF))
                }

                IconButton(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentBlue.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = if (isPaused) "▶" else "❚❚",
                        color = accentBlue,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2E))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color(0xFF8E8E93), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
