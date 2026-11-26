package com.example.alarm

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ExactTimeApplication
import com.example.R
import com.example.data.AlarmCategory
import com.example.data.AppThemeMode
import com.example.data.VibrationPattern
import com.example.domain.ScheduleCalculator
import com.example.ui.theme.ExactTimeTheme
import kotlinx.coroutines.delay

class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenFlags()

        val alarmId = intent.getLongExtra(AlarmConstants.EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(AlarmConstants.EXTRA_ALARM_LABEL)
            .orEmpty().ifBlank { "ExactTime Alarm" }
        val snoozeSeconds = intent.getIntExtra(AlarmConstants.EXTRA_SNOOZE_SECONDS, 300)
        val categoryName = intent.getStringExtra(AlarmConstants.EXTRA_CATEGORY) ?: AlarmCategory.GENERAL.name
        val vibrationName = intent.getStringExtra(AlarmConstants.EXTRA_VIBRATION_PATTERN)
            ?: VibrationPattern.SHARP_PULSE.name
        val soundEnabled = intent.getBooleanExtra(AlarmConstants.EXTRA_SOUND_ENABLED, true)
        val triggerMillis = intent.getLongExtra(AlarmConstants.EXTRA_TRIGGER_MILLIS, System.currentTimeMillis())

        val app = application as ExactTimeApplication

        setContent {
            ExactTimeTheme(themeMode = AppThemeMode.DARK) {
                AlarmRingingScreen(
                    label = label,
                    category = AlarmCategory.fromName(categoryName),
                    triggerMillis = triggerMillis,
                    defaultSnoozeSeconds = snoozeSeconds,
                    onDismiss = {
                        AlarmForegroundService.stop(this)
                        finish()
                    },
                    onSnooze = { seconds ->
                        AlarmForegroundService.stop(this)
                        app.alarmScheduler.scheduleSnooze(
                            alarmId = if (alarmId > 0L) alarmId else 99999L,
                            label = label.removeSuffix(" (Snoozed)"),
                            snoozeSeconds = seconds,
                            category = categoryName,
                            vibrationPattern = vibrationName,
                            soundEnabled = soundEnabled
                        )
                        finish()
                    }
                )
            }
        }
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
private fun AlarmRingingScreen(
    label: String,
    category: AlarmCategory,
    triggerMillis: Long,
    defaultSnoozeSeconds: Int,
    onDismiss: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(50L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val (liveMain, liveFrac, _) = ScheduleCalculator.formatEpochTime(
        epochMillis = nowMillis,
        use24Hour = true,
        includeCentiseconds = true
    )
    val (triggerMain, _, _) = ScheduleCalculator.formatEpochTime(
        epochMillis = triggerMillis,
        use24Hour = true,
        includeCentiseconds = false
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val bgColor = MaterialTheme.colorScheme.background

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = bgColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.24f),
                            bgColor
                        )
                    )
                )
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = category.displayName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${stringResource(R.string.ringing_exact_trigger)} · $triggerMain",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier.size(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseRadius = size.minDimension * 0.36f
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.16f),
                            radius = baseRadius * pulseScale
                        )
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.45f),
                            radius = baseRadius * (0.9f + (pulseScale - 0.85f) * 0.35f),
                            style = Stroke(width = 3.dp.toPx())
                        )
                    }
                    Surface(
                        modifier = Modifier.size(112.dp),
                        shape = CircleShape,
                        color = primaryColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "Alarm active",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = liveMain,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (liveFrac != null) {
                        Text(
                            text = liveFrac,
                            style = MaterialTheme.typography.displaySmall,
                            color = primaryColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                Spacer(Modifier.height(36.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("ringing_dismiss_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AlarmOff,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = stringResource(R.string.ringing_dismiss),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = { onSnooze(defaultSnoozeSeconds) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("ringing_snooze_button"),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Snooze,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "${stringResource(R.string.ringing_snooze)} (+${ScheduleCalculator.formatDurationCompact(defaultSnoozeSeconds)})",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30 to "+30s", 60 to "+1m", 300 to "+5m").forEach { (secs, chipLabel) ->
                            OutlinedButton(
                                onClick = { onSnooze(secs) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("ringing_quick_snooze_$secs"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(
                                    text = chipLabel,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
