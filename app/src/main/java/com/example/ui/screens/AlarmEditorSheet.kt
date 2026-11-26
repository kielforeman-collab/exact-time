package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AlarmCategory
import com.example.data.ExactAlarmEntity
import com.example.data.ScheduleType
import com.example.data.VibrationPattern
import com.example.domain.ScheduleCalculator
import com.example.ui.components.ExactTimePicker
import com.example.ui.theme.LocalExactTimeColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorSheet(
    initialAlarm: ExactAlarmEntity?,
    use24Hour: Boolean,
    nowMillis: Long,
    onDismiss: () -> Unit,
    onSave: (ExactAlarmEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
        }
    }

    var label by remember(initialAlarm) { mutableStateOf(initialAlarm?.label ?: "") }
    var category by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.categoryEnum ?: AlarmCategory.GENERAL)
    }
    var hour by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.hour ?: defaultCal.get(Calendar.HOUR_OF_DAY))
    }
    var minute by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.minute ?: defaultCal.get(Calendar.MINUTE))
    }
    var second by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.second ?: 0)
    }
    var scheduleType by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.scheduleTypeEnum ?: ScheduleType.ONE_TIME)
    }
    var repeatDaysMask by remember(initialAlarm) {
        mutableIntStateOf(
            initialAlarm?.repeatDaysMask?.takeIf { it != 0 } ?: ScheduleCalculator.MASK_WEEKDAYS
        )
    }
    var useSpecificDate by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.oneTimeYear != null)
    }
    var oneTimeYear by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.oneTimeYear ?: defaultCal.get(Calendar.YEAR))
    }
    var oneTimeMonth by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.oneTimeMonth ?: (defaultCal.get(Calendar.MONTH) + 1))
    }
    var oneTimeDay by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.oneTimeDay ?: defaultCal.get(Calendar.DAY_OF_MONTH))
    }
    var intervalSeconds by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.intervalSeconds ?: 300)
    }
    var monthlyDay by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.monthlyDay ?: defaultCal.get(Calendar.DAY_OF_MONTH))
    }
    var snoozeSeconds by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.snoozeSeconds ?: 300)
    }
    var preAlarmLeadSeconds by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.preAlarmLeadSeconds ?: 0)
    }
    var fadeInSeconds by remember(initialAlarm) {
        mutableIntStateOf(initialAlarm?.fadeInSeconds ?: 0)
    }
    var vibrationPattern by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.vibrationPatternEnum ?: VibrationPattern.SHARP_PULSE)
    }
    var soundEnabled by remember(initialAlarm) {
        mutableStateOf(initialAlarm?.soundEnabled ?: true)
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val draftAlarm = ExactAlarmEntity(
        id = initialAlarm?.id ?: 0L,
        label = label.trim().ifBlank { "${category.displayName} Alarm" },
        category = category.name,
        hour = hour,
        minute = minute,
        second = second,
        scheduleType = scheduleType.name,
        repeatDaysMask = if (scheduleType == ScheduleType.WEEKLY) repeatDaysMask else 0,
        oneTimeYear = if (scheduleType == ScheduleType.ONE_TIME && useSpecificDate) oneTimeYear else null,
        oneTimeMonth = if (scheduleType == ScheduleType.ONE_TIME && useSpecificDate) oneTimeMonth else null,
        oneTimeDay = if (scheduleType == ScheduleType.ONE_TIME && useSpecificDate) oneTimeDay else null,
        intervalSeconds = intervalSeconds.coerceAtLeast(10),
        monthlyDay = monthlyDay.coerceIn(1, 31),
        isEnabled = true,
        snoozeSeconds = snoozeSeconds,
        preAlarmLeadSeconds = preAlarmLeadSeconds,
        fadeInSeconds = fadeInSeconds,
        vibrationPattern = vibrationPattern.name,
        soundEnabled = soundEnabled,
        createdAtMillis = initialAlarm?.createdAtMillis ?: System.currentTimeMillis()
    )

    val previewNextMillis = ScheduleCalculator.computeNextTriggerMillis(draftAlarm, nowMillis)
    val ext = LocalExactTimeColors.current

    if (showDatePicker) {
        val initialUtcMillis = remember(oneTimeYear, oneTimeMonth, oneTimeDay) {
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(oneTimeYear, oneTimeMonth - 1, oneTimeDay, 12, 0, 0)
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            oneTimeYear = utcCal.get(Calendar.YEAR)
                            oneTimeMonth = utcCal.get(Calendar.MONTH) + 1
                            oneTimeDay = utcCal.get(Calendar.DAY_OF_MONTH)
                            useSpecificDate = true
                        }
                        showDatePicker = false
                    }
                ) { Text("Set Date") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("editor_close_button")
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close editor")
                }
                Text(
                    text = if (initialAlarm == null) "New Exact Alarm" else "Edit Exact Alarm",
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(
                    onClick = { onSave(draftAlarm) },
                    modifier = Modifier.testTag("editor_top_save_button")
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(color = ext.hairlineBorder)

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Exact HH:MM:SS picker
                ExactTimePicker(
                    hour = hour,
                    minute = minute,
                    second = second,
                    use24Hour = use24Hour,
                    onHourChange = { hour = it },
                    onMinuteChange = { minute = it },
                    onSecondChange = { second = it }
                )

                // Live trigger preview pill
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (previewNextMillis != null) {
                        ext.syncEmeraldContainer
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (previewNextMillis != null) {
                                val fmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                                val (t, ap) = ScheduleCalculator.formatEpochTime(previewNextMillis, use24Hour)
                                    .let { it.first to it.third }
                                "Next: ${fmt.format(previewNextMillis)} · $t${if (ap != null) " $ap" else ""}"
                            } else {
                                "Selected date/time is in the past"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = if (previewNextMillis != null) ext.syncEmerald else MaterialTheme.colorScheme.error
                        )
                        if (previewNextMillis != null) {
                            Text(
                                text = "T-${ScheduleCalculator.formatCountdown(previewNextMillis - nowMillis)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.syncEmerald
                            )
                        }
                    }
                }

                // Label input
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Alarm label") },
                    placeholder = { Text("e.g., Market Open, Transit Departure, Medication") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_label_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                // Category tags
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AlarmCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.displayName) },
                                modifier = Modifier.testTag("editor_cat_${cat.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // Schedule mode selector
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "SCHEDULE ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ScheduleType.entries.forEach { type ->
                            FilterChip(
                                selected = scheduleType == type,
                                onClick = { scheduleType = type },
                                label = {
                                    Text(
                                        text = type.displayName,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("editor_sched_${type.name.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    // Mode-specific schedule configuration
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, ext.hairlineBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            when (scheduleType) {
                                ScheduleType.ONE_TIME -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Pin specific calendar date",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = if (useSpecificDate) {
                                                    String.format(Locale.US, "%04d-%02d-%02d", oneTimeYear, oneTimeMonth, oneTimeDay)
                                                } else {
                                                    "Automatically fires at next occurrence"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = useSpecificDate,
                                            onCheckedChange = { useSpecificDate = it }
                                        )
                                    }
                                    if (useSpecificDate) {
                                        OutlinedButton(
                                            onClick = { showDatePicker = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("editor_pick_date_button"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                String.format(
                                                    Locale.US,
                                                    "Target Date: %04d-%02d-%02d",
                                                    oneTimeYear,
                                                    oneTimeMonth,
                                                    oneTimeDay
                                                )
                                            )
                                        }
                                    }
                                }

                                ScheduleType.WEEKLY -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        ScheduleCalculator.DAY_SHORT_LABELS.forEachIndexed { index, letter ->
                                            val bit = 1 shl index
                                            val selected = (repeatDaysMask and bit) != 0
                                            Surface(
                                                onClick = {
                                                    val next = repeatDaysMask xor bit
                                                    if (next != 0) repeatDaysMask = next
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .testTag("editor_day_$index"),
                                                shape = CircleShape,
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.surface
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selected) MaterialTheme.colorScheme.primary else ext.hairlineBorder
                                                )
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = letter,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = if (selected) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PresetSmallChip("Weekdays", repeatDaysMask == ScheduleCalculator.MASK_WEEKDAYS) {
                                            repeatDaysMask = ScheduleCalculator.MASK_WEEKDAYS
                                        }
                                        PresetSmallChip("Weekends", repeatDaysMask == ScheduleCalculator.MASK_WEEKENDS) {
                                            repeatDaysMask = ScheduleCalculator.MASK_WEEKENDS
                                        }
                                        PresetSmallChip("Every Day", repeatDaysMask == ScheduleCalculator.MASK_ALL_DAYS) {
                                            repeatDaysMask = ScheduleCalculator.MASK_ALL_DAYS
                                        }
                                    }
                                }

                                ScheduleType.INTERVAL -> {
                                    Text(
                                        text = "Repeat every ${ScheduleCalculator.formatDurationCompact(intervalSeconds)} anchored at ${
                                            String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second)
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(30, 60, 300, 900, 1800, 3600).forEach { secs ->
                                            PresetSmallChip(
                                                label = ScheduleCalculator.formatDurationCompact(secs),
                                                selected = intervalSeconds == secs,
                                                onClick = { intervalSeconds = secs }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Fine-tune (±15s)",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalIconButton(
                                                onClick = { intervalSeconds = (intervalSeconds - 15).coerceAtLeast(15) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Filled.Remove, contentDescription = "Decrease interval")
                                            }
                                            Text(
                                                text = ScheduleCalculator.formatDurationCompact(intervalSeconds),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            FilledTonalIconButton(
                                                onClick = { intervalSeconds = (intervalSeconds + 15).coerceAtMost(86400) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = "Increase interval")
                                            }
                                        }
                                    }
                                }

                                ScheduleType.MONTHLY -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Day of Month",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "Clamps to last day in shorter months",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalIconButton(
                                                onClick = { monthlyDay = if (monthlyDay > 1) monthlyDay - 1 else 31 },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Filled.Remove, contentDescription = "Previous day")
                                            }
                                            Text(
                                                text = "$monthlyDay",
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            FilledTonalIconButton(
                                                onClick = { monthlyDay = if (monthlyDay < 31) monthlyDay + 1 else 1 },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = "Next day")
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        PresetSmallChip("1st", monthlyDay == 1) { monthlyDay = 1 }
                                        PresetSmallChip("15th", monthlyDay == 15) { monthlyDay = 15 }
                                        PresetSmallChip("End (31)", monthlyDay == 31) { monthlyDay = 31 }
                                    }
                                }
                            }
                        }
                    }
                }

                // Precision alert options
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "PRECISION ALERT OPTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OptionChipRow(
                        title = "Exact Snooze",
                        options = listOf(30 to "30s", 60 to "1m", 300 to "5m", 540 to "9m", 900 to "15m"),
                        selectedValue = snoozeSeconds,
                        onSelect = { snoozeSeconds = it }
                    )

                    OptionChipRow(
                        title = "Pre-Alarm Countdown Warning",
                        options = listOf(0 to "Off", 5 to "-5s", 10 to "-10s", 30 to "-30s"),
                        selectedValue = preAlarmLeadSeconds,
                        onSelect = { preAlarmLeadSeconds = it }
                    )

                    OptionChipRow(
                        title = "Gentle Volume Fade-In",
                        options = listOf(0 to "Instant", 10 to "10s", 30 to "30s"),
                        selectedValue = fadeInSeconds,
                        onSelect = { fadeInSeconds = it }
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Vibration Pattern",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VibrationPattern.entries.forEach { pat ->
                                PresetSmallChip(
                                    label = pat.displayName,
                                    selected = vibrationPattern == pat,
                                    onClick = { vibrationPattern = pat }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Alarm Ringtone Audio", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Uses system alarm audio stream",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it }
                        )
                    }
                }

                Button(
                    onClick = { onSave(draftAlarm) },
                    enabled = previewNextMillis != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("editor_save_alarm_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (initialAlarm == null) "Arm Exact Alarm" else "Update Exact Alarm",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PresetSmallChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun OptionChipRow(
    title: String,
    options: List<Pair<Int, String>>,
    selectedValue: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (value, label) ->
                PresetSmallChip(
                    label = label,
                    selected = selectedValue == value,
                    onClick = { onSelect(value) }
                )
            }
        }
    }
}
