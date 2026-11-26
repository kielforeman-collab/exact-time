package com.example

import com.example.data.ExactAlarmEntity
import com.example.data.ScheduleType
import com.example.domain.ScheduleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ScheduleCalculatorTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun utcMillis(year: Int, month: Int, day: Int, hour: Int, min: Int, sec: Int): Long {
        return Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, day, hour, min, sec)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun oneTimeExactSecondSchedulesSameDayOrNextDay() {
        val now = utcMillis(2026, 11, 26, 10, 0, 15)
        val alarmSameDay = ExactAlarmEntity(
            label = "Market",
            hour = 10,
            minute = 0,
            second = 45,
            scheduleType = ScheduleType.ONE_TIME.name
        )
        val expectedSameDay = utcMillis(2026, 11, 26, 10, 0, 45)
        assertEquals(expectedSameDay, ScheduleCalculator.computeNextTriggerMillis(alarmSameDay, now, utc))

        val alarmPastToday = alarmSameDay.copy(second = 10)
        val expectedTomorrow = utcMillis(2026, 11, 27, 10, 0, 10)
        assertEquals(expectedTomorrow, ScheduleCalculator.computeNextTriggerMillis(alarmPastToday, now, utc))
    }

    @Test
    fun weeklyScheduleHonorsBitmaskAndSkipNext() {
        // 2026-11-26 is Thursday (bit 3)
        val now = utcMillis(2026, 11, 26, 8, 0, 0)
        val alarm = ExactAlarmEntity(
            label = "Standup",
            hour = 9,
            minute = 30,
            second = 15,
            scheduleType = ScheduleType.WEEKLY.name,
            repeatDaysMask = ScheduleCalculator.MASK_THU or ScheduleCalculator.MASK_FRI
        )
        val thuTrigger = utcMillis(2026, 11, 26, 9, 30, 15)
        val friTrigger = utcMillis(2026, 11, 27, 9, 30, 15)
        assertEquals(thuTrigger, ScheduleCalculator.computeNextTriggerMillis(alarm, now, utc))

        val skippedAlarm = alarm.copy(skipNextTriggerMillis = thuTrigger)
        assertEquals(friTrigger, ScheduleCalculator.computeNextTriggerMillis(skippedAlarm, now, utc))
    }

    @Test
    fun intervalScheduleAdvancesToNextExactSecondStep() {
        val now = utcMillis(2026, 11, 26, 9, 12, 10)
        val alarm = ExactAlarmEntity(
            label = "Cadence",
            hour = 9,
            minute = 0,
            second = 30,
            scheduleType = ScheduleType.INTERVAL.name,
            intervalSeconds = 300 // every 5m anchored at :30
        )
        val expected = utcMillis(2026, 11, 26, 9, 15, 30)
        assertEquals(expected, ScheduleCalculator.computeNextTriggerMillis(alarm, now, utc))
    }

    @Test
    fun monthlyScheduleClampsDay31InFebruary() {
        val now = utcMillis(2026, 2, 1, 10, 0, 0)
        val alarm = ExactAlarmEntity(
            label = "Month End",
            hour = 18,
            minute = 0,
            second = 5,
            scheduleType = ScheduleType.MONTHLY.name,
            monthlyDay = 31
        )
        val result = ScheduleCalculator.computeNextTriggerMillis(alarm, now, utc)
        assertNotNull(result)
        assertEquals(utcMillis(2026, 2, 28, 18, 0, 5), result)
    }
}
