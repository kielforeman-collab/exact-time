package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.AppThemeMode

@Immutable
data class ExactTimeExtendedColors(
    val syncEmerald: Color,
    val syncEmeraldContainer: Color,
    val warningAmber: Color,
    val warningAmberContainer: Color,
    val hairlineBorder: Color,
    val dialTickMajor: Color,
    val dialTickMinor: Color,
    val dialBackground: Color
)

val LocalExactTimeColors = staticCompositionLocalOf {
    ExactTimeExtendedColors(
        syncEmerald = EmeraldSync,
        syncEmeraldContainer = EmeraldSyncContainer,
        warningAmber = LuminousAmber,
        warningAmberContainer = LuminousAmberContainer,
        hairlineBorder = GraphiteBorder,
        dialTickMajor = TitaniumWhite,
        dialTickMinor = MutedSteel,
        dialBackground = GraphiteSurface
    )
}

private val ExactTimeDarkColorScheme = darkColorScheme(
    primary = ChronoVermilion,
    onPrimary = Color.White,
    primaryContainer = ChronoVermilionMuted,
    onPrimaryContainer = ChronoVermilionBright,
    secondary = LuminousAmber,
    onSecondary = ObsidianBackground,
    secondaryContainer = LuminousAmberContainer,
    onSecondaryContainer = LuminousAmber,
    tertiary = EmeraldSync,
    onTertiary = ObsidianBackground,
    tertiaryContainer = EmeraldSyncContainer,
    onTertiaryContainer = EmeraldSync,
    background = ObsidianBackground,
    onBackground = TitaniumWhite,
    surface = GraphiteSurface,
    onSurface = TitaniumWhite,
    surfaceVariant = GraphiteSurfaceHigh,
    onSurfaceVariant = CoolSlate,
    surfaceContainerLowest = ObsidianBackground,
    surfaceContainerLow = Color(0xFF12151B),
    surfaceContainer = GraphiteSurface,
    surfaceContainerHigh = GraphiteSurfaceHigh,
    surfaceContainerHighest = Color(0xFF262D3A),
    outline = GraphiteBorder,
    outlineVariant = Color(0xFF222833),
    error = Color(0xFFFF6B6B),
    onError = ObsidianBackground
)

private val ExactTimeLightColorScheme = lightColorScheme(
    primary = LightVermilion,
    onPrimary = Color.White,
    primaryContainer = LightVermilionContainer,
    onPrimaryContainer = LightVermilion,
    secondary = LightAmber,
    onSecondary = Color.White,
    secondaryContainer = LightAmberContainer,
    onSecondaryContainer = LightAmber,
    tertiary = LightEmerald,
    onTertiary = Color.White,
    tertiaryContainer = LightEmeraldContainer,
    onTertiaryContainer = LightEmerald,
    background = AlabasterBackground,
    onBackground = InkCharcoal,
    surface = PorcelainSurface,
    onSurface = InkCharcoal,
    surfaceVariant = PorcelainSurfaceHigh,
    onSurfaceVariant = SlateInk,
    surfaceContainerLowest = PorcelainSurface,
    surfaceContainerLow = Color(0xFFFAF9F5),
    surfaceContainer = PorcelainSurface,
    surfaceContainerHigh = PorcelainSurfaceHigh,
    surfaceContainerHighest = Color(0xFFE0E1D8),
    outline = PorcelainBorder,
    outlineVariant = Color(0xFFE2E3DA),
    error = Color(0xFFC62828),
    onError = Color.White
)

private val ExactTimeShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ExactTimeTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDarkTheme) ExactTimeDarkColorScheme else ExactTimeLightColorScheme
    val extendedColors = if (useDarkTheme) {
        ExactTimeExtendedColors(
            syncEmerald = EmeraldSync,
            syncEmeraldContainer = EmeraldSyncContainer,
            warningAmber = LuminousAmber,
            warningAmberContainer = LuminousAmberContainer,
            hairlineBorder = GraphiteBorder,
            dialTickMajor = TitaniumWhite,
            dialTickMinor = MutedSteel,
            dialBackground = GraphiteSurface
        )
    } else {
        ExactTimeExtendedColors(
            syncEmerald = LightEmerald,
            syncEmeraldContainer = LightEmeraldContainer,
            warningAmber = LightAmber,
            warningAmberContainer = LightAmberContainer,
            hairlineBorder = PorcelainBorder,
            dialTickMajor = InkCharcoal,
            dialTickMinor = Color(0xFF9EA5B0),
            dialBackground = PorcelainSurface
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !useDarkTheme
                    isAppearanceLightNavigationBars = !useDarkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalExactTimeColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ExactTimeTypography,
            shapes = ExactTimeShapes,
            content = content
        )
    }
}
