package com.example.creproductor.gui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.creproductor.i18n.tr
import com.example.creproductor.ui.theme.ThemeMode
import com.example.creproductor.ui.theme.ThemeSetting
import com.example.creproductor.ui.theme.previewCorner
import com.example.creproductor.ui.theme.swatch

/** Acentos rápidos (ARGB). El primero, `null`, deja el del propio tema. */
private val ACCENT_PRESETS: List<Pair<String, Int?>> = listOf(
    "Del tema" to null,
    "Morado" to 0xFFBB86FC.toInt(), "Azul" to 0xFF5C9DFF.toInt(), "Turquesa" to 0xFF03DAC6.toInt(),
    "Verde" to 0xFF7BE08A.toInt(), "Lima" to 0xFFC6E85C.toInt(), "Ámbar" to 0xFFFFC857.toInt(),
    "Naranja" to 0xFFFF8A50.toInt(), "Rojo" to 0xFFFF5A5F.toInt(), "Rosa" to 0xFFFF8FB1.toInt(), "Blanco" to 0xFFF2F2F2.toInt()
)

/**
 * Selector de tema: tarjetas con una vista previa en miniatura de cada tema (barra, portada, lista y reproductor) y,
 * si [showAccent], el selector de color de acento con colores rápidos y uno a medida.
 */
@Composable
fun ThemePicker(showAccent: Boolean = true, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val modes = ThemeMode.entries.filter { it != ThemeMode.DYNAMIC || ThemeSetting.dynamicAvailable }
    val accentOverride = ThemeSetting.accent?.let(::Color)
    Column(modifier) {
        // Al abrir, la lista ya muestra el tema elegido (el primero y el último quedan en los bordes).
        val listState = androidx.compose.foundation.lazy.rememberLazyListState(
            initialFirstVisibleItemIndex = (modes.indexOf(ThemeSetting.mode) - 1).coerceAtLeast(0)
        )
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(modes, key = { it.name }) { m ->
                ThemeCard(m, selected = ThemeSetting.mode == m, accentOverride = accentOverride) { ThemeSetting.set(context, m) }
            }
        }
        if (showAccent) {
            Text(
                tr("Color de acento"), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
            )
            AccentRow()
        }
    }
}

