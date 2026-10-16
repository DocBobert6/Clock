package com.example.clock.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "clock_preferences")

data class BedtimeSettings(
    val isEnabled: Boolean = false,
    val bedtimeHour: Int = 23,
    val bedtimeMinute: Int = 0,
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val daysOfWeek: Int = 0b0111110, // Mon-Fri default (bits 1..5)
    val windDownMinutes: Int = 30,
    val enableDnd: Boolean = false,
    val sleepGoalHours: Float = 8f,
    val soundName: String = "Gentle Rain",
    val soundVolume: Float = 0.7f,
    val autoStopMinutes: Int = 45, // 0 = until wake up
    val isBedsideRedShift: Boolean = false
)

enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ClockFaceStyle {
    DIGITAL,
    ANALOG
}

enum class VolumeButtonAction {
    SNOOZE,
    DISMISS,
    SILENCE,
    NOTHING
}

class ClockPreferences(private val context: Context) {

    private object Keys {
        val APPEARANCE_MODE = stringPreferencesKey("appearance_mode")
        val CLOCK_FACE_STYLE = stringPreferencesKey("clock_face_style")
        val TIME_FORMAT_24H = booleanPreferencesKey("time_format_24h")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val WEEK_START_MONDAY = booleanPreferencesKey("week_start_monday")
        val HOME_TIMEZONE_AUTO = booleanPreferencesKey("home_timezone_auto")
        val HOME_TIMEZONE_ID = stringPreferencesKey("home_timezone_id")

        // Alarm defaults
        val ALARM_SILENCE_AFTER_MINUTES = intPreferencesKey("alarm_silence_after_minutes")
        val ALARM_DEFAULT_SNOOZE_MINUTES = intPreferencesKey("alarm_default_snooze_minutes")
        val ALARM_GRADUAL_VOLUME_SECONDS = intPreferencesKey("alarm_gradual_volume_seconds")
        val ALARM_DEFAULT_VOLUME = floatPreferencesKey("alarm_default_volume")
        val ALARM_VOLUME_BUTTON_ACTION = stringPreferencesKey("alarm_volume_button_action")

        // Timer defaults
        val TIMER_DEFAULT_SOUND = stringPreferencesKey("timer_default_sound")
        val TIMER_VIBRATE = booleanPreferencesKey("timer_vibrate")

        // Bedtime
        val BEDTIME_ENABLED = booleanPreferencesKey("bedtime_enabled")
        val BEDTIME_HOUR = intPreferencesKey("bedtime_hour")
        val BEDTIME_MINUTE = intPreferencesKey("bedtime_minute")
        val BEDTIME_WAKE_HOUR = intPreferencesKey("bedtime_wake_hour")
        val BEDTIME_WAKE_MINUTE = intPreferencesKey("bedtime_wake_minute")
        val BEDTIME_DAYS = intPreferencesKey("bedtime_days")
        val BEDTIME_WIND_DOWN = intPreferencesKey("bedtime_wind_down")
        val BEDTIME_DND = booleanPreferencesKey("bedtime_dnd")
        val BEDTIME_SLEEP_GOAL = floatPreferencesKey("bedtime_sleep_goal")
        val BEDTIME_SOUND = stringPreferencesKey("bedtime_sound")
        val BEDTIME_SOUND_VOL = floatPreferencesKey("bedtime_sound_vol")
        val BEDTIME_AUTO_STOP = intPreferencesKey("bedtime_auto_stop")
        val BEDTIME_RED_SHIFT = booleanPreferencesKey("bedtime_red_shift")
    }

