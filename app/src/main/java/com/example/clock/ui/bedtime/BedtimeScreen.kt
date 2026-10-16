package com.example.clock.ui.bedtime

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clock.data.audio.ProceduralSoundGenerator
import com.example.clock.ui.components.CircularSleepDial

@Composable
fun BedtimeScreen(
    viewModel: BedtimeViewModel,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.bedtimeSettings.collectAsStateWithLifecycle()
    val isPlayingSound by viewModel.isSleepSoundPlaying.collectAsStateWithLifecycle()
    val activeSound by viewModel.activeSound.collectAsStateWithLifecycle()
    val soundVolume by viewModel.soundVolume.collectAsStateWithLifecycle()

    val bedTimeFormatted = String.format("%02d:%02d", settings.bedtimeHour, settings.bedtimeMinute)
    val wakeTimeFormatted = String.format("%02d:%02d", settings.wakeHour, settings.wakeMinute)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sleep Schedule",
                    fontSize = 17.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )

                IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("bedtime_settings_button")) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF0A84FF)
                    )
                }
            }

            // Title
            Text(
                text = "Bedtime",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Schedule Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Schedule On/Off Switch Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Bedtime Schedule",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (settings.isEnabled) "Schedule active" else "Schedule paused",
                                fontSize = 13.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }

                        Switch(
                            checked = settings.isEnabled,
                            onCheckedChange = { viewModel.toggleSchedule(it) },
                            modifier = Modifier.testTag("bedtime_schedule_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF30D158)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Days of week chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                        for (d in 1..7) {
                            val isSelected = (settings.daysOfWeek and (1 shl d)) != 0
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.toggleDay(d) },
                                label = { Text(dayLabels[d - 1], fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0A84FF),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = Color(0xFF8E8E93)
                                ),
                                modifier = Modifier.size(width = 42.dp, height = 34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Circular 24h Sleep Dial with Draggable Handles
                    CircularSleepDial(
                        bedtimeHour = settings.bedtimeHour,
                        bedtimeMinute = settings.bedtimeMinute,
                        wakeHour = settings.wakeHour,
                        wakeMinute = settings.wakeMinute,
                        onBedtimeChanged = { h, m -> viewModel.updateBedtime(h, m) },
                        onWakeChanged = { h, m -> viewModel.updateWakeTime(h, m) },
                        sleepGoalHours = settings.sleepGoalHours
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Readout row: Bedtime & Wake Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color(0xFF0A84FF), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("BEDTIME", fontSize = 11.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.SemiBold)
                                Text(bedTimeFormatted, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WbSunny, contentDescription = null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("WAKE UP", fontSize = 11.sp, color = Color(0xFF8E8E93), fontWeight = FontWeight.SemiBold)
                                Text(wakeTimeFormatted, fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wind Down & Do Not Disturb Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Wind Down Reminder", fontSize = 16.sp, color = Color.White)
                            Text("${settings.windDownMinutes} min before bedtime", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            val isSel = settings.windDownMinutes == mins
                            OutlinedButton(
                                onClick = { viewModel.updateWindDown(mins) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSel) Color(0xFF0A84FF).copy(alpha = 0.2f) else Color.Transparent,
                                    contentColor = if (isSel) Color(0xFF0A84FF) else Color(0xFF8E8E93)
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isSel) Color(0xFF0A84FF) else Color(0xFF38383A)
                                    )
                                ),
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                            ) {
                                Text("${mins}m", fontSize = 12.sp)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF38383A), thickness = 0.5.dp)

                    // Do Not Disturb toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Do Not Disturb During Sleep", fontSize = 16.sp, color = Color.White)
                            Text("Mutes non-alarm alerts during sleep window", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        }
                        Switch(
                            checked = settings.enableDnd,
                            onCheckedChange = { enable ->
                                if (enable && !viewModel.hasDndPermission(context)) {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                }
                                viewModel.toggleDnd(enable)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF30D158)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Procedural Sleep Sounds Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Sleep Ambiance", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text(
                                text = if (isPlayingSound) "Playing $activeSound" else "Procedural gapless relaxation sound",
                                fontSize = 13.sp,
                                color = if (isPlayingSound) Color(0xFF0A84FF) else Color(0xFF8E8E93)
                            )
                        }

                        IconButton(
                            onClick = {
                                if (isPlayingSound) viewModel.stopSleepSound()
                                else viewModel.startSleepSound(activeSound)
                            },
                            modifier = Modifier
                                .background(if (isPlayingSound) Color(0xFF0A84FF) else Color(0xFF2C2C2E), CircleShape)
                                .testTag("bedtime_sound_play_toggle")
                        ) {
                            Icon(
                                imageVector = if (isPlayingSound) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlayingSound) "Pause" else "Play",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Sound chips grid
                    val sounds = ProceduralSoundGenerator.SLEEP_SOUNDS
                    Column {
                        sounds.chunked(3).forEach { rowList ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowList.forEach { soundName ->
                                    val isSelected = activeSound == soundName
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.startSleepSound(soundName)
                                        },
                                        label = { Text(soundName, fontSize = 12.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0A84FF),
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFF2C2C2E),
                                            labelColor = Color(0xFF8E8E93)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    if (isPlayingSound) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.VolumeMute, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Slider(
                                value = soundVolume,
                                onValueChange = { viewModel.setSoundVolume(it) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF0A84FF)
                                )
                            )
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Launch Bedside Clock Mode Button
            Button(
                onClick = { viewModel.launchBedsideClock(context) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("launch_bedside_clock_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1C1C1E),
                    contentColor = Color(0xFF0A84FF)
                )
            ) {
                Icon(Icons.Default.Nightlight, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enter Bedside Clock Mode", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }
}
