package com.example.creproductor.ui.theme

import androidx.compose.material3.MaterialTheme
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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

/** Negro puro para pantallas OLED: mismo morado, fondo sin gris. */
private val AmoledColors = DezzColors.copy(
    background = Color.Black, surface = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0A0A0A), surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF1A1A1A), surfaceContainerHighest = Color(0xFF222222),
    surfaceVariant = Color(0xFF1E1E1E)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A3FC4), onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFF006B63), onSecondary = Color.White,
    secondaryContainer = Color(0xFFA7F5EC), onSecondaryContainer = Color(0xFF00201D),
    background = Color(0xFFFBF9FD), onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFBF9FD), onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE6E0EC), onSurfaceVariant = Color(0xFF49454E),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF5F2F8), surfaceContainer = Color(0xFFEFECF3),
    surfaceContainerHigh = Color(0xFFE9E6EE), surfaceContainerHighest = Color(0xFFE3E0E8),
    outline = Color(0xFF7A757F), outlineVariant = Color(0xFFCAC4CF)
)

/** Premium: negro cálido y caramelo (la bellota), con Manrope y esquinas redondeadas. */
private val PremiumColors = darkColorScheme(
    primary = Color(0xFFF2B36B), onPrimary = Color(0xFF2B1700),
    primaryContainer = Color(0xFF5A3A12), onPrimaryContainer = Color(0xFFFFDDB5),
    secondary = Color(0xFFD7C3AE), onSecondary = Color(0xFF3A2E22),
    secondaryContainer = Color(0xFF3F352C), onSecondaryContainer = Color(0xFFF3DFC9),
    tertiary = Color(0xFFA9CFA4), onTertiary = Color(0xFF15361A),
    background = Color(0xFF0F0D0C), onBackground = Color(0xFFEDE6E0),
    surface = Color(0xFF0F0D0C), onSurface = Color(0xFFEDE6E0),
    surfaceVariant = Color(0xFF2A2521), onSurfaceVariant = Color(0xFFBCB1A6),
    surfaceContainerLowest = Color(0xFF0A0908), surfaceContainerLow = Color(0xFF171412), surfaceContainer = Color(0xFF1C1916),
    surfaceContainerHigh = Color(0xFF26221F), surfaceContainerHighest = Color(0xFF312C28),
    outline = Color(0xFF6E645B), outlineVariant = Color(0xFF3D3631)
)

/** Omarchy: verde terminal sobre casi negro, tipografía pixel y esquinas rectas; el acento rosa queda para reproducir. */
private val OmarchyColors = darkColorScheme(
    primary = Color(0xFF8FE388), onPrimary = Color(0xFF062008),
    primaryContainer = Color(0xFF1B3A1E), onPrimaryContainer = Color(0xFFB7F5B1),
    secondary = Color(0xFFEF5078), onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1A28), onSecondaryContainer = Color(0xFFFFD9E0),
    tertiary = Color(0xFF7FD6C2), onTertiary = Color(0xFF00201A),
    background = Color(0xFF0B100C), onBackground = Color(0xFFCFE8CC),
    surface = Color(0xFF0B100C), onSurface = Color(0xFFCFE8CC),
    surfaceVariant = Color(0xFF1B241C), onSurfaceVariant = Color(0xFF8AA58A),
    surfaceContainerLowest = Color(0xFF070A08), surfaceContainerLow = Color(0xFF0F150F), surfaceContainer = Color(0xFF141C15),
    surfaceContainerHigh = Color(0xFF1A241B), surfaceContainerHighest = Color(0xFF212D22),
    outline = Color(0xFF3F5A40), outlineVariant = Color(0xFF243324)
)

/** Tema de la app. DEZZNUTS (morado) es el de siempre; los demás son opciones en Opciones. */
enum class ThemeMode(val label: String) {
    DEZZ("DEZZNUTS"), OMARCHY("Omarchy"), PREMIUM("Premium"), AMOLED("Negro AMOLED"), LIGHT("Claro"), DYNAMIC("Material You")
}

object ThemeSetting {
    private const val KEY = "theme_mode"
    var mode by mutableStateOf(ThemeMode.DEZZ)
        private set

    fun load(context: Context) {
        val name = context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).getString(KEY, null)
        mode = ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.DEZZ
    }

    fun set(context: Context, value: ThemeMode) {
        mode = value
        context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).edit().putString(KEY, value.name).apply()
    }

    /** Material You necesita Android 12+. */
    val dynamicAvailable get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

@Composable
fun DEZZNUTStheme(content: @Composable () -> Unit){
    val context = LocalContext.current
    val mode = ThemeSetting.mode
    val scheme = when {
        mode == ThemeMode.OMARCHY -> OmarchyColors
        mode == ThemeMode.PREMIUM -> PremiumColors
        mode == ThemeMode.AMOLED -> AmoledColors
        mode == ThemeMode.LIGHT -> LightColors
        mode == ThemeMode.DYNAMIC && ThemeSetting.dynamicAvailable -> dynamicDarkColorScheme(context)
        else -> DezzColors
    }
    // Con el tema claro los iconos de la barra de estado deben ser oscuros.
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        val window = (context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = mode == ThemeMode.LIGHT
    }
    val typography = when (mode) {
        ThemeMode.OMARCHY -> OmarchyTypography
        ThemeMode.PREMIUM -> PremiumTypography
        else -> MaterialTheme.typography
    }
    val shapes = when (mode) {
        ThemeMode.OMARCHY -> OmarchyShapes
        ThemeMode.PREMIUM -> PremiumShapes
        else -> MaterialTheme.shapes
    }
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}
