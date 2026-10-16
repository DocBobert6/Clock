package com.example.clock.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SunArcDisplay(
    sunrise: LocalTime?,
    sunset: LocalTime?,
    currentLocalTime: LocalTime,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val sunriseStr = sunrise?.format(formatter) ?: "--:--"
    val sunsetStr = sunset?.format(formatter) ?: "--:--"

    // Calculate current sun progress on arc (0.0 at sunrise, 1.0 at sunset)
    val progress: Float = if (sunrise != null && sunset != null) {
        val riseSec = sunrise.toSecondOfDay()
        val setSec = sunset.toSecondOfDay()
        val nowSec = currentLocalTime.toSecondOfDay()
        if (setSec > riseSec) {
            ((nowSec - riseSec).toFloat() / (setSec - riseSec)).coerceIn(0f, 1f)
        } else 0.5f
    } else 0.5f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 2.dp.toPx()
                val radius = size.width * 0.42f
                val center = Offset(size.width / 2f, size.height - 10f)

                // Horizon line
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(center.x - radius - 20f, center.y),
                    end = Offset(center.x + radius + 20f, center.y),
                    strokeWidth = 1.dp.toPx()
                )

                // Arc (180 degrees from 180° to 0°)
                val arcTopLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                drawArc(
                    color = Color(0xFFFFD60A).copy(alpha = 0.5f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(
                        width = strokeW,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                )

                // Sun icon indicator along the arc
                val currentAngle = 180f + (progress * 180f)
                val rad = Math.toRadians(currentAngle.toDouble())
                val sunX = center.x + radius * cos(rad).toFloat()
                val sunY = center.y + radius * sin(rad).toFloat()

                // Sun glow
                drawCircle(
                    color = Color(0xFFFFD60A).copy(alpha = 0.35f),
                    radius = 12.dp.toPx(),
                    center = Offset(sunX, sunY)
                )
                // Sun core
                drawCircle(
                    color = Color(0xFFFFD60A),
                    radius = 6.dp.toPx(),
                    center = Offset(sunX, sunY)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WbTwilight,
                    contentDescription = null,
                    tint = Color(0xFFFF9F0A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text("Sunrise", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sunriseStr, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sunset", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(sunsetStr, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = Color(0xFFFF453A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
