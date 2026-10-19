// Negociación bit-perfect con un DAC USB (Android 14+, `AudioMixerAttributes`).
//
// En modo bit-perfect el mezclador del sistema no toca la señal, pero SOLO si el formato del AudioTrack
// (frecuencia, codificación y canales) coincide exactamente con el formato de mezcla elegido. Por eso la elección es
// de coincidencia exacta con el formato en que ExoPlayer abrirá el AudioTrack, no de "el más cercano": un formato
// mayor no daría bit-perfect y podría degradar la ruta. Lógica pura (sin Android) para probarla sin hardware.

package com.example.creproductor.data

/** Formato de mezcla / de AudioTrack: frecuencia (Hz), bits (16, 24, 32), si es float y nº de canales. */
data class MixerFormat(val sampleRate: Int, val bits: Int, val isFloat: Boolean, val channels: Int)

object BitPerfect {

    private const val UPSIDE_DOWN_CAKE = 34

    /**
     * Formato en el que ExoPlayer abrirá el AudioTrack para una pista: float de 32 bits si la salida float está
     * activa y la fuente es de alta resolución; si no, 16 bits (así lo hace DefaultAudioSink, comprobado en emulador).
     * Si no se conoce la profundidad de la fuente se infiere por la frecuencia.
     * ponytail: heurística de frecuencia (>48 kHz) para fuentes de profundidad desconocida; si falla, solo se pierde el
     * bit-perfect (no hay coincidencia), nunca se rompe la reproducción.
     */
    fun trackFormat(sampleRate: Int, channels: Int, sourceBits: Int, floatOutput: Boolean): MixerFormat {
        val highRes = if (sourceBits > 0) sourceBits > 16 else sampleRate > 48_000
        return if (floatOutput && highRes) MixerFormat(sampleRate, 32, true, channels) else MixerFormat(sampleRate, 16, false, channels)
    }

    /** Índice del formato soportado que coincide exactamente con [target]; `null` si no hay (se queda el modo normal). */
    fun choose(supported: List<MixerFormat>, target: MixerFormat): Int? = supported.indexOf(target).takeIf { it >= 0 }

    /**
     * ¿Se debe intentar bit-perfect? Solo con Android 14+, función activa, un DAC USB conectado y la señal sin tocar:
     * el offload y el procesador de audio (ecualizador / subida de ReplayGain, que trabaja en float) la modifican.
     */
    fun allowed(sdk: Int, enabled: Boolean, usbConnected: Boolean, processorActive: Boolean, offload: Boolean) =
        sdk >= UPSIDE_DOWN_CAKE && enabled && usbConnected && !processorActive && !offload
}
