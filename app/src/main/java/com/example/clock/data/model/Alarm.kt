package com.example.clock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "Alarm",
    val isEnabled: Boolean = true,
    val daysOfWeek: Int = 0, // Bitmask: bit 1 = Monday (1<<1), bit 2 = Tuesday (1<<2), ... bit 7 = Sunday (1<<7). 0 = Once
    val snoozeLengthMinutes: Int = 9,
    val soundUri: String = "procedural:morning_bell",
    val soundName: String = "Morning Bell",
    val vibrationPattern: String = "Standard",
    val isGradualVolume: Boolean = false,
    val gradualVolumeSeconds: Int = 15,
    val volume: Float = 0.85f,
    val deleteAfterRings: Boolean = false
) {
    fun isRepeating(): Boolean = daysOfWeek != 0

    fun isDayEnabled(dayOfWeek: Int): Boolean { // 1 = Monday, 7 = Sunday
        return (daysOfWeek and (1 shl dayOfWeek)) != 0
    }

    fun getRepeatSummary(): String {
        if (daysOfWeek == 0) return "Never"
        val days = (1..7).filter { isDayEnabled(it) }
        return when {
            days.size == 7 -> "Every day"
            days == listOf(1, 2, 3, 4, 5) -> "Every weekday"
            days == listOf(6, 7) -> "Weekends"
            else -> {
                val names = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                days.joinToString(", ") { names[it - 1] }
            }
        }
    }
}
