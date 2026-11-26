package com.example.domain

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

data class ReferenceCity(
    val name: String,
    val region: String,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val timeZoneId: String
)

data class EphemerisSnapshot(
    val city: ReferenceCity,
    val sunriseText: String,
    val sunsetText: String,
    val solarElevationDeg: Double,
    val daylightProgress: Float, // 0f..1f during daytime, 0f or 1f at night
    val isDaylight: Boolean,
    val moonPhaseName: String,
    val moonIlluminationPercent: Int,
    val lunarAgeDays: Double,
    val moonPhaseFraction: Float, // 0f = New, 0.5f = Full, 1f = New
    val dayOfYear: Int,
    val daysInYear: Int,
    val isoWeek: Int,
    val utcIso: String,
    val unixEpochSeconds: Long
)

object EphemerisCalculator {

    val REFERENCE_CITIES = listOf(
        ReferenceCity("Local Meridian", "Device Time Zone", 40.7128, -74.0060, TimeZone.getDefault().id),
        ReferenceCity("Zurich", "CET / Horology", 47.3769, 8.5417, "Europe/Zurich"),
        ReferenceCity("New York", "Eastern / NYSE", 40.7128, -74.0060, "America/New_York"),
        ReferenceCity("London", "UTC / Greenwich", 51.5074, -0.1278, "Europe/London"),
        ReferenceCity("Tokyo", "JST / TSE", 35.6762, 139.6503, "Asia/Tokyo"),
        ReferenceCity("San Francisco", "Pacific / PST", 37.7749, -122.4194, "America/Los_Angeles"),
        ReferenceCity("Sydney", "AEST / ASX", -33.8688, 151.2093, "Australia/Sydney")
    )

    private const val SYNODIC_MONTH_DAYS = 29.53058867
    // Known new moon reference epoch: 2000-01-06 18:14:00 UTC (JD 2451550.25972)
    private const val KNOWN_NEW_MOON_UNIX_SECONDS = 947182440.0

