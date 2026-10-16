package com.example.clock.ui.alarms

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clock.data.alarm.AlarmScheduler
import com.example.clock.data.audio.ProceduralSoundGenerator
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.Alarm
import com.example.clock.data.preferences.ClockPreferences
import com.example.clock.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class AlarmDisplayItem(
    val alarm: Alarm,
    val timeFormatted: String,
    val amPmFormatted: String,
    val nextTriggerZoned: ZonedDateTime
)

class AlarmsViewModel(context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val prefs = ClockPreferences(context)
    private val appContext = context.applicationContext

    val is24Hour: StateFlow<Boolean> = prefs.is24HourFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _editingAlarm = MutableStateFlow<Alarm?>(null)
    val editingAlarm: StateFlow<Alarm?> = _editingAlarm.asStateFlow()

    private val _showCreateSheet = MutableStateFlow(false)
    val showCreateSheet: StateFlow<Boolean> = _showCreateSheet.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var recentlyDeletedAlarm: Alarm? = null

    init {
        // Ensure procedural sound files are generated on first launch
        ProceduralSoundGenerator.ensureSoundsGenerated(appContext)

        // Seed 2 default alarms if database is empty
        viewModelScope.launch {
            val existing = db.alarmDao().getEnabledAlarmsSync()
            if (existing.isEmpty()) {
                val a1 = Alarm(
                    hour = 7,
                    minute = 0,
                    label = "Wake up",
                    isEnabled = true,
                    daysOfWeek = 0b00111110, // Mon-Fri
                    soundUri = "procedural:morning_bell",
                    soundName = "Morning Bell"
                )
                val a2 = Alarm(
                    hour = 8,
                    minute = 30,
                    label = "Weekend",
                    isEnabled = false,
                    daysOfWeek = 0b01100000, // Sat-Sun
                    soundUri = "procedural:radiance",
                    soundName = "Radiance"
                )
                val id1 = db.alarmDao().insertAlarm(a1)
                db.alarmDao().insertAlarm(a2)
                AlarmScheduler.scheduleAlarm(appContext, a1.copy(id = id1))
            }
        }
    }

    val alarmsList: StateFlow<List<AlarmDisplayItem>> = combine(
        db.alarmDao().getAllAlarms(),
        is24Hour
    ) { alarms, is24h ->
        alarms.map { alarm ->
            val nextTrigger = AlarmScheduler.calculateNextTrigger(alarm.hour, alarm.minute, alarm.daysOfWeek)
            val h = if (is24h) alarm.hour else {
                val rem = alarm.hour % 12
                if (rem == 0) 12 else rem
            }
            val timeStr = String.format("%d:%02d", h, alarm.minute)
            val amPmStr = if (is24h) "" else if (alarm.hour < 12) "AM" else "PM"

            AlarmDisplayItem(
                alarm = alarm,
                timeFormatted = timeStr,
                amPmFormatted = amPmStr,
                nextTriggerZoned = nextTrigger
            )
        }.sortedBy { it.nextTriggerZoned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nextRingCountdown: StateFlow<String?> = alarmsList.map { list ->
        val nextEnabled = list.filter { it.alarm.isEnabled }.minByOrNull { it.nextTriggerZoned }
        nextEnabled?.let {
            "Rings in ${AlarmScheduler.formatDurationUntil(it.nextTriggerZoned)}"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleAlarm(alarm: Alarm, isEnabled: Boolean) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = isEnabled)
            db.alarmDao().updateAlarm(updated)
            if (isEnabled) {
                AlarmScheduler.scheduleAlarm(appContext, updated)
                val nextTrigger = AlarmScheduler.calculateNextTrigger(updated.hour, updated.minute, updated.daysOfWeek)
                _snackbarMessage.value = "Alarm set for ${AlarmScheduler.formatDurationUntil(nextTrigger)} from now"
            } else {
                AlarmScheduler.cancelAlarm(appContext, alarm.id)
            }
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun openCreateAlarm() {
        _editingAlarm.value = null
        _showCreateSheet.value = true
    }

    fun openEditAlarm(alarm: Alarm) {
        _editingAlarm.value = alarm
        _showCreateSheet.value = true
    }

    fun dismissCreateSheet() {
        _showCreateSheet.value = false
        _editingAlarm.value = null
    }

    fun saveAlarm(alarm: Alarm) {
        viewModelScope.launch {
            val isNew = alarm.id == 0L
            val finalId = if (isNew) {
                db.alarmDao().insertAlarm(alarm)
            } else {
                db.alarmDao().updateAlarm(alarm)
                alarm.id
            }

            val savedAlarm = alarm.copy(id = finalId)
            if (savedAlarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(appContext, savedAlarm)
                val nextTrigger = AlarmScheduler.calculateNextTrigger(savedAlarm.hour, savedAlarm.minute, savedAlarm.daysOfWeek)
                _snackbarMessage.value = "Alarm set for ${AlarmScheduler.formatDurationUntil(nextTrigger)} from now"
            } else {
                AlarmScheduler.cancelAlarm(appContext, finalId)
            }

            _showCreateSheet.value = false
            _editingAlarm.value = null
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            recentlyDeletedAlarm = alarm
            AlarmScheduler.cancelAlarm(appContext, alarm.id)
            db.alarmDao().deleteAlarm(alarm)
            _showCreateSheet.value = false
            _editingAlarm.value = null
            _snackbarMessage.value = "Alarm deleted"
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun undoDelete() {
        val toRestore = recentlyDeletedAlarm ?: return
        viewModelScope.launch {
            val id = db.alarmDao().insertAlarm(toRestore.copy(id = 0))
            if (toRestore.isEnabled) {
                AlarmScheduler.scheduleAlarm(appContext, toRestore.copy(id = id))
            }
            recentlyDeletedAlarm = null
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
