package com.example.ui

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ExactTimeApplication
import com.example.alarm.AlarmConstants
import com.example.alarm.AlarmReceiver
import com.example.alarm.TickFeedbackEngine
import com.example.data.AlarmCategory
import com.example.data.AlarmRepository
import com.example.data.AppThemeMode
import com.example.data.CountdownEventEntity
import com.example.data.ExactAlarmEntity
import com.example.data.ExactTimeSettings
import com.example.data.SettingsRepository
import com.example.data.TickCadence
import com.example.domain.ScheduleCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ExactTimeViewModel(
    private val alarmRepository: AlarmRepository,
    private val settingsRepository: SettingsRepository,
    private val tickFeedbackEngine: TickFeedbackEngine
) : ViewModel() {

    val alarms: StateFlow<List<ExactAlarmEntity>> = alarmRepository.allAlarms.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val events: StateFlow<List<CountdownEventEntity>> = alarmRepository.allEvents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val settings: StateFlow<ExactTimeSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExactTimeSettings()
    )

    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var isClockTickActive: Boolean = true

    init {
        viewModelScope.launch {
            var lastTickedSecond = System.currentTimeMillis() / 1_000L
            while (isActive) {
                val now = System.currentTimeMillis()
                _nowMillis.value = now
                val currentSecond = now / 1_000L
                if (currentSecond != lastTickedSecond) {
                    lastTickedSecond = currentSecond
                    if (isClockTickActive) {
                        val currentSettings = settings.value
                        val cadence = currentSettings.tickCadence
                        if (cadence != TickCadence.OFF &&
                            (currentSettings.hapticTickEnabled || currentSettings.audioTickEnabled)
                        ) {
                            val secOfMinute = (currentSecond % 60L).toInt()
                            if (secOfMinute % cadence.seconds == 0) {
                                tickFeedbackEngine.triggerTick(
                                    hapticEnabled = currentSettings.hapticTickEnabled,
                                    audioEnabled = currentSettings.audioTickEnabled,
                                    isMajorMark = secOfMinute == 0
                                )
                            }
                        }
                    }
                }
                delay(50L)
            }
        }
    }

    fun setForegroundActive(active: Boolean) {
        isClockTickActive = active
    }

    fun canScheduleExactAlarms(): Boolean = alarmRepository.canScheduleExactAlarms()

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun saveAlarm(alarm: ExactAlarmEntity) {
        viewModelScope.launch {
            alarmRepository.saveAlarm(alarm)
            val nextMillis = if (alarm.isEnabled) {
                ScheduleCalculator.computeNextTriggerMillis(alarm)
            } else {
                null
            }
            val msg = if (nextMillis != null) {
                "Armed · fires in ${ScheduleCalculator.formatCountdown(nextMillis - System.currentTimeMillis())}"
            } else {
                "Alarm saved (disarmed)"
            }
            _snackbarMessage.value = msg
        }
    }

    fun toggleAlarm(alarm: ExactAlarmEntity, enabled: Boolean) {
        viewModelScope.launch {
            alarmRepository.toggleAlarm(alarm, enabled)
            if (enabled) {
                val next = ScheduleCalculator.computeNextTriggerMillis(alarm.copy(isEnabled = true))
                if (next != null) {
                    _snackbarMessage.value =
                        "Armed · in ${ScheduleCalculator.formatCountdown(next - System.currentTimeMillis())}"
                }
            }
        }
    }

    fun toggleSkipNext(alarm: ExactAlarmEntity) {
        viewModelScope.launch {
            val wasSkipped = alarm.skipNextTriggerMillis != null
            alarmRepository.toggleSkipNext(alarm)
            _snackbarMessage.value = if (wasSkipped) {
                "Restored next occurrence"
            } else {
                "Skipped next occurrence"
            }
        }
    }

    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            alarmRepository.deleteAlarm(alarmId)
            _snackbarMessage.value = "Alarm deleted"
        }
    }

    fun testFireAlarm(context: Context, alarm: ExactAlarmEntity) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_TEST_FIRE
            putExtra(AlarmConstants.EXTRA_ALARM_ID, alarm.id)
            putExtra(AlarmConstants.EXTRA_ALARM_LABEL, alarm.label.ifBlank { "Test Precision Alarm" })
            putExtra(AlarmConstants.EXTRA_SNOOZE_SECONDS, alarm.snoozeSeconds)
            putExtra(AlarmConstants.EXTRA_FADE_IN_SECONDS, alarm.fadeInSeconds)
            putExtra(AlarmConstants.EXTRA_VIBRATION_PATTERN, alarm.vibrationPattern)
            putExtra(AlarmConstants.EXTRA_SOUND_ENABLED, alarm.soundEnabled)
            putExtra(AlarmConstants.EXTRA_CATEGORY, alarm.category)
            putExtra(AlarmConstants.EXTRA_TRIGGER_MILLIS, System.currentTimeMillis())
        }
        context.sendBroadcast(intent)
    }

    fun createQuickPreset(offsetSeconds: Int?, alignToNextMinute: Boolean = false, label: String) {
        viewModelScope.launch {
            alarmRepository.createQuickPresetAlarm(
                offsetSeconds = offsetSeconds,
                alignToNextMinute = alignToNextMinute,
                customLabel = label
            )
            _snackbarMessage.value = "Armed exact alarm · $label"
        }
    }

    fun createCountdownEvent(
        title: String,
        targetEpochMillis: Long,
        category: AlarmCategory,
        armAlarmAtZero: Boolean
    ) {
        viewModelScope.launch {
            alarmRepository.createCountdownEvent(
                title = title,
                targetEpochMillis = targetEpochMillis,
                category = category,
                armAlarmAtZero = armAlarmAtZero
            )
            _snackbarMessage.value = if (armAlarmAtZero) {
                "Countdown started with T-0 exact alarm"
            } else {
                "Countdown started"
            }
        }
    }

    fun deleteCountdownEvent(event: CountdownEventEntity) {
        viewModelScope.launch {
            alarmRepository.deleteCountdownEvent(event)
            _snackbarMessage.value = "Countdown removed"
        }
    }

    fun setUse24HourFormat(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setUse24HourFormat(enabled) }
    }

    fun setShowCentiseconds(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setShowCentiseconds(enabled) }
    }

    fun setClockStyleHybrid(hybrid: Boolean) {
        viewModelScope.launch { settingsRepository.setClockStyleHybrid(hybrid) }
    }

    fun setTickCadence(cadence: TickCadence) {
        viewModelScope.launch { settingsRepository.setTickCadence(cadence) }
    }

    fun setHapticTickEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setHapticTickEnabled(enabled) }
    }

    fun setAudioTickEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAudioTickEnabled(enabled) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setReferenceCityIndex(index: Int) {
        viewModelScope.launch { settingsRepository.setReferenceCityIndex(index) }
    }

    companion object {
        fun provideFactory(app: ExactTimeApplication): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ExactTimeViewModel(
                        alarmRepository = app.alarmRepository,
                        settingsRepository = app.settingsRepository,
                        tickFeedbackEngine = app.tickFeedbackEngine
                    ) as T
                }
            }
    }
}
