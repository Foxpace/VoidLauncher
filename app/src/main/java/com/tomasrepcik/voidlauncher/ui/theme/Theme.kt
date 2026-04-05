package com.tomasrepcik.voidlauncher.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

private val LightColors = lightColorScheme(
    primary = Color(0xFF111316),
    onPrimary = Color(0xFFF6F7F5),
    secondary = Color(0xFF5C6570),
    onSecondary = Color(0xFFF4F6F8),
    surface = Color(0xFFF7F4ED),
    onSurface = Color(0xFF17181A),
    surfaceVariant = Color(0xFFE7E1D3),
    onSurfaceVariant = Color(0xFF4A4E54),
    background = Color(0xFFF7F4ED),
    onBackground = Color(0xFF17181A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF0EEE6),
    onPrimary = Color(0xFF151618),
    secondary = Color(0xFFB6BFC8),
    onSecondary = Color(0xFF1C2024),
    surface = Color.Black,
    onSurface = Color(0xFFF2EFE7),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFD1C9BB),
    background = Color.Black,
    onBackground = Color(0xFFF2EFE7),
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
