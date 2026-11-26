package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.data.ExactAlarmEntity
import com.example.domain.ScheduleCalculator

object AlarmConstants {
    const val ACTION_ALARM_TRIGGER = "com.example.exacttime.ACTION_ALARM_TRIGGER"
    const val ACTION_PRE_ALARM_TRIGGER = "com.example.exacttime.ACTION_PRE_ALARM_TRIGGER"
    const val ACTION_DISMISS = "com.example.exacttime.ACTION_DISMISS"
    const val ACTION_SNOOZE = "com.example.exacttime.ACTION_SNOOZE"
    const val ACTION_TEST_FIRE = "com.example.exacttime.ACTION_TEST_FIRE"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_LABEL = "extra_alarm_label"
    const val EXTRA_EXACT_TIME_TEXT = "extra_exact_time_text"
    const val EXTRA_SNOOZE_SECONDS = "extra_snooze_seconds"
    const val EXTRA_FADE_IN_SECONDS = "extra_fade_in_seconds"
    const val EXTRA_VIBRATION_PATTERN = "extra_vibration_pattern"
    const val EXTRA_SOUND_ENABLED = "extra_sound_enabled"
    const val EXTRA_CATEGORY = "extra_category"
    const val EXTRA_LEAD_SECONDS = "extra_lead_seconds"
    const val EXTRA_TRIGGER_MILLIS = "extra_trigger_millis"
}

class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleAlarm(alarm: ExactAlarmEntity, fromMillis: Long = System.currentTimeMillis()): Long? {
        cancelAlarm(alarm.id)
        if (!alarm.isEnabled) return null

        val nextTriggerMillis = ScheduleCalculator.computeNextTriggerMillis(alarm, fromMillis)
            ?: return null

        val mainPendingIntent = buildAlarmPendingIntent(alarm, nextTriggerMillis)
        scheduleExactAt(nextTriggerMillis, mainPendingIntent, showClockIcon = true)

        if (alarm.preAlarmLeadSeconds > 0) {
            val preTriggerMillis = nextTriggerMillis - alarm.preAlarmLeadSeconds * 1000L
            if (preTriggerMillis > fromMillis + 1000L) {
                val preIntent = buildPreAlarmPendingIntent(alarm)
                scheduleExactAt(preTriggerMillis, preIntent, showClockIcon = false)
            }
        }
        return nextTriggerMillis
    }

    fun scheduleSnooze(
        alarmId: Long,
        label: String,
        snoozeSeconds: Int,
        category: String,
        vibrationPattern: String,
        soundEnabled: Boolean
    ): Long {
        val triggerMillis = ((System.currentTimeMillis() / 1000L) + snoozeSeconds.coerceAtLeast(10)) * 1000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_ALARM_TRIGGER
            putExtra(AlarmConstants.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmConstants.EXTRA_ALARM_LABEL, "$label (Snoozed)")
            putExtra(AlarmConstants.EXTRA_SNOOZE_SECONDS, snoozeSeconds)
            putExtra(AlarmConstants.EXTRA_FADE_IN_SECONDS, 0)
            putExtra(AlarmConstants.EXTRA_VIBRATION_PATTERN, vibrationPattern)
            putExtra(AlarmConstants.EXTRA_SOUND_ENABLED, soundEnabled)
            putExtra(AlarmConstants.EXTRA_CATEGORY, category)
            putExtra(AlarmConstants.EXTRA_TRIGGER_MILLIS, triggerMillis)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(alarmId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactAt(triggerMillis, pi, showClockIcon = true)
        return triggerMillis
    }

    fun cancelAlarm(alarmId: Long) {
        val mainIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_ALARM_TRIGGER
        }
        PendingIntent.getBroadcast(
            context,
            mainRequestCode(alarmId),
            mainIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        val preIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_PRE_ALARM_TRIGGER
        }
        PendingIntent.getBroadcast(
            context,
            preAlarmRequestCode(alarmId),
            preIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_ALARM_TRIGGER
        }
        PendingIntent.getBroadcast(
            context,
            snoozeRequestCode(alarmId),
            snoozeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    private fun scheduleExactAt(triggerMillis: Long, operation: PendingIntent, showClockIcon: Boolean) {
        try {
            if (canScheduleExactAlarms()) {
                if (showClockIcon) {
                    val showIntent = PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                        operation
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        operation
                    )
                }
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    operation
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                operation
            )
        }
    }

    private fun buildAlarmPendingIntent(alarm: ExactAlarmEntity, triggerMillis: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_ALARM_TRIGGER
            putExtra(AlarmConstants.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmConstants.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmConstants.EXTRA_SNOOZE_SECONDS, alarm.snoozeSeconds)
            putExtra(AlarmConstants.EXTRA_FADE_IN_SECONDS, alarm.fadeInSeconds)
            putExtra(AlarmConstants.EXTRA_VIBRATION_PATTERN, alarm.vibrationPattern)
            putExtra(AlarmConstants.EXTRA_SOUND_ENABLED, alarm.soundEnabled)
            putExtra(AlarmConstants.EXTRA_CATEGORY, alarm.category)
            putExtra(AlarmConstants.EXTRA_TRIGGER_MILLIS, triggerMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            mainRequestCode(alarm.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildPreAlarmPendingIntent(alarm: ExactAlarmEntity): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_PRE_ALARM_TRIGGER
            putExtra(AlarmConstants.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmConstants.EXTRA_ALARM_LABEL, alarm.label)
            putExtra(AlarmConstants.EXTRA_LEAD_SECONDS, alarm.preAlarmLeadSeconds)
        }
        return PendingIntent.getBroadcast(
            context,
            preAlarmRequestCode(alarm.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun mainRequestCode(alarmId: Long): Int = ((alarmId % 100_000L) * 10L + 1L).toInt()
    private fun preAlarmRequestCode(alarmId: Long): Int = ((alarmId % 100_000L) * 10L + 2L).toInt()
    private fun snoozeRequestCode(alarmId: Long): Int = ((alarmId % 100_000L) * 10L + 3L).toInt()
}
