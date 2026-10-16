package com.example.clock.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.clock.data.alarm.AlarmScheduler
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.TimerState
import com.example.clock.service.AlarmRingService
import com.example.clock.service.TimerRingService
import com.example.clock.widget.WidgetUpdateHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIRE_ALARM = "com.example.clock.ACTION_FIRE_ALARM"
        const val ACTION_SNOOZE_ALARM = "com.example.clock.ACTION_SNOOZE_ALARM"
        const val ACTION_DISMISS_ALARM = "com.example.clock.ACTION_DISMISS_ALARM"
        const val ACTION_UPCOMING_NOTICE = "com.example.clock.ACTION_UPCOMING_NOTICE"
        const val ACTION_UPCOMING_DISMISS = "com.example.clock.ACTION_UPCOMING_DISMISS"

        const val ACTION_FIRE_TIMER = "com.example.clock.ACTION_FIRE_TIMER"
        const val ACTION_DISMISS_TIMER = "com.example.clock.ACTION_DISMISS_TIMER"
        const val ACTION_ADD_ONE_MIN_TIMER = "com.example.clock.ACTION_ADD_ONE_MIN_TIMER"

        const val UPCOMING_CHANNEL_ID = "upcoming_alarm_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val scope = CoroutineScope(Dispatchers.IO)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> {
                scope.launch {
                    rescheduleAll(context)
                    WidgetUpdateHelper.updateAllWidgets(context)
                }
            }
            ACTION_FIRE_ALARM -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
                val ringIntent = Intent(context, AlarmRingService::class.java).apply {
                    this.action = AlarmRingService.ACTION_START_RINGING
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, ringIntent)
            }
            ACTION_SNOOZE_ALARM -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
                val ringIntent = Intent(context, AlarmRingService::class.java).apply {
                    this.action = AlarmRingService.ACTION_SNOOZE_RINGING
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, ringIntent)
            }
            ACTION_DISMISS_ALARM -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
                val ringIntent = Intent(context, AlarmRingService::class.java).apply {
                    this.action = AlarmRingService.ACTION_STOP_RINGING
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                }
                ContextCompat.startForegroundService(context, ringIntent)
            }
            ACTION_UPCOMING_NOTICE -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
                showUpcomingNotice(context, alarmId)
            }
            ACTION_UPCOMING_DISMISS -> {
                val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
                dismissUpcomingAlarm(context, alarmId)
            }
            ACTION_FIRE_TIMER -> {
                val timerId = intent.getLongExtra("extra_timer_id", -1L)
                val ringIntent = Intent(context, TimerRingService::class.java).apply {
                    this.action = TimerRingService.ACTION_START
                    putExtra("extra_timer_id", timerId)
                }
                ContextCompat.startForegroundService(context, ringIntent)
            }
            ACTION_DISMISS_TIMER -> {
                val timerId = intent.getLongExtra("extra_timer_id", -1L)
                val ringIntent = Intent(context, TimerRingService::class.java).apply {
                    this.action = TimerRingService.ACTION_STOP
                    putExtra("extra_timer_id", timerId)
                }
                ContextCompat.startForegroundService(context, ringIntent)
            }
            ACTION_ADD_ONE_MIN_TIMER -> {
                val timerId = intent.getLongExtra("extra_timer_id", -1L)
                val ringIntent = Intent(context, TimerRingService::class.java).apply {
                    this.action = TimerRingService.ACTION_ADD_ONE_MIN
                    putExtra("extra_timer_id", timerId)
                }
                ContextCompat.startForegroundService(context, ringIntent)
            }
        }
    }

    private fun showUpcomingNotice(context: Context, alarmId: Long) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val db = AppDatabase.getInstance(context)
            val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch
            if (!alarm.isEnabled) return@launch

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    UPCOMING_CHANNEL_ID,
                    "Upcoming Alarms",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Advance notification before alarm rings"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_UPCOMING_DISMISS
                putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            }
            val dismissPi = PendingIntent.getBroadcast(
                context,
                (alarmId + 200000).toInt(),
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val openIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("open_tab", "alarms")
            }
            val openPi = PendingIntent.getActivity(
                context,
                (alarmId + 200000).toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)
            val notification = NotificationCompat.Builder(context, UPCOMING_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Upcoming alarm: ${alarm.label}")
                .setContentText("Rings at $timeStr")
                .setContentIntent(openPi)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss now", dismissPi)
                .setAutoCancel(true)
                .build()

            notificationManager.notify((alarmId + 200000).toInt(), notification)
        }
    }

    private fun dismissUpcomingAlarm(context: Context, alarmId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel((alarmId + 200000).toInt())
        AlarmScheduler.cancelAlarm(context, alarmId)

        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val db = AppDatabase.getInstance(context)
            val alarm = db.alarmDao().getAlarmById(alarmId) ?: return@launch
            if (alarm.isRepeating()) {
                // Schedule the next occurrence after tomorrow
                AlarmScheduler.scheduleAlarm(context, alarm)
            } else {
                db.alarmDao().updateAlarm(alarm.copy(isEnabled = false))
            }
        }
    }

    private suspend fun rescheduleAll(context: Context) {
        val db = AppDatabase.getInstance(context)
        val alarms = db.alarmDao().getEnabledAlarmsSync()
        alarms.forEach { alarm ->
            AlarmScheduler.scheduleAlarm(context, alarm)
        }
        val timers = db.timerDao().getActiveTimersSync()
        timers.forEach { timer ->
            val remainingSec = (timer.endsAtTimestamp - System.currentTimeMillis()) / 1000L
            if (remainingSec > 0) {
                AlarmScheduler.scheduleTimer(context, timer.id, remainingSec)
            } else if (timer.state == TimerState.RUNNING) {
                // Timer should fire
                AlarmScheduler.scheduleTimer(context, timer.id, 1L)
            }
        }
    }
}
