package com.example.clock.service

import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clock.data.preferences.ClockPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

class BedsideDreamService : DreamService() {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = false

        val prefs = ClockPreferences(this)
        val bedtime = runBlocking { prefs.bedtimeSettings.first() }
        val isRedShift = bedtime.isBedsideRedShift

        val composeView = ComposeView(this).apply {
            setContent {
                BedsideDreamClock(isRedShift = isRedShift)
            }
        }
        setContentView(composeView)
    }
}

@Composable
fun BedsideDreamClock(isRedShift: Boolean) {
    var timeText by remember { mutableStateOf("") }
    var offsetDx by remember { mutableStateOf(0f) }
    var offsetDy by remember { mutableStateOf(0f) }

    val textColor = if (isRedShift) Color(0xFFFF3B30).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.55f)

    LaunchedEffect(Unit) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        while (true) {
            timeText = LocalTime.now().format(formatter)
            // Pixel shift every minute to prevent OLED burn-in
            offsetDx = (Random.nextFloat() * 40f) - 20f
            offsetDy = (Random.nextFloat() * 40f) - 20f
            kotlinx.coroutines.delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(x = offsetDx.dp, y = offsetDy.dp)
        ) {
            Text(
                text = timeText,
                color = textColor,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bedside Clock",
                color = textColor.copy(alpha = 0.4f),
                fontSize = 14.sp
            )
        }
    }
}
