package com.example.clock.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetUpdateHelper {

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val providers = listOf(
            DigitalClockWidgetProvider::class.java,
            AnalogClockWidgetProvider::class.java,
            ClockDateWidgetProvider::class.java,
            WorldClockSmallWidgetProvider::class.java,
            WorldClockLargeWidgetProvider::class.java,
            NextAlarmWidgetProvider::class.java,
            AlarmListWidgetProvider::class.java,
            StopwatchWidgetProvider::class.java,
            TimerWidgetProvider::class.java,
            BedtimeWidgetProvider::class.java,
            DashboardWidgetProvider::class.java
        )

        providers.forEach { providerClass ->
            val ids = appWidgetManager.getAppWidgetIds(ComponentName(context, providerClass))
            if (ids.isNotEmpty()) {
                val intent = Intent(context, providerClass).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
