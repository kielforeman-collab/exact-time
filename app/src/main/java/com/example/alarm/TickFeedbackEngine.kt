package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Generates crisp horological escapement audio ticks and tactile haptic pulses
 * synced to exact second boundaries.
 */
class TickFeedbackEngine(private val context: Context) {

    private val sampleRate = 22050
    private val clickPcm: ShortArray = synthesizeMechanicalTick(accent = false)
    private val accentPcm: ShortArray = synthesizeMechanicalTick(accent = true)

    fun triggerTick(
        hapticEnabled: Boolean,
        audioEnabled: Boolean,
        isMajorMark: Boolean
    ) {
        if (hapticEnabled) {
            vibrateTick(isMajorMark)
        }
        if (audioEnabled) {
            playMechanicalClick(isMajorMark)
        }
    }

    fun triggerPreAlarmPulse() {
        val vibrator = getVibrator() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 45, 70, 65)
                val amplitudes = intArrayOf(0, 140, 0, 220)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 45, 70, 65), -1)
            }
        } catch (_: Exception) {
        }
    }

    private fun vibrateTick(isMajorMark: Boolean) {
        val vibrator = getVibrator() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effectId = if (isMajorMark) {
                    VibrationEffect.EFFECT_CLICK
                } else {
                    VibrationEffect.EFFECT_TICK
                }
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val duration = if (isMajorMark) 20L else 10L
                val amplitude = if (isMajorMark) 180 else 95
                vibrator.vibrate(VibrationEffect.createOneShot(duration, amplitude))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (isMajorMark) 20L else 10L)
            }
        } catch (_: Exception) {
        }
    }

    private fun playMechanicalClick(isMajorMark: Boolean) {
        val samples = if (isMajorMark) accentPcm else clickPcm
        try {
            val byteSize = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(byteSize)
                .build()
            track.write(samples, 0, samples.size)
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    try {
                        t?.release()
                    } catch (_: Exception) {
                    }
                }
                override fun onPeriodicNotification(t: AudioTrack?) = Unit
            })
            track.play()
        } catch (_: Exception) {
        }
    }

    private fun synthesizeMechanicalTick(accent: Boolean): ShortArray {
        val durationMs = if (accent) 26 else 18
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        val baseFreq = if (accent) 1850.0 else 2450.0
        val overtoneFreq = if (accent) 3600.0 else 4700.0
        val decay = if (accent) 160.0 else 230.0
        val gain = if (accent) 0.55 else 0.35

        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-t * decay)
            val wave = 0.72 * sin(2.0 * PI * baseFreq * t) + 0.28 * sin(2.0 * PI * overtoneFreq * t)
            val sample = (wave * envelope * gain * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            out[i] = sample.toShort()
        }
        return out
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
