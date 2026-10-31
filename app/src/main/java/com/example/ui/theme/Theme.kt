package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    secondary = AccentGreen,
    onSecondary = Color.White,
    tertiary = AccentViolet,
    background = DarkObsidianBg,
    onBackground = TextWhite,
    surface = DarkNavyCard,
    onSurface = TextWhite,
    surfaceVariant = DarkInputBg,
    onSurfaceVariant = TextMuted,
    outline = DarkNavyCardBorder
)

@Composable
fun ApkBuilderProTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
