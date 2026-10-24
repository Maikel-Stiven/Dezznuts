package com.example.creproductor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Identidad DEZZNUTS: el morado de siempre (#BB86FC) sobre negro neutro, con turquesa de acento. Es la base de toda la
 * app; la estructura y el estilo son los de la maqueta (Material estándar).
 * El color de la carátula en el reproductor es opcional (Ajustes → Apariencia) y está apagado por defecto.
 */
private val DezzColors = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF1F0640),
    primaryContainer = Color(0xFF4A2A7A),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFF03DAC6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00504A),
    onSecondaryContainer = Color(0xFFA7F5EC),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF3B1523),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceContainerLowest = Color(0xFF0C0C0C),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF2C2C2C),
    outline = Color(0xFF7A7580),
    outlineVariant = Color(0xFF3A3640),
)

@Composable
fun DEZZNUTStheme(content: @Composable () -> Unit){
    MaterialTheme(
        colorScheme = DezzColors,
        content = content
    )
}
