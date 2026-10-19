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
//
// Las DOS vienen apagadas por defecto: son opt-in y las decide el usuario,
// porque ambas cambian la ruta de salida y dependen del dispositivo.

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
     * La salida float se ofrece APAGADA por defecto: el usuario decide si la
     * activa después de leer sus riesgos en el diálogo. La app nunca cambia la
     * ruta de audio por su cuenta.
     */
    const val DEFAULT_FLOAT_OUTPUT = false

    /**
     * El offload también se ofrece APAGADO por defecto: depende del chip y
     * Media3 lo deja off a propósito al existir dispositivos con soporte
     * defectuoso. Activar cualquiera de los dos es una decisión consciente
     * del usuario final.
     */
    const val DEFAULT_OFFLOAD = false

    /** ReplayGain: normaliza el volumen entre pistas (solo atenúa). Apagado por defecto. */
    const val KEY_REPLAYGAIN = "hifi_replaygain"

    /** Dirigir la salida a un DAC USB cuando esté conectado. Apagado por defecto. */
    const val KEY_PREFER_USB = "hifi_prefer_usb"

    /** Ecualizador: activo, niveles por banda (mB, separados por coma) y datos de bandas publicados por el servicio. */
    const val KEY_EQ_ENABLED = "eq_enabled"
    const val KEY_EQ_LEVELS = "eq_levels"
    const val KEY_EQ_INFO = "eq_info"

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

    fun isReplayGainEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_REPLAYGAIN, false)
    fun setReplayGainEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_REPLAYGAIN, enabled).apply()
    }

    fun isPreferUsbEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_PREFER_USB, false)
    fun setPreferUsbEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PREFER_USB, enabled).apply()
    }

    /**
     * Salidas de audio conectadas, en texto: `Nombre · tipo · tasas`. Sirve para ver si el DAC USB
     * está conectado y qué frecuencias admite (lista vacía = el sistema no las declara).
     */
    fun outputDevices(context: Context): List<String> {
        val manager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return emptyList()
        return manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { d ->
            val type = when (d.type) {
                android.media.AudioDeviceInfo.TYPE_USB_DEVICE, android.media.AudioDeviceInfo.TYPE_USB_HEADSET -> "USB"
                android.media.AudioDeviceInfo.TYPE_WIRED_HEADPHONES, android.media.AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Cable"
                android.media.AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"
                android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Altavoz"
                else -> return@mapNotNull null
            }
            val rates = d.sampleRates.takeIf { it.isNotEmpty() }?.joinToString("/") { "${it / 1000.0}".removeSuffix(".0") } ?: "—"
            "${d.productName} · $type · $rates kHz"
        }
    }
}
