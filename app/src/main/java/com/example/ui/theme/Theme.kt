package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SnaporaPurple,
    onPrimary = Color.White,
    primaryContainer = SnaporaPurpleDark,
    onPrimaryContainer = Color.White,
    secondary = SnaporaPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A102E),
    onSecondaryContainer = Color.White,
    tertiary = SnaporaCyan,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = SnaporaRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = SnaporaPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = SnaporaPurpleDark,
    secondary = SnaporaPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE4E6),
    onSecondaryContainer = Color(0xFF881337),
    tertiary = SnaporaCyan,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFE2E8F0),
    error = SnaporaRed,
    onError = Color.White
)

@Composable
fun SnaporaTheme(
    darkTheme: Boolean = true, // Social camera apps shine brightest in modern dark mode
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = SnaporaTheme(darkTheme = darkTheme, content = content)
