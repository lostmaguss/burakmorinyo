package com.burakmorinyo.gelirgider.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.burakmorinyo.gelirgider.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFFDB2777),
    tertiaryContainer = Color(0xFFFCE7F3),
    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF5F6FB),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8EAF3),
    onSurfaceVariant = Color(0xFF5B6478),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEEF0F7),
    surfaceContainerHigh = Color(0xFFE6E9F2),
    surfaceContainerHighest = Color(0xFFDDE1EC),
    outline = Color(0xFFC3C8D6),
    outlineVariant = Color(0xFFE2E5EE),
    error = Color(0xFFE11D48)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFF9A8D4),
    tertiaryContainer = Color(0xFF831843),
    background = Color(0xFF0B0D14),
    onBackground = Color(0xFFE5E7EF),
    surface = Color(0xFF0B0D14),
    onSurface = Color(0xFFE5E7EF),
    surfaceVariant = Color(0xFF1D2130),
    onSurfaceVariant = Color(0xFF9AA3B8),
    surfaceContainerLowest = Color(0xFF0E1018),
    surfaceContainerLow = Color(0xFF141724),
    surfaceContainer = Color(0xFF181C2A),
    surfaceContainerHigh = Color(0xFF1F2333),
    surfaceContainerHighest = Color(0xFF272C3E),
    outline = Color(0xFF3A4055),
    outlineVariant = Color(0xFF252A3A),
    error = Color(0xFFFB7185)
)

@Immutable
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val warning: Color,
    val heroGradient: Brush,
    val isDark: Boolean
)

val LocalFinanceColors = staticCompositionLocalOf {
    FinanceColors(Color.Green, Color.Red, Color.Blue, Color.Yellow, Brush.linearGradient(listOf(Color.Blue, Color.Magenta)), false)
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val base = Typography()
private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelMedium = base.labelMedium,
    labelSmall = base.labelSmall
)

val AmountStyle = TextStyle(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)

@Composable
fun GelirGiderTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
        dark -> DarkColors
        else -> LightColors
    }
    val finance = if (dark) FinanceColors(
        income = Color(0xFF34D399),
        expense = Color(0xFFFB7185),
        transfer = Color(0xFF7DD3FC),
        warning = Color(0xFFFBBF24),
        heroGradient = Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFFBE185D))),
        isDark = true
    ) else FinanceColors(
        income = Color(0xFF059669),
        expense = Color(0xFFE11D48),
        transfer = Color(0xFF0284C7),
        warning = Color(0xFFD97706),
        heroGradient = Brush.linearGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFDB2777))),
        isDark = false
    )
    androidx.compose.runtime.CompositionLocalProvider(LocalFinanceColors provides finance) {
        MaterialTheme(colorScheme = colors, typography = AppTypography, shapes = AppShapes, content = content)
    }
}
