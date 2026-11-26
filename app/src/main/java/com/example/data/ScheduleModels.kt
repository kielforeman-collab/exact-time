package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScheduleType(val displayName: String) {
    ONE_TIME("One-Time"),
    WEEKLY("Weekly"),
    INTERVAL("Interval"),
    MONTHLY("Monthly");

    companion object {
        fun fromName(name: String): ScheduleType =
            entries.firstOrNull { it.name == name } ?: ONE_TIME
    }
}

enum class AlarmCategory(val displayName: String) {
    GENERAL("Precision"),
    ROUTINE("Routine"),
    WORK("Work"),
    MARKET("Market"),
    TRANSIT("Transit"),
    FOCUS("Focus"),
    MEDICATION("Health");

    companion object {
        fun fromName(name: String): AlarmCategory =
            entries.firstOrNull { it.name == name } ?: GENERAL
    }
}

enum class VibrationPattern(val displayName: String) {
    SHARP_PULSE("Sharp Pulse"),
    DOUBLE_TAP("Double Tap"),
    CONTINUOUS("Continuous"),
    OFF("Off");

    companion object {
        fun fromName(name: String): VibrationPattern =
            entries.firstOrNull { it.name == name } ?: SHARP_PULSE
    }
}

enum class TickCadence(val seconds: Int, val shortLabel: String) {
    OFF(0, "Off"),
    EVERY_1S(1, "1s"),
    EVERY_5S(5, "5s"),
    EVERY_10S(10, "10s"),
    EVERY_30S(30, "30s");

    companion object {
        fun fromSeconds(seconds: Int): TickCadence =
            entries.firstOrNull { it.seconds == seconds } ?: OFF
    }
}

enum class AppThemeMode(val displayName: String) {
    DARK("Obsidian"),
    LIGHT("Alabaster"),
    SYSTEM("System");

    companion object {
        fun fromName(name: String): AppThemeMode =
            entries.firstOrNull { it.name == name } ?: DARK
    }
}

@Entity(tableName = "exact_alarms")
data class ExactAlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    val category: String = AlarmCategory.GENERAL.name,
    val hour: Int, // 0..23
    val minute: Int, // 0..59
    val second: Int, // 0..59
    val scheduleType: String = ScheduleType.ONE_TIME.name,
    val repeatDaysMask: Int = 0, // Bit 0 = Mon, 1 = Tue, 2 = Wed, 3 = Thu, 4 = Fri, 5 = Sat, 6 = Sun
    val oneTimeYear: Int? = null,
    val oneTimeMonth: Int? = null, // 1..12
    val oneTimeDay: Int? = null, // 1..31
    val intervalSeconds: Int = 300, // For INTERVAL mode (>= 10s)
    val monthlyDay: Int = 1, // 1..31 for MONTHLY mode
    val isEnabled: Boolean = true,
    val skipNextTriggerMillis: Long? = null,
    val snoozeSeconds: Int = 300,
    val preAlarmLeadSeconds: Int = 0, // 0, 5, 10, 30
    val fadeInSeconds: Int = 0, // 0, 10, 30
    val vibrationPattern: String = VibrationPattern.SHARP_PULSE.name,
    val soundEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastTriggeredMillis: Long? = null
) {
    val scheduleTypeEnum: ScheduleType get() = ScheduleType.fromName(scheduleType)
    val categoryEnum: AlarmCategory get() = AlarmCategory.fromName(category)
    val vibrationPatternEnum: VibrationPattern get() = VibrationPattern.fromName(vibrationPattern)

    val isRecurring: Boolean
        get() = when (scheduleTypeEnum) {
            ScheduleType.ONE_TIME -> false
            ScheduleType.WEEKLY -> repeatDaysMask != 0
            ScheduleType.INTERVAL -> intervalSeconds >= 10
            ScheduleType.MONTHLY -> true
        }
}

@Entity(tableName = "countdown_events")
data class CountdownEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val targetEpochMillis: Long,
    val category: String = AlarmCategory.GENERAL.name,
    val armAlarmAtZero: Boolean = true,
    val linkedAlarmId: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)
