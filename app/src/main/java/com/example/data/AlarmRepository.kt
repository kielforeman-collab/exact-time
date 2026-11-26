package com.example.data

import com.example.alarm.AlarmScheduler
import com.example.domain.ScheduleCalculator
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class AlarmRepository(
    private val alarmDao: ExactAlarmDao,
    private val eventDao: CountdownEventDao,
    private val scheduler: AlarmScheduler
) {
    val allAlarms: Flow<List<ExactAlarmEntity>> = alarmDao.observeAllAlarms()
    val allEvents: Flow<List<CountdownEventEntity>> = eventDao.observeAllEvents()

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExactAlarms()

    suspend fun saveAlarm(alarm: ExactAlarmEntity): Long {
        val savedId = if (alarm.id == 0L) {
            alarmDao.insertAlarm(alarm)
        } else {
            alarmDao.updateAlarm(alarm)
            alarm.id
        }
        val persisted = alarm.copy(id = savedId)
        if (persisted.isEnabled) {
            scheduler.scheduleAlarm(persisted)
        } else {
            scheduler.cancelAlarm(savedId)
        }
        return savedId
    }

    suspend fun toggleAlarm(alarm: ExactAlarmEntity, enabled: Boolean) {
        val updated = alarm.copy(
            isEnabled = enabled,
            skipNextTriggerMillis = if (!enabled) null else alarm.skipNextTriggerMillis
        )
        alarmDao.updateAlarm(updated)
        if (enabled) {
            scheduler.scheduleAlarm(updated)
        } else {
            scheduler.cancelAlarm(updated.id)
        }
    }

    suspend fun toggleSkipNext(alarm: ExactAlarmEntity, nowMillis: Long = System.currentTimeMillis()) {
        if (!alarm.isRecurring || !alarm.isEnabled) return
        val updated = if (alarm.skipNextTriggerMillis != null) {
            alarm.copy(skipNextTriggerMillis = null)
        } else {
            val nextRaw = ScheduleCalculator.computeNextTriggerMillis(
                alarm = alarm,
                fromMillis = nowMillis,
                ignoreSkip = true
            )
            alarm.copy(skipNextTriggerMillis = nextRaw)
        }
        alarmDao.updateAlarm(updated)
        scheduler.scheduleAlarm(updated, nowMillis)
    }

    suspend fun deleteAlarm(alarmId: Long) {
        scheduler.cancelAlarm(alarmId)
        alarmDao.deleteAlarmById(alarmId)
    }

    suspend fun handleAlarmFired(alarmId: Long, firedAtMillis: Long) {
        val alarm = alarmDao.getAlarmById(alarmId) ?: return
        if (alarm.isRecurring) {
            val updated = alarm.copy(
                lastTriggeredMillis = firedAtMillis,
                skipNextTriggerMillis = null
            )
            alarmDao.updateAlarm(updated)
            scheduler.scheduleAlarm(updated, firedAtMillis + 500L)
        } else {
            val updated = alarm.copy(
                isEnabled = false,
                lastTriggeredMillis = firedAtMillis,
                skipNextTriggerMillis = null
            )
            alarmDao.updateAlarm(updated)
            scheduler.cancelAlarm(alarmId)
        }
    }

    suspend fun rescheduleAllEnabled() {
        val enabledAlarms = alarmDao.getEnabledAlarms()
        val now = System.currentTimeMillis()
        for (alarm in enabledAlarms) {
            val scheduled = scheduler.scheduleAlarm(alarm, now)
            if (scheduled == null && !alarm.isRecurring) {
                alarmDao.updateAlarm(alarm.copy(isEnabled = false))
            }
        }
    }

    suspend fun createQuickPresetAlarm(
        offsetSeconds: Int?,
        alignToNextMinute: Boolean = false,
        customLabel: String
    ): Long {
        val now = System.currentTimeMillis()
        val targetMillis = if (alignToNextMinute) {
            ((now / 60_000L) + 1L) * 60_000L
        } else {
            ((now / 1_000L) + (offsetSeconds ?: 30).toLong()) * 1_000L
        }
        val cal = Calendar.getInstance().apply { timeInMillis = targetMillis }
        val entity = ExactAlarmEntity(
            label = customLabel,
            category = AlarmCategory.FOCUS.name,
            hour = cal.get(Calendar.HOUR_OF_DAY),
            minute = cal.get(Calendar.MINUTE),
            second = cal.get(Calendar.SECOND),
            scheduleType = ScheduleType.ONE_TIME.name,
            oneTimeYear = cal.get(Calendar.YEAR),
            oneTimeMonth = cal.get(Calendar.MONTH) + 1,
            oneTimeDay = cal.get(Calendar.DAY_OF_MONTH),
            isEnabled = true,
            snoozeSeconds = 60,
            preAlarmLeadSeconds = if ((targetMillis - now) >= 25_000L) 5 else 0
        )
        return saveAlarm(entity)
    }

    suspend fun createCountdownEvent(
        title: String,
        targetEpochMillis: Long,
        category: AlarmCategory,
        armAlarmAtZero: Boolean
    ): Long {
        var linkedAlarmId: Long? = null
        if (armAlarmAtZero && targetEpochMillis > System.currentTimeMillis()) {
            val cal = Calendar.getInstance().apply { timeInMillis = targetEpochMillis }
            linkedAlarmId = saveAlarm(
                ExactAlarmEntity(
                    label = title,
                    category = category.name,
                    hour = cal.get(Calendar.HOUR_OF_DAY),
                    minute = cal.get(Calendar.MINUTE),
                    second = cal.get(Calendar.SECOND),
                    scheduleType = ScheduleType.ONE_TIME.name,
                    oneTimeYear = cal.get(Calendar.YEAR),
                    oneTimeMonth = cal.get(Calendar.MONTH) + 1,
                    oneTimeDay = cal.get(Calendar.DAY_OF_MONTH),
                    isEnabled = true,
                    preAlarmLeadSeconds = 10
                )
            )
        }
        return eventDao.insertEvent(
            CountdownEventEntity(
                title = title,
                targetEpochMillis = targetEpochMillis,
                category = category.name,
                armAlarmAtZero = armAlarmAtZero,
                linkedAlarmId = linkedAlarmId
            )
        )
    }

    suspend fun deleteCountdownEvent(event: CountdownEventEntity) {
        event.linkedAlarmId?.let { deleteAlarm(it) }
        eventDao.deleteEventById(event.id)
    }
}
