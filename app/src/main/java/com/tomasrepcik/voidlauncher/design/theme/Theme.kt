package com.tomasrepcik.voidlauncher.design.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun gray(level: Int) = Color(level, level, level)

private val LightAccent = gray(0x11)
private val LightOnAccent = gray(0xF7)
private val DarkAccent = gray(0xF0)
private val DarkOnAccent = gray(0x15)

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAccent,
    onPrimaryContainer = LightOnAccent,
    inversePrimary = gray(0xE3),
    secondary = gray(0x5F),
    onSecondary = gray(0xF5),
    secondaryContainer = LightAccent,
    onSecondaryContainer = LightOnAccent,
    tertiary = gray(0x4A),
    onTertiary = gray(0xF7),
    tertiaryContainer = LightAccent,
    onTertiaryContainer = LightOnAccent,
    surface = gray(0xF7),
    onSurface = gray(0x17),
    surfaceVariant = gray(0xE5),
    onSurfaceVariant = gray(0x4A),
    surfaceTint = LightAccent,
    inverseSurface = gray(0x2D),
    inverseOnSurface = gray(0xF2),
    background = gray(0xF7),
    onBackground = gray(0x17),
    error = gray(0x2B),
    onError = Color.White,
    errorContainer = gray(0xD6),
    onErrorContainer = gray(0x11),
    outline = gray(0x77),
    outlineVariant = gray(0xC6),
    scrim = Color.Black,
    surfaceDim = gray(0xD8),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = gray(0xF1),
    surfaceContainer = gray(0xEB),
    surfaceContainerHigh = gray(0xE5),
    surfaceContainerHighest = gray(0xDF),
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccent,
    onPrimaryContainer = DarkOnAccent,
    inversePrimary = gray(0x4A),
    secondary = gray(0xB9),
    onSecondary = gray(0x1C),
    secondaryContainer = DarkAccent,
    onSecondaryContainer = DarkOnAccent,
    tertiary = gray(0xCC),
    onTertiary = gray(0x20),
    tertiaryContainer = DarkAccent,
    onTertiaryContainer = DarkOnAccent,
    surface = Color.Black,
    onSurface = gray(0xF2),
    surfaceVariant = gray(0x1A),
    onSurfaceVariant = gray(0xD0),
    surfaceTint = DarkAccent,
    inverseSurface = gray(0xE8),
    inverseOnSurface = gray(0x1F),
    background = Color.Black,
    onBackground = gray(0xF2),
    error = gray(0xD0),
    onError = gray(0x1A),
    errorContainer = gray(0x3D),
    onErrorContainer = gray(0xF0),
    outline = gray(0x92),
    outlineVariant = gray(0x45),
    scrim = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = gray(0x24),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = gray(0x0D),
    surfaceContainer = gray(0x12),
    surfaceContainerHigh = gray(0x18),
    surfaceContainerHighest = gray(0x1F),
)

private val VoidLauncherTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Thin,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = 1.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.8.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
    ),
)

@Composable
internal fun voidLauncherSwitchColors(): SwitchColors = SwitchDefaults.colors(
    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
    disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(
        alpha = 0.6f,
    ),
    disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = 0.7f,
    ),
    disabledUncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(
        alpha = 0.7f,
    ),
)

@Composable
fun VoidLauncherTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme()) DarkColors else LightColors,
        typography = VoidLauncherTypography,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content,
        )
    }
}
