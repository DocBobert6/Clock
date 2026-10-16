package com.example.clock.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.clock.data.alarm.AlarmScheduler
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.TimerState
import com.example.clock.data.preferences.ClockPreferences
import com.example.clock.ui.stopwatch.StopwatchViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 1. Digital Clock Widget
 */
class DigitalClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "world_clock")
        }
        val pi = PendingIntent.getActivity(context, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_digital_clock)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

/**
 * 2. Analog Clock Widget
 */
class AnalogClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_tab", "world_clock")
        }
        val pi = PendingIntent.getActivity(context, 102, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_analog_clock)
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

/**
 * 3. Clock and Date Combo Widget
 */
class ClockDateWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_tab", "alarms")
        }
        val pi = PendingIntent.getActivity(context, 103, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        runBlocking {
            val db = AppDatabase.getInstance(context)
            val enabledAlarms = db.alarmDao().getEnabledAlarmsSync()
            val nextAlarm = enabledAlarms.minByOrNull {
                AlarmScheduler.calculateNextTrigger(it.hour, it.minute, it.daysOfWeek)
            }
            val alarmText = nextAlarm?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: "No Alarm"

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_clock_date)
                views.setOnClickPendingIntent(R.id.widget_root, pi)
                views.setTextViewText(R.id.widget_combo_alarm, alarmText)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}

/**
 * 4. World Clock Small Widget
 */
class WorldClockSmallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_tab", "world_clock")
        }
        val pi = PendingIntent.getActivity(context, 104, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        runBlocking {
            val db = AppDatabase.getInstance(context)
            val cities = db.cityDao().getAllCitiesSync()
            val city1 = cities.getOrNull(0)
            val city2 = cities.getOrNull(1)

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_world_clock_small)
                views.setOnClickPendingIntent(R.id.widget_root, pi)

                if (city1 != null) {
                    views.setTextViewText(R.id.city_1_name, city1.name)
                    views.setString(R.id.city_1_clock, "setTimeZone", city1.timeZoneId)
                }
                if (city2 != null) {
                    views.setTextViewText(R.id.city_2_name, city2.name)
                    views.setString(R.id.city_2_clock, "setTimeZone", city2.timeZoneId)
                }
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}

/**
 * 5. World Clock Large Widget
 */
class WorldClockLargeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_tab", "world_clock")
        }
        val pi = PendingIntent.getActivity(context, 105, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        runBlocking {
            val db = AppDatabase.getInstance(context)
            val cities = db.cityDao().getAllCitiesSync()

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_world_clock_large)
                views.setOnClickPendingIntent(R.id.widget_root, pi)

                val nameIds = listOf(R.id.city_1_name, R.id.city_2_name, R.id.city_3_name, R.id.city_4_name)
                val clockIds = listOf(R.id.city_1_clock, R.id.city_2_clock, R.id.city_3_clock, R.id.city_4_clock)

                for (i in 0..3) {
                    val c = cities.getOrNull(i)
                    if (c != null) {
                        views.setTextViewText(nameIds[i], c.name)
                        views.setString(clockIds[i], "setTimeZone", c.timeZoneId)
                    }
                }
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}

/**
 * 6. Next Alarm Widget
 */
class NextAlarmWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_tab", "alarms")
        }
        val pi = PendingIntent.getActivity(context, 106, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        runBlocking {
            val db = AppDatabase.getInstance(context)
            val enabledAlarms = db.alarmDao().getEnabledAlarmsSync()
            val nextAlarm = enabledAlarms.minByOrNull {
                AlarmScheduler.calculateNextTrigger(it.hour, it.minute, it.daysOfWeek)
            }

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_next_alarm)
                views.setOnClickPendingIntent(R.id.widget_root, pi)

                if (nextAlarm != null) {
                    val nextZoned = AlarmScheduler.calculateNextTrigger(nextAlarm.hour, nextAlarm.minute, nextAlarm.daysOfWeek)
                    val timeStr = String.format("%02d:%02d", nextAlarm.hour, nextAlarm.minute)
                    views.setTextViewText(R.id.widget_next_alarm_time, timeStr)
                    views.setTextViewText(R.id.widget_next_alarm_label, nextAlarm.label)
                    views.setTextViewText(R.id.widget_next_alarm_countdown, "Rings in ${AlarmScheduler.formatDurationUntil(nextZoned)}")
                } else {
                    views.setTextViewText(R.id.widget_next_alarm_time, "No Alarm")
                    views.setTextViewText(R.id.widget_next_alarm_label, "No upcoming alarms scheduled")
                    views.setTextViewText(R.id.widget_next_alarm_countdown, "")
                }
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}

/**
 * 7. Alarm List Widget
 */
class AlarmListWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_alarm_list)

            val serviceIntent = Intent(context, AlarmListRemoteViewsService::class.java)
            views.setRemoteAdapter(R.id.widget_alarm_listview, serviceIntent)

            // PendingIntent template for items toggle
            val toggleIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_TOGGLE_ALARM
            }
            val togglePi = PendingIntent.getBroadcast(
                context,
                107,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_alarm_listview, togglePi)

            appWidgetManager.notifyAppWidgetViewDataChanged(id, R.id.widget_alarm_listview)
            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

/**
 * 8. Stopwatch Widget
 */
class StopwatchWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val isRunning = StopwatchViewModel.isRunningStatic
        val base = SystemClock.elapsedRealtime() - StopwatchViewModel.accumulatedElapsedMsStatic

        val startStopIntent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_STOPWATCH_START_STOP
        }
        val startStopPi = PendingIntent.getBroadcast(context, 108, startStopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val lapResetIntent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_STOPWATCH_LAP_RESET
        }
        val lapResetPi = PendingIntent.getBroadcast(context, 109, lapResetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_stopwatch)
            views.setChronometer(R.id.widget_stopwatch_chronometer, base, "%s", isRunning)
            views.setTextViewText(R.id.widget_stopwatch_btn_start_stop, if (isRunning) "Stop" else "Start")
            views.setTextViewText(R.id.widget_stopwatch_btn_lap_reset, if (isRunning) "Lap" else "Reset")

            views.setOnClickPendingIntent(R.id.widget_stopwatch_btn_start_stop, startStopPi)
            views.setOnClickPendingIntent(R.id.widget_stopwatch_btn_lap_reset, lapResetPi)

            appWidgetManager.updateAppWidget(id, views)
        }
    }
}

/**
 * 9. Timer Widget
 */
class TimerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            val active = db.timerDao().getActiveTimersSync().firstOrNull()

            val p1Intent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_TIMER_START_PRESET
                putExtra(WidgetActionReceiver.EXTRA_PRESET_SECONDS, 60L)
            }
            val p1Pi = PendingIntent.getBroadcast(context, 110, p1Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val p5Intent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_TIMER_START_PRESET
                putExtra(WidgetActionReceiver.EXTRA_PRESET_SECONDS, 300L)
            }
            val p5Pi = PendingIntent.getBroadcast(context, 111, p5Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            val cancelIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = WidgetActionReceiver.ACTION_TIMER_CANCEL
            }
            val cancelPi = PendingIntent.getBroadcast(context, 112, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_timer)
                if (active != null && active.state == TimerState.RUNNING) {
                    val remainingMs = (active.endsAtTimestamp - System.currentTimeMillis()).coerceAtLeast(0)
                    val base = SystemClock.elapsedRealtime() + remainingMs
                    views.setChronometerCountDown(R.id.widget_timer_countdown, true)
                    views.setChronometer(R.id.widget_timer_countdown, base, "%s", true)
                    views.setTextViewText(R.id.widget_timer_status, active.label)
                } else {
                    views.setChronometer(R.id.widget_timer_countdown, SystemClock.elapsedRealtime(), "00:00", false)
                    views.setTextViewText(R.id.widget_timer_status, "Tap preset to start")
                }

                views.setOnClickPendingIntent(R.id.widget_timer_preset_1, p1Pi)
                views.setOnClickPendingIntent(R.id.widget_timer_preset_5, p5Pi)
                views.setOnClickPendingIntent(R.id.widget_timer_btn_cancel, cancelPi)

                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}

/**
 * 10. Bedtime Widget
 */
class BedtimeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val soundIntent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_BEDTIME_SOUND_TOGGLE
        }
        val soundPi = PendingIntent.getBroadcast(context, 113, soundIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val toggleIntent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_BEDTIME_TOGGLE
        }
        val togglePi = PendingIntent.getBroadcast(context, 114, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        runBlocking {
            val prefs = ClockPreferences(context)
            val settings = prefs.bedtimeSettings.first()
            val timesStr = String.format("%02d:%02d → %02d:%02d", settings.bedtimeHour, settings.bedtimeMinute, settings.wakeHour, settings.wakeMinute)

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_bedtime)
                views.setTextViewText(R.id.widget_bedtime_times, timesStr)
                views.setTextViewText(R.id.widget_bedtime_toggle_btn, if (settings.isEnabled) "Active" else "Off")
                views.setOnClickPendingIntent(R.id.widget_bedtime_sound_btn, soundPi)
                views.setOnClickPendingIntent(R.id.widget_bedtime_toggle_btn, togglePi)
                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}

/**
 * 11. Dashboard Widget
 */
class DashboardWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val p5Intent = Intent(context, WidgetActionReceiver::class.java).apply {
            action = WidgetActionReceiver.ACTION_TIMER_START_PRESET
            putExtra(WidgetActionReceiver.EXTRA_PRESET_SECONDS, 300L)
        }
        val p5Pi = PendingIntent.getBroadcast(context, 115, p5Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        runBlocking {
            val db = AppDatabase.getInstance(context)
            val enabledAlarms = db.alarmDao().getEnabledAlarmsSync()
            val nextAlarm = enabledAlarms.minByOrNull {
                AlarmScheduler.calculateNextTrigger(it.hour, it.minute, it.daysOfWeek)
            }
            val alarmStr = nextAlarm?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: "No Alarm"

            val cities = db.cityDao().getAllCitiesSync()
            val c1 = cities.getOrNull(0)
            val c2 = cities.getOrNull(1)

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_dashboard)
                views.setTextViewText(R.id.widget_dash_alarm, alarmStr)
                if (c1 != null) views.setString(R.id.widget_dash_city_1, "setTimeZone", c1.timeZoneId)
                if (c2 != null) views.setString(R.id.widget_dash_city_2, "setTimeZone", c2.timeZoneId)
                views.setOnClickPendingIntent(R.id.widget_dash_timer_quick_btn, p5Pi)

                appWidgetManager.updateAppWidget(id, views)
            }
        }
    }
}
