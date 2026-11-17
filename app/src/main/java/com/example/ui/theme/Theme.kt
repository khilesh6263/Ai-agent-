package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val CallAgentDarkColorScheme = darkColorScheme(
    primary = CyanPulse,
    onPrimary = Color(0xFF02201B),
    primaryContainer = CyanPulseDark,
    onPrimaryContainer = MintGlow,
    secondary = SkySignal,
    onSecondary = Color(0xFF041D30),
    secondaryContainer = SkySignalDark,
    onSecondaryContainer = Color(0xFFD2ECFF),
    tertiary = AmberAlert,
    onTertiary = Color(0xFF281800),
    tertiaryContainer = Color(0xFF3A2504),
    onTertiaryContainer = Color(0xFFFFDEA8),
    background = ObsidianCanvas,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    outline = ObsidianOutline,
    error = CoralHangup,
    onError = Color.White,
    errorContainer = Color(0xFF4B101D),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val CallAgentLightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = Color(0xFF00201A),
    secondary = SkySecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = SkyContainerLight,
    onSecondaryContainer = Color(0xFF001D33),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = DaylightCanvas,
    onBackground = TextPrimaryLight,
    surface = DaylightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = DaylightSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLow = DaylightSurface,
    surfaceContainer = DaylightSurfaceHigh,
    surfaceContainerHigh = Color(0xFFDCE7F3),
    outline = DaylightOutline,
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337)
)

val CallAgentShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun CallAgentTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CallAgentDarkColorScheme else CallAgentLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CallAgentTypography,
        shapes = CallAgentShapes,
        content = content
    )
}
