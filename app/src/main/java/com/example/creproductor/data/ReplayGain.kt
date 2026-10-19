// ReplayGain: normaliza el volumen entre pistas leyendo la etiqueta del archivo, sin comprimir la
// dinámica. Solo se **atenúa** (nunca se amplifica) para no provocar recortes (clipping).

package com.example.creproductor.data

import kotlin.math.pow

object ReplayGain {

    private val NUMBER = Regex("""[-+]?\d+(\.\d+)?""")

    /** `"-6.50 dB"` → `-6.5`; `null` si no hay un número. Acepta coma decimal. */
    fun parseDb(raw: String?): Float? =
        raw?.replace(',', '.')?.let { NUMBER.find(it)?.value?.toFloatOrNull() }

    /** Volumen lineal (0..1) para una ganancia en dB. Sin ganancia: 1 (sin cambios). */
    fun volumeFor(gainDb: Float?): Float =
        if (gainDb == null) 1f else 10f.pow(gainDb / 20f).coerceIn(0f, 1f)
}
