package com.example.clock.data.suncalc

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.*

object SunCalculator {

    /**
     * Calculates local sunrise and sunset times based on coordinates, date, and zone.
     * Uses the standard NOAA solar calculation algorithm.
     */
    fun calculateSunriseSunset(
        latitude: Double,
        longitude: Double,
        date: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Pair<LocalTime?, LocalTime?> {
        val dayOfYear = date.dayOfYear

        // Convert longitude to hour value and calculate an approximate time
        val lngHour = longitude / 15.0

        // Sunrise
        val sunriseTime = calculateTime(latitude, lngHour, dayOfYear, isSunrise = true)
        // Sunset
        val sunsetTime = calculateTime(latitude, lngHour, dayOfYear, isSunrise = false)

        val sunrise = sunriseTime?.let { toLocalTime(it, date, zoneId) }
        val sunset = sunsetTime?.let { toLocalTime(it, date, zoneId) }

        return Pair(sunrise, sunset)
    }

    private fun calculateTime(
        lat: Double,
        lngHour: Double,
        dayOfYear: Int,
        isSunrise: Boolean
    ): Double? {
        val t = if (isSunrise) {
            dayOfYear + ((6.0 - lngHour) / 24.0)
        } else {
            dayOfYear + ((18.0 - lngHour) / 24.0)
        }

        // Sun's mean anomaly
        val m = (0.9856 * t) - 3.289
        // Sun's true longitude
        var l = m + (1.916 * sin(Math.toRadians(m))) + (0.020 * sin(Math.toRadians(2 * m))) + 282.634
        l = normalizeDegrees(l)

        // Sun's right ascension
        var ra = Math.toDegrees(atan(0.91764 * tan(Math.toRadians(l))))
        ra = normalizeDegrees(ra)

        // Right ascension value needs to be in the same quadrant as L
        val lQuadrant = floor(l / 90.0) * 90.0
        val raQuadrant = floor(ra / 90.0) * 90.0
        ra += (lQuadrant - raQuadrant)
        ra /= 15.0 // Convert to hours

        // Sun's declination
        val sinDec = 0.39782 * sin(Math.toRadians(l))
        val cosDec = cos(asin(sinDec))

        // Sun's local hour angle (Zenith for official sunrise/sunset is 90° 50' = 90.8333°)
        val zenith = 90.8333
        val cosH = (cos(Math.toRadians(zenith)) - (sinDec * sin(Math.toRadians(lat)))) /
                (cosDec * cos(Math.toRadians(lat)))

        if (cosH > 1.0 || cosH < -1.0) {
            // Sun never rises or sets (polar day/night)
            return null
        }

        val h = if (isSunrise) {
            360.0 - Math.toDegrees(acos(cosH))
        } else {
            Math.toDegrees(acos(cosH))
        } / 15.0

        // Local mean time of rising/setting
        val tLocal = h + ra - (0.06571 * t) - 6.622

        // Universal Time (UTC)
        var ut = tLocal - lngHour
        while (ut < 0.0) ut += 24.0
        while (ut >= 24.0) ut -= 24.0

        return ut
    }

    private fun normalizeDegrees(degrees: Double): Double {
        var d = degrees % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun toLocalTime(utcHourDecimal: Double, date: LocalDate, zoneId: ZoneId): LocalTime {
        val totalSeconds = (utcHourDecimal * 3600.0).roundToInt()
        val hours = (totalSeconds / 3600) % 24
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val utcZoned = ZonedDateTime.of(
            date,
            LocalTime.of(hours, minutes, seconds),
            ZoneId.of("UTC")
        )
        return utcZoned.withZoneSameInstant(zoneId).toLocalTime()
    }
}
