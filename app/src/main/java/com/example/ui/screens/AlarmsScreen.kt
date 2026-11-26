package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ExactAlarmEntity
import com.example.data.ExactTimeSettings
import com.example.domain.ScheduleCalculator
import com.example.ui.theme.LocalExactTimeColors

private enum class AlarmFilter(val label: String) {
    ALL("All"),
    ARMED("Armed"),
    RECURRING("Recurring")
}

@Composable
fun AlarmsScreen(
    nowMillis: Long,
    alarms: List<ExactAlarmEntity>,
    settings: ExactTimeSettings,
    onAddAlarm: () -> Unit,
    onEditAlarm: (ExactAlarmEntity) -> Unit,
    onToggleAlarm: (ExactAlarmEntity, Boolean) -> Unit,
    onToggleSkipNext: (ExactAlarmEntity) -> Unit,
    onTestFire: (ExactAlarmEntity) -> Unit,
    onDeleteAlarm: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf(AlarmFilter.ALL) }
    val filteredAlarms = remember(alarms, selectedFilter) {
        when (selectedFilter) {
            AlarmFilter.ALL -> alarms
            AlarmFilter.ARMED -> alarms.filter { it.isEnabled }
            AlarmFilter.RECURRING -> alarms.filter { it.isRecurring }
        }
    }
    val armedCount = remember(alarms) { alarms.count { it.isEnabled } }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.alarms_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "$armedCount armed · ${alarms.size} total schedules",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (alarms.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AlarmFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("alarm_filter_${filter.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }

            if (filteredAlarms.isEmpty()) {
                item {
                    EmptyAlarmsCard(
                        isFilteredEmpty = alarms.isNotEmpty(),
                        onAddAlarm = onAddAlarm
                    )
                }
            } else {
                items(filteredAlarms, key = { it.id }) { alarm ->
                    ExactAlarmCard(
                        alarm = alarm,
                        nowMillis = nowMillis,
                        use24Hour = settings.use24HourFormat,
                        onEdit = { onEditAlarm(alarm) },
                        onToggle = { enabled -> onToggleAlarm(alarm, enabled) },
                        onToggleSkipNext = { onToggleSkipNext(alarm) },
                        onTestFire = { onTestFire(alarm) },
                        onDelete = { onDeleteAlarm(alarm.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAlarmsCard(
    isFilteredEmpty: Boolean,
    onAddAlarm: () -> Unit
) {
    val ext = LocalExactTimeColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.hairlineBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_chrono_banner_1795707535997),
                    contentDescription = "Precision chronometer rings artwork",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isFilteredEmpty) "No Matching Alarms" else stringResource(R.string.alarms_empty_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = if (isFilteredEmpty) {
                        "Switch filters or add a new second-precision schedule."
                    } else {
                        stringResource(R.string.alarms_empty_subtitle)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = onAddAlarm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("empty_create_alarm_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.alarms_add_new))
                }
            }
        }
    }
}

@Composable
private fun ExactAlarmCard(
    alarm: ExactAlarmEntity,
    nowMillis: Long,
    use24Hour: Boolean,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onToggleSkipNext: () -> Unit,
    onTestFire: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalExactTimeColors.current
    val (timeStr, amPm) = ScheduleCalculator.formatExactTime(
        alarm.hour,
        alarm.minute,
        alarm.second,
        use24Hour
    )
    val timeParts = timeStr.split(":")
    val nextTriggerMillis = remember(alarm, nowMillis / 1_000L) {
        if (alarm.isEnabled) ScheduleCalculator.computeNextTriggerMillis(alarm, nowMillis) else null
    }
    val isSkipped = alarm.skipNextTriggerMillis != null && alarm.isEnabled && alarm.isRecurring

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onEdit)
            .testTag("alarm_card_${alarm.id}"),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (alarm.isEnabled) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            } else {
                ext.hairlineBorder
            }
        ),
        tonalElevation = if (alarm.isEnabled) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = alarm.categoryEnum.displayName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (alarm.isEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = alarm.label.ifBlank { "ExactTime Alarm" },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (alarm.isEnabled) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("alarm_switch_${alarm.id}"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (timeParts.size == 3) {
                        Text(
                            text = "${timeParts[0]}:${timeParts[1]}:",
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
                            color = if (alarm.isEnabled) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = timeParts[2],
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 32.sp),
                            color = if (alarm.isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    } else {
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (amPm != null) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = amPm,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 5.dp)
                        )
                    }
                }

                if (nextTriggerMillis != null) {
                    Surface(
                        color = if (isSkipped) ext.warningAmberContainer else ext.syncEmeraldContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isSkipped) {
                                "SKIPPED · T-${ScheduleCalculator.formatCountdown(nextTriggerMillis - nowMillis)}"
                            } else {
                                "T-${ScheduleCalculator.formatCountdown(nextTriggerMillis - nowMillis)}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSkipped) ext.warningAmber else ext.syncEmerald,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ScheduleCalculator.formatScheduleSummary(alarm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (alarm.preAlarmLeadSeconds > 0) {
                    MicroMetaChip("Pre -${alarm.preAlarmLeadSeconds}s")
                }
                if (alarm.fadeInSeconds > 0) {
                    MicroMetaChip("Fade ${alarm.fadeInSeconds}s")
                }
                MicroMetaChip("Snooze ${ScheduleCalculator.formatDurationCompact(alarm.snoozeSeconds)}")
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = ext.hairlineBorder.copy(alpha = 0.6f))
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(
                        onClick = onTestFire,
                        modifier = Modifier.testTag("alarm_test_fire_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.alarms_test_fire))
                    }

                    if (alarm.isRecurring && alarm.isEnabled) {
                        TextButton(
                            onClick = onToggleSkipNext,
                            modifier = Modifier.testTag("alarm_skip_next_${alarm.id}")
                        ) {
                            Icon(
                                imageVector = if (isSkipped) Icons.Filled.Undo else Icons.Filled.SkipNext,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isSkipped) {
                                    stringResource(R.string.alarms_unskip_next)
                                } else {
                                    stringResource(R.string.alarms_skip_next)
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("alarm_edit_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.alarms_edit),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("alarm_delete_${alarm.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = stringResource(R.string.alarms_delete),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MicroMetaChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
