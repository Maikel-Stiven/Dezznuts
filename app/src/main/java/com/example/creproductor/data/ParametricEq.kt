// Ecualizador paramétrico: filtros "peaking" biquad (fórmulas RBJ del Audio EQ Cookbook).
//
// Lógica pura y sin dependencias de Android, así que se prueba con tests unitarios midiendo la
// ganancia real sobre tonos puros. El procesador de audio (service/ParametricEqProcessor) solo
// convierte formatos y llama a [ParametricEq.process].

package com.example.creproductor.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Una banda: frecuencia central (Hz), ganancia (dB) y factor de calidad Q (ancho de banda). */
data class EqBand(val freqHz: Float, val gainDb: Float, val q: Float)

object EqBands {
    const val MIN_GAIN_DB = -12f
    const val MAX_GAIN_DB = 12f
    const val MIN_Q = 0.3f
    const val MAX_Q = 4f
    const val MIN_FREQ = 20f
    const val MAX_FREQ = 20_000f

    /** Cinco bandas planas repartidas por el espectro (graves, medios, agudos). */
    val DEFAULT = listOf(
        EqBand(60f, 0f, 1f), EqBand(230f, 0f, 1f), EqBand(910f, 0f, 1f),
        EqBand(3600f, 0f, 1f), EqBand(14000f, 0f, 1f)
    )

    /** `"60:0.0:1.0;230:3.5:1.2;…"`. */
    fun encode(bands: List<EqBand>): String = bands.joinToString(";") { "${it.freqHz}:${it.gainDb}:${it.q}" }

    /** Tolerante: ignora bandas mal formadas y limita los valores; sin datos válidos devuelve [DEFAULT]. */
    fun decode(raw: String?): List<EqBand> {
        val parsed = raw.orEmpty().split(";").mapNotNull { item ->
            val p = item.split(":")
            val f = p.getOrNull(0)?.toFloatOrNull() ?: return@mapNotNull null
            val g = p.getOrNull(1)?.toFloatOrNull() ?: return@mapNotNull null
            val q = p.getOrNull(2)?.toFloatOrNull() ?: return@mapNotNull null
            EqBand(f.coerceIn(MIN_FREQ, MAX_FREQ), g.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB), q.coerceIn(MIN_Q, MAX_Q))
        }
        return parsed.ifEmpty { DEFAULT }
    }
}

class ParametricEq(private val sampleRate: Int, private val channels: Int) {

    private class Coeffs(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double)

    /** Estado (Direct Form I) de un filtro para un canal. */
    private class State { var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0 }

    /** Coeficientes inmutables publicados de una vez: el hilo de audio nunca ve una banda a medias. */
    @Volatile private var filters: Array<Coeffs> = emptyArray()
    @Volatile private var preamp = 1.0f
    private var states: Array<Array<State>> = emptyArray()   // [banda][canal]

    /** Aplica las bandas (las de ganancia ≈ 0 se omiten) y fija el preamp automático = −(mayor subida). */
    fun setBands(bands: List<EqBand>) {
        val active = bands.filter { abs(it.gainDb) >= 0.05f }
        filters = active.map { coeffs(it) }.toTypedArray()
        preamp = 10f.pow(-max(0f, active.maxOfOrNull { it.gainDb } ?: 0f) / 20f)
        if (states.size != active.size) states = Array(active.size) { Array(channels) { State() } }
    }

    /** Ganancia lineal extra (p. ej. la subida de ReplayGain), aplicada junto al preamp. */
    @Volatile var extraGain = 1.0f

    fun reset() { states.forEach { band -> band.forEach { s -> s.x1 = 0.0; s.x2 = 0.0; s.y1 = 0.0; s.y2 = 0.0 } } }

    /** Procesa en el sitio `frames` fotogramas intercalados (L R L R…). */
    fun process(buf: FloatArray, frames: Int) {
        val fs = filters
        val gain = preamp * extraGain
        if (fs.isEmpty() && gain == 1f) return
        for (f in 0 until frames) {
            for (c in 0 until channels) {
                val i = f * channels + c
                var x = buf[i].toDouble()
                for (b in fs.indices) {
                    val k = fs[b]
                    val s = states[b][c]
                    val y = k.b0 * x + k.b1 * s.x1 + k.b2 * s.x2 - k.a1 * s.y1 - k.a2 * s.y2
                    s.x2 = s.x1; s.x1 = x; s.y2 = s.y1; s.y1 = y
                    x = y
                }
                buf[i] = (x * gain).toFloat()
            }
        }
    }

    private fun coeffs(b: EqBand): Coeffs {
        val f = min(b.freqHz, sampleRate * 0.45f).toDouble()
        val a = 10.0.pow(b.gainDb / 40.0)
        val w0 = 2 * PI * f / sampleRate
        val alpha = sin(w0) / (2 * b.q)
        val a0 = 1 + alpha / a
        return Coeffs(
            b0 = (1 + alpha * a) / a0,
            b1 = -2 * cos(w0) / a0,
            b2 = (1 - alpha * a) / a0,
            a1 = -2 * cos(w0) / a0,
            a2 = (1 - alpha / a) / a0
        )
    }
}
