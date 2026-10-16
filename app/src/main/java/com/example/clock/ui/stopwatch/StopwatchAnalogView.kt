package com.example.clock.ui.stopwatch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StopwatchAnalogView(
    totalElapsedMs: Long,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp
) {
    val totalSeconds = totalElapsedMs / 1000f
    val secondsInMinute = totalSeconds % 60f
    val minutes = (totalElapsedMs / 60000) % 30 // 30-minute subdial

    val secondsAngle = (secondsInMinute / 60f * 360f) - 90f
    val subdialMinutesAngle = (minutes / 30f * 360f) - 90f

    val accentBlue = Color(0xFF0A84FF)
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f

            // Main outer rim
            drawCircle(
                color = onSurface.copy(alpha = 0.08f),
                radius = radius,
                center = center
            )

            // Second tick marks (60 ticks)
            for (i in 0 until 60) {
                val angle = (i / 60f * 360f) - 90f
                val rad = Math.toRadians(angle.toDouble())
                val isMajor = i % 5 == 0
                val tickLen = if (isMajor) radius * 0.12f else radius * 0.06f
                val startR = radius - tickLen
                val endR = radius - (radius * 0.02f)

                drawLine(
                    color = onSurface.copy(alpha = if (isMajor) 0.8f else 0.3f),
                    start = Offset(center.x + startR * cos(rad).toFloat(), center.y + startR * sin(rad).toFloat()),
                    end = Offset(center.x + endR * cos(rad).toFloat(), center.y + endR * sin(rad).toFloat()),
                    strokeWidth = if (isMajor) 2.5f else 1.2f,
                    cap = StrokeCap.Round
                )
            }

            // Top 30-minute Subdial
            val subdialCenter = Offset(center.x, center.y - (radius * 0.42f))
            val subdialRadius = radius * 0.28f
            drawCircle(
                color = onSurface.copy(alpha = 0.06f),
                radius = subdialRadius,
                center = subdialCenter
            )
            // Subdial ticks
            for (i in 0 until 30 step 5) {
                val angle = (i / 30f * 360f) - 90f
                val rad = Math.toRadians(angle.toDouble())
                val tickLen = subdialRadius * 0.2f
                val startR = subdialRadius - tickLen
                val endR = subdialRadius
                drawLine(
                    color = onSurface.copy(alpha = 0.5f),
                    start = Offset(subdialCenter.x + startR * cos(rad).toFloat(), subdialCenter.y + startR * sin(rad).toFloat()),
                    end = Offset(subdialCenter.x + endR * cos(rad).toFloat(), subdialCenter.y + endR * sin(rad).toFloat()),
                    strokeWidth = 1.2f
                )
            }
            // Subdial Minute Hand
            val subRad = Math.toRadians(subdialMinutesAngle.toDouble())
            val subHandLen = subdialRadius * 0.75f
            drawLine(
                color = accentBlue,
                start = subdialCenter,
                end = Offset(subdialCenter.x + subHandLen * cos(subRad).toFloat(), subdialCenter.y + subHandLen * sin(subRad).toFloat()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(color = accentBlue, radius = 2.5.dp.toPx(), center = subdialCenter)

            // Main Sweep Second Hand (Accent Blue)
            val secRad = Math.toRadians(secondsAngle.toDouble())
            val secLen = radius * 0.82f
            val tailLen = radius * 0.18f

            drawLine(
                color = accentBlue,
                start = Offset(center.x - tailLen * cos(secRad).toFloat(), center.y - tailLen * sin(secRad).toFloat()),
                end = Offset(center.x + secLen * cos(secRad).toFloat(), center.y + secLen * sin(secRad).toFloat()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center Cap
            drawCircle(
                color = accentBlue,
                radius = 4.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.Black,
                radius = 1.5.dp.toPx(),
                center = center
            )
        }
    }
}
