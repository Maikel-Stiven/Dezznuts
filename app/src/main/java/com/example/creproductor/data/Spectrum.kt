package com.example.creproductor.data

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Espectro para el visualizador tipo CAVA: FFT radix-2 con ventana de Hann y reparto de los bins en barras de
 * frecuencia logarítmica. Lógica pura (sin Android) para poder probarla.
 */
class Spectrum(val size: Int = 2048) {
    init { require(size >= 2 && size and (size - 1) == 0) { "size debe ser potencia de 2" } }

    private val window = FloatArray(size) { (0.5 - 0.5 * cos(2 * PI * it / (size - 1))).toFloat() }
    private val cosT = FloatArray(size / 2) { cos(2 * PI * it / size).toFloat() }
    private val sinT = FloatArray(size / 2) { -sin(2 * PI * it / size).toFloat() }
    private val re = FloatArray(size)
    private val im = FloatArray(size)
    /** Amplitud de cada bin 0..size/2, normalizada para que una senoidal de amplitud 1 dé ≈ 1. */
    val magnitudes = FloatArray(size / 2 + 1)

    /** [samples] = los últimos [size] valores (mono, −1..1), el más viejo primero. */
    fun analyze(samples: FloatArray) {
        for (i in 0 until size) { re[i] = samples[i] * window[i]; im[i] = 0f }
        // Orden de bits invertido
        var j = 0
        for (i in 1 until size) {
            var bit = size shr 1
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { val a = re[i]; re[i] = re[j]; re[j] = a; val b = im[i]; im[i] = im[j]; im[j] = b }
        }
        var len = 2
        while (len <= size) {
            val half = len / 2; val step = size / len
            var i = 0
            while (i < size) {
                for (k in 0 until half) {
                    val wr = cosT[k * step]; val wi = sinT[k * step]
                    val a = i + k; val b = a + half
                    val tr = re[b] * wr - im[b] * wi; val ti = re[b] * wi + im[b] * wr
                    re[b] = re[a] - tr; im[b] = im[a] - ti
                    re[a] += tr; im[a] += ti
                }
                i += len
            }
            len = len shl 1
        }
        // Hann: ganancia coherente 0,5 → una senoidal de amplitud A da |X| = A·N/4.
        val norm = 4f / size
        for (k in 0..size / 2) magnitudes[k] = sqrt(re[k] * re[k] + im[k] * im[k]) * norm
    }

    /**
     * Reparte [magnitudes] en [out].size barras entre [fMin] y [fMax] Hz (escala logarítmica, como CAVA) y las lleva a
     * 0..1 por dB (de −[floorDb] a 0), con una ligera subida hacia los agudos para que no domine solo el grave.
     */
    fun bars(sampleRate: Int, out: FloatArray, fMin: Float = 40f, fMax: Float = 16000f, floorDb: Float = 62f) {
        val n = out.size
        val maxF = minOf(fMax, sampleRate / 2f - 1f)
        val binHz = sampleRate.toFloat() / size
        for (i in 0 until n) {
            val lo = fMin * (maxF / fMin).pow(i / n.toFloat())
            val hi = fMin * (maxF / fMin).pow((i + 1) / n.toFloat())
            val b0 = (lo / binHz).toInt().coerceIn(1, size / 2)
            val b1 = (hi / binHz).toInt().coerceIn(b0, size / 2)
            var peak = 0f
            for (b in b0..b1) if (magnitudes[b] > peak) peak = magnitudes[b]
            val tilt = 9f * i / n   // dB extra hacia los agudos
            val db = 20f * log10(peak + 1e-7f) + tilt
            out[i] = ((db + floorDb) / floorDb).coerceIn(0f, 1f)
        }
    }
}

/**
 * Caída y suavizado de las barras como en CAVA: suben de golpe, bajan con "gravedad" y cada barra arrastra a las
 * vecinas (efecto "monstercat"). [step] se llama una vez por fotograma con el tiempo transcurrido.
 */
class CavaSmoother(val count: Int, private val fall: Float = 2.2f, private val spread: Float = 1.6f) {
    val shown = FloatArray(count)
    private val vel = FloatArray(count)

    fun step(target: FloatArray, dtSec: Float) {
        for (i in 0 until count) {
            val t = target[i]
            if (t >= shown[i]) { shown[i] = t; vel[i] = 0f }
            else { vel[i] += fall * dtSec; shown[i] = (shown[i] - vel[i] * dtSec * 2f).coerceAtLeast(0f) }
        }
        // Cada barra levanta a sus vecinas, más débil cuanto más lejos.
        for (i in 1 until count) shown[i] = maxOf(shown[i], shown[i - 1] / spread)
        for (i in count - 2 downTo 0) shown[i] = maxOf(shown[i], shown[i + 1] / spread)
    }
}
