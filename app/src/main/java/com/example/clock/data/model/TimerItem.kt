package com.example.clock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    RINGING
}

@Entity(tableName = "timers")
data class TimerItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val totalSeconds: Long, // e.g. 300 for 5 min
    val remainingSeconds: Long,
    val label: String = "Timer",
    val soundUri: String = "procedural:cosmic_pulse",
    val soundName: String = "Cosmic Pulse",
    val state: TimerState = TimerState.IDLE,
    val startedAtElapsed: Long = 0L, // SystemClock.elapsedRealtime() when started
    val endsAtTimestamp: Long = 0L, // System.currentTimeMillis() when it will expire
    val orderIndex: Int = 0
)
