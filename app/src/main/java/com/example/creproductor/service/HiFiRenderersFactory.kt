// Fábrica de renderers de ExoPlayer afinada para alta fidelidad.

package com.example.creproductor.service

import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * [DefaultRenderersFactory] que construye un [DefaultAudioSink] con los ajustes
 * de máxima fidelidad. Es el único punto del proyecto donde se puede tocar la
 * cadena de salida de audio, porque `ExoPlayer.Builder` no expone el `AudioSink`.
 *
 * Qué hace exactamente cada ajuste:
 *
 * - **[useFloatOutput]** → `setEnableAudioFloatOutput(true)`. Convierte el PCM de
 *   alta resolución (24/32 bits) a punto flotante de 32 bits antes de mezclar,
 *   evitando el truncado a 16 bits. ExoPlayer solo lo aplica cuando el archivo es
 *   de alta resolución (`Util.isEncodingHighResolutionPcm`); en 16 bits sigue
 *   usando la cadena normal, así que no hay riesgo de perder funciones.
 *
 * - **[useOffload]** → `setOffloadMode(OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED)`.
 *   Pasa el bitstream comprimido (FLAC, ALAC, MP3, AAC…) directamente al DSP del
 *   dispositivo: no hay remuestreo ni mezcla en la app, y el DAC recibe la
 *   frecuencia y profundidad originales. Es la única vía real para acercarse al
 *   bit-perfect en Android. Se usa `GAPLESS_REQUIRED` para que ExoPlayer solo
 *   entre en offload cuando NO rompe la reproducción sin cortes entre pistas
 *   (importante en discos en directo o mezclas continuas). Si el chip no soporta
 *   offload para ese formato, ExoPlayer cae automáticamente a PCM.
 *
 * Requiere **API 29+** para el offload; en versiones anteriores se ignora sin
 * más (el resto de ajustes sí se aplican).
 */
@OptIn(UnstableApi::class)
class HiFiRenderersFactory(
    context: Context,
    useFloatOutput: Boolean,
    private val useOffload: Boolean,
    private val eqProcessor: AudioProcessor? = null,
    private val spectrumTap: AudioProcessor? = null
) : DefaultRenderersFactory(context) {

    init {
        setEnableAudioFloatOutput(useFloatOutput)
    }

    /**
     * Réplica del método padre sustituyendo la creación del `AudioSink` para
     * poder activar el offload. El padre lo hace privado de facto (no hay hook
     * de builder), así que hay que reconstruirlo aquí con los mismos parámetros.
     */
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        val sink = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .apply { if (eqProcessor != null) setAudioProcessorChain(ProcessorChain(listOfNotNull(eqProcessor, spectrumTap))) }
            .build()

        if (useOffload && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sink.setOffloadMode(AudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED)
        }

        return sink
    }
}

/**
 * Cadena con los procesadores propios (ecualizador y, si está encendido, el espía del visualizador). La cadena por defecto añade Sonic (velocidad) y silenciador, que solo admiten
 * 16 bits y rechazarían la salida float del ecualizador. Como la app no cambia velocidad ni omite silencios,
 * se prescinde de ellos: velocidad fija a 1× y sin recorte de silencios.
 */
@OptIn(UnstableApi::class)
private class ProcessorChain(private val processors: List<AudioProcessor>) : DefaultAudioSink.AudioProcessorChain {
    override fun getAudioProcessors(): Array<AudioProcessor> = processors.toTypedArray()
    override fun applyPlaybackParameters(playbackParameters: androidx.media3.common.PlaybackParameters) = playbackParameters
    override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean) = false
    override fun getMediaDuration(playoutDuration: Long) = playoutDuration
    override fun getSkippedOutputFrameCount() = 0L
}
