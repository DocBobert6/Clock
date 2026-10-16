package com.example.clock.ui.settings

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Widgets
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
import com.example.clock.data.preferences.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: ClockPreferences,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val appearance by prefs.appearanceMode.collectAsStateWithLifecycle(initialValue = AppearanceMode.DARK)
    val clockFaceStyle by prefs.clockFaceStyle.collectAsStateWithLifecycle(initialValue = ClockFaceStyle.DIGITAL)
    val is24Hour by prefs.is24HourFormat.collectAsStateWithLifecycle(initialValue = false)
    val showSeconds by prefs.showSeconds.collectAsStateWithLifecycle(initialValue = true)
    val silenceMinutes by prefs.alarmSilenceAfterMinutes.collectAsStateWithLifecycle(initialValue = 10)
    val snoozeMinutes by prefs.alarmSnoozeLengthMinutes.collectAsStateWithLifecycle(initialValue = 9)
    val volumeAction by prefs.volumeButtonAction.collectAsStateWithLifecycle(initialValue = VolumeButtonAction.SNOOZE)

    var showWidgetGallery by remember { mutableStateOf(false) }
    var showAboutSheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Battery optimization check
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        pm.isIgnoringBatteryOptimizations(context.packageName)
    } else true

    // Notification check
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notificationsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        nm.areNotificationsEnabled()
    } else true

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
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(onClick = onClose, modifier = Modifier.testTag("close_settings_button")) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF8E8E93))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Appearance Section
            Text(
                text = "APPEARANCE",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme", fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("Automatic", AppearanceMode.SYSTEM),
                            Pair("Light", AppearanceMode.LIGHT),
                            Pair("Dark", AppearanceMode.DARK)
                        ).forEach { (label, mode) ->
                            val isSel = appearance == mode
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    kotlinx.coroutines.runBlocking { prefs.setAppearanceMode(mode) }
                                },
                                label = { Text(label, fontSize = 13.sp) },
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C2E), thickness = 0.5.dp)

                    // Clock Face Style: Digital vs Analog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clock Face Style", fontSize = 16.sp, color = Color.White)
                        Row {
                            FilterChip(
                                selected = clockFaceStyle == ClockFaceStyle.DIGITAL,
                                onClick = {
                                    kotlinx.coroutines.runBlocking { prefs.setClockFaceStyle(ClockFaceStyle.DIGITAL) }
                                },
                                label = { Text("Digital", fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0A84FF),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = Color(0xFF8E8E93)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilterChip(
                                selected = clockFaceStyle == ClockFaceStyle.ANALOG,
                                onClick = {
                                    kotlinx.coroutines.runBlocking { prefs.setClockFaceStyle(ClockFaceStyle.ANALOG) }
                                },
                                label = { Text("Analog", fontSize = 13.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0A84FF),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF2C2C2E),
                                    labelColor = Color(0xFF8E8E93)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time & Display Section
            Text(
                text = "TIME & DISPLAY",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("24-Hour Time", fontSize = 16.sp, color = Color.White)
                        Switch(
                            checked = is24Hour,
                            onCheckedChange = {
                                kotlinx.coroutines.runBlocking { prefs.set24HourFormat(it) }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF30D158)
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C2E), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Seconds", fontSize = 16.sp, color = Color.White)
                        Switch(
                            checked = showSeconds,
                            onCheckedChange = {
                                kotlinx.coroutines.runBlocking { prefs.setShowSeconds(it) }
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

            // Alarm Defaults Section
            Text(
                text = "ALARM DEFAULTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Silence After", fontSize = 16.sp, color = Color.White)
                        Text("$silenceMinutes min", fontSize = 16.sp, color = Color(0xFF0A84FF))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C2E), thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Default Snooze Length", fontSize = 16.sp, color = Color.White)
                        Text("$snoozeMinutes min", fontSize = 16.sp, color = Color(0xFF0A84FF))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C2E), thickness = 0.5.dp)

                    Text("Volume Buttons Action", fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Pair("Snooze", VolumeButtonAction.SNOOZE),
                            Pair("Dismiss", VolumeButtonAction.DISMISS),
                            Pair("Silence", VolumeButtonAction.SILENCE)
                        ).forEach { (lbl, act) ->
                            val isSel = volumeAction == act
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    kotlinx.coroutines.runBlocking { prefs.setVolumeButtonAction(act) }
                                },
                                label = { Text(lbl, fontSize = 12.sp) },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Add Widgets Gallery Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWidgetGallery = true }
                    .testTag("settings_add_widgets_row"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Widgets, contentDescription = null, tint = Color(0xFF0A84FF))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Add Widgets", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("11 live interactive widgets gallery", fontSize = 13.sp, color = Color(0xFF8E8E93))
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8E93))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reliability & Permissions Status Card
            Text(
                text = "RELIABILITY & PERMISSIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF8E8E93),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Notifications
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Notifications", fontSize = 16.sp, color = Color.White)
                            Text(
                                if (notificationsEnabled) "Granted" else "Disabled — Tap to enable",
                                fontSize = 13.sp,
                                color = if (notificationsEnabled) Color(0xFF30D158) else Color(0xFFFF453A)
                            )
                        }
                        if (!notificationsEnabled) {
                            TextButton(onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }) {
                                Text("Fix", color = Color(0xFF0A84FF))
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF2C2C2E), thickness = 0.5.dp)

                    // Battery optimization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Exact Alarms & Background", fontSize = 16.sp, color = Color.White)
                            Text(
                                if (isIgnoringBatteryOptimizations) "Optimal (unrestricted)" else "Optimized — Tap to allow background",
                                fontSize = 13.sp,
                                color = if (isIgnoringBatteryOptimizations) Color(0xFF30D158) else Color(0xFFFF9F0A)
                            )
                        }
                        if (!isIgnoringBatteryOptimizations) {
                            TextButton(onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(intent)
                                }
                            }) {
                                Text("Fix", color = Color(0xFF0A84FF))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About Clock Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAboutSheet = true }
                    .testTag("settings_about_row"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("About Clock", fontSize = 16.sp, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Version 1.0", fontSize = 14.sp, color = Color(0xFF8E8E93))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF8E8E93))
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        if (showWidgetGallery) {
            WidgetGallerySheet(onDismiss = { showWidgetGallery = false })
        }

        if (showAboutSheet) {
            AboutSheet(onDismiss = { showAboutSheet = false })
        }
    }
}
