package com.lostfound.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Dùng một bảng màu sáng cố định theo Figma; không dùng màu động của Android 12
// để app luôn đúng nhận diện của nhóm.
private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueLight,
    onPrimaryContainer = BrandBlueDark,
    secondary = LostText,
    onSecondary = Color.White,
    secondaryContainer = LostOrange,
    onSecondaryContainer = LostText,
    tertiary = FoundText,
    onTertiary = Color.White,
    tertiaryContainer = FoundGreen,
    onTertiaryContainer = FoundText,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE4E9EC),
    onSurfaceVariant = TextSecondary,
    surfaceContainer = Color.White,
    surfaceContainerLow = CardWhite,
    surfaceContainerHigh = Color(0xFFF1F4F6),
    outline = Color(0xFFB9C3C9),
    outlineVariant = Divider,
    error = ErrorRed,
    onError = Color.White
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LostFoundTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
