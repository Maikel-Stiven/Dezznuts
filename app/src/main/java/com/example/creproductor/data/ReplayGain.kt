// ReplayGain: normaliza el volumen entre pistas leyendo la etiqueta del archivo, sin comprimir la
// dinámica. Solo se **atenúa** (nunca se amplifica) para no provocar recortes (clipping).

package com.example.creproductor.data

import kotlin.math.pow

object ReplayGain {

    private val NUMBER = Regex("""[-+]?\d+(\.\d+)?""")

    /** `"-6.50 dB"` → `-6.5`; `null` si no hay un número. Acepta coma decimal. */
    fun parseDb(raw: String?): Float? =
        raw?.replace(',', '.')?.let { NUMBER.find(it)?.value?.toFloatOrNull() }

    /** Pico de la pista en escala lineal (`"0.988553"` → 0.988553); `null` si no hay un valor válido (>0). */
    fun parsePeak(raw: String?): Float? = raw?.replace(',', '.')?.trim()?.toFloatOrNull()?.takeIf { it > 0f }

    /**
     * Qué hacer con una pista: `volume` (0..1, vía `Player.volume`, funciona siempre) y `boostDb`
     * (subida en dB, solo si [allowBoost] y hay pico conocido). La subida nunca supera lo que el pico
     * permite sin recortar: `−20·log10(pico)`.
     */
    data class Plan(val volume: Float, val boostDb: Float)

    fun plan(gainDb: Float?, peak: Float?, allowBoost: Boolean): Plan {
        if (gainDb == null) return Plan(1f, 0f)
        // Techo en dB: sin subida es 0 (solo atenúa); con subida y pico conocido, lo que el pico permite sin recortar.
        val ceilingDb = if (allowBoost && peak != null) -20f * kotlin.math.log10(peak) else 0f
        val db = minOf(gainDb, ceilingDb)
        return if (db <= 0f) Plan(10f.pow(db / 20f).coerceIn(0f, 1f), 0f) else Plan(1f, db)
    }

    /** Volumen lineal (0..1) para una ganancia en dB. Sin ganancia: 1 (sin cambios). */
    fun volumeFor(gainDb: Float?): Float =
        if (gainDb == null) 1f else 10f.pow(gainDb / 20f).coerceIn(0f, 1f)
}
