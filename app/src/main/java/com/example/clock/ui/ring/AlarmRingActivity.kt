package com.example.clock.ui.ring

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clock.data.alarm.AlarmScheduler
import com.example.clock.data.preferences.ClockPreferences
import com.example.clock.data.preferences.VolumeButtonAction
import com.example.clock.receiver.AlarmReceiver
import com.example.clock.service.AlarmRingService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AlarmRingActivity : ComponentActivity() {

    private var alarmId: Long = -1L
    private var volumeAction = VolumeButtonAction.SNOOZE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra("extra_label") ?: "Alarm"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val prefs = ClockPreferences(this)
        volumeAction = runBlocking { prefs.volumeButtonAction.first() }

        setContent {
            AlarmRingContent(
                label = label,
                onSnooze = { triggerSnooze() },
                onStop = { triggerStop() }
            )
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            when (volumeAction) {
                VolumeButtonAction.SNOOZE -> {
                    triggerSnooze()
                    return true
                }
                VolumeButtonAction.DISMISS -> {
                    triggerStop()
                    return true
                }
                VolumeButtonAction.SILENCE -> {
                    val stopServiceIntent = Intent(this, AlarmRingService::class.java).apply {
                        action = AlarmRingService.ACTION_STOP_RINGING
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                    }
                    startService(stopServiceIntent)
                    return true
                }
                VolumeButtonAction.NOTHING -> return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun triggerSnooze() {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SNOOZE_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
        finish()
    }

    private fun triggerStop() {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_DISMISS_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
        }
        sendBroadcast(intent)
        finish()
    }
}

@Composable
fun AlarmRingContent(
    label: String,
    onSnooze: () -> Unit,
    onStop: () -> Unit
) {
    var currentTime by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }

    LaunchedEffect(Unit) {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        while (true) {
            currentTime = LocalTime.now().format(formatter)
            kotlinx.coroutines.delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = currentTime,
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8E8E93)
            )

            Spacer(modifier = Modifier.height(80.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Snooze Button (Blue accent)
                Button(
                    onClick = onSnooze,
                    modifier = Modifier
                        .size(110.dp)
                        .testTag("alarm_ring_snooze_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A84FF),
                        contentColor = Color.White
                    )
                ) {
                    Text("Snooze", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }

                // Stop Button (Red)
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .size(110.dp)
                        .testTag("alarm_ring_stop_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF453A),
                        contentColor = Color.White
                    )
                ) {
                    Text("Stop", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
