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
    outlineVariant = Color(0xFF383838),
    error = PrecisionRed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFFA000), // Material Amber 700 aus WatchCheck
    onPrimary = Color(0xFF212121),
    primaryContainer = Color(0xFFFFECB3), // Material Amber 100
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFF2196F3), // Material Blue 500 aus WatchCheck
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = Color(0xFF0D47A1),
    tertiary = Color(0xFFFFC107), // Material Amber 500 aus WatchCheck
    background = Color(0xFFFAFAFA), // Material light background
    onBackground = Color(0xFF212121), // Material black 87%
    surface = Color(0xFFFFFFFF), // Reine weiße Kartenoberfläche
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF616161),
    outline = Color(0xFFBDBDBD),
    outlineVariant = Color(0xFFEEEEEE),
    error = Color(0xFFAA0000) // measure_red aus WatchCheck
)

enum class AppThemeMode(val title: String) {
    LIGHT("Heller Modus"),
    DARK("Dunkler Modus"),
    SYSTEM("Systemstandard")
}

@Composable
fun WatchAccurancyTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

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
