package com.example.clock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClockView(
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
    size: Dp = 180.dp,
    showSeconds: Boolean = true
) {
    var currentTime by remember { mutableStateOf(LocalTime.now(zoneId)) }

    LaunchedEffect(zoneId) {
        while (true) {
            currentTime = LocalTime.now(zoneId)
            kotlinx.coroutines.delay(100L) // smooth 100ms updates
        }
    }

    val hour = currentTime.hour % 12
    val minute = currentTime.minute
    val second = currentTime.second
    val nano = currentTime.nano

    val secondProgress = second + (nano / 1_000_000_000f)
    val minuteProgress = minute + (secondProgress / 60f)
    val hourProgress = hour + (minuteProgress / 60f)

    val hourAngle = (hourProgress / 12f * 360f) - 90f
    val minuteAngle = (minuteProgress / 60f * 360f) - 90f
    val secondAngle = (secondProgress / 60f * 360f) - 90f

    val onSurface = MaterialTheme.colorScheme.onSurface
    val accentBlue = Color(0xFF0A84FF)

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // Clock dial rim
            drawCircle(
                color = onSurface.copy(alpha = 0.08f),
                radius = radius,
                center = center
            )

            // Hour tick marks (12 ticks)
            for (i in 0 until 12) {
                val angle = (i / 12f * 360f) - 90f
                val rad = Math.toRadians(angle.toDouble())
                val isMajor = i % 3 == 0
                val tickLength = if (isMajor) radius * 0.12f else radius * 0.07f
                val startR = radius - tickLength
                val endR = radius - (radius * 0.03f)

                drawLine(
                    color = onSurface.copy(alpha = if (isMajor) 0.8f else 0.4f),
                    start = Offset(center.x + startR * cos(rad).toFloat(), center.y + startR * sin(rad).toFloat()),
                    end = Offset(center.x + endR * cos(rad).toFloat(), center.y + endR * sin(rad).toFloat()),
                    strokeWidth = if (isMajor) 2.5f else 1.5f,
                    cap = StrokeCap.Round
                )
            }

            // Hour Hand
            val hourRad = Math.toRadians(hourAngle.toDouble())
            val hourLen = radius * 0.52f
            drawLine(
                color = onSurface,
                start = center,
                end = Offset(center.x + hourLen * cos(hourRad).toFloat(), center.y + hourLen * sin(hourRad).toFloat()),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minute Hand
            val minRad = Math.toRadians(minuteAngle.toDouble())
            val minLen = radius * 0.76f
            drawLine(
                color = onSurface,
                start = center,
                end = Offset(center.x + minLen * cos(minRad).toFloat(), center.y + minLen * sin(minRad).toFloat()),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Blue Second Hand
            if (showSeconds) {
                val secRad = Math.toRadians(secondAngle.toDouble())
                val secLen = radius * 0.84f
                val tailLen = radius * 0.18f

                // Tail opposite direction
                drawLine(
                    color = accentBlue,
                    start = Offset(center.x - tailLen * cos(secRad).toFloat(), center.y - tailLen * sin(secRad).toFloat()),
                    end = Offset(center.x + secLen * cos(secRad).toFloat(), center.y + secLen * sin(secRad).toFloat()),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center Cap
                drawCircle(
                    color = accentBlue,
                    radius = 3.5.dp.toPx(),
                    center = center
                )
            } else {
                drawCircle(
                    color = onSurface,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }
        }
    }
}
