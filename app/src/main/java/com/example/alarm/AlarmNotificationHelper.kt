package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

object AlarmNotificationHelper {
    const val CHANNEL_ALARM = "exact_alarms_v1"
    const val CHANNEL_PRE_ALARM = "exact_pre_alarms_v1"
    const val NOTIFICATION_ID_RINGING = 41001
    const val NOTIFICATION_ID_PRE_ALARM_BASE = 42000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                context.getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_alarm_desc)
                setSound(null, null) // Audio is managed directly by AlarmForegroundService
                enableVibration(false) // Vibration is managed directly by AlarmForegroundService
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val preAlarmChannel = NotificationChannel(
                CHANNEL_PRE_ALARM,
                context.getString(R.string.channel_pre_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_pre_alarm_desc)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(alarmChannel)
            manager.createNotificationChannel(preAlarmChannel)
        }
    }

    fun buildRingingNotification(
        context: Context,
        alarmId: Long,
        label: String,
        exactTimeFormatted: String,
        snoozeSeconds: Int,
        category: String
    ): Notification {
        ensureChannels(context)

        val fullScreenIntent = Intent(context, AlarmRingingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(AlarmConstants.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmConstants.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmConstants.EXTRA_EXACT_TIME_TEXT, exactTimeFormatted)
            putExtra(AlarmConstants.EXTRA_SNOOZE_SECONDS, snoozeSeconds)
            putExtra(AlarmConstants.EXTRA_CATEGORY, category)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_DISMISS
            putExtra(AlarmConstants.EXTRA_ALARM_ID, alarmId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId.toInt() * 10) + 7,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmConstants.ACTION_SNOOZE
            putExtra(AlarmConstants.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmConstants.EXTRA_ALARM_LABEL, label)
            putExtra(AlarmConstants.EXTRA_SNOOZE_SECONDS, snoozeSeconds)
            putExtra(AlarmConstants.EXTRA_CATEGORY, category)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId.toInt() * 10) + 8,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(label.ifBlank { "ExactTime Alarm" })
            .setContentText("Exact trigger · $exactTimeFormatted")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(0, context.getString(R.string.ringing_dismiss), dismissPendingIntent)
            .addAction(0, context.getString(R.string.ringing_snooze), snoozePendingIntent)
            .build()
    }

    fun showPreAlarmWarning(
        context: Context,
        alarmId: Long,
        label: String,
        leadSeconds: Int
    ) {
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_PRE_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Upcoming Exact Alarm (${leadSeconds}s)")
            .setContentText(label.ifBlank { "ExactTime Alarm" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter((leadSeconds + 2) * 1000L)
            .build()
        manager.notify(NOTIFICATION_ID_PRE_ALARM_BASE + (alarmId % 500).toInt(), notification)
    }
}
