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

    /** Presets listos (5 bandas, mismas frecuencias que [DEFAULT]); "Plano" es el primero. */
    val PRESETS: List<Pair<String, List<EqBand>>> = listOf(
        "Plano" to DEFAULT,
        "Graves" to listOf(EqBand(60f, 6f, 0.9f), EqBand(230f, 3f, 1f), EqBand(910f, 0f, 1f), EqBand(3600f, 0f, 1f), EqBand(14000f, 0f, 1f)),
        "Voces" to listOf(EqBand(60f, -2f, 1f), EqBand(230f, -1f, 1f), EqBand(910f, 3f, 1.1f), EqBand(3600f, 4f, 1.2f), EqBand(14000f, 1f, 1f)),
        "Brillo" to listOf(EqBand(60f, 0f, 1f), EqBand(230f, 0f, 1f), EqBand(910f, 1f, 1f), EqBand(3600f, 3f, 1f), EqBand(14000f, 6f, 0.8f)),
        "Loudness" to listOf(EqBand(60f, 5f, 0.9f), EqBand(230f, 1f, 1f), EqBand(910f, -2f, 1f), EqBand(3600f, 1f, 1f), EqBand(14000f, 5f, 0.9f))
    )

    /** Nombre del preset que coincide con `bands`, o `null` si son ajustes propios ("Personalizado"). */
    fun presetNameOf(bands: List<EqBand>): String? = PRESETS.firstOrNull { (_, p) ->
        p.size == bands.size && p.zip(bands).all { (a, b) ->
            abs(a.freqHz - b.freqHz) < 1f && abs(a.gainDb - b.gainDb) < 0.05f && abs(a.q - b.q) < 0.05f
        }
    }?.first

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

    private companion object { const val MAX_BANDS = 16 }


    private class Coeffs(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double)

    /** Estado (Direct Form I) de un filtro para un canal. */
    private class State { var x1 = 0.0; var x2 = 0.0; var y1 = 0.0; var y2 = 0.0 }

    /**
     * Una entrada por banda (`null` = banda plana, se omite). Se publica de una vez (inmutable + `@Volatile`) y los
     * estados tienen tamaño fijo: así el hilo de audio nunca ve una combinación de tamaños distintos mientras la
     * interfaz cambia las bandas.
     */
    @Volatile private var filters: Array<Coeffs?> = emptyArray()
    @Volatile private var preamp = 1.0f
    private val states = Array(MAX_BANDS) { Array(channels) { State() } }   // [banda][canal]

    /** Aplica las bandas (las de ganancia ≈ 0 se omiten) y fija el preamp automático = −(mayor subida). */
    fun setBands(bands: List<EqBand>) {
        val list = bands.take(MAX_BANDS)
        val prev = filters
        val next = Array(list.size) { i -> if (abs(list[i].gainDb) >= 0.05f) coeffs(list[i]) else null }
        // Una banda que pasa de plana a activa arranca con el estado limpio (sin restos de antes).
        for (i in next.indices) if (next[i] != null && (i >= prev.size || prev[i] == null)) resetBand(i)
        filters = next
        preamp = 10f.pow(-max(0f, list.maxOfOrNull { if (abs(it.gainDb) >= 0.05f) it.gainDb else 0f } ?: 0f) / 20f)
    }

    /** Ganancia lineal extra (p. ej. la subida de ReplayGain), aplicada junto al preamp. */
    @Volatile var extraGain = 1.0f

    private fun resetBand(b: Int) { states[b].forEach { s -> s.x1 = 0.0; s.x2 = 0.0; s.y1 = 0.0; s.y2 = 0.0 } }

    fun reset() { for (b in states.indices) resetBand(b) }

    /** Procesa en el sitio `frames` fotogramas intercalados (L R L R…). */
    fun process(buf: FloatArray, frames: Int) {
        val fs = filters
        val gain = preamp * extraGain
        if (gain == 1f && fs.all { it == null }) return
        for (f in 0 until frames) {
            for (c in 0 until channels) {
                val i = f * channels + c
                var x = buf[i].toDouble()
                for (b in fs.indices) {
                    val k = fs[b] ?: continue
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
        val c = PeakingFilter.coeffs(b, sampleRate)
        return Coeffs(c[0], c[1], c[2], c[3], c[4])
    }
}

/**
 * Filtro "peaking" RBJ. La gráfica del ecualizador y el procesador de audio comparten esta fórmula, así que
 * la curva que se dibuja es exactamente la que suena.
 */
object PeakingFilter {
    /** `[b0, b1, b2, a1, a2]` ya normalizados por a0. */
    fun coeffs(b: EqBand, sampleRate: Int): DoubleArray {
        val f = min(b.freqHz, sampleRate * 0.45f).toDouble()
        val a = 10.0.pow(b.gainDb / 40.0)
        val w0 = 2 * PI * f / sampleRate
        val alpha = sin(w0) / (2 * b.q)
        val a0 = 1 + alpha / a
        return doubleArrayOf(
            (1 + alpha * a) / a0,
            -2 * cos(w0) / a0,
            (1 - alpha * a) / a0,
            -2 * cos(w0) / a0,
            (1 - alpha / a) / a0
        )
    }

    /** Respuesta en dB de la cascada de bandas a `freqHz` (módulo de H(e^jw)). */
    fun responseDb(bands: List<EqBand>, freqHz: Float, sampleRate: Int = 48_000): Float =
        responseCurveDb(bands, floatArrayOf(freqHz), sampleRate)[0]

    /**
     * Respuesta en dB en muchas frecuencias a la vez; es lo que dibuja la curva en cada fotograma. Los coeficientes
     * se calculan **una vez por banda** (no por punto) y el seno/coseno de cada frecuencia una vez para todas las
     * bandas: con 160 puntos y 5 bandas son unas 10 veces menos funciones trigonométricas.
     */
    fun responseCurveDb(bands: List<EqBand>, freqs: FloatArray, sampleRate: Int = 48_000): FloatArray {
        val out = FloatArray(freqs.size)
        val active = bands.filter { abs(it.gainDb) >= 0.05f }
        if (active.isEmpty()) return out
        val coeffs = active.map { coeffs(it, sampleRate) }
        for (i in freqs.indices) {
            val w = 2 * PI * min(freqs[i], sampleRate * 0.49f) / sampleRate
            val c1 = cos(w); val s1 = sin(w)
            val c2 = c1 * c1 - s1 * s1; val s2 = 2 * s1 * c1          // cos(2w), sin(2w) sin llamar otra vez
            var db = 0.0
            for (k in coeffs) {
                // H = (b0 + b1 z⁻¹ + b2 z⁻²) / (1 + a1 z⁻¹ + a2 z⁻²) con z⁻ⁿ = cos(nw) − j·sin(nw)
                val nr = k[0] + k[1] * c1 + k[2] * c2; val ni = -(k[1] * s1 + k[2] * s2)
                val dr = 1 + k[3] * c1 + k[4] * c2;    val di = -(k[3] * s1 + k[4] * s2)
                db += 10 * kotlin.math.log10((nr * nr + ni * ni) / (dr * dr + di * di))
            }
            out[i] = db.toFloat()
        }
        return out
    }
}
