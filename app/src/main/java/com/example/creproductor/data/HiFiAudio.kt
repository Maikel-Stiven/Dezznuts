// Preferencias de calidad de audio ("Hi-Fi") de DEZZNUTS.
//
// Estas opciones no alteran la decodificación (FLAC/ALAC/WAV ya se decodifican
// de forma exacta y sin pérdida), sino CÓMO se entrega ese PCM al DAC del
// dispositivo. Son las dos palancas reales que tiene una app de Android para
// acercarse al "bit-perfect":
//
//  1) Salida en 32 bits (float): evita truncar el PCM de 24/32 bits a 16 bits.
//  2) Audio offload: manda el archivo comprimido tal cual al DSP, sin remuestreo
//     ni mezcla en la app (mantiene frecuencia y profundidad originales).

package com.example.creproductor.data

import android.content.Context
import android.media.AudioManager

object HiFiAudio {

    /** Mismas prefs que usa el resto de la app (ViewModel y servicio). */
    const val PREFS_NAME = "dezznuts_prefs"

    /** Clave de la salida en punto flotante de 32 bits. */
    const val KEY_FLOAT_OUTPUT = "hifi_float_output"

    /** Clave del modo audio offload. */
    const val KEY_OFFLOAD = "hifi_audio_offload"

    /**
     * La salida float viene activada por defecto: solo actúa sobre material de
     * alta resolución (24/32 bits) y en 16 bits no cambia absolutamente nada.
     */
    const val DEFAULT_FLOAT_OUTPUT = true

    /**
     * El offload viene desactivado por defecto porque depende del dispositivo:
     * Media3 lo deja off a propósito al existir chips con soporte defectuoso.
     * Se ofrece como opción para quien quiera el máximo de fidelidad.
     */
    const val DEFAULT_OFFLOAD = false

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** @return `true` si se debe usar salida PCM en punto flotante (32 bits). */
    fun isFloatOutputEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FLOAT_OUTPUT, DEFAULT_FLOAT_OUTPUT)

    /** @return `true` si se debe intentar reproducir en modo offload. */
    fun isOffloadEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OFFLOAD, DEFAULT_OFFLOAD)

    fun setFloatOutputEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FLOAT_OUTPUT, enabled).apply()
    }

    fun setOffloadEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_OFFLOAD, enabled).apply()
    }

    /**
     * Frecuencia de muestreo nativa de la salida de audio del dispositivo, en Hz.
     *
     * Es el dato clave para entender el remuestreo: el mezclador del sistema
     * (AudioFlinger) convierte todo lo que no coincida con esta frecuencia. En
     * la mayoría de móviles son 48000 Hz, así que un FLAC de CD (44100 Hz) se
     * remuestrea salvo que se use offload (o un DAC USB que negocie la tasa).
     *
     * @return la frecuencia en Hz, o `null` si el sistema no la informa.
     */
    fun deviceSampleRate(context: Context): Int? =
        audioManagerProperty(context, AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)

    /**
     * Tamaño nativo del búfer de salida en fotogramas (afecta a la latencia,
     * no a la fidelidad). Se usa solo como dato informativo.
     */
    fun deviceFramesPerBuffer(context: Context): Int? =
        audioManagerProperty(context, AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER)

    private fun audioManagerProperty(context: Context, key: String): Int? {
        val manager = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return null
        return manager.getProperty(key)?.toIntOrNull()
    }
}
