package com.example.clock.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.clock.data.model.Alarm
import com.example.clock.receiver.AlarmReceiver
import java.time.*
import java.time.temporal.TemporalAdjusters

object AlarmScheduler {

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_IS_BEDTIME = "extra_is_bedtime"
    const val EXTRA_IS_UPCOMING = "extra_is_upcoming"

    fun scheduleAlarm(context: Context, alarm: Alarm) {
        if (!alarm.isEnabled) {
            cancelAlarm(context, alarm.id)
            return
        }

        val nextTriggerZonedDateTime = calculateNextTrigger(alarm.hour, alarm.minute, alarm.daysOfWeek)
        val triggerMillis = nextTriggerZonedDateTime.toInstant().toEpochMilli()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Intent to fire ring screen/service
        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val firePendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show intent for system lock screen clock display
        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarm.id.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)

        if (canScheduleExactAlarms(alarmManager)) {
            alarmManager.setAlarmClock(alarmClockInfo, firePendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, firePendingIntent)
        }

        // Schedule upcoming 30-min notice if >= 30 min away
        scheduleUpcomingNotice(context, alarm, triggerMillis)
    }

    private fun scheduleUpcomingNotice(context: Context, alarm: Alarm, triggerMillis: Long) {
        val thirtyMinBefore = triggerMillis - (30 * 60 * 1000L)
        if (thirtyMinBefore > System.currentTimeMillis()) {
            val noticeIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_UPCOMING_NOTICE
                putExtra(EXTRA_ALARM_ID, alarm.id)
            }
            val noticePending = PendingIntent.getBroadcast(
                context,
                (alarm.id + 100000).toInt(),
                noticeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC_WAKEUP, thirtyMinBefore, noticePending)
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val firePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            fireIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (firePendingIntent != null) {
            alarmManager.cancel(firePendingIntent)
            firePendingIntent.cancel()
        }

        val noticeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_UPCOMING_NOTICE
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val noticePending = PendingIntent.getBroadcast(
            context,
            (alarmId + 100000).toInt(),
            noticeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (noticePending != null) {
            alarmManager.cancel(noticePending)
            noticePending.cancel()
        }
    }

    fun snoozeAlarm(context: Context, alarmId: Long, snoozeMinutes: Int) {
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_ALARM
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        val firePendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
        if (canScheduleExactAlarms(alarmManager)) {
            alarmManager.setAlarmClock(alarmClockInfo, firePendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, firePendingIntent)
        }
    }

    fun scheduleTimer(context: Context, timerId: Long, durationSeconds: Long) {
        val triggerMillis = System.currentTimeMillis() + (durationSeconds * 1000L)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_TIMER
            putExtra("extra_timer_id", timerId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            (timerId + 500000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = Intent(context, MainActivity::class.java)
        val showPi = PendingIntent.getActivity(
            context,
            (timerId + 500000).toInt(),
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val info = AlarmManager.AlarmClockInfo(triggerMillis, showPi)
        if (canScheduleExactAlarms(alarmManager)) {
            alarmManager.setAlarmClock(info, pi)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    fun cancelTimer(context: Context, timerId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIRE_TIMER
            putExtra("extra_timer_id", timerId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            (timerId + 500000).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    private fun canScheduleExactAlarms(alarmManager: AlarmManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Correctly calculates next trigger time with ZonedDateTime taking into account daylight saving time.
     */
    fun calculateNextTrigger(hour: Int, minute: Int, daysOfWeekMask: Int): ZonedDateTime {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        if (daysOfWeekMask == 0) {
            // One-time alarm
            var candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusDays(1)
            }
            return candidate
        }

        // Repeating alarm: check next 7 days for the earliest enabled day
        val candidateDays = (1..7).filter { (daysOfWeekMask and (1 shl it)) != 0 }
        var earliest: ZonedDateTime? = null

        for (dayVal in candidateDays) {
            val targetDow = DayOfWeek.of(dayVal)
            var candidate = now.with(TemporalAdjusters.nextOrSame(targetDow))
                .withHour(hour).withMinute(minute).withSecond(0).withNano(0)
            if (!candidate.isAfter(now)) {
                candidate = candidate.plusWeeks(1)
            }
            if (earliest == null || candidate.isBefore(earliest)) {
                earliest = candidate
            }
        }

        return earliest ?: now.plusDays(1).withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    }

    fun formatDurationUntil(target: ZonedDateTime): String {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val duration = Duration.between(now, target)
        val totalMinutes = duration.toMinutes().coerceAtLeast(1)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        return when {
            hours == 0L -> "$mins min"
            mins == 0L -> "$hours hr"
            else -> "$hours hr $mins min"
        }
    }
}
