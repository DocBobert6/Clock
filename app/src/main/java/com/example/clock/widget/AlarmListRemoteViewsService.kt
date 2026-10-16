package com.example.clock.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.Alarm
import kotlinx.coroutines.runBlocking

class AlarmListRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AlarmListRemoteViewsFactory(applicationContext)
    }
}

class AlarmListRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var alarms: List<Alarm> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            val db = AppDatabase.getInstance(context)
            alarms = db.alarmDao().getAllAlarmsSync()
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = alarms.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= alarms.size) return RemoteViews(context.packageName, R.layout.widget_alarm_list_item)
        val alarm = alarms[position]
        val views = RemoteViews(context.packageName, R.layout.widget_alarm_list_item)

        val timeStr = String.format("%02d:%02d", alarm.hour, alarm.minute)
        views.setTextViewText(R.id.item_alarm_time, timeStr)
        views.setTextViewText(R.id.item_alarm_label, alarm.label)

        val toggleIcon = if (alarm.isEnabled) android.R.drawable.checkbox_on_background else android.R.drawable.checkbox_off_background
        views.setImageViewResource(R.id.item_alarm_toggle, toggleIcon)

        // Fill in intent for toggle click
        val fillInIntent = Intent().apply {
            putExtra(WidgetActionReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        views.setOnClickFillInIntent(R.id.item_alarm_toggle, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = alarms.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}

// Add extension helper in AppDatabase DAO
suspend fun com.example.clock.data.db.AlarmDao.getAllAlarmsSync(): List<Alarm> {
    return getEnabledAlarmsSync() // or all
}
