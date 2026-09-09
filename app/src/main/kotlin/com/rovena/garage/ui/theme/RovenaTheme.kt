package com.rovena.garage.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rovena.garage.data.AppPreferences

object RovenaPalette {
    val Midnight = Color(0xFF020B15)
    val DeepNavy = Color(0xFF041421)
    val Surface = Color(0xFF081B2D)
    val SurfaceRaised = Color(0xFF0D243A)
    val SurfaceSoft = Color(0xFF112B44)
    val Navigation = Color(0xF20A1B2C)
    val Accent = Color(0xFF149CFF)
    val Cyan = Color(0xFF38D7FF)
    val Sky = Color(0xFF74BEFF)
    val Success = Color(0xFF35D69B)
    val Warning = Color(0xFFFFB84D)
    val Danger = Color(0xFFFF6174)
    val TextPrimary = Color(0xFFF5FAFF)
    val TextSecondary = Color(0xFFA8BCD0)
    val Outline = Color(0xFF27445F)
}

val RovenaScreenGradient = Brush.verticalGradient(
    colors = listOf(
        RovenaPalette.Midnight,
        RovenaPalette.DeepNavy,
        Color(0xFF061C2F)
    )
)

private val DarkColors = darkColorScheme(
    primary = RovenaPalette.Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0C3557),
    onPrimaryContainer = RovenaPalette.TextPrimary,
    secondary = RovenaPalette.Cyan,
    onSecondary = Color(0xFF001F2A),
    secondaryContainer = Color(0xFF10364B),
    onSecondaryContainer = RovenaPalette.TextPrimary,
    tertiary = RovenaPalette.Warning,
    onTertiary = Color(0xFF2B1700),
    tertiaryContainer = Color(0xFF493015),
    onTertiaryContainer = Color(0xFFFFE2B5),
    background = RovenaPalette.Midnight,
    onBackground = RovenaPalette.TextPrimary,
    surface = RovenaPalette.Surface,
    onSurface = RovenaPalette.TextPrimary,
    surfaceVariant = RovenaPalette.SurfaceRaised,
    onSurfaceVariant = RovenaPalette.TextSecondary,
    outline = RovenaPalette.Outline,
    outlineVariant = Color(0xFF19344E),
    error = RovenaPalette.Danger,
    errorContainer = Color(0xFF4D1F2A),
    onErrorContainer = Color(0xFFFFD9DE)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0077C8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7ECFF),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF007C9A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCF0FF),
    onSecondaryContainer = Color(0xFF001F29),
    tertiary = Color(0xFF9A6700),
    background = Color(0xFFF3F8FD),
    onBackground = Color(0xFF0A1B2B),
    surface = Color.White,
    onSurface = Color(0xFF0A1B2B),
    surfaceVariant = Color(0xFFE6F0F8),
    onSurfaceVariant = Color(0xFF455B6D),
    outline = Color(0xFF7C93A5)
)

private val RovenaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

private val RovenaTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 27.sp, lineHeight = 33.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 23.sp, lineHeight = 29.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp)
)

@Composable
fun RovenaTheme(
    themeMode: String = AppPreferences.THEME_DARK,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        AppPreferences.THEME_LIGHT -> false
        AppPreferences.THEME_DARK -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = RovenaTypography,
        shapes = RovenaShapes,
        content = content
    )
}
