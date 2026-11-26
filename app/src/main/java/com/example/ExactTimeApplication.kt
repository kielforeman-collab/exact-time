package com.example

import android.app.Application
import com.example.alarm.AlarmNotificationHelper
import com.example.alarm.AlarmScheduler
import com.example.alarm.TickFeedbackEngine
import com.example.data.AlarmRepository
import com.example.data.ExactTimeDatabase
import com.example.data.SettingsRepository

class ExactTimeApplication : Application() {

    val database: ExactTimeDatabase by lazy {
        ExactTimeDatabase.getInstance(this)
    }

    val alarmScheduler: AlarmScheduler by lazy {
        AlarmScheduler(this)
    }

    val alarmRepository: AlarmRepository by lazy {
        AlarmRepository(
            alarmDao = database.exactAlarmDao(),
            eventDao = database.countdownEventDao(),
            scheduler = alarmScheduler
        )
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    val tickFeedbackEngine: TickFeedbackEngine by lazy {
        TickFeedbackEngine(this)
    }

    override fun onCreate() {
        super.onCreate()
        AlarmNotificationHelper.ensureChannels(this)
    }
}
