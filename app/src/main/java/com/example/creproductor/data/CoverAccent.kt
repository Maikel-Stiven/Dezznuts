package com.example.creproductor.data

import kotlin.math.max
import kotlin.math.min

/**
 * Color de acento a partir de una carátula reducida (24×24 basta): el tono que más "pesa" (saturación × brillo),
 * llevado a un brillo legible sobre fondo oscuro. Devuelve null si la carátula es casi gris: entonces se usa el
 * color de la marca.
 */
object CoverAccent {
    private const val BINS = 12

    fun fromPixels(pixels: IntArray): Int? {
        val weight = FloatArray(BINS)
        val sumS = FloatArray(BINS)
        val sumX = FloatArray(BINS)   // el tono se promedia como vector para no partir el rojo (0°/360°)
        val sumY = FloatArray(BINS)
        var total = 0f
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            val (h, s, v) = hsv(p)
            val w = s * v * v
            if (s < 0.2f || v < 0.2f) continue
            val bin = ((h / 360f) * BINS).toInt().coerceIn(0, BINS - 1)
            weight[bin] += w; sumS[bin] += s * w
            sumX[bin] += kotlin.math.cos(Math.toRadians(h.toDouble())).toFloat() * w
            sumY[bin] += kotlin.math.sin(Math.toRadians(h.toDouble())).toFloat() * w
            total += w
        }
        if (total < pixels.size * 0.03f) return null
        val b = weight.indices.maxBy { weight[it] }
        val hue = ((Math.toDegrees(kotlin.math.atan2(sumY[b], sumX[b]).toDouble()) + 360) % 360).toFloat()
        val sat = (sumS[b] / weight[b]).coerceIn(0.35f, 0.7f)
        return hsvToRgb(hue, sat, 0.95f)
    }

    private fun hsv(p: Int): Triple<Float, Float, Float> {
        val r = (p shr 16 and 0xFF) / 255f; val g = (p shr 8 and 0xFF) / 255f; val bl = (p and 0xFF) / 255f
        val mx = max(r, max(g, bl)); val mn = min(r, min(g, bl)); val d = mx - mn
        val h = when {
            d == 0f -> 0f
            mx == r -> 60f * (((g - bl) / d) % 6f)
            mx == g -> 60f * ((bl - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }.let { if (it < 0) it + 360f else it }
        return Triple(h, if (mx == 0f) 0f else d / mx, mx)
    }

    private fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        val c = v * s; val x = c * (1 - kotlin.math.abs((h / 60f) % 2 - 1)); val m = v - c
        val (r, g, b) = when ((h / 60f).toInt()) {
            0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
        }
        fun ch(f: Float) = ((f + m) * 255).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
