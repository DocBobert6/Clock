package com.example.clock.ui.ring

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.clock.receiver.AlarmReceiver

class TimerRingActivity : ComponentActivity() {

    private var timerId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timerId = intent.getLongExtra("extra_timer_id", -1L)
        val label = intent.getStringExtra("extra_label") ?: "Timer"

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

        setContent {
            TimerRingContent(
                label = label,
                onStop = {
                    val intent = Intent(this, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_DISMISS_TIMER
                        putExtra("extra_timer_id", timerId)
                    }
                    sendBroadcast(intent)
                    finish()
                },
                onAddOneMin = {
                    val intent = Intent(this, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_ADD_ONE_MIN_TIMER
                        putExtra("extra_timer_id", timerId)
                    }
                    sendBroadcast(intent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun TimerRingContent(
    label: String,
    onStop: () -> Unit,
    onAddOneMin: () -> Unit
) {
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
                text = "00:00",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stop Button
                Button(
                    onClick = onStop,
                    modifier = Modifier
                        .size(110.dp)
                        .testTag("timer_ring_stop_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF453A),
                        contentColor = Color.White
                    )
                ) {
                    Text("Stop", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }

                // +1 Min Button (Blue)
                Button(
                    onClick = onAddOneMin,
                    modifier = Modifier
                        .size(110.dp)
                        .testTag("timer_ring_add_min_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0A84FF),
                        contentColor = Color.White
                    )
                ) {
                    Text("+1 min", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
