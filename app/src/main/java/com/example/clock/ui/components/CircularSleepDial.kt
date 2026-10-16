package com.example.clock.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun CircularSleepDial(
    bedtimeHour: Int,
    bedtimeMinute: Int,
    wakeHour: Int,
    wakeMinute: Int,
    onBedtimeChanged: (Int, Int) -> Unit,
    onWakeChanged: (Int, Int) -> Unit,
    sleepGoalHours: Float = 8f,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val accentColor = Color(0xFF0A84FF)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    // Convert time to angles: 0:00 (midnight) is top (-90 degrees), 12:00 is bottom (+90 degrees)
    // 24 hours = 360 degrees, 1 hour = 15 degrees, 1 minute = 0.25 degrees
    fun timeToAngle(h: Int, m: Int): Float {
        val totalMinutes = (h % 24) * 60 + m
        return (totalMinutes / 1440f * 360f) - 90f
    }

    fun angleToTime(angleDeg: Float): Pair<Int, Int> {
        var norm = (angleDeg + 90f) % 360f
        if (norm < 0) norm += 360f
        val totalMinutes = ((norm / 360f) * 1440f).roundToInt()
        // Snap to nearest 5 minutes
        val snappedMinutes = ((totalMinutes + 2) / 5) * 5
        val h = (snappedMinutes / 60) % 24
        val m = snappedMinutes % 60
        return Pair(h, m)
    }

    val bedAngle = timeToAngle(bedtimeHour, bedtimeMinute)
    val wakeAngle = timeToAngle(wakeHour, wakeMinute)

    // Calculate sleep duration in hours and minutes
    val bedMinutes = bedtimeHour * 60 + bedtimeMinute
    val wakeMinutes = wakeHour * 60 + wakeMinute
    val diffMinutes = if (wakeMinutes >= bedMinutes) {
        wakeMinutes - bedMinutes
    } else {
        (1440 - bedMinutes) + wakeMinutes
    }
    val durationHours = diffMinutes / 60
    val durationMins = diffMinutes % 60

    var draggingHandle by remember { mutableStateOf<String?>(null) } // "bed" or "wake"

    Box(
        modifier = modifier
            .size(280.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = (min(size.width, size.height) / 2f) - 30f
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val touchAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

                            // Check proximity to bed handle vs wake handle
                            fun angleDiff(a: Float, b: Float): Float {
                                var d = abs(a - b) % 360f
                                if (d > 180f) d = 360f - d
                                return d
                            }

                            val diffBed = angleDiff(touchAngle, bedAngle)
                            val diffWake = angleDiff(touchAngle, wakeAngle)

                            draggingHandle = if (diffBed < diffWake && diffBed < 40f) {
                                "bed"
                            } else if (diffWake < 40f) {
                                "wake"
                            } else {
                                null
                            }
                        },
                        onDrag = { change, _ ->
                            if (draggingHandle != null) {
                                change.consume()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = change.position.x - center.x
                                val dy = change.position.y - center.y
                                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                val (h, m) = angleToTime(angle)

                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                                if (draggingHandle == "bed") {
                                    onBedtimeChanged(h, m)
                                } else if (draggingHandle == "wake") {
                                    onWakeChanged(h, m)
                                }
                            }
                        },
                        onDragEnd = { draggingHandle = null },
                        onDragCancel = { draggingHandle = null }
                    )
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidth = 24.dp.toPx()
            val radius = (size.minDimension / 2f) - (strokeWidth / 2f) - 8.dp.toPx()
            val arcSize = Size(radius * 2, radius * 2)
            val arcTopLeft = Offset(center.x - radius, center.y - radius)

            // Background circular track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Hour tick marks around dial (24 hour ticks)
            for (i in 0 until 24) {
                val tickAngle = (i / 24f * 360f) - 90f
                val rad = Math.toRadians(tickAngle.toDouble())
                val innerR = radius - (strokeWidth / 2f) - 6.dp.toPx()
                val outerR = radius - (strokeWidth / 2f) - (if (i % 6 == 0) 14.dp.toPx() else 10.dp.toPx())
                val start = Offset(
                    center.x + innerR * cos(rad).toFloat(),
                    center.y + innerR * sin(rad).toFloat()
                )
                val end = Offset(
                    center.x + outerR * cos(rad).toFloat(),
                    center.y + outerR * sin(rad).toFloat()
                )
                drawLine(
                    color = Color.Gray.copy(alpha = if (i % 6 == 0) 0.6f else 0.3f),
                    start = start,
                    end = end,
                    strokeWidth = if (i % 6 == 0) 2.dp.toPx() else 1.dp.toPx()
                )
            }

            // Sleep Arc
            var sweep = wakeAngle - bedAngle
            if (sweep < 0) sweep += 360f

            drawArc(
                color = accentColor,
                startAngle = bedAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Draw Bed Handle (Circle)
            val bedRad = Math.toRadians(bedAngle.toDouble())
            val bedCenter = Offset(
                center.x + radius * cos(bedRad).toFloat(),
                center.y + radius * sin(bedRad).toFloat()
            )
            drawCircle(
                color = Color.Black,
                radius = 18.dp.toPx(),
                center = bedCenter
            )
            drawCircle(
                color = Color(0xFF38383A),
                radius = 16.dp.toPx(),
                center = bedCenter
            )

            // Draw Wake Handle (Circle)
            val wakeRad = Math.toRadians(wakeAngle.toDouble())
            val wakeCenter = Offset(
                center.x + radius * cos(wakeRad).toFloat(),
                center.y + radius * sin(wakeRad).toFloat()
            )
            drawCircle(
                color = Color.Black,
                radius = 18.dp.toPx(),
                center = wakeCenter
            )
            drawCircle(
                color = Color(0xFF38383A),
                radius = 16.dp.toPx(),
                center = wakeCenter
            )
        }

        // Center Sleep Duration readout & goal
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${durationHours}h ${durationMins}m",
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            val meetsGoal = (diffMinutes / 60f) >= sleepGoalHours
            Text(
                text = if (meetsGoal) "Goal reached" else "Below ${sleepGoalHours.toInt()}h goal",
                fontSize = 13.sp,
                color = if (meetsGoal) Color(0xFF30D158) else Color(0xFF8E8E93)
            )
        }
    }
}
