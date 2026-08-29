package com.tomasrepcik.voidlauncher.appearance

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag

private const val DARK_IMAGE_LUMINANCE = 0.45f
private const val CONTAINER_TINT_RATIO = 0.64f
private val DarkContent = Color(0xFF151515)

@Composable
internal fun HomeBackgroundContainer(
    state: HomeAppearanceState,
    content: @Composable () -> Unit,
) {
    val baseColors = MaterialTheme.colorScheme
    val homeColors = remember(baseColors, state.background, state.useBackgroundColors) {
        if (state.background != null && state.useBackgroundColors) {
            baseColors.withBackgroundColors(state.background)
        } else {
            baseColors
        }
    }

    MaterialTheme(colorScheme = homeColors) {
        CompositionLocalProvider(LocalContentColor provides homeColors.onBackground) {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeBackgroundLayer(state.background, homeColors)
                content()
            }
        }
    }
}

@Composable
private fun HomeBackgroundLayer(
    background: HomeBackgroundImage?,
    colors: ColorScheme,
) {
    if (background == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.surface,
                            colors.surfaceVariant.copy(alpha = 0.72f),
                        )
                    )
                ),
        )
        return
    }

    Image(
        bitmap = background.bitmap,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_background_image"),
    )
    val scrim = if (colors.onBackground.luminance() > DARK_IMAGE_LUMINANCE) {
        Color.Black.copy(alpha = 0.34f)
    } else {
        Color.White.copy(alpha = 0.34f)
    }
    Box(modifier = Modifier.fillMaxSize().background(scrim))
}

private fun ColorScheme.withBackgroundColors(background: HomeBackgroundImage): ColorScheme {
    val anchor = background.primary
    val content = if (anchor.luminance() < DARK_IMAGE_LUMINANCE) Color.White else DarkContent
    val neutral = if (content == Color.White) Color.Black else Color.White
    val surface = lerp(anchor, neutral, 0.82f)
    val surfaceVariant = lerp(anchor, neutral, 0.68f)
    val primary = background.secondary ?: background.primary
    val tertiary = background.tertiary ?: lerp(primary, content, 0.32f)

    return copy(
        primary = primary,
        onPrimary = primary.contrastingColor(),
        primaryContainer = lerp(primary, neutral, CONTAINER_TINT_RATIO),
        onPrimaryContainer = content,
        secondary = content.copy(alpha = 0.76f),
        onSecondary = neutral,
        secondaryContainer = surfaceVariant,
        onSecondaryContainer = content,
        tertiary = tertiary,
        onTertiary = tertiary.contrastingColor(),
        tertiaryContainer = lerp(tertiary, neutral, CONTAINER_TINT_RATIO),
        onTertiaryContainer = content,
        surface = surface,
        onSurface = content,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = content.copy(alpha = 0.78f),
        background = surface,
        onBackground = content,
        outline = content.copy(alpha = 0.58f),
        outlineVariant = content.copy(alpha = 0.28f),
    )
}

private fun Color.contrastingColor(): Color =
    if (luminance() < DARK_IMAGE_LUMINANCE) Color.White else DarkContent