    fun compute(epochMillis: Long, cityIndex: Int, use24Hour: Boolean): EphemerisSnapshot {
        val safeIndex = cityIndex.coerceIn(0, REFERENCE_CITIES.lastIndex)
        val rawCity = REFERENCE_CITIES[safeIndex]
        val tz = if (safeIndex == 0) TimeZone.getDefault() else TimeZone.getTimeZone(rawCity.timeZoneId)
        val tzOffsetHours = tz.getOffset(epochMillis) / 3_600_000.0
        val effectiveLon = if (safeIndex == 0) tzOffsetHours * 15.0 else rawCity.longitudeDeg
        val effectiveLat = if (safeIndex == 0) 38.0 else rawCity.latitudeDeg
        val city = rawCity.copy(longitudeDeg = effectiveLon, latitudeDeg = effectiveLat)

        val cal = Calendar.getInstance(tz).apply { timeInMillis = epochMillis }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val daysInYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
        val isoWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val localMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60.0 +
            cal.get(Calendar.MINUTE) +
            cal.get(Calendar.SECOND) / 60.0

        // NOAA simplified solar equations
        val gamma = 2.0 * PI / daysInYear * (dayOfYear - 1 + (localMinutes / 60.0 - 12.0) / 24.0)
        val eqTimeMinutes = 229.18 * (
            0.000075 +
                0.001868 * cos(gamma) -
                0.032077 * sin(gamma) -
                0.014615 * cos(2 * gamma) -
                0.040849 * sin(2 * gamma)
            )
        val declRad = 0.006918 -
            0.399912 * cos(gamma) +
            0.070257 * sin(gamma) -
            0.006758 * cos(2 * gamma) +
            0.000907 * sin(2 * gamma) -
            0.002697 * cos(3 * gamma) +
            0.00148 * sin(3 * gamma)

        val latRad = Math.toRadians(city.latitudeDeg)
        val zenithRad = Math.toRadians(90.833)
        val cosHourAngle = (cos(zenithRad) / (cos(latRad) * cos(declRad)) -
            kotlin.math.tan(latRad) * kotlin.math.tan(declRad)).coerceIn(-1.0, 1.0)
        val hourAngleDeg = Math.toDegrees(acos(cosHourAngle))

        val solarNoonMinutes = 720.0 - 4.0 * city.longitudeDeg - eqTimeMinutes + tzOffsetHours * 60.0
        val sunriseMinutes = solarNoonMinutes - hourAngleDeg * 4.0
        val sunsetMinutes = solarNoonMinutes + hourAngleDeg * 4.0

        val trueSolarTime = localMinutes + eqTimeMinutes + 4.0 * city.longitudeDeg - 60.0 * tzOffsetHours
        val currentHourAngleRad = Math.toRadians(trueSolarTime / 4.0 - 180.0)
        val sinElevation = (sin(latRad) * sin(declRad) +
            cos(latRad) * cos(declRad) * cos(currentHourAngleRad)).coerceIn(-1.0, 1.0)
        val solarElevationDeg = Math.toDegrees(asin(sinElevation))

        val isDaylight = localMinutes in sunriseMinutes..sunsetMinutes
        val daylightSpan = (sunsetMinutes - sunriseMinutes).coerceAtLeast(1.0)
        val daylightProgress = ((localMinutes - sunriseMinutes) / daylightSpan).toFloat().coerceIn(0f, 1f)

        // Synodic lunar phase calculation
        val unixSeconds = epochMillis / 1000.0
        val daysSinceKnownNew = (unixSeconds - KNOWN_NEW_MOON_UNIX_SECONDS) / 86_400.0
        val cycles = daysSinceKnownNew / SYNODIC_MONTH_DAYS
        val phaseFraction = (cycles - floor(cycles)).toFloat().coerceIn(0f, 1f)
        val lunarAgeDays = phaseFraction * SYNODIC_MONTH_DAYS
        val illumination = ((1.0 - cos(phaseFraction * 2.0 * PI)) * 50.0).toInt().coerceIn(0, 100)

        val moonPhaseName = when {
            phaseFraction < 0.03f || phaseFraction > 0.97f -> "New Moon"
            phaseFraction < 0.22f -> "Waxing Crescent"
            phaseFraction < 0.28f -> "First Quarter"
            phaseFraction < 0.47f -> "Waxing Gibbous"
            phaseFraction < 0.53f -> "Full Moon"
            phaseFraction < 0.72f -> "Waning Gibbous"
            phaseFraction < 0.78f -> "Last Quarter"
            else -> "Waning Crescent"
        }

        return EphemerisSnapshot(
            city = city,
            sunriseText = formatMinutesOfDay(sunriseMinutes, use24Hour),
            sunsetText = formatMinutesOfDay(sunsetMinutes, use24Hour),
            solarElevationDeg = solarElevationDeg,
            daylightProgress = daylightProgress,
            isDaylight = isDaylight,
            moonPhaseName = moonPhaseName,
            moonIlluminationPercent = illumination,
            lunarAgeDays = lunarAgeDays,
            moonPhaseFraction = phaseFraction,
            dayOfYear = dayOfYear,
            daysInYear = daysInYear,
            isoWeek = isoWeek,
            utcIso = ScheduleCalculator.formatUtcIso(epochMillis),
            unixEpochSeconds = epochMillis / 1000L
        )
    }

    private fun formatMinutesOfDay(minutes: Double, use24Hour: Boolean): String {
        val wrapped = ((minutes % 1440.0) + 1440.0) % 1440.0
        val totalSeconds = (wrapped * 60.0).toInt()
        val h = (totalSeconds / 3600) % 24
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        val (timeStr, amPm) = ScheduleCalculator.formatExactTime(h, m, s, use24Hour)
        return if (amPm != null) "$timeStr $amPm" else timeStr
    }
}
