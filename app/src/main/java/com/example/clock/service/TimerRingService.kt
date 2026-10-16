package com.example.clock.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.clock.data.audio.ProceduralSoundGenerator
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.TimerItem
import com.example.clock.data.model.TimerState
import com.example.clock.receiver.AlarmReceiver
import com.example.clock.ui.ring.TimerRingActivity
import kotlinx.coroutines.*

class TimerRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        const val CHANNEL_ID = "timer_ring_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "action_start_timer_ring"
        const val ACTION_STOP = "action_stop_timer_ring"
        const val ACTION_ADD_ONE_MIN = "action_add_one_min"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val timerId = intent?.getLongExtra("extra_timer_id", -1L) ?: -1L

        when (action) {
            ACTION_STOP -> {
                stopTimerRinging(timerId)
                stopSelf()
            }
            ACTION_ADD_ONE_MIN -> {
                addOneMinute(timerId)
                stopSelf()
            }
            else -> {
                startRinging(timerId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startRinging(timerId: Long) {
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val timer = if (timerId > 0) db.timerDao().getTimerById(timerId) else null
            val label = timer?.label ?: "Timer"

            val fullScreenIntent = Intent(applicationContext, TimerRingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_timer_id", timerId)
                putExtra("extra_label", label)
            }
            val fullScreenPi = PendingIntent.getActivity(
                applicationContext,
                2001,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val stopIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
                this.action = AlarmReceiver.ACTION_DISMISS_TIMER
                putExtra("extra_timer_id", timerId)
            }
            val stopPi = PendingIntent.getBroadcast(
                applicationContext,
                2002,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val addMinIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
                this.action = AlarmReceiver.ACTION_ADD_ONE_MIN_TIMER
                putExtra("extra_timer_id", timerId)
            }
            val addMinPi = PendingIntent.getBroadcast(
                applicationContext,
                2003,
                addMinIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(label)
                .setContentText("Timer expired!")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPi, true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi)
                .addAction(android.R.drawable.ic_input_add, "+1 min", addMinPi)
                .build()

            startForeground(NOTIFICATION_ID, notification)

            playAudioAndVibrate(timer)
        }
    }

    private fun playAudioAndVibrate(timer: TimerItem?) {
        try {
            val uri = if (timer != null && timer.soundUri.isNotEmpty()) {
                ProceduralSoundGenerator.getSoundFileUri(applicationContext, timer.soundUri)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
                start()
            }

            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 500, 250, 500, 250)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopTimerRinging(timerId: Long) {
        cleanUp()
        if (timerId > 0) {
            serviceScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val timer = db.timerDao().getTimerById(timerId)
                if (timer != null) {
                    db.timerDao().updateTimer(
                        timer.copy(state = TimerState.IDLE, remainingSeconds = timer.totalSeconds)
                    )
                }
            }
        }
    }

    private fun addOneMinute(timerId: Long) {
        cleanUp()
        if (timerId > 0) {
            serviceScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val timer = db.timerDao().getTimerById(timerId)
                if (timer != null) {
                    val endsAt = System.currentTimeMillis() + 60_000L
                    db.timerDao().updateTimer(
                        timer.copy(
                            state = TimerState.RUNNING,
                            remainingSeconds = 60L,
                            endsAtTimestamp = endsAt
                        )
                    )
                    com.example.clock.data.alarm.AlarmScheduler.scheduleTimer(applicationContext, timerId, 60L)
                }
            }
        }
    }

    private fun cleanUp() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
        vibrator = null
    }

    override fun onDestroy() {
        cleanUp()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Clock Timer Finished Alert"
                setSound(null, null)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
