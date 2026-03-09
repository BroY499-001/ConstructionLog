package com.constructionlog.app.ui.theme

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
    primary = Color(0xFFE65100),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFCC80),
    onPrimaryContainer = Color(0xFF3E1E00),
    secondary = Color(0xFF00897B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF80CBC4),
    onSecondaryContainer = Color(0xFF003730),
    tertiary = Color(0xFF795548),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBCAAA4),
    onTertiaryContainer = Color(0xFF2A170F),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1F2329),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2329),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF757575),
    outline = Color(0xFFE0E0E0)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF8F00),
    onPrimary = Color(0xFF3E1E00),
    secondary = Color(0xFF00BCD4),
    onSecondary = Color(0xFF003730),
    tertiary = Color(0xFF8D6E63),
    onTertiary = Color(0xFF2A170F),
    background = Color(0xFF121212),
    onBackground = Color(0xFFECE1D6),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFECE1D6),
    surfaceVariant = Color(0xFF2D2D2D),
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
    small = RoundedCornerShape(10),
    medium = RoundedCornerShape(14),
    large = RoundedCornerShape(18)
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
