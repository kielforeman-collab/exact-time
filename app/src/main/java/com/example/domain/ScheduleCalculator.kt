package com.example.domain

import com.example.data.ExactAlarmEntity
import com.example.data.ScheduleType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

object ScheduleCalculator {

    const val MASK_MON = 1 shl 0
    const val MASK_TUE = 1 shl 1
    const val MASK_WED = 1 shl 2
    const val MASK_THU = 1 shl 3
    const val MASK_FRI = 1 shl 4
    const val MASK_SAT = 1 shl 5
    const val MASK_SUN = 1 shl 6
    const val MASK_WEEKDAYS = MASK_MON or MASK_TUE or MASK_WED or MASK_THU or MASK_FRI
    const val MASK_WEEKENDS = MASK_SAT or MASK_SUN
    const val MASK_ALL_DAYS = MASK_WEEKDAYS or MASK_WEEKENDS

    val DAY_SHORT_LABELS = listOf("M", "T", "W", "T", "F", "S", "S")
    val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun calendarDayToBitIndex(calendarDayOfWeek: Int): Int = when (calendarDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    /**
     * Computes the exact next trigger epoch milliseconds strictly after [fromMillis],
     * honoring any active [ExactAlarmEntity.skipNextTriggerMillis].
     */
    fun computeNextTriggerMillis(
        alarm: ExactAlarmEntity,
        fromMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        ignoreSkip: Boolean = false
    ): Long? {
        val rawFirst = computeCandidateAfter(alarm, fromMillis, timeZone) ?: return null
        val skipTarget = alarm.skipNextTriggerMillis
        if (!ignoreSkip && skipTarget != null && alarm.isRecurring && rawFirst == skipTarget) {
            return computeCandidateAfter(alarm, rawFirst, timeZone)
        }
        return rawFirst
    }

    private fun computeCandidateAfter(
        alarm: ExactAlarmEntity,
        afterMillis: Long,
        timeZone: TimeZone
    ): Long? {
        return when (alarm.scheduleTypeEnum) {
            ScheduleType.ONE_TIME -> computeOneTime(alarm, afterMillis, timeZone)
            ScheduleType.WEEKLY -> computeWeekly(alarm, afterMillis, timeZone)
            ScheduleType.INTERVAL -> computeInterval(alarm, afterMillis, timeZone)
            ScheduleType.MONTHLY -> computeMonthly(alarm, afterMillis, timeZone)
        }
    }

    private fun computeOneTime(
        alarm: ExactAlarmEntity,
        afterMillis: Long,
        timeZone: TimeZone
    ): Long? {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = afterMillis
            set(Calendar.MILLISECOND, 0)
        }
        if (alarm.oneTimeYear != null && alarm.oneTimeMonth != null && alarm.oneTimeDay != null) {
            cal.set(Calendar.YEAR, alarm.oneTimeYear)
            cal.set(Calendar.MONTH, (alarm.oneTimeMonth - 1).coerceIn(0, 11))
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, alarm.oneTimeDay.coerceIn(1, maxDay))
            cal.set(Calendar.HOUR_OF_DAY, alarm.hour.coerceIn(0, 23))
            cal.set(Calendar.MINUTE, alarm.minute.coerceIn(0, 59))
            cal.set(Calendar.SECOND, alarm.second.coerceIn(0, 59))
            cal.set(Calendar.MILLISECOND, 0)
            return if (cal.timeInMillis > afterMillis) cal.timeInMillis else null
        }

