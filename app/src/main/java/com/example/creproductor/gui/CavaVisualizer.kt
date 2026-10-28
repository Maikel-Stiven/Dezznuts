package com.example.creproductor.gui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.withFrameNanos
import com.example.creproductor.data.CavaSmoother
import com.example.creproductor.data.Spectrum
import com.example.creproductor.service.SpectrumTap
import kotlin.math.PI
import kotlin.math.sin

/** El audio llega al espía antes de sonar (búfer de salida): se dibuja con este retraso para que las barras cuadren. ponytail: calibrable por dispositivo */
private const val OUTPUT_LATENCY_NANOS = 110_000_000L

/**
 * Visualizador tipo CAVA: barras de frecuencia logarítmica que suben de golpe y caen con gravedad, con degradado del
 * color principal al secundario. Lee el espectro que calcula [com.example.creproductor.service.SpectrumTapProcessor].
 * Sin datos (offload, pausa o audio aún sin empezar) las barras se aquietan en vez de congelarse.
 */
@Composable
fun CavaVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val lowEnd = !rememberVisualEffectsEnabled()
    val count = if (lowEnd) 24 else 48
    val smoother = remember(count) { CavaSmoother(count) }
    val target = remember(count) { FloatArray(count) }
    val bars = remember { Spectrum(2048) }
    var tick by remember { mutableLongStateOf(0L) }
    val minGap = if (lowEnd) 33_000_000L else 0L   // en equipos modestos, 30 fps

    LaunchedEffect(isPlaying, count) {
        var last = 0L
        var idle = 0
        while (true) {
            val now = withFrameNanos { it }
            if (last != 0L && now - last < minGap) continue
            val dt = if (last == 0L) 0.016f else ((now - last) / 1e9f).coerceAtMost(0.05f)
            last = now
            val frame = if (isPlaying) SpectrumTap.latest(System.nanoTime() - OUTPUT_LATENCY_NANOS) else null
            if (frame != null) {
                System.arraycopy(frame.magnitudes, 0, bars.magnitudes, 0, bars.magnitudes.size)
                bars.bars(frame.sampleRate, target)
            } else target.fill(0f)
            smoother.step(target, dt)
            tick = now
            // Todo en silencio y sin audio: deja de repintar hasta que algo cambie.
            if (frame == null && smoother.shown.all { it < 0.003f }) {
                if (++idle > 20) { kotlinx.coroutines.delay(250); idle = 20 }
            } else idle = 0
        }
    }

    val c1 = MaterialTheme.colorScheme.primary
    val c2 = MaterialTheme.colorScheme.tertiary
    Canvas(modifier.fillMaxSize()) {
        tick   // se lee aquí: solo se repinta el lienzo, no se recompone nada
        val n = smoother.count
        val gap = 3.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        val brush = Brush.verticalGradient(listOf(c1.copy(alpha = 0.95f), c2.copy(alpha = 0.55f)), startY = 0f, endY = size.height)
        for (i in 0 until n) {
            val h = (smoother.shown[i] * size.height).coerceAtLeast(2.dp.toPx())
            drawRoundRect(
                brush = brush, topLeft = Offset(i * (w + gap), size.height - h), size = Size(w, h),
                cornerRadius = CornerRadius(w / 2, w / 2)
            )
        }
    }
}
