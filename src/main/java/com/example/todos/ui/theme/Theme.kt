package com.example.todos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

// Сгенерированные цвета для светлой темы
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFFF9800),       // Оранжевый
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFECDC),
    onPrimaryContainer = Color(0xFF261900),
    secondary = Color(0xFF6D5F00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF9E56C),
    onSecondaryContainer = Color(0xFF211C00),
    tertiary = Color(0xFF4C6300),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC9EB7E),
    onTertiaryContainer = Color(0xFF141E00),
    background = Color(0xFFFCFCFC),    // Светло-серый фон
    onBackground = Color(0xFF1D1D1D),
    surface = Color(0xFFFCFCFC),
    onSurface = Color(0xFF1D1D1D),
    surfaceVariant = Color(0xFFEFEFEF),
    onSurfaceVariant = Color(0xFF4A4A4A),
    outline = Color(0xFF7C7C7C)
    // ... остальные цвета
)

// Сгенерированные цвета для темной темы (инвертированная схема)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF5722),       // Более темный оранжевый
    secondary = Color(0xFFFFD600),
    background = Color(0xFF0190FF),    // Темно-серый фон
    // ... остальные цвета
)
@Composable
fun ToDosTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
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