        cal.set(Calendar.HOUR_OF_DAY, alarm.hour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, alarm.minute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, alarm.second.coerceIn(0, 59))
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= afterMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun computeWeekly(
        alarm: ExactAlarmEntity,
        afterMillis: Long,
        timeZone: TimeZone
    ): Long? {
        val mask = alarm.repeatDaysMask
        if (mask == 0) return computeOneTime(alarm, afterMillis, timeZone)

        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = afterMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour.coerceIn(0, 23))
            set(Calendar.MINUTE, alarm.minute.coerceIn(0, 59))
            set(Calendar.SECOND, alarm.second.coerceIn(0, 59))
            set(Calendar.MILLISECOND, 0)
        }

        for (dayOffset in 0..7) {
            val candidate = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val bit = 1 shl calendarDayToBitIndex(candidate.get(Calendar.DAY_OF_WEEK))
            if ((mask and bit) != 0 && candidate.timeInMillis > afterMillis) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    private fun computeInterval(
        alarm: ExactAlarmEntity,
        afterMillis: Long,
        timeZone: TimeZone
    ): Long {
        val stepMillis = max(10, alarm.intervalSeconds) * 1000L
        val anchor = Calendar.getInstance(timeZone).apply {
            timeInMillis = afterMillis
            set(Calendar.HOUR_OF_DAY, alarm.hour.coerceIn(0, 23))
            set(Calendar.MINUTE, alarm.minute.coerceIn(0, 59))
            set(Calendar.SECOND, alarm.second.coerceIn(0, 59))
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        return if (anchor > afterMillis) {
            val stepsBack = (anchor - afterMillis - 1) / stepMillis
            anchor - stepsBack * stepMillis
        } else {
            val stepsForward = (afterMillis - anchor) / stepMillis + 1
            anchor + stepsForward * stepMillis
        }
    }

    private fun computeMonthly(
        alarm: ExactAlarmEntity,
        afterMillis: Long,
        timeZone: TimeZone
    ): Long {
        val targetDay = alarm.monthlyDay.coerceIn(1, 31)
        val base = Calendar.getInstance(timeZone).apply {
            timeInMillis = afterMillis
        }
        val startYear = base.get(Calendar.YEAR)
        val startMonth = base.get(Calendar.MONTH)

        for (monthOffset in 0..14) {
            val candidate = Calendar.getInstance(timeZone).apply {
                clear()
                set(Calendar.YEAR, startYear)
                set(Calendar.MONTH, startMonth)
                set(Calendar.DAY_OF_MONTH, 1)
                add(Calendar.MONTH, monthOffset)
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(maxDay))
                set(Calendar.HOUR_OF_DAY, alarm.hour.coerceIn(0, 23))
                set(Calendar.MINUTE, alarm.minute.coerceIn(0, 59))
                set(Calendar.SECOND, alarm.second.coerceIn(0, 59))
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis > afterMillis) {
                return candidate.timeInMillis
            }
        }
        return afterMillis + 86_400_000L
    }

    fun formatExactTime(
        hour: Int,
        minute: Int,
        second: Int,
        use24Hour: Boolean
    ): Pair<String, String?> {
        return if (use24Hour) {
            String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second) to null
        } else {
            val amPm = if (hour < 12) "AM" else "PM"
            val h12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format(Locale.US, "%02d:%02d:%02d", h12, minute, second) to amPm
        }
    }

    fun formatEpochTime(
        epochMillis: Long,
        use24Hour: Boolean,
        includeCentiseconds: Boolean = false
    ): Triple<String, String?, String?> {
        val cal = Calendar.getInstance().apply { timeInMillis = epochMillis }
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        val s = cal.get(Calendar.SECOND)
        val cs = (cal.get(Calendar.MILLISECOND) / 10).coerceIn(0, 99)
        val (main, amPm) = formatExactTime(h, m, s, use24Hour)
        val frac = if (includeCentiseconds) String.format(Locale.US, ".%02d", cs) else null
        return Triple(main, frac, amPm)
    }

    fun formatCountdown(diffMillis: Long): String {
        val totalSeconds = max(0L, (diffMillis + 999L) / 1000L)
        val days = totalSeconds / 86_400L
        val hours = (totalSeconds % 86_400L) / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (days > 0) {
            String.format(Locale.US, "%dd %02d:%02d:%02d", days, hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    fun formatDurationCompact(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return buildString {
            if (h > 0) append("${h}h ")
            if (m > 0) append("${m}m ")
            if (s > 0 || (h == 0 && m == 0)) append("${s}s")
        }.trim()
    }

    fun formatScheduleSummary(alarm: ExactAlarmEntity): String {
        return when (alarm.scheduleTypeEnum) {
            ScheduleType.ONE_TIME -> {
                if (alarm.oneTimeYear != null && alarm.oneTimeMonth != null && alarm.oneTimeDay != null) {
                    val cal = Calendar.getInstance().apply {
                        set(alarm.oneTimeYear, alarm.oneTimeMonth - 1, alarm.oneTimeDay)
                    }
                    SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(cal.time)
                } else {
                    "One-time"
                }
            }
            ScheduleType.WEEKLY -> {
                when (alarm.repeatDaysMask) {
                    MASK_ALL_DAYS -> "Every day"
                    MASK_WEEKDAYS -> "Weekdays"
                    MASK_WEEKENDS -> "Weekends"
                    0 -> "One-time"
                    else -> DAY_NAMES.indices
                        .filter { (alarm.repeatDaysMask and (1 shl it)) != 0 }
                        .joinToString(" · ") { DAY_NAMES[it] }
                }
            }
            ScheduleType.INTERVAL -> "Every ${formatDurationCompact(alarm.intervalSeconds)}"
            ScheduleType.MONTHLY -> "Monthly on day ${alarm.monthlyDay}"
        }
    }

    fun formatDateHeader(epochMillis: Long): String {
        val formatter = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }

    fun formatUtcIso(epochMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(epochMillis))
    }
}
