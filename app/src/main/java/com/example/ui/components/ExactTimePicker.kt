package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalExactTimeColors
import java.util.Locale

@Composable
fun ExactTimePicker(
    hour: Int,
    minute: Int,
    second: Int,
    use24Hour: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onSecondChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalExactTimeColors.current
    val isPm = hour >= 12
    val displayHour = if (use24Hour) {
        hour
    } else {
        when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, ext.hairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeUnitStepper(
                        label = "HR",
                        valueText = String.format(Locale.US, "%02d", displayHour),
                        accent = false,
                        tagPrefix = "hour",
                        onIncrement = { onHourChange((hour + 1) % 24) },
                        onDecrement = { onHourChange((hour + 23) % 24) }
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TimeUnitStepper(
                        label = "MIN",
                        valueText = String.format(Locale.US, "%02d", minute),
                        accent = false,
                        tagPrefix = "minute",
                        onIncrement = { onMinuteChange((minute + 1) % 60) },
                        onDecrement = { onMinuteChange((minute + 59) % 60) }
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TimeUnitStepper(
                        label = "SEC",
                        valueText = String.format(Locale.US, "%02d", second),
                        accent = true,
                        tagPrefix = "second",
                        onIncrement = { onSecondChange((second + 1) % 60) },
                        onDecrement = { onSecondChange((second + 59) % 60) }
                    )

                    if (!use24Hour) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AmPmToggleButton(
                                text = "AM",
                                selected = !isPm,
                                onClick = { if (isPm) onHourChange(hour - 12) }
                            )
                            AmPmToggleButton(
                                text = "PM",
                                selected = isPm,
                                onClick = { if (!isPm) onHourChange(hour + 12) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Precision second calibration chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickCalibrations = listOf(
                        "-5s" to { onSecondChange((second + 55) % 60) },
                        "-1s" to { onSecondChange((second + 59) % 60) },
                        ":00" to { onSecondChange(0) },
                        ":30" to { onSecondChange(30) },
                        "+1s" to { onSecondChange((second + 1) % 60) },
                        "+5s" to { onSecondChange((second + 5) % 60) }
                    )
                    quickCalibrations.forEach { (chipLabel, action) ->
                        Surface(
                            onClick = action,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("sec_cal_$chipLabel"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (chipLabel == ":00" || chipLabel == ":30") {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(1.dp, ext.hairlineBorder)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = chipLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (chipLabel == ":00" || chipLabel == ":30") {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeUnitStepper(
    label: String,
    valueText: String,
    accent: Boolean,
    tagPrefix: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        FilledTonalIconButton(
            onClick = onIncrement,
            modifier = Modifier
                .size(48.dp)
                .testTag("${tagPrefix}_inc_button"),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowUp,
                contentDescription = "Increase $label"
            )
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        FilledTonalIconButton(
            onClick = onDecrement,
            modifier = Modifier
                .size(48.dp)
                .testTag("${tagPrefix}_dec_button"),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Decrease $label"
            )
        }
    }
}

@Composable
private fun AmPmToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(width = 48.dp, height = 40.dp)
            .testTag("ampm_${text.lowercase()}"),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
