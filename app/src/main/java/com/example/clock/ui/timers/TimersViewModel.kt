package com.example.clock.ui.timers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clock.data.alarm.AlarmScheduler
import com.example.clock.data.audio.ProceduralSoundGenerator
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.TimerItem
import com.example.clock.data.model.TimerState
import com.example.clock.widget.WidgetUpdateHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimersViewModel(context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val appContext = context.applicationContext

    val timers: StateFlow<List<TimerItem>> = db.timerDao().getAllTimers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val runningTimers: StateFlow<List<TimerItem>> = timers.map { list ->
        list.filter { it.state != TimerState.IDLE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var tickerJob: Job? = null

    init {
        ProceduralSoundGenerator.ensureSoundsGenerated(appContext)
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                val now = System.currentTimeMillis()
                val currentList = db.timerDao().getActiveTimersSync()
                for (timer in currentList) {
                    if (timer.state == TimerState.RUNNING) {
                        val remaining = ((timer.endsAtTimestamp - now) / 1000L).coerceAtLeast(0)
                        if (remaining <= 0) {
                            // Ring timer!
                            db.timerDao().updateTimer(timer.copy(state = TimerState.RINGING, remainingSeconds = 0))
                            AlarmScheduler.scheduleTimer(appContext, timer.id, 0L)
                            WidgetUpdateHelper.updateAllWidgets(appContext)
                        } else if (remaining != timer.remainingSeconds) {
                            db.timerDao().updateTimer(timer.copy(remainingSeconds = remaining))
                        }
                    }
                }
                delay(1000L)
            }
        }
    }

    fun startNewTimer(hours: Int, minutes: Int, seconds: Int, label: String, soundUri: String, soundName: String) {
        val totalSec = (hours * 3600L) + (minutes * 60L) + seconds
        if (totalSec <= 0) return

        viewModelScope.launch {
            val endsAt = System.currentTimeMillis() + (totalSec * 1000L)
            val item = TimerItem(
                totalSeconds = totalSec,
                remainingSeconds = totalSec,
                label = label.ifBlank { "Timer" },
                soundUri = soundUri,
                soundName = soundName,
                state = TimerState.RUNNING,
                startedAtElapsed = android.os.SystemClock.elapsedRealtime(),
                endsAtTimestamp = endsAt
            )
            val id = db.timerDao().insertTimer(item)
            AlarmScheduler.scheduleTimer(appContext, id, totalSec)
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun pauseTimer(timer: TimerItem) {
        viewModelScope.launch {
            AlarmScheduler.cancelTimer(appContext, timer.id)
            db.timerDao().updateTimer(timer.copy(state = TimerState.PAUSED))
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun resumeTimer(timer: TimerItem) {
        viewModelScope.launch {
            val endsAt = System.currentTimeMillis() + (timer.remainingSeconds * 1000L)
            db.timerDao().updateTimer(
                timer.copy(
                    state = TimerState.RUNNING,
                    endsAtTimestamp = endsAt
                )
            )
            AlarmScheduler.scheduleTimer(appContext, timer.id, timer.remainingSeconds)
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun cancelTimer(timer: TimerItem) {
        viewModelScope.launch {
            AlarmScheduler.cancelTimer(appContext, timer.id)
            db.timerDao().deleteTimer(timer)
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun addOneMinute(timer: TimerItem) {
        viewModelScope.launch {
            val newRemaining = timer.remainingSeconds + 60L
            val endsAt = System.currentTimeMillis() + (newRemaining * 1000L)
            db.timerDao().updateTimer(
                timer.copy(
                    remainingSeconds = newRemaining,
                    totalSeconds = maxOf(timer.totalSeconds, newRemaining),
                    endsAtTimestamp = endsAt,
                    state = TimerState.RUNNING
                )
            )
            AlarmScheduler.scheduleTimer(appContext, timer.id, newRemaining)
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun formatRemaining(sec: Long): String {
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    fun formatEndsAt(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val zone = ZoneId.systemDefault()
        return DateTimeFormatter.ofPattern("h:mm a").format(instant.atZone(zone))
    }
}
