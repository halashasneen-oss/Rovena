package com.rovena.garage.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.rovena.garage.data.AppPreferences

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6EE7B7),
    onPrimary = Color(0xFF002116),
    secondary = Color(0xFF9FB8FF),
    background = Color(0xFF080B10),
    surface = Color(0xFF10151D),
    surfaceVariant = Color(0xFF18202B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C4C),
    secondary = Color(0xFF345C9C),
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EDF4)
)

@Composable
fun RovenaTheme(
    themeMode: String = AppPreferences.THEME_SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        AppPreferences.THEME_LIGHT -> false
        AppPreferences.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
