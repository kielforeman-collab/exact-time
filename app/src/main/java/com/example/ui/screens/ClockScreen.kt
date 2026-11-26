package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ExactAlarmEntity
import com.example.data.ExactTimeSettings
import com.example.data.TickCadence
import com.example.domain.ScheduleCalculator
import com.example.ui.components.PrecisionClockCanvas
import com.example.ui.theme.LocalExactTimeColors
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun ClockScreen(
    nowMillis: Long,
    alarms: List<ExactAlarmEntity>,
    settings: ExactTimeSettings,
    onToggleClockStyle: (Boolean) -> Unit,
    onOpenStandBy: () -> Unit,
    onQuickPreset: (offsetSeconds: Int?, alignToNextMinute: Boolean, label: String) -> Unit,
    onSelectTickCadence: (TickCadence) -> Unit,
    onToggleHapticTick: (Boolean) -> Unit,
    onToggleAudioTick: (Boolean) -> Unit,
    onNavigateToAlarms: () -> Unit,
    onAddAlarm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalExactTimeColors.current

    val armedWithNext = remember(alarms, nowMillis / 1_000L) {
        alarms
            .filter { it.isEnabled }
            .mapNotNull { alarm ->
                ScheduleCalculator.computeNextTriggerMillis(alarm, nowMillis)?.let { alarm to it }
            }
            .sortedBy { it.second }
    }
    val nextAlarmPair = armedWithNext.firstOrNull()

    val (timeMain, timeFrac, amPm) = ScheduleCalculator.formatEpochTime(
        epochMillis = nowMillis,
        use24Hour = settings.use24HourFormat,
        includeCentiseconds = settings.showCentiseconds
    )
    val dateHeader = remember(nowMillis / 60_000L) {
        ScheduleCalculator.formatDateHeader(nowMillis)
    }
    val tzBadge = remember(nowMillis / 60_000L) {
        val tz = TimeZone.getDefault()
        val offsetMin = tz.getOffset(nowMillis) / 60_000
        val sign = if (offsetMin >= 0) "+" else "-"
        val absMin = kotlin.math.abs(offsetMin)
        String.format(Locale.US, "%s · UTC%s%02d:%02d", tz.getDisplayName(false, TimeZone.SHORT), sign, absMin / 60, absMin % 60)
    }

    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val secondProgress = (cal.get(Calendar.SECOND) + cal.get(Calendar.MILLISECOND) / 1000f) / 60f

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Precision instrument top header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ext.syncEmerald)
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tzBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, ext.hairlineBorder)
                        ) {
                            Row(modifier = Modifier.padding(3.dp)) {
                                ModeTogglePill(
                                    label = stringResource(R.string.clock_mode_hybrid),
                                    selected = settings.clockStyleHybrid,
                                    tag = "clock_mode_dial",
                                    onClick = { onToggleClockStyle(true) }
                                )
                                ModeTogglePill(
                                    label = stringResource(R.string.clock_mode_digital),
                                    selected = !settings.clockStyleHybrid,
                                    tag = "clock_mode_digital",
                                    onClick = { onToggleClockStyle(false) }
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenStandBy,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_standby_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = stringResource(R.string.clock_standby_mode),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Hero Chronometer Display Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.hairlineBorder),
                    tonalElevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 22.dp)
                    ) {
                        AnimatedContent(
                            targetState = settings.clockStyleHybrid,
                            transitionSpec = {
                                fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                            },
                            label = "clock_style_transition"
                        ) { hybrid ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (hybrid) {
                                    PrecisionClockCanvas(
                                        nowMillis = nowMillis,
                                        upcomingAlarmTriggerMillis = armedWithNext.map { it.second },
                                        modifier = Modifier.size(216.dp)
                                    )
                                    Spacer(Modifier.height(18.dp))
                                } else {
                                    Text(
                                        text = stringResource(R.string.clock_subtitle_live),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    val timeParts = timeMain.split(":")
                                    if (timeParts.size == 3) {
                                        Text(
                                            text = "${timeParts[0]}:${timeParts[1]}:",
                                            style = if (hybrid) {
                                                MaterialTheme.typography.displayMedium.copy(fontSize = 40.sp)
                                            } else {
                                                MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp)
                                            },
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = timeParts[2],
                                            style = if (hybrid) {
                                                MaterialTheme.typography.displayMedium.copy(fontSize = 40.sp)
                                            } else {
                                                MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp)
                                            },
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = timeMain,
                                            style = MaterialTheme.typography.displayLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (timeFrac != null) {
                                        Text(
                                            text = timeFrac,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = MaterialTheme.typography.displaySmall.fontFamily
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = if (hybrid) 5.dp else 8.dp)
                                        )
                                    }

                                    if (amPm != null) {
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = amPm,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(bottom = if (hybrid) 6.dp else 10.dp)
                                        )
                                    }
                                }

                                if (!hybrid) {
                                    Spacer(Modifier.height(16.dp))
                                    LinearProgressIndicator(
                                        progress = { secondProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                }

                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Next Armed Alarm Telemetry Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (nextAlarmPair != null) onNavigateToAlarms() else onAddAlarm()
                        }
                        .testTag("next_alarm_telemetry_card"),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (nextAlarmPair != null) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        } else {
                            ext.hairlineBorder
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Alarm,
                                    contentDescription = null,
                                    tint = if (nextAlarmPair != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.clock_next_alarm_label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            if (nextAlarmPair != null) {
                                val (alarm, triggerAt) = nextAlarmPair
                                val (alarmTimeStr, alarmAmPm) = ScheduleCalculator.formatExactTime(
                                    alarm.hour,
                                    alarm.minute,
                                    alarm.second,
                                    settings.use24HourFormat
                                )
                                Text(
                                    text = alarm.label.ifBlank { "ExactTime Alarm" },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = buildString {
                                        append(alarmTimeStr)
                                        if (alarmAmPm != null) append(" $alarmAmPm")
                                        append(" · ")
                                        append(ScheduleCalculator.formatScheduleSummary(alarm))
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "T-${ScheduleCalculator.formatCountdown(triggerAt - nowMillis)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.clock_no_armed_alarms),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "Tap to arm an exact-second alarm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (nextAlarmPair == null) {
                            FilledTonalButton(
                                onClick = onAddAlarm,
                                modifier = Modifier.testTag("clock_empty_arm_button")
                            ) {
                                Text("Arm")
                            }
                        } else {
                            Surface(
                                color = ext.syncEmeraldContainer,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "${armedWithNext.size} ARMED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.syncEmerald,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Second-Precision Alarm Presets
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.clock_quick_exact_alarms),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickPresetTile(
                            title = "+30s",
                            subtitle = "Exact",
                            tag = "quick_preset_30s",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickPreset(30, false, "+30s Precision") }
                        )
                        QuickPresetTile(
                            title = "+1m",
                            subtitle = "60 sec",
                            tag = "quick_preset_1m",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickPreset(60, false, "+1m Precision") }
                        )
                        QuickPresetTile(
                            title = "+5m",
                            subtitle = "300 sec",
                            tag = "quick_preset_5m",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickPreset(300, false, "+5m Precision") }
                        )
                        QuickPresetTile(
                            title = "Next :00",
                            subtitle = "Top min",
                            tag = "quick_preset_top_min",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickPreset(null, true, "Top of Minute") }
                        )
                    }
                }
            }

            // Haptic & Audio Tick Cadence Engine Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ext.hairlineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.clock_haptic_tick_cadence),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { onToggleHapticTick(!settings.hapticTickEnabled) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("toggle_haptic_tick")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Vibration,
                                        contentDescription = "Toggle haptic tick",
                                        tint = if (settings.hapticTickEnabled && settings.tickCadence != TickCadence.OFF) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { onToggleAudioTick(!settings.audioTickEnabled) },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("toggle_audio_tick")
                                ) {
                                    Icon(
                                        imageVector = if (settings.audioTickEnabled) {
                                            Icons.AutoMirrored.Filled.VolumeUp
                                        } else {
                                            Icons.AutoMirrored.Filled.VolumeOff
                                        },
                                        contentDescription = "Toggle escapement audio tick",
                                        tint = if (settings.audioTickEnabled && settings.tickCadence != TickCadence.OFF) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TickCadence.entries.forEach { cadence ->
                                val selected = settings.tickCadence == cadence
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectTickCadence(cadence) },
                                    label = {
                                        Text(
                                            text = cadence.shortLabel,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tick_cadence_${cadence.seconds}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (settings.tickCadence == TickCadence.OFF) {
                                "Select an interval to feel tactile pulses or hear an escapement click on exact seconds."
                            } else {
                                val channels = listOfNotNull(
                                    if (settings.hapticTickEnabled) "Haptic" else null,
                                    if (settings.audioTickEnabled) "Escapement Audio" else null
                                ).ifEmpty { listOf("Muted — enable Haptic or Audio icon above") }
                                "Ticking every ${settings.tickCadence.shortLabel} (${channels.joinToString(" + ")}) · accented at :00."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun ModeTogglePill(
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(9.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .height(36.dp)
            .testTag(tag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun QuickPresetTile(
    title: String,
    subtitle: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ext = LocalExactTimeColors.current
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(68.dp)
            .testTag(tag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.hairlineBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = MaterialTheme.typography.displaySmall.fontFamily
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
