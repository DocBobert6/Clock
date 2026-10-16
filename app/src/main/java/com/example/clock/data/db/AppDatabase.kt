package com.example.clock.data.db

import androidx.room.*
import com.example.clock.data.model.Alarm
import com.example.clock.data.model.City
import com.example.clock.data.model.LapItem
import com.example.clock.data.model.TimerItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<Alarm>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarmsSync(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE id = :id LIMIT 1")
    suspend fun getAlarmById(id: Long): Alarm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: Alarm): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm)

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun deleteAlarmById(id: Long)
}

@Dao
interface CityDao {
    @Query("SELECT * FROM saved_cities ORDER BY orderIndex ASC, name ASC")
    fun getAllCities(): Flow<List<City>>

    @Query("SELECT * FROM saved_cities ORDER BY orderIndex ASC")
    suspend fun getAllCitiesSync(): List<City>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCity(city: City)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCities(cities: List<City>)

    @Update
    suspend fun updateCity(city: City)

    @Delete
    suspend fun deleteCity(city: City)

    @Query("DELETE FROM saved_cities WHERE id = :id")
    suspend fun deleteCityById(id: String)
}

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY orderIndex ASC, id DESC")
    fun getAllTimers(): Flow<List<TimerItem>>

    @Query("SELECT * FROM timers WHERE state = 'RUNNING' OR state = 'RINGING'")
    suspend fun getActiveTimersSync(): List<TimerItem>

    @Query("SELECT * FROM timers WHERE id = :id LIMIT 1")
    suspend fun getTimerById(id: Long): TimerItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: TimerItem): Long

    @Update
    suspend fun updateTimer(timer: TimerItem)

    @Delete
    suspend fun deleteTimer(timer: TimerItem)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun deleteTimerById(id: Long)
}

@Dao
interface LapDao {
    @Query("SELECT * FROM stopwatch_laps ORDER BY lapNumber DESC")
    fun getAllLaps(): Flow<List<LapItem>>

    @Query("SELECT * FROM stopwatch_laps ORDER BY lapNumber DESC")
    suspend fun getAllLapsSync(): List<LapItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLap(lap: LapItem)

    @Query("DELETE FROM stopwatch_laps")
    suspend fun clearLaps()
}

@Database(
    entities = [Alarm::class, City::class, TimerItem::class, LapItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun cityDao(): CityDao
    abstract fun timerDao(): TimerDao
    abstract fun lapDao(): LapDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clock_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