@Composable
private fun ThemeCard(mode: ThemeMode, selected: Boolean, accentOverride: Color?, onClick: () -> Unit) {
    val (bg, themeAccent) = mode.swatch()
    val accent = accentOverride ?: themeAccent
    val ink = if (bg.luminance() > 0.5f) Color(0xFF1A1A1A) else Color(0xFFF2F2F2)
    val corner = mode.previewCorner().dp
    val name = tr(mode.label)
    Column(
        Modifier.width(108.dp).clickable(onClickLabel = name, onClick = onClick)
            .semantics { contentDescription = name; this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(corner + 6.dp)).background(bg)
                .border(
                    BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) accent else ink.copy(alpha = 0.18f)),
                    RoundedCornerShape(corner + 6.dp)
                )
        ) {
            Column(Modifier.fillMaxWidth().fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Barra superior
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(width = 10.dp, height = 3.dp).background(ink.copy(alpha = 0.7f)))
                    Box(Modifier.weight(1f).height(3.dp).background(accent.copy(alpha = 0.9f), RoundedCornerShape(corner / 2)))
                }
                // Portada + dos líneas
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(34.dp).background(accent, RoundedCornerShape(corner / 2 + 2.dp)))
                    Column(Modifier.weight(1f).padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.fillMaxWidth(0.9f).height(4.dp).background(ink.copy(alpha = 0.8f), RoundedCornerShape(2.dp)))
                        Box(Modifier.fillMaxWidth(0.55f).height(3.dp).background(ink.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                    }
                }
                // Filas de lista
                repeat(2) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(14.dp).background(ink.copy(alpha = 0.14f), RoundedCornerShape(corner / 3 + 1.dp)))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.width(44.dp).height(3.dp).background(ink.copy(alpha = 0.6f), RoundedCornerShape(2.dp)))
                            Box(Modifier.width(28.dp).height(2.dp).background(ink.copy(alpha = 0.3f), RoundedCornerShape(2.dp)))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                // Mini reproductor
                Row(
                    Modifier.fillMaxWidth().background(ink.copy(alpha = 0.1f), RoundedCornerShape(corner / 2 + 2.dp)).padding(5.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(Modifier.weight(1f).height(3.dp).background(ink.copy(alpha = 0.5f), RoundedCornerShape(2.dp)))
                    Box(Modifier.size(11.dp).background(accent, CircleShape))
                }
            }
            if (selected) Box(
                Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp).background(accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = if (accent.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(12.dp))
            }
        }
        Text(
            name, style = MaterialTheme.typography.labelMedium, maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun AccentRow() {
    val context = LocalContext.current
    val current = ThemeSetting.accent
    var picking by remember { mutableStateOf(false) }
    val isPreset = ACCENT_PRESETS.any { it.second == current }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(ACCENT_PRESETS, key = { it.first }) { (label, argb) ->
            val sel = current == argb
            val c = argb?.let(::Color)
            val name = tr(label)
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(c ?: MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(onClickLabel = name) { ThemeSetting.setAccent(context, argb) }
                    .semantics { contentDescription = name; this.selected = sel },
                contentAlignment = Alignment.Center
            ) {
                if (c == null) Text("∅", color = MaterialTheme.colorScheme.onSurfaceVariant)
                else if (sel) Icon(Icons.Default.Check, null, tint = if (c.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(18.dp))
            }
        }
        item {
            val custom = current != null && !isPreset
            val name = tr("Color a medida")
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                    .border(if (custom) 3.dp else 1.dp, if (custom) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                    .clickable(onClickLabel = name) { picking = true }
                    .semantics { contentDescription = name; this.selected = custom },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
    if (picking) AccentPickerDialog(
        initial = current?.let(::Color) ?: MaterialTheme.colorScheme.primary,
        onPick = { ThemeSetting.setAccent(context, it.toArgb()); picking = false },
        onDismiss = { picking = false }
    )
}

/** Color a medida con tres deslizadores (tono, intensidad y brillo) y vista previa en vivo. */
@Composable
private fun AccentPickerDialog(initial: Color, onPick: (Color) -> Unit, onDismiss: () -> Unit) {
    val hsv = remember(initial) { FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) } }
    var h by remember { mutableFloatStateOf(hsv[0]) }
    var s by remember { mutableFloatStateOf(hsv[1].coerceAtLeast(0.15f)) }
    var v by remember { mutableFloatStateOf(hsv[2].coerceAtLeast(0.35f)) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v)))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Color a medida")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(color), contentAlignment = Alignment.Center) {
                    Text(
                        "#%06X".format(color.toArgb() and 0xFFFFFF), fontWeight = FontWeight.Bold,
                        color = if (color.luminance() > 0.5f) Color.Black else Color.White
                    )
                }
                GradientSlider(tr("Tono"), h / 360f, (0..12).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it * 30f, 1f, 1f))) }) { h = it * 360f }
                GradientSlider(tr("Intensidad"), s, listOf(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 0f, v))), Color(android.graphics.Color.HSVToColor(floatArrayOf(h, 1f, v))))) { s = it }
                GradientSlider(tr("Brillo"), v, listOf(Color.Black, Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, 1f))))) { v = it }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(color) }) { Text(tr("Aplicar")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } }
    )
}

@Composable
private fun GradientSlider(label: String, value: Float, track: List<Color>, onChange: (Float) -> Unit) {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box(Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Brush.horizontalGradient(track)))
        Slider(
            value = value, onValueChange = onChange,
            colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent, thumbColor = Color.White)
        )
    }
}
