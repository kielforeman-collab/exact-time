package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.ui.theme.LocalExactTimeColors
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom Swiss-chronometer-inspired precision clock dial with 60 calibrated ticks,
 * continuous sweeping vermilion second indicator, and amber markers for upcoming armed alarms.
 */
@Composable
fun PrecisionClockCanvas(
    nowMillis: Long,
    upcomingAlarmTriggerMillis: List<Long>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val ext = LocalExactTimeColors.current

    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val hour12 = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val millis = cal.get(Calendar.MILLISECOND)

    val smoothSecond = second + millis / 1000f
    val smoothMinute = minute + smoothSecond / 60f
    val smoothHour = hour12 + smoothMinute / 60f

    val alarmMarkerAngles = upcomingAlarmTriggerMillis
        .filter { it in nowMillis..(nowMillis + 12L * 3_600_000L) }
        .map { trigger ->
            val aCal = Calendar.getInstance().apply { timeInMillis = trigger }
            val aHour = aCal.get(Calendar.HOUR) + aCal.get(Calendar.MINUTE) / 60f
            (aHour / 12f) * 360f - 90f
        }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = "Precision analog chronometer dial" }
    ) {
        val radius = size.minDimension * 0.48f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Subtle metallic bezel backdrop
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    surfaceColor,
                    ext.dialBackground.copy(alpha = 0.92f)
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // Outer hairline bezel ring
        drawCircle(
            color = ext.hairlineBorder,
            radius = radius,
            center = center,
            style = Stroke(width = 1.5f)
        )

        // Inner sub-track ring
        drawCircle(
            color = ext.hairlineBorder.copy(alpha = 0.45f),
            radius = radius * 0.78f,
            center = center,
            style = Stroke(width = 1f)
        )

        // Active second progress sweep arc along outer track
        val arcInset = radius * 0.04f
        val arcRadius = radius - arcInset
        drawArc(
            color = primaryColor.copy(alpha = 0.25f),
            startAngle = -90f,
            sweepAngle = (smoothSecond / 60f) * 360f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2f, arcRadius * 2f),
            style = Stroke(width = 4.5f, cap = StrokeCap.Round)
        )

        // 60 calibrated ticks
        for (i in 0 until 60) {
            val angleRad = ((i * 6f) - 90f) * (PI / 180.0)
            val isQuarter = i % 15 == 0
            val isFive = i % 5 == 0
            val outer = radius * 0.92f
            val inner = when {
                isQuarter -> radius * 0.77f
                isFive -> radius * 0.81f
                else -> radius * 0.87f
            }
            val stroke = when {
                isQuarter -> 4.2f
                isFive -> 2.8f
                else -> 1.3f
            }
            val tickColor = when {
                i == second -> primaryColor
                isFive -> ext.dialTickMajor
                else -> ext.dialTickMinor.copy(alpha = 0.75f)
            }
            val start = Offset(
                x = center.x + (cos(angleRad) * inner).toFloat(),
                y = center.y + (sin(angleRad) * inner).toFloat()
            )
            val end = Offset(
                x = center.x + (cos(angleRad) * outer).toFloat(),
                y = center.y + (sin(angleRad) * outer).toFloat()
            )
            drawLine(
                color = tickColor,
                start = start,
                end = end,
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        // Armed alarm markers on outer rim
        alarmMarkerAngles.forEach { deg ->
            val rad = deg * (PI / 180.0)
            val pipPos = Offset(
                x = center.x + (cos(rad) * radius * 0.96f).toFloat(),
                y = center.y + (sin(rad) * radius * 0.96f).toFloat()
            )
            drawCircle(
                color = ext.warningAmber,
                radius = 5.5f,
                center = pipPos
            )
        }

        // Hour hand
        val hourAngleRad = ((smoothHour * 30f) - 90f) * (PI / 180.0)
        drawLine(
            color = onSurface,
            start = Offset(
                x = center.x - (cos(hourAngleRad) * radius * 0.12f).toFloat(),
                y = center.y - (sin(hourAngleRad) * radius * 0.12f).toFloat()
            ),
            end = Offset(
                x = center.x + (cos(hourAngleRad) * radius * 0.50f).toFloat(),
                y = center.y + (sin(hourAngleRad) * radius * 0.50f).toFloat()
            ),
            strokeWidth = 7.5f,
            cap = StrokeCap.Round
        )

        // Minute hand
        val minuteAngleRad = ((smoothMinute * 6f) - 90f) * (PI / 180.0)
        drawLine(
            color = onSurface.copy(alpha = 0.92f),
            start = Offset(
                x = center.x - (cos(minuteAngleRad) * radius * 0.15f).toFloat(),
                y = center.y - (sin(minuteAngleRad) * radius * 0.15f).toFloat()
            ),
            end = Offset(
                x = center.x + (cos(minuteAngleRad) * radius * 0.72f).toFloat(),
                y = center.y + (sin(minuteAngleRad) * radius * 0.72f).toFloat()
            ),
            strokeWidth = 4.8f,
            cap = StrokeCap.Round
        )

        // Signature Swiss vermilion second needle with counterweight tail
        val secondAngleRad = ((smoothSecond * 6f) - 90f) * (PI / 180.0)
        drawLine(
            color = primaryColor,
            start = Offset(
                x = center.x - (cos(secondAngleRad) * radius * 0.22f).toFloat(),
                y = center.y - (sin(secondAngleRad) * radius * 0.22f).toFloat()
            ),
            end = Offset(
                x = center.x + (cos(secondAngleRad) * radius * 0.85f).toFloat(),
                y = center.y + (sin(secondAngleRad) * radius * 0.85f).toFloat()
            ),
            strokeWidth = 2.6f,
            cap = StrokeCap.Round
        )

        // Center jewel pivot
        drawCircle(
            color = primaryColor,
            radius = 8.5f,
            center = center
        )
        drawCircle(
            color = Color.White,
            radius = 2.8f,
            center = center
        )
    }
}
