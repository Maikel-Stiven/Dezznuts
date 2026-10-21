package com.example.creproductor.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import com.example.creproductor.data.SavedEqPresets
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.creproductor.data.EqBand
import com.example.creproductor.data.EqBands
import com.example.creproductor.data.HiFiAudio
import com.example.creproductor.data.PeakingFilter
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.roundToInt

private const val F_MIN = EqBands.MIN_FREQ
private const val F_MAX = EqBands.MAX_FREQ
private val GRID_FREQS = listOf(50f, 100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f)
private val GRID_DB = listOf(12, 6, 0, -6, -12)

/**
 * Ecualizador paramétrico a pantalla completa. Se maneja **sobre la curva**: cada banda es un nodo que se arrastra
 * (horizontal = frecuencia, vertical = ganancia); la curva es la respuesta real del filtro (misma fórmula que el
 * audio, ver [PeakingFilter]) y se actualiza mientras se arrastra y se oye en vivo.
 *
 * Apagado por defecto, para no tocar la señal bit-perfect. Sin efecto con audio offload.
 */
@Composable
fun EqualizerDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            EqualizerContent(onDismiss)
        }
    }
}

@Composable
private fun EqualizerContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(HiFiAudio.PREFS_NAME, android.content.Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean(HiFiAudio.KEY_EQ_ENABLED, false)) }
    val bands = remember { mutableStateListOf<EqBand>().apply { addAll(EqBands.decode(prefs.getString(HiFiAudio.KEY_EQ_BANDS, null))) } }
    var selected by remember { mutableIntStateOf(0) }

    fun save() = prefs.edit().putString(HiFiAudio.KEY_EQ_BANDS, EqBands.encode(bands)).apply()
    // "Saved": presets propios (máximo 5). Guardar aparece al tocar la curva; borrar pide confirmación.
    var saved by remember { mutableStateOf(SavedEqPresets.decode(prefs.getString(KEY_EQ_SAVED, null))) }
    fun persistSaved(list: List<Pair<String, List<EqBand>>>) { saved = list; prefs.edit().putString(KEY_EQ_SAVED, SavedEqPresets.encode(list)).apply() }
    var naming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<String?>(null) }
    val savedMatch = saved.firstOrNull { (_, b) -> EqBands.sameBands(b, bands) }?.first
    val preset = EqBands.presetNameOf(bands) ?: savedMatch

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cabecera
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar", Modifier.padding(0.dp)) }
            Column(Modifier.weight(1f)) {
                Text("Ecualizador", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    if (enabled) "Activo" else "Apagado · señal sin tocar",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                prefs.edit().putBoolean(HiFiAudio.KEY_EQ_ENABLED, it).apply()
            })
        }

        // Presets
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EqBands.PRESETS.forEach { (name, p) ->
                FilterChip(
                    selected = preset == name,
                    onClick = { bands.clear(); bands.addAll(p); save() },
                    label = { Text(name) }
                )
            }
            FilterChip(selected = preset == null, onClick = {}, enabled = false, label = { Text("Personalizado") })
        }

        // Saved
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Saved", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            saved.forEach { (name, p) ->
                FilterChip(
                    selected = preset == name,
                    onClick = { bands.clear(); bands.addAll(p); save() },
                    label = { Text(name) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Eliminar $name", Modifier.size(16.dp).clickable { deleting = name }) }
                )
            }
            if (preset == null && saved.size < SavedEqPresets.MAX) TextButton(onClick = { naming = true }) { Text("Guardar +") }
            if (saved.isEmpty() && preset != null) Text("Mueve la curva para guardar el tuyo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Curva
        EqCurve(
            bands = bands,
            selected = selected,
            active = enabled,
            onSelect = { selected = it },
            onChange = { i, band -> bands[i] = band },
            onSave = { save() }
        )

        // Banda seleccionada
        val band = bands.getOrNull(selected)
        if (band != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Readout("Banda", "${selected + 1}", Modifier.weight(0.85f))
                Readout("Frecuencia", formatHz(band.freqHz), Modifier.weight(1.2f))
                Readout("Ganancia", "%+.1f dB".format(band.gainDb), Modifier.weight(1.2f))
                Readout("Q", "%.1f".format(band.q), Modifier.weight(0.7f))
            }
            Column {
                Text("Ancho de banda (Q)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    modifier = Modifier.semantics { contentDescription = "Ancho de banda Q de la banda ${selected + 1}" },
                    value = band.q,
                    onValueChange = { bands[selected] = band.copy(q = it) },
                    onValueChangeFinished = { save() },
                    valueRange = EqBands.MIN_Q..EqBands.MAX_Q
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Arrastra los puntos sobre la curva. Sin efecto con audio offload.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { bands.clear(); bands.addAll(EqBands.DEFAULT); save() }) { Text("Restablecer") }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (naming) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { naming = false },
            title = { Text("Guardar preset") },
            text = { OutlinedTextField(value = name, onValueChange = { name = SavedEqPresets.cleanName(it) }, label = { Text("Nombre") }, singleLine = true) },
            confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { SavedEqPresets.add(saved, name, bands.toList())?.let(::persistSaved); naming = false }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { naming = false }) { Text("Cancelar") } }
        )
    }
    deleting?.let { name ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("¿Eliminar el preset?") },
            text = { Text("\"$name\" se borrará y quedará libre su espacio (máximo ${SavedEqPresets.MAX}).") },
            confirmButton = { TextButton(onClick = { persistSaved(SavedEqPresets.remove(saved, name)); deleting = null }) { Text("Sí") } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("No") } }
        )
    }
}

