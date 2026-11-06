package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = WatchCheckOrange,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF4A1F00),
    onPrimaryContainer = WatchCheckOrangeLight,
    secondary = WatchCheckAccentAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF4D3300),
    onSecondaryContainer = Color(0xFFFFE082),
    tertiary = Color(0xFFFFB74D),
    background = WatchCheckDarkBg,
    onBackground = Color(0xFFEDEDED),
    surface = WatchCheckDarkSurface,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = WatchCheckDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD5D5D5),
    outline = Color(0xFF666666),
    error = PrecisionRed
)

private val LightColorScheme = lightColorScheme(
    primary = WatchCheckOrangePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF5A1A00),
    secondary = Color(0xFFFB8C00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFF7B3300),
    tertiary = Color(0xFFFF9800),
    background = WatchCheckLightBg,
    onBackground = Color(0xFF1C1B1F),
    surface = WatchCheckLightSurface,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = WatchCheckLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    error = PrecisionRed
)

@Composable
fun WatchAccurancyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
