package com.example.clock.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.clock.data.alarm.AlarmScheduler
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.LapItem
import com.example.clock.data.model.TimerItem
import com.example.clock.data.model.TimerState
import com.example.clock.data.preferences.ClockPreferences
import com.example.clock.service.SleepSoundService
import com.example.clock.ui.stopwatch.StopwatchViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TOGGLE_ALARM = "com.example.clock.widget.ACTION_TOGGLE_ALARM"
        const val EXTRA_ALARM_ID = "extra_alarm_id"

        const val ACTION_STOPWATCH_START_STOP = "com.example.clock.widget.ACTION_STOPWATCH_START_STOP"
        const val ACTION_STOPWATCH_LAP_RESET = "com.example.clock.widget.ACTION_STOPWATCH_LAP_RESET"

        const val ACTION_TIMER_START_PRESET = "com.example.clock.widget.ACTION_TIMER_START_PRESET"
        const val EXTRA_PRESET_SECONDS = "extra_preset_seconds"
        const val ACTION_TIMER_CANCEL = "com.example.clock.widget.ACTION_TIMER_CANCEL"

        const val ACTION_BEDTIME_TOGGLE = "com.example.clock.widget.ACTION_BEDTIME_TOGGLE"
        const val ACTION_BEDTIME_SOUND_TOGGLE = "com.example.clock.widget.ACTION_BEDTIME_SOUND_TOGGLE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val scope = CoroutineScope(Dispatchers.IO)

        when (action) {
            ACTION_TOGGLE_ALARM -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId > 0) {
                    scope.launch {
                        val db = AppDatabase.getInstance(context)
                        val alarm = db.alarmDao().getAlarmById(alarmId)
                        if (alarm != null) {
                            val newEnabled = !alarm.isEnabled
                            val updated = alarm.copy(isEnabled = newEnabled)
                            db.alarmDao().updateAlarm(updated)
                            if (newEnabled) {
                                AlarmScheduler.scheduleAlarm(context, updated)
                            } else {
                                AlarmScheduler.cancelAlarm(context, alarmId)
                            }
                            WidgetUpdateHelper.updateAllWidgets(context)
                        }
                    }
                }
            }
            ACTION_STOPWATCH_START_STOP -> {
                if (StopwatchViewModel.isRunningStatic) {
                    // Stop
                    StopwatchViewModel.accumulatedElapsedMsStatic += (SystemClock.elapsedRealtime() - StopwatchViewModel.baseElapsedRealtimeStatic)
                    StopwatchViewModel.isRunningStatic = false
                } else {
                    // Start
                    StopwatchViewModel.isRunningStatic = true
                    StopwatchViewModel.baseElapsedRealtimeStatic = SystemClock.elapsedRealtime()
                }
                WidgetUpdateHelper.updateAllWidgets(context)
            }
            ACTION_STOPWATCH_LAP_RESET -> {
                if (StopwatchViewModel.isRunningStatic) {
                    // Record lap
                    val currentTotal = StopwatchViewModel.accumulatedElapsedMsStatic + (SystemClock.elapsedRealtime() - StopwatchViewModel.baseElapsedRealtimeStatic)
                    scope.launch {
                        val db = AppDatabase.getInstance(context)
                        val existing = db.lapDao().getAllLapsSync()
                        val lastTotal = existing.firstOrNull()?.totalTimeMs ?: 0L
                        val lapTime = currentTotal - lastTotal
                        db.lapDao().insertLap(
                            LapItem(lapNumber = existing.size + 1, lapTimeMs = lapTime, totalTimeMs = currentTotal)
                        )
                        WidgetUpdateHelper.updateAllWidgets(context)
                    }
                } else {
                    // Reset
                    StopwatchViewModel.accumulatedElapsedMsStatic = 0L
                    StopwatchViewModel.baseElapsedRealtimeStatic = 0L
                    scope.launch {
                        val db = AppDatabase.getInstance(context)
                        db.lapDao().clearLaps()
                        WidgetUpdateHelper.updateAllWidgets(context)
                    }
                }
            }
            ACTION_TIMER_START_PRESET -> {
                val sec = intent.getLongExtra(EXTRA_PRESET_SECONDS, 300L)
                scope.launch {
                    val db = AppDatabase.getInstance(context)
                    val endsAt = System.currentTimeMillis() + (sec * 1000L)
                    val timer = TimerItem(
                        totalSeconds = sec,
                        remainingSeconds = sec,
                        label = "Timer (${sec / 60}m)",
                        state = TimerState.RUNNING,
                        startedAtElapsed = SystemClock.elapsedRealtime(),
                        endsAtTimestamp = endsAt
                    )
                    val id = db.timerDao().insertTimer(timer)
                    AlarmScheduler.scheduleTimer(context, id, sec)
                    WidgetUpdateHelper.updateAllWidgets(context)
                }
            }
            ACTION_TIMER_CANCEL -> {
                scope.launch {
                    val db = AppDatabase.getInstance(context)
                    val active = db.timerDao().getActiveTimersSync()
                    active.forEach { timer ->
                        AlarmScheduler.cancelTimer(context, timer.id)
                        db.timerDao().deleteTimer(timer)
                    }
                    WidgetUpdateHelper.updateAllWidgets(context)
                }
            }
            ACTION_BEDTIME_TOGGLE -> {
                scope.launch {
                    val prefs = ClockPreferences(context)
                    val current = prefs.bedtimeSettings.first()
                    prefs.updateBedtimeSettings(current.copy(isEnabled = !current.isEnabled))
                    WidgetUpdateHelper.updateAllWidgets(context)
                }
            }
            ACTION_BEDTIME_SOUND_TOGGLE -> {
                if (SleepSoundService.isRunning) {
                    val stopIntent = Intent(context, SleepSoundService::class.java).apply {
                        this.action = SleepSoundService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                } else {
                    val startIntent = Intent(context, SleepSoundService::class.java).apply {
                        this.action = SleepSoundService.ACTION_START
                        putExtra(SleepSoundService.EXTRA_SOUND_NAME, "Gentle Rain")
                    }
                    context.startService(startIntent)
                }
                WidgetUpdateHelper.updateAllWidgets(context)
            }
        }
    }
}
