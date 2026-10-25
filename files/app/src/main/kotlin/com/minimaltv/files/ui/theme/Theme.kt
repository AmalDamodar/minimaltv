package com.minimaltv.files.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minimaltv.files.R
import com.minimaltv.files.ui.DarkGlass
import com.minimaltv.files.ui.LocalGlass

// Black, white and shades of them. The only colour is a soft red for errors, so a failure stays visible.
private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color(0xFF111114),
    primaryContainer = Color(0x38FFFFFF),
    onPrimaryContainer = Color.White,
    inversePrimary = Color(0xFF111114),
    secondary = Color(0xB3FFFFFF),
    onSecondary = Color(0xFF111114),
    secondaryContainer = Color(0x38FFFFFF),
    onSecondaryContainer = Color.White,
    tertiary = Color.White,
    onTertiary = Color(0xFF111114),
    tertiaryContainer = Color(0x38FFFFFF),
    onTertiaryContainer = Color.White,
    background = Color.Transparent,
    onBackground = Color.White,
    surface = Color(0x24FFFFFF),
    onSurface = Color.White,
    surfaceVariant = Color(0x1FFFFFFF),
    onSurfaceVariant = Color(0xB3FFFFFF),
    surfaceTint = Color.Transparent,
    surfaceDim = Color(0xFF1C1C20),
    surfaceBright = Color(0xFF3A3A40),
    surfaceContainerLowest = Color(0x14FFFFFF),
    surfaceContainerLow = Color(0x1AFFFFFF),
    surfaceContainer = Color(0x24FFFFFF),
    surfaceContainerHigh = Color(0xFF2C2C31),
    surfaceContainerHighest = Color(0xFF38383E),
    inverseSurface = Color.White,
    inverseOnSurface = Color(0xFF111114),
    outline = Color(0x66FFFFFF),
    outlineVariant = Color(0x29FFFFFF),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF3A0B0B),
    errorContainer = Color(0x33FF8A8A),
    onErrorContainer = Color(0xFFFFD9D9),
    scrim = Color.Black,
)

private val FilesShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// Outfit (SIL Open Font License) is a variable font: one file, any weight.
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun outfit(weight: FontWeight) = Font(
    R.font.outfit, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val OutfitFamily = FontFamily(
    outfit(FontWeight.Normal), outfit(FontWeight.Medium), outfit(FontWeight.SemiBold), outfit(FontWeight.Bold),
)

private fun TextStyle.styled(weight: FontWeight? = null): TextStyle {
    val scale = 1.15f
    return copy(
        fontFamily = OutfitFamily,
        fontSize = fontSize * scale,
        lineHeight = lineHeight * scale,
        fontWeight = weight ?: fontWeight,
        letterSpacing = if (fontSize.value >= 28f) (-0.4).sp else letterSpacing,
    )
}

private fun typography(): Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.styled(FontWeight.SemiBold), displayMedium = displayMedium.styled(FontWeight.SemiBold),
        displaySmall = displaySmall.styled(FontWeight.SemiBold), headlineLarge = headlineLarge.styled(FontWeight.SemiBold),
        headlineMedium = headlineMedium.styled(FontWeight.SemiBold), headlineSmall = headlineSmall.styled(FontWeight.SemiBold),
        titleLarge = titleLarge.styled(FontWeight.SemiBold), titleMedium = titleMedium.styled(FontWeight.Medium),
        titleSmall = titleSmall.styled(FontWeight.Medium),
        bodyLarge = bodyLarge.styled(), bodyMedium = bodyMedium.styled(), bodySmall = bodySmall.styled(),
        labelLarge = labelLarge.styled(FontWeight.Medium), labelMedium = labelMedium.styled(FontWeight.Medium),
        labelSmall = labelSmall.styled(),
    )
}

@Composable
fun FilesTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlass provides DarkGlass) {
        MaterialTheme(
            colorScheme = DarkColors,
            typography = typography(),
            shapes = FilesShapes,
            content = content,
        )
    }
}
