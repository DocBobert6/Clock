package com.example.clock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_cities")
data class City(
    @PrimaryKey
    val id: String, // e.g., "America/New_York#New York"
    val name: String,
    val country: String,
    val timeZoneId: String,
    val latitude: Double,
    val longitude: Double,
    val isHome: Boolean = false,
    val orderIndex: Int = 0
)
