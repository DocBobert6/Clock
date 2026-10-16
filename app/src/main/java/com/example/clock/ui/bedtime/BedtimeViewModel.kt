package com.example.clock.ui.bedtime

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.clock.data.alarm.AlarmScheduler
import com.example.clock.data.audio.ProceduralSoundGenerator
import com.example.clock.data.preferences.BedtimeSettings
import com.example.clock.data.preferences.ClockPreferences
import com.example.clock.service.SleepSoundService
import com.example.clock.ui.bedside.BedsideClockActivity
import com.example.clock.widget.WidgetUpdateHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BedtimeViewModel(context: Context) : ViewModel() {

    private val prefs = ClockPreferences(context)
    private val appContext = context.applicationContext

    val bedtimeSettings: StateFlow<BedtimeSettings> = prefs.bedtimeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BedtimeSettings())

    private val _isSleepSoundPlaying = MutableStateFlow(false)
    val isSleepSoundPlaying: StateFlow<Boolean> = _isSleepSoundPlaying.asStateFlow()

    private val _activeSound = MutableStateFlow("Gentle Rain")
    val activeSound: StateFlow<String> = _activeSound.asStateFlow()

    private val _soundVolume = MutableStateFlow(0.7f)
    val soundVolume: StateFlow<Float> = _soundVolume.asStateFlow()

    init {
        ProceduralSoundGenerator.ensureSoundsGenerated(appContext)
    }

    fun updateBedtime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val cur = bedtimeSettings.value
            val updated = cur.copy(bedtimeHour = hour, bedtimeMinute = minute)
            prefs.updateBedtimeSettings(updated)
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun updateWakeTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val cur = bedtimeSettings.value
            val updated = cur.copy(wakeHour = hour, wakeMinute = minute)
            prefs.updateBedtimeSettings(updated)
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun toggleSchedule(enabled: Boolean) {
        viewModelScope.launch {
            val cur = bedtimeSettings.value
            val updated = cur.copy(isEnabled = enabled)
            prefs.updateBedtimeSettings(updated)
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun toggleDay(dayOfWeek: Int) { // 1 = Mon .. 7 = Sun
        viewModelScope.launch {
            val cur = bedtimeSettings.value
            val mask = cur.daysOfWeek
            val bit = 1 shl dayOfWeek
            val newMask = if ((mask and bit) != 0) (mask and bit.inv()) else (mask or bit)
            prefs.updateBedtimeSettings(cur.copy(daysOfWeek = newMask))
        }
    }

    fun updateWindDown(minutes: Int) {
        viewModelScope.launch {
            val cur = bedtimeSettings.value
            prefs.updateBedtimeSettings(cur.copy(windDownMinutes = minutes))
        }
    }

    fun toggleDnd(enabled: Boolean) {
        viewModelScope.launch {
            val cur = bedtimeSettings.value
            prefs.updateBedtimeSettings(cur.copy(enableDnd = enabled))
        }
    }

    fun updateSleepGoal(hours: Float) {
        viewModelScope.launch {
            val cur = bedtimeSettings.value
            prefs.updateBedtimeSettings(cur.copy(sleepGoalHours = hours))
        }
    }

    fun toggleRedShift(enabled: Boolean) {
        viewModelScope.launch {
            val cur = bedtimeSettings.value
            prefs.updateBedtimeSettings(cur.copy(isBedsideRedShift = enabled))
        }
    }

    fun startSleepSound(soundName: String, durationMinutes: Int = 45) {
        _activeSound.value = soundName
        _isSleepSoundPlaying.value = true
        val intent = Intent(appContext, SleepSoundService::class.java).apply {
            action = SleepSoundService.ACTION_START
            putExtra(SleepSoundService.EXTRA_SOUND_NAME, soundName)
            putExtra(SleepSoundService.EXTRA_VOLUME, _soundVolume.value)
            putExtra(SleepSoundService.EXTRA_DURATION_MINUTES, durationMinutes)
        }
        appContext.startService(intent)
    }

    fun stopSleepSound() {
        _isSleepSoundPlaying.value = false
        val intent = Intent(appContext, SleepSoundService::class.java).apply {
            action = SleepSoundService.ACTION_STOP
        }
        appContext.startService(intent)
    }

    fun setSoundVolume(volume: Float) {
        _soundVolume.value = volume
        if (_isSleepSoundPlaying.value) {
            val intent = Intent(appContext, SleepSoundService::class.java).apply {
                action = SleepSoundService.ACTION_START
                putExtra(SleepSoundService.EXTRA_SOUND_NAME, _activeSound.value)
                putExtra(SleepSoundService.EXTRA_VOLUME, volume)
            }
            appContext.startService(intent)
        }
    }

    fun launchBedsideClock(context: Context) {
        val intent = Intent(context, BedsideClockActivity::class.java)
        context.startActivity(intent)
    }

    fun hasDndPermission(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            nm.isNotificationPolicyAccessGranted
        } else true
    }
}
