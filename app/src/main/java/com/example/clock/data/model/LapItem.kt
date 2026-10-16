package com.example.clock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stopwatch_laps")
data class LapItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val lapNumber: Int,
    val lapTimeMs: Long,
    val totalTimeMs: Long
)
