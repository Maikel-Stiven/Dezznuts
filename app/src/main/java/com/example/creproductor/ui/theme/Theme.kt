package com.example.creproductor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

/** Cuatro radios para toda la app (antes había once distintos). */
val DezzShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DEZZNUTStheme(content: @Composable () -> Unit){
    MaterialTheme(
        colorScheme = DezzColors,
        typography = Typography,
        shapes = DezzShapes,
        content = content
    )
}