private const val KEY_EQ_SAVED = "eq_saved_presets"

@Composable
private fun Readout(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

private fun formatHz(f: Float) = if (f >= 1000f) "%.1f kHz".format(f / 1000f).replace(".0 ", " ") else "${f.roundToInt()} Hz"

/** Gráfica interactiva: rejilla, curva con relleno y brillo, y un nodo por banda. */
@Composable
private fun EqCurve(
    bands: List<EqBand>,
    selected: Int,
    active: Boolean,
    onSelect: (Int) -> Unit,
    onChange: (Int, EqBand) -> Unit,
    onSave: () -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    // Se miden ~20 textos distintos por fotograma: con la caché por defecto (8) se volvían a maquetar todos cada vez.
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val primary = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val gridStrong = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val surface = MaterialTheme.colorScheme.background
    val curveColor = if (active) primary else labelColor
    val padL = with(density) { 34.dp.toPx() }
    val padR = with(density) { 12.dp.toPx() }
    val padT = with(density) { 14.dp.toPx() }
    val padB = with(density) { 22.dp.toPx() }
    val hit = with(density) { 44.dp.toPx() }

    var size by remember { mutableStateOf(IntSize.Zero) }
    var lastSave by remember { mutableLongStateOf(0L) }
    var lastSign by remember { mutableIntStateOf(0) }
    val plotW = (size.width - padL - padR).coerceAtLeast(1f)
    val plotH = (size.height - padT - padB).coerceAtLeast(1f)
    val lnMin = ln(F_MIN); val lnMax = ln(F_MAX)

    fun fx(f: Float) = padL + (ln(f) - lnMin) / (lnMax - lnMin) * plotW
    fun gy(g: Float) = padT + (1f - (g + 12f) / 24f) * plotH
    fun xToF(x: Float) = exp(lnMin + ((x - padL) / plotW).coerceIn(0f, 1f) * (lnMax - lnMin))
    fun yToG(y: Float) = 12f - ((y - padT) / plotH).coerceIn(0f, 1f) * 24f

    // Los gestos viven en un bloque que no se reinicia al recomponer: todo lo que leen (tamaño, selección, bandas)
    // se entrega por `rememberUpdatedState` para que nunca use valores viejos.
    val nearest by rememberUpdatedState<(Offset) -> Int?> { p ->
        bands.indices.minByOrNull { hypot(fx(bands[it].freqHz) - p.x, gy(bands[it].gainDb) - p.y) }
            ?.takeIf { hypot(fx(bands[it].freqHz) - p.x, gy(bands[it].gainDb) - p.y) <= hit }
    }
    var dragging by remember { mutableIntStateOf(-1) }
    val moveBand by rememberUpdatedState<(Int, Offset) -> Unit> { i, p ->
        // El paso de 0,5 dB y el imán en 0 dB hacen que sea fácil dejar una banda plana.
        var g = (yToG(p.y) * 2f).roundToInt() / 2f
        if (abs(g) < 0.4f) g = 0f
        val sign = if (g > 0f) 1 else if (g < 0f) -1 else 0
        if (sign != lastSign) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); lastSign = sign }
        onChange(i, bands[i].copy(freqHz = xToF(p.x), gainDb = g))
        val now = System.currentTimeMillis()
        if (now - lastSave > 80) { lastSave = now; onSave() }   // se oye mientras arrastras, sin escribir en cada píxel
    }
    val select by rememberUpdatedState(onSelect)
    val save by rememberUpdatedState(onSave)

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .onSizeChanged { size = it }
            .semantics { contentDescription = "Curva del ecualizador: arrastra los puntos para cambiar frecuencia y ganancia" }
            .pointerInput(Unit) {
                detectTapGestures { p -> nearest(p)?.let { select(it) } }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { p ->
                        dragging = nearest(p) ?: -1
                        if (dragging >= 0) { select(dragging); lastSign = Int.MIN_VALUE }
                    },
                    onDrag = { change, _ -> change.consume(); if (dragging >= 0) moveBand(dragging, change.position) },
                    onDragEnd = { dragging = -1; save() },
                    onDragCancel = { dragging = -1; save() }
                )
            }
    ) {
        val label = TextStyle(fontSize = 10.sp, color = labelColor)

        // Rejilla horizontal (dB) y vertical (Hz)
        GRID_DB.forEach { db ->
            val y = gy(db.toFloat())
            drawLine(if (db == 0) gridStrong else grid, Offset(padL, y), Offset(padL + plotW, y), strokeWidth = if (db == 0) 1.5f else 1f)
            text(measurer, if (db > 0) "+$db" else "$db", Offset(4f, y), label, centerY = true)
        }
        GRID_FREQS.forEach { f ->
            val x = fx(f)
            drawLine(grid, Offset(x, padT), Offset(x, padT + plotH), strokeWidth = 1f)
            text(measurer, if (f >= 1000f) "${(f / 1000).toInt()}k" else "${f.toInt()}", Offset(x, size.height - padB + 4f), label, centerX = true)
        }

        // Curva: respuesta real del filtro en 160 puntos logarítmicos
        val n = 160
        val xs = FloatArray(n) { padL + it * plotW / (n - 1) }
        val db = PeakingFilter.responseCurveDb(bands, FloatArray(n) { xToF(xs[it]) })
        val ys = FloatArray(n) { gy(db[it].coerceIn(-12f, 12f)) }
        val y0 = gy(0f)
        val line = Path().apply { moveTo(xs[0], ys[0]); for (i in 1 until n) lineTo(xs[i], ys[i]) }
        val fill = Path().apply {
            moveTo(xs[0], y0); for (i in 0 until n) lineTo(xs[i], ys[i]); lineTo(xs[n - 1], y0); close()
        }
        val alpha = if (active) 1f else 0.5f
        drawPath(fill, Brush.verticalGradient(listOf(curveColor.copy(alpha = 0.40f * alpha), curveColor.copy(alpha = 0.02f)), startY = padT, endY = padT + plotH))
        drawPath(line, curveColor.copy(alpha = 0.20f * alpha), style = Stroke(width = 14f, cap = StrokeCap.Round))   // brillo
        drawPath(line, curveColor.copy(alpha = alpha), style = Stroke(width = 5f, cap = StrokeCap.Round))

        // Nodos
        bands.forEachIndexed { i, b ->
            val c = Offset(fx(b.freqHz), gy(b.gainDb))
            val isSel = i == selected
            if (isSel) drawCircle(curveColor.copy(alpha = 0.22f), radius = 34f, center = c)
            drawCircle(surface, radius = if (isSel) 20f else 15f, center = c)
            drawCircle(curveColor, radius = if (isSel) 20f else 15f, center = c, style = Stroke(width = if (isSel) 5f else 3.5f))
            text(measurer, "${i + 1}", c, TextStyle(fontSize = if (isSel) 12.sp else 10.sp, color = curveColor, fontWeight = FontWeight.Bold), centerX = true, centerY = true)
        }

        // Etiqueta flotante de la banda seleccionada
        bands.getOrNull(selected)?.let { b ->
            val c = Offset(fx(b.freqHz), gy(b.gainDb))
            val t = "${formatHz(b.freqHz)} · ${"%+.1f".format(b.gainDb)} dB"
            val m = measurer.measure(t, TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = surface))
            val w = m.size.width + 24f; val h = m.size.height + 12f
            val x = (c.x - w / 2).coerceIn(2f, size.width - w - 2f)
            val y = if (c.y - 50f - h > 0f) c.y - 50f - h + 20f else c.y + 36f
            drawRoundRect(curveColor, Offset(x, y), Size(w, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
            drawText(m, topLeft = Offset(x + 12f, y + 6f))
        }
    }
}

/** Dibuja texto en el lienzo, opcionalmente centrado respecto al punto dado. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.text(
    measurer: TextMeasurer, text: String, at: Offset, style: TextStyle, centerX: Boolean = false, centerY: Boolean = false
) {
    val m = measurer.measure(text, style)
    drawText(m, topLeft = Offset(at.x - if (centerX) m.size.width / 2f else 0f, at.y - if (centerY) m.size.height / 2f else 0f))
}
