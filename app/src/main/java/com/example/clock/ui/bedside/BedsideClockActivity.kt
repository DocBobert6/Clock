package com.example.clock.ui.bedside

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class BedsideClockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Set dim window brightness initially
        setWindowBrightness(0.2f)

        setContent {
            BedsideClockScreen(
                onBrightnessChange = { fraction ->
                    setWindowBrightness(fraction)
                },
                onClose = { finish() }
            )
        }
    }

    private fun setWindowBrightness(fraction: Float) {
        val lp = window.attributes
        lp.screenBrightness = fraction.coerceIn(0.01f, 1.0f)
        window.attributes = lp
    }
}

@Composable
fun BedsideClockScreen(
    onBrightnessChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    var isRedShift by remember { mutableStateOf(false) }
    var brightness by remember { mutableFloatStateOf(0.25f) }
    var currentTime by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))) }
    var offsetDx by remember { mutableFloatStateOf(0f) }
    var offsetDy by remember { mutableFloatStateOf(0f) }

    val textColor = if (isRedShift) {
        Color(0xFFFF3B30).copy(alpha = brightness.coerceIn(0.3f, 1.0f))
    } else {
        Color.White.copy(alpha = brightness.coerceIn(0.3f, 1.0f))
    }

    LaunchedEffect(Unit) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        var tick = 0
        while (true) {
            currentTime = LocalTime.now().format(formatter)
            tick++
            // Pixel shift every 30 seconds
            if (tick % 30 == 0) {
                offsetDx = (Random.nextFloat() * 40f) - 20f
                offsetDy = (Random.nextFloat() * 40f) - 20f
            }
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Drag up = brighter, Drag down = dimmer
                    val delta = -dragAmount / 1000f
                    val newB = (brightness + delta).coerceIn(0.05f, 1.0f)
                    brightness = newB
                    onBrightnessChange(newB)
                }
            }
    ) {
        // Controls at top corners
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isRedShift = !isRedShift },
                modifier = Modifier
                    .background(Color(0x33FFFFFF), CircleShape)
                    .testTag("bedside_red_shift_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Nightlight,
                    contentDescription = "Toggle Red Shift Night Mode",
                    tint = if (isRedShift) Color(0xFFFF3B30) else Color.White
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color(0x33FFFFFF), CircleShape)
                    .testTag("bedside_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Bedside Mode",
                    tint = Color.White
                )
            }
        }

        // Center OLED Clock with pixel shift
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = offsetDx.dp, y = offsetDy.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentTime,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = textColor,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Swipe up / down for brightness",
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.4f)
            )
        }
    }
}
