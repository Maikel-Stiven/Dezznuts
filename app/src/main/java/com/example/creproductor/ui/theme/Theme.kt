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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance

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

/**
 * Esquema oscuro a partir de un fondo y dos acentos: las superficies suben desde el fondo hacia el tono del texto, así
 * cada tema nuevo cuesta tres colores y no veinte. [text] es el color del texto principal.
 */
private fun tinted(bg: Long, text: Long, primary: Long, secondary: Long, onPrimary: Long = 0xFF101010) = run {
    val b = Color(bg); val t = Color(text); val p = Color(primary); val sec = Color(secondary)
    fun up(f: Float) = lerp(b, t, f)
    darkColorScheme(
        primary = p, onPrimary = Color(onPrimary),
        primaryContainer = lerp(b, p, 0.28f), onPrimaryContainer = lerp(p, Color.White, 0.55f),
        secondary = sec, onSecondary = Color(onPrimary),
        secondaryContainer = lerp(b, sec, 0.28f), onSecondaryContainer = lerp(sec, Color.White, 0.55f),
        tertiary = lerp(p, sec, 0.5f),
        background = b, onBackground = t, surface = b, onSurface = t,
        surfaceVariant = up(0.12f), onSurfaceVariant = up(0.68f),
        surfaceContainerLowest = lerp(b, Color.Black, 0.35f), surfaceContainerLow = up(0.04f), surfaceContainer = up(0.07f),
        surfaceContainerHigh = up(0.10f), surfaceContainerHighest = up(0.14f),
        outline = up(0.42f), outlineVariant = up(0.2f)
    )
}

private val SakuraColors = tinted(0xFF160E12, 0xFFF6E8EE, 0xFFFF8FB1, 0xFFC9A7FF, 0xFF3A0F20)
private val OceanColors = tinted(0xFF0A1218, 0xFFE3F1F8, 0xFF5CC8F5, 0xFF4FE3C1, 0xFF00263A)
private val SynthwaveColors = tinted(0xFF120B1F, 0xFFF1E9FF, 0xFFFF4FD8, 0xFF3DE1FF, 0xFF2B0033)
private val EmberColors = tinted(0xFF140D0A, 0xFFF5E9E2, 0xFFFF7A45, 0xFFFFC857, 0xFF3A1200)

/** Tema de la app. DEZZNUTS (morado) es el de siempre; los demás son opciones en Opciones. */
enum class ThemeMode(val label: String) {
    DEZZ("DEZZNUTS"), OMARCHY("Omarchy"), PREMIUM("Premium"), SAKURA("Sakura"), OCEAN("Océano"), SYNTHWAVE("Synthwave"), EMBER("Brasa"), AMOLED("Negro AMOLED"), LIGHT("Claro"), DYNAMIC("Material You")
}

/** Fondo y acento de cada tema, para elegirlo de un vistazo sin abrir nada. */
fun ThemeMode.swatch(): Pair<Color, Color> = when (this) {
    ThemeMode.DEZZ -> Color(0xFF121212) to Color(0xFFBB86FC)
    ThemeMode.OMARCHY -> Color(0xFF0B100C) to Color(0xFF8FE388)
    ThemeMode.PREMIUM -> Color(0xFF0F0D0C) to Color(0xFFF2B36B)
    ThemeMode.SAKURA -> Color(0xFF160E12) to Color(0xFFFF8FB1)
    ThemeMode.OCEAN -> Color(0xFF0A1218) to Color(0xFF5CC8F5)
    ThemeMode.SYNTHWAVE -> Color(0xFF120B1F) to Color(0xFFFF4FD8)
    ThemeMode.EMBER -> Color(0xFF140D0A) to Color(0xFFFF7A45)
    ThemeMode.AMOLED -> Color.Black to Color(0xFFBB86FC)
    ThemeMode.LIGHT -> Color(0xFFFBF9FD) to Color(0xFF6A3FC4)
    ThemeMode.DYNAMIC -> Color(0xFF1F1F1F) to Color(0xFF8AB4F8)
}

/** Esquinas del tema (dp) para dibujar su vista previa; Omarchy es el único recto. */
fun ThemeMode.previewCorner() = if (this == ThemeMode.OMARCHY) 0 else 8

object ThemeSetting {
    private const val KEY = "theme_mode"
    private const val KEY_ACCENT = "accent_color"
    var mode by mutableStateOf(ThemeMode.DEZZ)
        private set

    /** Color de acento propio (ARGB) que sustituye al del tema, o `null` para el del tema. */
    var accent by mutableStateOf<Int?>(null)
        private set

    fun load(context: Context) {
        val prefs = context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE)
        val name = prefs.getString(KEY, null)
        mode = ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.DEZZ
        accent = if (prefs.contains(KEY_ACCENT)) prefs.getInt(KEY_ACCENT, 0) else null
    }

    fun setAccent(context: Context, argb: Int?) {
        accent = argb
        context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).edit().apply {
            if (argb == null) remove(KEY_ACCENT) else putInt(KEY_ACCENT, argb)
        }.apply()
    }

    fun set(context: Context, value: ThemeMode) {
        mode = value
        context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).edit().putString(KEY, value.name).apply()
    }

    /** Material You necesita Android 12+. */
    val dynamicAvailable get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/**
 * Pone [accent] como color principal de cualquier tema y le saca los contenedores y textos a juego (con fondo oscuro o
 * claro). Idea del selector de acento de Maikel (rama temas/interfaces).
 */
fun androidx.compose.material3.ColorScheme.withAccent(accent: Color, light: Boolean): androidx.compose.material3.ColorScheme {
    val onAccent = if (accent.luminance() > 0.5f) Color(0xFF101010) else Color.White
    val container = lerp(surface, accent, if (light) 0.22f else 0.30f)
    val onContainer = if (light) lerp(accent, Color.Black, 0.6f) else lerp(accent, Color.White, 0.55f)
    return copy(
        primary = accent, onPrimary = onAccent, primaryContainer = container, onPrimaryContainer = onContainer,
        secondary = accent, onSecondary = onAccent, secondaryContainer = container, onSecondaryContainer = onContainer,
        tertiary = accent
    )
}

@Composable
fun DEZZNUTStheme(content: @Composable () -> Unit){
    val context = LocalContext.current
    val mode = ThemeSetting.mode
    val base = when {
        mode == ThemeMode.OMARCHY -> OmarchyColors
        mode == ThemeMode.PREMIUM -> PremiumColors
        mode == ThemeMode.SAKURA -> SakuraColors
        mode == ThemeMode.OCEAN -> OceanColors
        mode == ThemeMode.SYNTHWAVE -> SynthwaveColors
        mode == ThemeMode.EMBER -> EmberColors
        mode == ThemeMode.AMOLED -> AmoledColors
        mode == ThemeMode.LIGHT -> LightColors
        mode == ThemeMode.DYNAMIC && ThemeSetting.dynamicAvailable -> dynamicDarkColorScheme(context)
        else -> DezzColors
    }
    val accent = ThemeSetting.accent
    val scheme = if (accent != null) base.withAccent(Color(accent), light = mode == ThemeMode.LIGHT) else base
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
