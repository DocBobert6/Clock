package com.example.clock.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.clock.data.audio.SleepSoundEngine
import kotlinx.coroutines.*

class SleepSoundService : Service() {

    private val engine = SleepSoundEngine()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "sleep_sound_channel"
        const val NOTIFICATION_ID = 3001

        const val ACTION_START = "action_start_sleep_sound"
        const val ACTION_STOP = "action_stop_sleep_sound"
        const val ACTION_TOGGLE = "action_toggle_sleep_sound"
        const val EXTRA_SOUND_NAME = "extra_sound_name"
        const val EXTRA_VOLUME = "extra_volume"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"

        @Volatile
        var isRunning: Boolean = false
            private set
        @Volatile
        var activeSoundName: String = "Gentle Rain"
            private set
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_STOP -> {
                stopPlaying()
                stopSelf()
            }
            ACTION_TOGGLE -> {
                if (engine.isPlaying) {
                    engine.stop()
                    isRunning = false
                    updateNotification(isPaused = true)
                } else {
                    val sound = intent?.getStringExtra(EXTRA_SOUND_NAME) ?: activeSoundName
                    val vol = intent?.getFloatExtra(EXTRA_VOLUME, 0.7f) ?: 0.7f
                    engine.start(sound, vol)
                    isRunning = true
                    updateNotification(isPaused = false)
                }
            }
            else -> {
                val sound = intent?.getStringExtra(EXTRA_SOUND_NAME) ?: "Gentle Rain"
                val vol = intent?.getFloatExtra(EXTRA_VOLUME, 0.7f) ?: 0.7f
                val durationMin = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 45) ?: 45
                startPlaying(sound, vol, durationMin)
            }
        }
        return START_NOT_STICKY
    }

    private fun startPlaying(sound: String, vol: Float, durationMin: Int) {
        activeSoundName = sound
        isRunning = true
        engine.start(sound, vol)

        val notification = buildNotification(sound, isPaused = false)
        startForeground(NOTIFICATION_ID, notification)

        timerJob?.cancel()
        if (durationMin > 0) {
            timerJob = serviceScope.launch {
                delay(durationMin * 60 * 1000L)
                stopPlaying()
                stopSelf()
            }
        }
    }

    private fun stopPlaying() {
        isRunning = false
        timerJob?.cancel()
        engine.stop()
    }

    private fun updateNotification(isPaused: Boolean) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(activeSoundName, isPaused))
    }

    private fun buildNotification(sound: String, isPaused: Boolean): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "bedtime")
        }
        val openPi = PendingIntent.getActivity(
            this,
            3001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, SleepSoundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPi = PendingIntent.getService(
            this,
            3002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, SleepSoundService::class.java).apply {
            action = ACTION_TOGGLE
        }
        val togglePi = PendingIntent.getService(
            this,
            3003,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseTitle = if (isPaused) "Resume" else "Pause"
        val playPauseIcon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Sleep Sound: $sound")
            .setContentText(if (isPaused) "Paused" else "Playing sleep ambiance")
            .setContentIntent(openPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(!isPaused)
            .addAction(playPauseIcon, playPauseTitle, togglePi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPi)
            .build()
    }

    override fun onDestroy() {
        stopPlaying()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sleep Sounds",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Clock Sleep Sounds Ambient Player"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
