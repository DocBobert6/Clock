package com.example.clock.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.clock.data.alarm.AlarmScheduler
import com.example.clock.data.audio.ProceduralSoundGenerator
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.Alarm
import com.example.clock.receiver.AlarmReceiver
import com.example.clock.ui.ring.AlarmRingActivity
import kotlinx.coroutines.*

class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var silenceJob: Job? = null
    private var gradualVolumeJob: Job? = null

    companion object {
        const val CHANNEL_ID = "alarm_ring_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_RINGING = "action_start_ringing"
        const val ACTION_STOP_RINGING = "action_stop_ringing"
        const val ACTION_SNOOZE_RINGING = "action_snooze_ringing"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Clock:AlarmWakeLock").apply {
            setReferenceCounted(false)
            acquire(15 * 60 * 1000L) // 15 min safeguard
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_RINGING
        val alarmId = intent?.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L) ?: -1L

        when (action) {
            ACTION_STOP_RINGING -> {
                stopRinging(alarmId)
                stopSelf()
            }
            ACTION_SNOOZE_RINGING -> {
                snoozeRinging(alarmId)
                stopSelf()
            }
            else -> {
                startRinging(alarmId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startRinging(alarmId: Long) {
        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val alarm = if (alarmId > 0) db.alarmDao().getAlarmById(alarmId) else null
            val label = alarm?.label ?: "Alarm"
            val timeText = if (alarm != null) String.format("%02d:%02d", alarm.hour, alarm.minute) else "Alarm"

            // Build Full Screen Intent to AlarmRingActivity
            val ringActivityIntent = Intent(applicationContext, AlarmRingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                putExtra("extra_label", label)
                putExtra("extra_time", timeText)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                applicationContext,
                1001,
                ringActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Notification Actions: Snooze & Stop
            val snoozeIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
                this.action = AlarmReceiver.ACTION_SNOOZE_ALARM
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            }
            val snoozePi = PendingIntent.getBroadcast(
                applicationContext,
                1002,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dismissIntent = Intent(applicationContext, AlarmReceiver::class.java).apply {
                this.action = AlarmReceiver.ACTION_DISMISS_ALARM
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            }
            val dismissPi = PendingIntent.getBroadcast(
                applicationContext,
                1003,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(label)
                .setContentText("Alarm ringing ($timeText)")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .addAction(android.R.drawable.ic_media_pause, "Snooze", snoozePi)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", dismissPi)
                .build()

            startForeground(NOTIFICATION_ID, notification)

            playAudioAndVibrate(alarm)
        }
    }

    private fun playAudioAndVibrate(alarm: Alarm?) {
        try {
            val audioUri = if (alarm != null) {
                ProceduralSoundGenerator.getSoundFileUri(applicationContext, alarm.soundUri)
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
                setDataSource(applicationContext, audioUri)
                isLooping = true
                prepare()
            }

            val targetVol = alarm?.volume ?: 0.9f
            if (alarm?.isGradualVolume == true && alarm.gradualVolumeSeconds > 0) {
                mediaPlayer?.setVolume(0.05f, 0.05f)
                mediaPlayer?.start()
                gradualVolumeJob = serviceScope.launch {
                    val steps = 20
                    val stepDelay = (alarm.gradualVolumeSeconds * 1000L) / steps
                    for (i in 1..steps) {
                        delay(stepDelay)
                        val fraction = i.toFloat() / steps
                        val curVol = 0.05f + (targetVol - 0.05f) * fraction
                        mediaPlayer?.setVolume(curVol, curVol)
                    }
                }
            } else {
                mediaPlayer?.setVolume(targetVol, targetVol)
                mediaPlayer?.start()
            }

            // Start vibration
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0, 800, 400, 800, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }

            // Auto silence after 10 minutes
            silenceJob = serviceScope.launch {
                delay(10 * 60 * 1000L)
                stopRinging(alarm?.id ?: -1L)
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopRinging(alarmId: Long) {
        cleanAudio()
        if (alarmId > 0) {
            serviceScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val alarm = db.alarmDao().getAlarmById(alarmId)
                if (alarm != null) {
                    if (alarm.deleteAfterRings) {
                        db.alarmDao().deleteAlarm(alarm)
                    } else if (!alarm.isRepeating()) {
                        db.alarmDao().updateAlarm(alarm.copy(isEnabled = false))
                    } else {
                        // Reschedule next occurrence
                        AlarmScheduler.scheduleAlarm(applicationContext, alarm)
                    }
                }
            }
        }
    }

    private fun snoozeRinging(alarmId: Long) {
        cleanAudio()
        if (alarmId > 0) {
            serviceScope.launch {
                val db = AppDatabase.getInstance(applicationContext)
                val alarm = db.alarmDao().getAlarmById(alarmId)
                val snoozeMinutes = alarm?.snoozeLengthMinutes ?: 9
                AlarmScheduler.snoozeAlarm(applicationContext, alarmId, snoozeMinutes)
            }
        }
    }

    private fun cleanAudio() {
        silenceJob?.cancel()
        gradualVolumeJob?.cancel()
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
        cleanAudio()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Clock Alarm Ringing Alert"
                setSound(null, null)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
