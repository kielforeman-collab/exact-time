package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExactAlarmEntity
import com.example.data.ExactTimeSettings
import com.example.domain.ScheduleCalculator
import com.example.ui.theme.ChronoVermilion
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.TitaniumWhite

/**
 * Full-screen OLED StandBy precision clock supporting both portrait and landscape orientations
 * with keep-screen-on wake lock and optional Night Red low-glare mode.
 */
@Composable
fun StandByScreen(
    nowMillis: Long,
    alarms: List<ExactAlarmEntity>,
    settings: ExactTimeSettings,
    onExit: () -> Unit
) {
    BackHandler(onBack = onExit)

    val context = LocalContext.current
    DisposableEffect(context) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var nightDimMode by remember { mutableStateOf(false) }

    val nextAlarm = remember(alarms, nowMillis / 1_000L) {
        alarms
            .filter { it.isEnabled }
            .mapNotNull { alarm ->
                ScheduleCalculator.computeNextTriggerMillis(alarm, nowMillis)?.let { alarm to it }
            }
            .minByOrNull { it.second }
    }

    val (timeMain, timeFrac, amPm) = ScheduleCalculator.formatEpochTime(
        epochMillis = nowMillis,
        use24Hour = settings.use24HourFormat,
        includeCentiseconds = settings.showCentiseconds
    )
    val dateHeader = remember(nowMillis / 60_000L) {
        ScheduleCalculator.formatDateHeader(nowMillis)
    }

    val primaryTextColor = if (nightDimMode) Color(0xFFD63922) else TitaniumWhite
    val accentTextColor = if (nightDimMode) Color(0xFF9C2515) else ChronoVermilion
    val mutedTextColor = if (nightDimMode) Color(0xFF7D2418) else Color(0xFF7D8796)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .testTag("standby_screen_root")
    ) {
        val isWide = maxWidth > maxHeight
        val mainFontSize = if (isWide) 88.sp else 54.sp
        val fracFontSize = if (isWide) 34.sp else 24.sp

        // Top controls bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STANDBY CHRONOMETER",
                style = MaterialTheme.typography.labelMedium,
                color = mutedTextColor
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { nightDimMode = !nightDimMode },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("standby_night_dim_toggle")
                ) {
                    Icon(
                        imageVector = if (nightDimMode) Icons.Filled.WbSunny else Icons.Filled.Bedtime,
                        contentDescription = "Toggle night dim red mode",
                        tint = accentTextColor
                    )
                }
                IconButton(
                    onClick = onExit,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("standby_exit_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Exit StandBy mode",
                        tint = primaryTextColor
                    )
                }
            }
        }

        // Centered giant exact readout
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .clickable(onClick = { nightDimMode = !nightDimMode }),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val parts = timeMain.split(":")
            Row(verticalAlignment = Alignment.Bottom) {
                if (parts.size == 3) {
                    Text(
                        text = "${parts[0]}:${parts[1]}:",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = mainFontSize,
                            lineHeight = mainFontSize
                        ),
                        color = primaryTextColor
                    )
                    Text(
                        text = parts[2],
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = mainFontSize,
                            lineHeight = mainFontSize
                        ),
                        color = accentTextColor
                    )
                } else {
                    Text(
                        text = timeMain,
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = mainFontSize),
                        color = primaryTextColor
                    )
                }
                if (timeFrac != null) {
                    Text(
                        text = timeFrac,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = fracFontSize),
                        color = mutedTextColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (amPm != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = amPm,
                        style = MaterialTheme.typography.titleLarge,
                        color = mutedTextColor,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = dateHeader.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = mutedTextColor
            )
        }

        // Bottom next alarm pill
        if (nextAlarm != null) {
            val (alarm, triggerMillis) = nextAlarm
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                color = ObsidianBackground,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, accentTextColor.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Alarm,
                        contentDescription = null,
                        tint = accentTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${alarm.label.ifBlank { "Alarm" }} · T-${ScheduleCalculator.formatCountdown(triggerMillis - nowMillis)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = primaryTextColor
                    )
                }
            }
        }
    }
}
