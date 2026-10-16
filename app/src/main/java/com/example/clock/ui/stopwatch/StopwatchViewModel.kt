package com.example.clock.ui.stopwatch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.MainActivity
import com.example.clock.data.db.AppDatabase
import com.example.clock.data.model.LapItem
import com.example.clock.widget.WidgetUpdateHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class StopwatchViewModel(context: Context) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val appContext = context.applicationContext
    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "stopwatch_channel"
        const val NOTIFICATION_ID = 4001

        // Singleton state memory for continuous running across config changes and background
        var isRunningStatic = false
        var baseElapsedRealtimeStatic = 0L
        var accumulatedElapsedMsStatic = 0L
    }

    private val _isRunning = MutableStateFlow(isRunningStatic)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _elapsedTimeMs = MutableStateFlow(calculateCurrentElapsed())
    val elapsedTimeMs: StateFlow<Long> = _elapsedTimeMs.asStateFlow()

    val laps: StateFlow<List<LapItem>> = db.lapDao().getAllLaps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fastestLapNumber: StateFlow<Int?> = laps.map { list ->
        if (list.size >= 2) list.minByOrNull { it.lapTimeMs }?.lapNumber else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val slowestLapNumber: StateFlow<Int?> = laps.map { list ->
        if (list.size >= 2) list.maxByOrNull { it.lapTimeMs }?.lapNumber else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private var tickerJob: Job? = null

    init {
        createNotificationChannel()
        if (isRunningStatic) {
            startTicker()
        }
    }

    private fun calculateCurrentElapsed(): Long {
        return if (isRunningStatic) {
            accumulatedElapsedMsStatic + (SystemClock.elapsedRealtime() - baseElapsedRealtimeStatic)
        } else {
            accumulatedElapsedMsStatic
        }
    }

    fun start() {
        if (isRunningStatic) return
        isRunningStatic = true
        _isRunning.value = true
        baseElapsedRealtimeStatic = SystemClock.elapsedRealtime()
        startTicker()
        showNotification()
        WidgetUpdateHelper.updateAllWidgets(appContext)
    }

    fun stop() {
        if (!isRunningStatic) return
        accumulatedElapsedMsStatic += (SystemClock.elapsedRealtime() - baseElapsedRealtimeStatic)
        isRunningStatic = false
        _isRunning.value = false
        tickerJob?.cancel()
        _elapsedTimeMs.value = accumulatedElapsedMsStatic
        cancelNotification()
        WidgetUpdateHelper.updateAllWidgets(appContext)
    }

    fun lap() {
        if (!isRunningStatic) return
        val currentTotal = calculateCurrentElapsed()
        viewModelScope.launch {
            val existingLaps = db.lapDao().getAllLapsSync()
            val lastTotal = existingLaps.firstOrNull()?.totalTimeMs ?: 0L
            val lapTime = currentTotal - lastTotal
            val nextNumber = existingLaps.size + 1

            db.lapDao().insertLap(
                LapItem(
                    lapNumber = nextNumber,
                    lapTimeMs = lapTime,
                    totalTimeMs = currentTotal
                )
            )
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    fun reset() {
        stop()
        accumulatedElapsedMsStatic = 0L
        baseElapsedRealtimeStatic = 0L
        _elapsedTimeMs.value = 0L
        viewModelScope.launch {
            db.lapDao().clearLaps()
            WidgetUpdateHelper.updateAllWidgets(appContext)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive && isRunningStatic) {
                _elapsedTimeMs.value = calculateCurrentElapsed()
                delay(30L) // smooth 30ms updates for hundredths
            }
        }
    }

    private fun showNotification() {
        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_tab", "stopwatch")
        }
        val openPi = PendingIntent.getActivity(
            appContext,
            4001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val whenTime = System.currentTimeMillis() - accumulatedElapsedMsStatic

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Stopwatch")
            .setWhen(whenTime)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setContentIntent(openPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun cancelNotification() {
        nm.cancel(NOTIFICATION_ID)
    }

    fun shareLaps(context: Context) {
        viewModelScope.launch {
            val currentLaps = db.lapDao().getAllLapsSync()
            if (currentLaps.isEmpty()) return@launch

            val sb = StringBuilder()
            sb.appendLine("Clock — Stopwatch Laps")
            sb.appendLine("----------------------")
            currentLaps.forEach { lap ->
                sb.appendLine("Lap ${lap.lapNumber}: ${formatTime(lap.lapTimeMs)} (Total: ${formatTime(lap.totalTimeMs)})")
            }
            sb.appendLine("----------------------")
            sb.appendLine("Total: ${formatTime(_elapsedTimeMs.value)}")

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Laps")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        }
    }

    fun formatTime(ms: Long): String {
        val hundredths = (ms / 10) % 100
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000)
        return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Stopwatch",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active Stopwatch Chronometer"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }
}
