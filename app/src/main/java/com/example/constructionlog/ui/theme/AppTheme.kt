package com.example.constructionlog.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0052D9),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2BA471),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF4E5969),
    background = Color(0xFFF2F3F5),
    onBackground = Color(0xFF1D2129),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D2129),
    surfaceVariant = Color(0xFFE5E6EB),
    onSurfaceVariant = Color(0xFF4E5969)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF70D4C0),
    onPrimary = Color(0xFF003730),
    secondary = Color(0xFFFFB680),
    onSecondary = Color(0xFF4A2500),
    tertiary = Color(0xFFA6CBFF),
    background = Color(0xFF151311),
    onBackground = Color(0xFFECE1D6),
    surface = Color(0xFF1D1A18),
    onSurface = Color(0xFFECE1D6),
    surfaceVariant = Color(0xFF48413A),
    onSurfaceVariant = Color(0xFFD0C5B8)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = Color(0xFF1D2129)
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = Color(0xFF4E5969)
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        color = Color(0xFF4E5969)
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8),
    small = RoundedCornerShape(12),
    medium = RoundedCornerShape(12),
    large = RoundedCornerShape(16)
)

@Composable
fun ConstructionLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
