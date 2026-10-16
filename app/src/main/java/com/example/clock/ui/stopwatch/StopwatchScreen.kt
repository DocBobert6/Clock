package com.example.clock.ui.stopwatch

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WatchLater
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
import com.example.clock.ui.components.CircularActionButton

@Composable
fun StopwatchScreen(
    viewModel: StopwatchViewModel
) {
    val context = LocalContext.current
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val elapsedMs by viewModel.elapsedTimeMs.collectAsStateWithLifecycle()
    val laps by viewModel.laps.collectAsStateWithLifecycle()
    val fastestLap by viewModel.fastestLapNumber.collectAsStateWithLifecycle()
    val slowestLap by viewModel.slowestLapNumber.collectAsStateWithLifecycle()

    var showAnalogFace by remember { mutableStateOf(false) }

    val formattedTime = remember(elapsedMs) { viewModel.formatTime(elapsedMs) }

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
                IconButton(
                    onClick = { showAnalogFace = !showAnalogFace },
                    modifier = Modifier.testTag("stopwatch_toggle_face_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.WatchLater,
                        contentDescription = "Toggle Analog/Digital Face",
                        tint = if (showAnalogFace) Color(0xFF0A84FF) else Color(0xFF8E8E93)
                    )
                }

                if (laps.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.shareLaps(context) },
                        modifier = Modifier.testTag("stopwatch_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Laps",
                            tint = Color(0xFF0A84FF)
                        )
                    }
                }
            }

            // Display: Digital or Analog
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = showAnalogFace, label = "stopwatch_face") { analog ->
                    if (analog) {
                        StopwatchAnalogView(
                            totalElapsedMs = elapsedMs,
                            size = 230.dp
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedTime,
                                fontSize = 68.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dual Action Buttons (Lap/Reset on left, Start/Stop on right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Button: Lap (when running) or Reset (when stopped)
                val leftButtonText = if (isRunning) "Lap" else "Reset"
                val leftEnabled = isRunning || elapsedMs > 0
                CircularActionButton(
                    text = leftButtonText,
                    containerColor = if (leftEnabled) Color(0xFF38383A) else Color(0xFF2C2C2E),
                    contentColor = if (leftEnabled) Color.White else Color(0xFF8E8E93),
                    onClick = {
                        if (isRunning) viewModel.lap()
                        else viewModel.reset()
                    },
                    testTag = "stopwatch_left_button"
                )

                // Right Button: Start (Green) or Stop (Red)
                if (isRunning) {
                    CircularActionButton(
                        text = "Stop",
                        containerColor = Color(0xFFFF453A),
                        contentColor = Color(0xFFFF453A),
                        onClick = { viewModel.stop() },
                        testTag = "stopwatch_stop_button"
                    )
                } else {
                    CircularActionButton(
                        text = "Start",
                        containerColor = Color(0xFF30D158),
                        contentColor = Color(0xFF30D158),
                        onClick = { viewModel.start() },
                        testTag = "stopwatch_start_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lap List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                items(laps, key = { it.id }) { lap ->
                    val isFastest = lap.lapNumber == fastestLap
                    val isSlowest = lap.lapNumber == slowestLap

                    val lapTextColor = when {
                        isFastest -> Color(0xFF30D158) // Green
                        isSlowest -> Color(0xFFFF453A) // Red
                        else -> MaterialTheme.colorScheme.onBackground
                    }

                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lap ${lap.lapNumber}",
                                fontSize = 17.sp,
                                color = lapTextColor,
                                fontWeight = if (isFastest || isSlowest) FontWeight.Medium else FontWeight.Normal
                            )

                            Text(
                                text = viewModel.formatTime(lap.lapTimeMs),
                                fontSize = 17.sp,
                                color = lapTextColor,
                                fontWeight = if (isFastest || isSlowest) FontWeight.Medium else FontWeight.Normal
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
    }
}
