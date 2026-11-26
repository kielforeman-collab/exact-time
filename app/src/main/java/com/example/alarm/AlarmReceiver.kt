package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.ExactTimeApplication
import com.example.data.VibrationPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as ExactTimeApplication
        val action = intent.action ?: return

        when (action) {
            AlarmConstants.ACTION_ALARM_TRIGGER,
            AlarmConstants.ACTION_TEST_FIRE -> {
                val alarmId = intent.getLongExtra(AlarmConstants.EXTRA_ALARM_ID, -1L)
                val firedMillis = System.currentTimeMillis()
                val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
                    putExtras(intent)
                    if (!hasExtra(AlarmConstants.EXTRA_TRIGGER_MILLIS)) {
                        putExtra(AlarmConstants.EXTRA_TRIGGER_MILLIS, firedMillis)
                    }
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                if (action == AlarmConstants.ACTION_ALARM_TRIGGER && alarmId > 0L) {
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            app.alarmRepository.handleAlarmFired(alarmId, firedMillis)
                        } finally {
                            pending.finish()
                        }
                    }
                }
            }

            AlarmConstants.ACTION_PRE_ALARM_TRIGGER -> {
                val alarmId = intent.getLongExtra(AlarmConstants.EXTRA_ALARM_ID, -1L)
                val label = intent.getStringExtra(AlarmConstants.EXTRA_ALARM_LABEL).orEmpty()
                val lead = intent.getIntExtra(AlarmConstants.EXTRA_LEAD_SECONDS, 10)
                app.tickFeedbackEngine.triggerPreAlarmPulse()
                AlarmNotificationHelper.showPreAlarmWarning(context, alarmId, label, lead)
            }

            AlarmConstants.ACTION_DISMISS -> {
                AlarmForegroundService.stop(context)
            }

            AlarmConstants.ACTION_SNOOZE -> {
                AlarmForegroundService.stop(context)
                val alarmId = intent.getLongExtra(AlarmConstants.EXTRA_ALARM_ID, -1L)
                val label = intent.getStringExtra(AlarmConstants.EXTRA_ALARM_LABEL)
                    .orEmpty().ifBlank { "ExactTime Alarm" }
                val snoozeSeconds = intent.getIntExtra(AlarmConstants.EXTRA_SNOOZE_SECONDS, 300)
                val category = intent.getStringExtra(AlarmConstants.EXTRA_CATEGORY) ?: "GENERAL"
                val vibration = intent.getStringExtra(AlarmConstants.EXTRA_VIBRATION_PATTERN)
                    ?: VibrationPattern.SHARP_PULSE.name
                val soundEnabled = intent.getBooleanExtra(AlarmConstants.EXTRA_SOUND_ENABLED, true)
                app.alarmScheduler.scheduleSnooze(
                    alarmId = if (alarmId > 0L) alarmId else 99999L,
                    label = label.removeSuffix(" (Snoozed)"),
                    snoozeSeconds = snoozeSeconds,
                    category = category,
                    vibrationPattern = vibration,
                    soundEnabled = soundEnabled
                )
            }
        }
    }
}
