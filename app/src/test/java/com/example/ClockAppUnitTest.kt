package com.example

import com.example.clock.data.alarm.AlarmScheduler
import com.example.clock.data.citydata.CityDatabase
import com.example.clock.data.model.Alarm
import com.example.clock.data.suncalc.SunCalculator
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ClockAppUnitTest {

    @Test
    fun testAlarmRepeatSummary() {
        val onceAlarm = Alarm(hour = 7, minute = 0, daysOfWeek = 0)
        assertEquals("Never", onceAlarm.getRepeatSummary())

        // Monday to Friday: bits 1..5 = 0b00111110 = 62
        val weekdayAlarm = Alarm(hour = 7, minute = 0, daysOfWeek = 0b00111110)
        assertEquals("Every weekday", weekdayAlarm.getRepeatSummary())

        // Saturday and Sunday: (1 shl 6) or (1 shl 7) = 64 + 128 = 192
        val weekendAlarm = Alarm(hour = 9, minute = 0, daysOfWeek = (1 shl 6) or (1 shl 7))
        assertEquals("Weekends", weekendAlarm.getRepeatSummary())

        // Every day: (1..7).fold(0) { acc, d -> acc or (1 shl d) }
        val everyDayMask = (1..7).fold(0) { acc, d -> acc or (1 shl d) }
        val everyDayAlarm = Alarm(hour = 8, minute = 0, daysOfWeek = everyDayMask)
        assertEquals("Every day", everyDayAlarm.getRepeatSummary())
    }

    @Test
    fun testSunCalculator() {
        // Paris coordinates: lat 48.8566, lon 2.3522 on Equinox
        val zone = ZoneId.of("Europe/Paris")
        val date = LocalDate.of(2026, 3, 20)
        val (sunrise, sunset) = SunCalculator.calculateSunriseSunset(48.8566, 2.3522, date, zone)

        assertNotNull(sunrise)
        assertNotNull(sunset)
        // Sunrise in Paris around equinox is around 6:00-7:00 local time
        assertTrue(sunrise!!.hour in 5..8)
        assertTrue(sunset!!.hour in 18..21)
        assertTrue(sunset.isAfter(sunrise))
    }

    @Test
    fun testCityDatabaseContent() {
        val cities = CityDatabase.CITIES
        assertTrue(cities.size >= 50)
        val ny = cities.find { it.name == "New York" }
        assertNotNull(ny)
        assertEquals("America/New_York", ny?.timeZoneId)

        val tokyo = cities.find { it.name == "Tokyo" }
        assertNotNull(tokyo)
        assertEquals("Asia/Tokyo", tokyo?.timeZoneId)
    }

    @Test
    fun testAlarmSchedulerNextTrigger() {
        val nextTrigger = AlarmScheduler.calculateNextTrigger(10, 30, 0)
        assertNotNull(nextTrigger)
        assertEquals(10, nextTrigger.hour)
        assertEquals(30, nextTrigger.minute)
    }
}
