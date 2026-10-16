package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.clock.data.preferences.AppearanceMode

private val DarkColorScheme = darkColorScheme(
    primary = DarkAccentBlue,
    onPrimary = Color.White,
    primaryContainer = DarkAccentBlue.copy(alpha = 0.2f),
    onPrimaryContainer = DarkAccentBlue,
    secondary = DarkSecondaryText,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkPrimaryText,
    surface = DarkBackground,
    onSurface = DarkPrimaryText,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkHairlineSeparator,
    error = DestructiveRedDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LightAccentBlue,
    onPrimary = Color.White,
    primaryContainer = LightAccentBlue.copy(alpha = 0.15f),
    onPrimaryContainer = LightAccentBlue,
    secondary = LightSecondaryText,
    onSecondary = Color.Black,
    background = LightBackground,
    onBackground = LightPrimaryText,
    surface = LightBackground,
    onSurface = LightPrimaryText,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightSecondaryText,
    outline = LightHairlineSeparator,
    error = DestructiveRedLight,
    onError = Color.White
)

@Composable
fun ClockTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appearanceMode) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
