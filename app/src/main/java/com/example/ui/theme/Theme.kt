package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = XBlue,
    onPrimary = Color(0xFF00192A),
    primaryContainer = XBlueContainer,
    onPrimaryContainer = OnXBlueContainer,
    secondary = TextSecondary,
    onSecondary = ObsidianBlack,
    secondaryContainer = DarkSurfaceCard,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentSuccess,
    onTertiary = Color.Black,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderMedium,
    outlineVariant = BorderSubtle,
    error = AccentError,
    errorContainer = AccentErrorContainer,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = XBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE1F0FF),
    onPrimaryContainer = Color(0xFF003355),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = AccentSuccess,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = AccentError,
    onError = Color.White
)

@Composable
fun AutoXTheme(
    darkTheme: Boolean = true, // Premium dark theme by default as specified
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
