package com.example.creproductor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creproductor.R


val SilkscreenFont = FontFamily(
    Font(R.font.silkscreen, FontWeight.Normal),
    Font(R.font.silkscreen_bold, FontWeight.Bold)
    )

private val PixelBaseStyle = TextStyle (
    fontFamily = SilkscreenFont,
    platformStyle = PlatformTextStyle(
        includeFontPadding = false
    ),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both
    )
)

/**
 * Identidad DEZZNUTS: negro cálido y caramelo (la bellota), con un verde hoja de acento.
 * El reproductor sustituye `primary` por el color de la carátula (ver `rememberCoverAccent`).
 */
private val DezzColors = darkColorScheme(
    primary = Color(0xFFF2B36B),
    onPrimary = Color(0xFF2B1700),
    primaryContainer = Color(0xFF5A3A12),
    onPrimaryContainer = Color(0xFFFFDDB5),
    secondary = Color(0xFFD7C3AE),
    onSecondary = Color(0xFF3A2E22),
    secondaryContainer = Color(0xFF3F352C),
    onSecondaryContainer = Color(0xFFF3DFC9),
    tertiary = Color(0xFFA9CFA4),
    onTertiary = Color(0xFF15361A),
    background = Color(0xFF0F0D0C),
    onBackground = Color(0xFFEDE6E0),
    surface = Color(0xFF0F0D0C),
    onSurface = Color(0xFFEDE6E0),
    surfaceVariant = Color(0xFF2A2521),
    onSurfaceVariant = Color(0xFFBCB1A6),
    surfaceContainerLowest = Color(0xFF0A0908),
    surfaceContainerLow = Color(0xFF171412),
    surfaceContainer = Color(0xFF1C1916),
    surfaceContainerHigh = Color(0xFF26221F),
    surfaceContainerHighest = Color(0xFF312C28),
    outline = Color(0xFF6E645B),
    outlineVariant = Color(0xFF3D3631),
)

private val OmargeColors = darkColorScheme(
    primary = Color(0xFF85D667),
    onPrimary = Color(0xFF0D120B),
    primaryContainer = Color(0xFF22381B),
    onPrimaryContainer = Color(0xFFC3F3AF),
    secondary = Color(0xFF85D667),
    onSecondary = Color(0xFF0D120B),
    secondaryContainer = Color(0xFF192814),
    onSecondaryContainer = Color(0xFFD0F0C0),
    background = Color(0xFF0A0E0A),
    onBackground = Color(0xFFE0EFE0),
    surface = Color(0xFF0A0E0A),
    onSurface = Color(0xFFE0EFE0),
    surfaceVariant = Color(0xFF141E13),
    onSurfaceVariant = Color(0xFFA0BFA0),
    surfaceContainerLowest = Color(0xFF050805),
    surfaceContainerLow = Color(0xFF0D140D),
    surfaceContainer = Color(0xFF121B12),
    surfaceContainerHigh = Color(0xFF1A261A),
    surfaceContainerHighest = Color(0xFF223222),
    outline = Color(0xFF3A5438),
    outlineVariant = Color(0xFF223421)
)

private val OledColors = DezzColors.copy(
    background = Color(0xFF000000),
    surface = Color(0xFF000000),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF080706),
    surfaceContainer = Color(0xFF0E0C0A),
    surfaceContainerHigh = Color(0xFF161310),
    surfaceContainerHighest = Color(0xFF1F1B17)
)

/** Cuatro radios para toda la app (antes había once distintos). */
val DezzShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val OmargeShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

private val OmargeTypography = Typography(

    headlineSmall = PixelBaseStyle.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 16.sp),
    titleLarge = PixelBaseStyle.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 14.sp),
    titleMedium = PixelBaseStyle.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, lineHeight = 12.sp),
    titleSmall = PixelBaseStyle.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 11.sp),

    bodyLarge = PixelBaseStyle.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 8.sp),

    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    labelLarge = PixelBaseStyle.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 12.sp),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 9.sp,
        lineHeight = 11.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
)

/** Genera variaciones adecuadas que encajan bien al aplicar un color primario personalizado*/
private fun ColorScheme.applyAccent(accent: Color): ColorScheme{
    val argb = accent.toArgb()
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)

    val containerHsv = floatArrayOf(
        hsv[0],
        (hsv[1] * 0.7f).coerceIn(0.2f, 0.85f),
        (hsv[2] * 0.3f).coerceIn(0.12f, 0.35f)
    )

    val containerColor = Color(android.graphics.Color.HSVToColor(containerHsv))
    val onContainerHsv = floatArrayOf(hsv[0], (hsv[1] * 0.3f), 0.95f)
    val onContainerColor = Color(android.graphics.Color.HSVToColor(onContainerHsv))

    return this.copy(
        primary = accent,
        primaryContainer = containerColor,
        onPrimaryContainer = onContainerColor,
        secondary = accent,
        secondaryContainer = containerColor,
        onSecondaryContainer = onContainerColor
    )

}

@Composable
fun DEZZNUTStheme(
    appStyle: AppStyle = AppStyle.MATERIAL_3,
    accentPreset: AppAccentPreset = AppAccentPreset.DEFAULT,
    customAccentColor: Color? = null,
    content: @Composable () -> Unit
){

    val (baseColors, shapes, typography) = when (appStyle){
        AppStyle.MATERIAL_3 -> Triple(DezzColors, DezzShapes, Typography)
        AppStyle.OMARGE -> Triple(OmargeColors, OmargeShapes, OmargeTypography)
        AppStyle.OLED_DARK -> Triple(OledColors, DezzShapes, Typography)
    }

    val targetAccent = when (accentPreset){
        AppAccentPreset.DEFAULT -> baseColors.primary
        AppAccentPreset.CUSTOM -> customAccentColor ?: baseColors.primary
        else -> accentPreset.defaultColor ?: baseColors.primary
    }

    val finalColors = if (targetAccent == baseColors.primary){
        baseColors
    }else{
        baseColors.applyAccent(targetAccent)
    }

    MaterialTheme(
        colorScheme = finalColors,
        typography = typography,
        shapes = shapes,
        content = content
    )
}
