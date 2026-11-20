package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightPinkColorScheme = lightColorScheme(
    primary = PinkPrimary,
    onPrimary = PinkOnPrimary,
    primaryContainer = PinkPrimaryContainer,
    onPrimaryContainer = PinkOnPrimaryContainer,
    secondary = PinkSecondary,
    onSecondary = PinkOnSecondary,
    secondaryContainer = PinkSecondaryContainer,
    onSecondaryContainer = PinkOnSecondaryContainer,
    tertiary = HighlighterTertiary,
    onTertiary = HighlighterOnTertiary,
    tertiaryContainer = HighlighterTertiaryContainer,
    onTertiaryContainer = HighlighterOnTertiaryContainer,
    background = PinkBackground,
    onBackground = PinkOnBackground,
    surface = PinkSurface,
    onSurface = PinkOnSurface,
    surfaceVariant = PinkSurfaceVariant,
    onSurfaceVariant = PinkOnSurfaceVariant,
    surfaceContainerLow = ColorTokens.SurfaceLow,
    surfaceContainer = ColorTokens.SurfaceMid,
    surfaceContainerHigh = ColorTokens.SurfaceHigh,
    outline = PinkOutline,
    outlineVariant = PinkOutlineVariant
)

private val DarkPinkColorScheme = darkColorScheme(
    primary = PinkDarkPrimary,
    onPrimary = PinkDarkOnPrimary,
    primaryContainer = PinkDarkPrimaryContainer,
    onPrimaryContainer = PinkDarkOnPrimaryContainer,
    secondary = PinkDarkSecondary,
    onSecondary = PinkDarkOnSecondary,
    secondaryContainer = PinkDarkSecondaryContainer,
    onSecondaryContainer = PinkDarkOnSecondaryContainer,
    tertiary = HighlighterTertiary,
    onTertiary = HighlighterOnTertiary,
    tertiaryContainer = HighlighterOnTertiaryContainer,
    onTertiaryContainer = HighlighterTertiaryContainer,
    background = PinkDarkBackground,
    onBackground = PinkDarkOnBackground,
    surface = PinkDarkSurface,
    onSurface = PinkDarkOnSurface,
    surfaceVariant = PinkDarkSurfaceVariant,
    onSurfaceVariant = PinkDarkOnSurfaceVariant,
    outline = PinkDarkOutline
)

private object ColorTokens {
    val SurfaceLow = androidx.compose.ui.graphics.Color(0xFFFFF5F9)
    val SurfaceMid = androidx.compose.ui.graphics.Color(0xFFFFEDF4)
    val SurfaceHigh = androidx.compose.ui.graphics.Color(0xFFFCE3EE)
}

val EduFlowShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EduFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkPinkColorScheme else LightPinkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EduFlowTypography,
        shapes = EduFlowShapes,
        content = content
    )
}
