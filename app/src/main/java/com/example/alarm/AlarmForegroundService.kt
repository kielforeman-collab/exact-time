package com.example.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import com.example.data.VibrationPattern
import com.example.domain.ScheduleCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AlarmForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var fadeJob: Job? = null
    private var autoStopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == AlarmConstants.ACTION_DISMISS || action == AlarmConstants.ACTION_SNOOZE) {
            stopAlarmAndSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AlarmConstants.EXTRA_ALARM_ID, -1L) ?: -1L
        val label = intent?.getStringExtra(AlarmConstants.EXTRA_ALARM_LABEL).orEmpty().ifBlank { "ExactTime Alarm" }
        val snoozeSeconds = intent?.getIntExtra(AlarmConstants.EXTRA_SNOOZE_SECONDS, 300) ?: 300
        val fadeInSeconds = intent?.getIntExtra(AlarmConstants.EXTRA_FADE_IN_SECONDS, 0) ?: 0
        val vibrationName = intent?.getStringExtra(AlarmConstants.EXTRA_VIBRATION_PATTERN)
            ?: VibrationPattern.SHARP_PULSE.name
        val soundEnabled = intent?.getBooleanExtra(AlarmConstants.EXTRA_SOUND_ENABLED, true) ?: true
        val category = intent?.getStringExtra(AlarmConstants.EXTRA_CATEGORY) ?: "GENERAL"
        val triggerMillis = intent?.getLongExtra(AlarmConstants.EXTRA_TRIGGER_MILLIS, System.currentTimeMillis())
            ?: System.currentTimeMillis()

        val (timePart, _) = ScheduleCalculator.formatEpochTime(triggerMillis, use24Hour = true, includeCentiseconds = false)
            .let { it.first to it.third }

        val notification = AlarmNotificationHelper.buildRingingNotification(
            context = this,
            alarmId = alarmId,
            label = label,
            exactTimeFormatted = timePart,
            snoozeSeconds = snoozeSeconds,
            category = category
        )

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            AlarmNotificationHelper.NOTIFICATION_ID_RINGING,
            notification,
            fgsType
        )

        stopAudioAndVibration()
        if (soundEnabled) {
            startAlarmSound(fadeInSeconds)
        }
        startAlarmVibration(VibrationPattern.fromName(vibrationName))

        val ringingIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmConstants.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmConstants.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmConstants.EXTRA_EXACT_TIME_TEXT, timePart)
            putExtra(AlarmConstants.EXTRA_SNOOZE_SECONDS, snoozeSeconds)
            putExtra(AlarmConstants.EXTRA_VIBRATION_PATTERN, vibrationName)
            putExtra(AlarmConstants.EXTRA_SOUND_ENABLED, soundEnabled)
            putExtra(AlarmConstants.EXTRA_CATEGORY, category)
            putExtra(AlarmConstants.EXTRA_TRIGGER_MILLIS, triggerMillis)
        }
        try {
            startActivity(ringingIntent)
        } catch (_: Exception) {
            // Full-screen notification handles lockscreen launch on Android 10+ background restrictions.
        }

        // Safety auto-silence after 5 minutes if untouched
        autoStopJob?.cancel()
        autoStopJob = serviceScope.launch {
            delay(5 * 60 * 1000L)
            stopAlarmAndSelf()
        }

        return START_NOT_STICKY
    }

    private fun startAlarmSound(fadeInSeconds: Int) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: return

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, uri)
                isLooping = true
                prepare()
            }
            mediaPlayer = player

            if (fadeInSeconds > 0) {
                player.setVolume(0.08f, 0.08f)
                player.start()
                fadeJob?.cancel()
                fadeJob = serviceScope.launch {
                    val steps = (fadeInSeconds * 4).coerceAtLeast(4)
                    for (step in 1..steps) {
                        if (!isActive) break
                        delay(250L)
                        val vol = (0.08f + 0.92f * (step.toFloat() / steps)).coerceIn(0.08f, 1f)
                        try {
                            mediaPlayer?.setVolume(vol, vol)
                        } catch (_: Exception) {
                            break
                        }
                    }
                }
            } else {
                player.setVolume(1.0f, 1.0f)
                player.start()
            }
        } catch (_: Exception) {
        }
    }

    private fun startAlarmVibration(pattern: VibrationPattern) {
        if (pattern == VibrationPattern.OFF) return
        val vibrator = getVibrator() ?: return
        val timings = when (pattern) {
            VibrationPattern.SHARP_PULSE -> longArrayOf(0, 180, 220, 180, 600)
            VibrationPattern.DOUBLE_TAP -> longArrayOf(0, 90, 90, 90, 730)
            VibrationPattern.CONTINUOUS -> longArrayOf(0, 800, 250)
            VibrationPattern.OFF -> return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopAudioAndVibration() {
        fadeJob?.cancel()
        fadeJob = null
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }
        try {
            getVibrator()?.cancel()
        } catch (_: Exception) {
        }
    }

    private fun stopAlarmAndSelf() {
        autoStopJob?.cancel()
        stopAudioAndVibration()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopAudioAndVibration()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    companion object {
        fun stop(context: Context) {
            val intent = Intent(context, AlarmForegroundService::class.java).apply {
                action = AlarmConstants.ACTION_DISMISS
            }
            try {
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
