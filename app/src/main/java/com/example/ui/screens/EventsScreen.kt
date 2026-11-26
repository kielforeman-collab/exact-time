package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AlarmCategory
import com.example.data.CountdownEventEntity
import com.example.data.ExactTimeSettings
import com.example.domain.EphemerisCalculator
import com.example.domain.EphemerisSnapshot
import com.example.domain.ScheduleCalculator
import com.example.ui.theme.LocalExactTimeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun EventsScreen(
    nowMillis: Long,
    events: List<CountdownEventEntity>,
    settings: ExactTimeSettings,
    onSelectCity: (Int) -> Unit,
    onCreateEvent: (title: String, targetEpochMillis: Long, category: AlarmCategory, armAlarm: Boolean) -> Unit,
    onDeleteEvent: (CountdownEventEntity) -> Unit,
    showAddDialog: Boolean,
    onDismissAddDialog: () -> Unit,
    onOpenAddDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ephemeris = remember(nowMillis / 1_000L, settings.referenceCityIndex, settings.use24HourFormat) {
        EphemerisCalculator.compute(
            epochMillis = nowMillis,
            cityIndex = settings.referenceCityIndex,
            use24Hour = settings.use24HourFormat
        )
    }

    if (showAddDialog) {
        CreateCountdownDialog(
            nowMillis = nowMillis,
            onDismiss = onDismissAddDialog,
            onConfirm = { title, targetMillis, cat, arm ->
                onCreateEvent(title, targetMillis, cat, arm)
                onDismissAddDialog()
            }
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column {
                    Text(
                        text = stringResource(R.string.events_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Second-precision event timers & astronomical ephemeris",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ephemeris & Astronomical Card
            item {
                EphemerisTelemetryCard(
                    snapshot = ephemeris,
                    selectedCityIndex = settings.referenceCityIndex,
                    onSelectCity = onSelectCity
                )
            }

            // Countdowns section header
            item {
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
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ACTIVE EVENT COUNTDOWNS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        onClick = onOpenAddDialog,
                        modifier = Modifier.testTag("events_inline_add_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            if (events.isEmpty()) {
                item {
                    val ext = LocalExactTimeColors.current
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ext.hairlineBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.events_empty_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.events_empty_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = onOpenAddDialog,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("events_empty_create_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.events_add_new))
                            }
                        }
                    }
                }
            } else {
                items(events, key = { it.id }) { event ->
                    CountdownEventCard(
                        event = event,
                        nowMillis = nowMillis,
                        use24Hour = settings.use24HourFormat,
                        onDelete = { onDeleteEvent(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EphemerisTelemetryCard(
    snapshot: EphemerisSnapshot,
    selectedCityIndex: Int,
    onSelectCity: (Int) -> Unit
) {
    val ext = LocalExactTimeColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val amberColor = ext.warningAmber

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.hairlineBorder),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(R.string.ephemeris_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reference meridian chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EphemerisCalculator.REFERENCE_CITIES.forEachIndexed { idx, city ->
                    FilterChip(
                        selected = selectedCityIndex == idx,
                        onClick = { onSelectCity(idx) },
                        label = { Text(city.name, style = MaterialTheme.typography.labelMedium) },
                        modifier = Modifier.testTag("ephemeris_city_$idx"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Solar + Lunar side-by-side panels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Solar Panel
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, ext.hairlineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WbSunny,
                                contentDescription = null,
                                tint = amberColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "SOLAR ARC",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            val padX = 12f
                            val arcWidth = size.width - padX * 2
                            val arcHeight = size.height * 1.4f
                            val horizonY = size.height - 6f

                            // Horizon line
                            drawLine(
                                color = ext.hairlineBorder,
                                start = Offset(padX, horizonY),
                                end = Offset(size.width - padX, horizonY),
                                strokeWidth = 2f
                            )
                            // Track arc
                            drawArc(
                                color = ext.hairlineBorder,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = Offset(padX, horizonY - arcHeight / 2f),
                                size = Size(arcWidth, arcHeight),
                                style = Stroke(width = 3f, cap = StrokeCap.Round)
                            )
                            // Progress arc
                            if (snapshot.isDaylight) {
                                drawArc(
                                    color = amberColor,
                                    startAngle = 180f,
                                    sweepAngle = 180f * snapshot.daylightProgress,
                                    useCenter = false,
                                    topLeft = Offset(padX, horizonY - arcHeight / 2f),
                                    size = Size(arcWidth, arcHeight),
                                    style = Stroke(width = 4.5f, cap = StrokeCap.Round)
                                )
                                val theta = PI - (snapshot.daylightProgress * PI)
                                val sunX = padX + arcWidth / 2f + (cos(theta) * (arcWidth / 2f)).toFloat()
                                val sunY = horizonY - (sin(theta) * (arcHeight / 2f)).toFloat()
                                drawCircle(
                                    color = primaryColor,
                                    radius = 7f,
                                    center = Offset(sunX, sunY)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("RISE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(snapshot.sunriseText.take(8), style = MaterialTheme.typography.labelMedium)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("SET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(snapshot.sunsetText.take(8), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "Elev %+.1f°", snapshot.solarElevationDeg),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (snapshot.isDaylight) amberColor else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Lunar Panel
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, ext.hairlineBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LUNAR PHASE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        val litColor = MaterialTheme.colorScheme.onSurface
                        val darkColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        Canvas(modifier = Modifier.size(56.dp)) {
                            val r = size.minDimension * 0.44f
                            val c = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(color = darkColor, radius = r, center = c)
                            // Illuminated crescent/gibbous approximation
                            val phase = snapshot.moonPhaseFraction
                            val isWaxing = phase <= 0.5f
                            val startAngle = if (isWaxing) -90f else 90f
                            if (snapshot.moonIlluminationPercent > 4) {
                                drawArc(
                                    color = litColor.copy(alpha = 0.9f),
                                    startAngle = startAngle,
                                    sweepAngle = 180f,
                                    useCenter = true,
                                    topLeft = Offset(c.x - r, c.y - r),
                                    size = Size(r * 2, r * 2)
                                )
                                val terminatorFactor = cos(phase * 2.0 * PI).toFloat()
                                val ovalWidth = (kotlin.math.abs(terminatorFactor) * r * 2f).coerceAtLeast(2f)
                                val fillCol = if (terminatorFactor > 0f) darkColor else litColor.copy(alpha = 0.9f)
                                drawOval(
                                    color = fillCol,
                                    topLeft = Offset(c.x - ovalWidth / 2f, c.y - r),
                                    size = Size(ovalWidth, r * 2f)
                                )
                            }
                            drawCircle(
                                color = ext.hairlineBorder,
                                radius = r,
                                center = c,
                                style = Stroke(width = 1.5f)
                            )
                        }

                        Text(
                            text = snapshot.moonPhaseName,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = String.format(Locale.US, "%d%% lit · Day %.1f", snapshot.moonIlluminationPercent, snapshot.lunarAgeDays),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = ext.hairlineBorder.copy(alpha = 0.7f))

            // Monospace UTC / Epoch / Calendar telemetry row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TelemetryMiniCell("UTC ISO", snapshot.utcIso.substringAfter("T"))
                TelemetryMiniCell("EPOCH", "${snapshot.unixEpochSeconds}")
                TelemetryMiniCell("DOY", "${snapshot.dayOfYear}/${snapshot.daysInYear}")
                TelemetryMiniCell("WEEK", "W${String.format(Locale.US, "%02d", snapshot.isoWeek)}")
            }
        }
    }
}

@Composable
private fun TelemetryMiniCell(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CountdownEventCard(
    event: CountdownEventEntity,
    nowMillis: Long,
    use24Hour: Boolean,
    onDelete: () -> Unit
) {
    val ext = LocalExactTimeColors.current
    val remaining = event.targetEpochMillis - nowMillis
    val reached = remaining <= 0L
    val dateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }
    val (targetTime, amPm) = ScheduleCalculator.formatEpochTime(event.targetEpochMillis, use24Hour)
        .let { it.first to it.third }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("countdown_card_${event.id}"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (reached) ext.syncEmerald.copy(alpha = 0.5f) else ext.hairlineBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (event.armAlarmAtZero) {
                        Icon(
                            imageVector = Icons.Filled.Alarm,
                            contentDescription = "T-0 Alarm Armed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = if (reached) {
                        "T-0 REACHED"
                    } else {
                        ScheduleCalculator.formatCountdown(remaining)
                    },
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 26.sp),
                    color = if (reached) ext.syncEmerald else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Target: ${dateFmt.format(Date(event.targetEpochMillis))} · $targetTime${if (amPm != null) " $amPm" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("countdown_delete_${event.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete countdown",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CreateCountdownDialog(
    nowMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetEpochMillis: Long, category: AlarmCategory, armAlarm: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var hoursAhead by remember { mutableIntStateOf(0) }
    var minutesAhead by remember { mutableIntStateOf(15) }
    var secondsAhead by remember { mutableIntStateOf(0) }
    var armAlarm by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf(AlarmCategory.FOCUS) }

    val totalOffsetSeconds = (hoursAhead * 3600 + minutesAhead * 60 + secondsAhead).coerceAtLeast(10)
    val previewTarget = ((nowMillis / 1_000L) + totalOffsetSeconds.toLong()) * 1_000L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Precision Countdown") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Event title") },
                    placeholder = { Text("e.g., Launch Window, Auction Close") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("countdown_title_input")
                )

                Text(
                    text = "QUICK OFFSET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple(0, 1, 0) to "1m",
                        Triple(0, 15, 0) to "15m",
                        Triple(1, 0, 0) to "1h",
                        Triple(24, 0, 0) to "24h"
                    ).forEach { (hms, chip) ->
                        FilterChip(
                            selected = hoursAhead == hms.first && minutesAhead == hms.second && secondsAhead == hms.third,
                            onClick = {
                                hoursAhead = hms.first
                                minutesAhead = hms.second
                                secondsAhead = hms.third
                            },
                            label = { Text(chip, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompactStepper("HR", hoursAhead, 0..720) { hoursAhead = it }
                    CompactStepper("MIN", minutesAhead, 0..59) { minutesAhead = it }
                    CompactStepper("SEC", secondsAhead, 0..59) { secondsAhead = it }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Arm Exact Alarm at T-0", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Fires full alarm at exact target second",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = armAlarm, onCheckedChange = { armAlarm = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title.trim().ifBlank { "Precision Event" },
                        previewTarget,
                        selectedCategory,
                        armAlarm
                    )
                },
                modifier = Modifier.testTag("countdown_confirm_button")
            ) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CompactStepper(
    label: String,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { onChange((value - 1).coerceIn(range)) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) { Text("-") }
            Text(
                text = String.format(Locale.US, "%02d", value),
                style = MaterialTheme.typography.titleMedium
            )
            TextButton(
                onClick = { onChange((value + 1).coerceIn(range)) },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(36.dp)
            ) { Text("+") }
        }
    }
}