    val appearanceMode: Flow<AppearanceMode> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.APPEARANCE_MODE] ?: AppearanceMode.DARK.name
        runCatching { AppearanceMode.valueOf(name) }.getOrDefault(AppearanceMode.DARK)
    }

    suspend fun setAppearanceMode(mode: AppearanceMode) {
        context.dataStore.edit { it[Keys.APPEARANCE_MODE] = mode.name }
    }

    val clockFaceStyle: Flow<ClockFaceStyle> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.CLOCK_FACE_STYLE] ?: ClockFaceStyle.DIGITAL.name
        runCatching { ClockFaceStyle.valueOf(name) }.getOrDefault(ClockFaceStyle.DIGITAL)
    }

    suspend fun setClockFaceStyle(style: ClockFaceStyle) {
        context.dataStore.edit { it[Keys.CLOCK_FACE_STYLE] = style.name }
    }

    val is24HourFormat: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.TIME_FORMAT_24H] ?: android.text.format.DateFormat.is24HourFormat(context)
    }

    suspend fun set24HourFormat(is24h: Boolean) {
        context.dataStore.edit { it[Keys.TIME_FORMAT_24H] = is24h }
    }

    val showSeconds: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_SECONDS] ?: true
    }

    suspend fun setShowSeconds(show: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SECONDS] = show }
    }

    val alarmSilenceAfterMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.ALARM_SILENCE_AFTER_MINUTES] ?: 10
    }

    suspend fun setAlarmSilenceAfterMinutes(min: Int) {
        context.dataStore.edit { it[Keys.ALARM_SILENCE_AFTER_MINUTES] = min }
    }

    val alarmSnoozeLengthMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.ALARM_DEFAULT_SNOOZE_MINUTES] ?: 9
    }

    suspend fun setAlarmSnoozeLengthMinutes(min: Int) {
        context.dataStore.edit { it[Keys.ALARM_DEFAULT_SNOOZE_MINUTES] = min }
    }

    val volumeButtonAction: Flow<VolumeButtonAction> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.ALARM_VOLUME_BUTTON_ACTION] ?: VolumeButtonAction.SNOOZE.name
        runCatching { VolumeButtonAction.valueOf(name) }.getOrDefault(VolumeButtonAction.SNOOZE)
    }

    suspend fun setVolumeButtonAction(action: VolumeButtonAction) {
        context.dataStore.edit { it[Keys.ALARM_VOLUME_BUTTON_ACTION] = action.name }
    }

    val bedtimeSettings: Flow<BedtimeSettings> = context.dataStore.data.map { prefs ->
        BedtimeSettings(
            isEnabled = prefs[Keys.BEDTIME_ENABLED] ?: false,
            bedtimeHour = prefs[Keys.BEDTIME_HOUR] ?: 23,
            bedtimeMinute = prefs[Keys.BEDTIME_MINUTE] ?: 0,
            wakeHour = prefs[Keys.BEDTIME_WAKE_HOUR] ?: 7,
            wakeMinute = prefs[Keys.BEDTIME_WAKE_MINUTE] ?: 0,
            daysOfWeek = prefs[Keys.BEDTIME_DAYS] ?: 0b0111110,
            windDownMinutes = prefs[Keys.BEDTIME_WIND_DOWN] ?: 30,
            enableDnd = prefs[Keys.BEDTIME_DND] ?: false,
            sleepGoalHours = prefs[Keys.BEDTIME_SLEEP_GOAL] ?: 8f,
            soundName = prefs[Keys.BEDTIME_SOUND] ?: "Gentle Rain",
            soundVolume = prefs[Keys.BEDTIME_SOUND_VOL] ?: 0.7f,
            autoStopMinutes = prefs[Keys.BEDTIME_AUTO_STOP] ?: 45,
            isBedsideRedShift = prefs[Keys.BEDTIME_RED_SHIFT] ?: false
        )
    }

    suspend fun updateBedtimeSettings(settings: BedtimeSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.BEDTIME_ENABLED] = settings.isEnabled
            prefs[Keys.BEDTIME_HOUR] = settings.bedtimeHour
            prefs[Keys.BEDTIME_MINUTE] = settings.bedtimeMinute
            prefs[Keys.BEDTIME_WAKE_HOUR] = settings.wakeHour
            prefs[Keys.BEDTIME_WAKE_MINUTE] = settings.wakeMinute
            prefs[Keys.BEDTIME_DAYS] = settings.daysOfWeek
            prefs[Keys.BEDTIME_WIND_DOWN] = settings.windDownMinutes
            prefs[Keys.BEDTIME_DND] = settings.enableDnd
            prefs[Keys.BEDTIME_SLEEP_GOAL] = settings.sleepGoalHours
            prefs[Keys.BEDTIME_SOUND] = settings.soundName
            prefs[Keys.BEDTIME_SOUND_VOL] = settings.soundVolume
            prefs[Keys.BEDTIME_AUTO_STOP] = settings.autoStopMinutes
            prefs[Keys.BEDTIME_RED_SHIFT] = settings.isBedsideRedShift
        }
    }
}
