// Diagnóstico de latencia del *seek* (mover el tiempo de una canción).

package com.example.creproductor.service

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import java.io.IOException

/** Etiqueta de logcat: `adb logcat -s DezzSeek`. */
private const val TAG = "DezzSeek"

/** Solo se registran lecturas que tarden al menos esto (evita inundar logcat). */
private const val SLOW_LOAD_THRESHOLD_MS = 40L

/** Ventana durante la cual las marcas se expresan como "+N ms" desde el *seek*. */
private const val SEEK_WINDOW_MS = 20_000L

/**
 * Instrumentación para averiguar **dónde** se pierde el tiempo al mover el
 * tiempo de una canción.
 *
 * El problema: al adelantar o atrasar, la reproducción tarda "un poco" en
 * reanudar. Ese retardo puede estar en sitios muy distintos, y adivinar cuál es
 * una pérdida de tiempo. Esto imprime una línea de tiempo en logcat con los
 * milisegundos transcurridos desde que el usuario suelta el dedo del slider.
 *
 * ### Cómo usarlo
 *
 * ```
 * adb logcat -s DezzSeek
 * ```
 *
 * Y luego mover el tiempo de una canción. La traza esperada es:
 *
 * ```
 * [+0 ms]    seek pedido -> 45230 ms
 * [+6 ms]    estado: BUFFERING
 * [+9 ms]    posición movida a 45230 ms (seek)
 * [+14 ms]   AudioTrack creado: 48000 Hz, encoding=PCM float 32 bits, ...
 * [+31 ms]   lectura que tardó 18 ms (65536 bytes)
 * [+48 ms]   decodificador 'c2.android.flac.decoder' inicializado en 12 ms
 * [+52 ms]   estado: READY
 * [+55 ms]   isPlaying = true
 * [+63 ms]   >>> AUDIO SONANDO (el AudioTrack empezó su reloj hace 2 ms)
 * ```
 *
 * ### Cómo interpretar la traza
 *
 * | Dónde se va el tiempo | Qué mirar |
 * |-----------------------|-----------|
 * | `seek pedido` → `posición movida` | El comando tarda en llegar: IPC de la sesión o **hilo principal ocupado**. |
 * | `posición movida` con otra posición | El *seek* cayó lejos de lo pedido (típico sin `SEEKTABLE`). |
 * | `lectura que tardó N ms` (varias seguidas) | I/O. Típico si el FLAC **no trae bloque `SEEKTABLE`**: Media3 hace búsqueda binaria (`FlacBinarySearchSeeker`). |
 * | `decodificador ... inicializado en N ms` | El `MediaCodec` de FLAC tardando en arrancar. |
 * | `AudioTrack creado` | Búfer del `AudioTrack` y, sobre todo, el **formato real de salida**. |
 * | `>>> AUDIO SONANDO` | El instante real en que empieza a oírse. **Es la línea que importa.** |
 * | `UNDERRUN` | El `AudioTrack` se quedó sin datos (se oye un salto). |
 *
 * Si `>>> AUDIO SONANDO` llega a los pocos milisegundos y aun así se percibe
 * lento, el problema no está en el motor de audio sino en la interfaz o en la
 * percepción; si llega a los 500 ms, el tiempo está en la lectura, el
 * decodificador o el propio `AudioTrack`.
 *
 * La instrumentación es barata y solo escribe en logcat; no cambia la
 * reproducción. Para silenciarla basta con no filtrar por esa etiqueta.
 */
@OptIn(UnstableApi::class)
object PlaybackDiagnostics {

    @Volatile
    private var seekRequestAtMs = 0L

    /**
     * Marca el instante en que el usuario pidió un salto.
     *
     * Se llama desde el ViewModel **antes** de hablar con el reproductor, para
     * poder medir el trayecto completo: interfaz → sesión → ExoPlayer → sonido.
     *
     * @param positionMs posición solicitada, en milisegundos.
     */
    fun markSeekRequest(positionMs: Long) {
        seekRequestAtMs = System.currentTimeMillis()
        Log.i(TAG, "[+0 ms] seek pedido -> $positionMs ms")
    }

    /**
     * Engancha la traza a un reproductor.
     *
     * Se puede llamar con cada reproductor nuevo (el de arranque y el que crea
     * [MusicService.rebuildPlayer]): al liberar el antiguo, sus eventos dejan de
     * llegar solos.
     */
    fun attach(player: ExoPlayer) {
        player.addAnalyticsListener(SeekTimelineListener(::stamp, ::inSeekWindow))
    }

    /** `true` si estamos dentro de la ventana de medición de un *seek*. */
    private fun inSeekWindow(): Boolean {
        val base = seekRequestAtMs
        return base != 0L && (System.currentTimeMillis() - base) in 0..SEEK_WINDOW_MS
    }

    /** Marca temporal relativa al último *seek*. */
    private fun stamp(): String {
        val base = seekRequestAtMs
        if (base == 0L) return ""
        val delta = System.currentTimeMillis() - base
        // Fuera de la ventana es reproducción normal, no un salto del usuario.
        return if (delta in 0..SEEK_WINDOW_MS) "[+$delta ms] " else "[reproducción normal] "
    }

    /** Nombre legible de un `AudioFormat.ENCODING_*` del `AudioTrack` real. */
    private fun encodingName(encoding: Int): String = when (encoding) {
        2 -> "PCM 16 bits"                       // AudioFormat.ENCODING_PCM_16BIT
        3 -> "PCM 8 bits"                        // AudioFormat.ENCODING_PCM_8BIT
        4 -> "PCM float 32 bits"                 // AudioFormat.ENCODING_PCM_FLOAT
        5, 6 -> "offload (comprimido)"           // ENCODING_AC3 / ENCODING_E_AC3
        21 -> "PCM 24 bits empaquetado"          // API 31+
        22 -> "PCM 32 bits"                      // API 31+
        -1 -> "offload (sin decodificar)"
        else -> "encoding=$encoding"
    }

    /**
     * Traduce los eventos de ExoPlayer a una línea de tiempo legible.
     *
     * Recibe [stamp] como lambda para no depender del estado del objeto y poder
     * probarse o reutilizarse sin arrastrar el singleton.
     */
    private class SeekTimelineListener(
        private val stamp: () -> String,
        private val inSeekWindow: () -> Boolean
    ) : AnalyticsListener {

        private fun log(message: String) {
            Log.i(TAG, stamp() + message)
        }

        override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
            val name = when (state) {
                Player.STATE_IDLE -> "IDLE"
                Player.STATE_BUFFERING -> "BUFFERING"
                Player.STATE_READY -> "READY"
                Player.STATE_ENDED -> "ENDED"
                else -> "DESCONOCIDO($state)"
            }
            log("estado: $name")
        }

        override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
            log("isPlaying = $isPlaying")
        }

        override fun onPositionDiscontinuity(
            eventTime: AnalyticsListener.EventTime,
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            val name = when (reason) {
                Player.DISCONTINUITY_REASON_SEEK -> "seek"
                Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT -> "ajuste de seek"
                Player.DISCONTINUITY_REASON_AUTO_TRANSITION -> "cambio automático"
                Player.DISCONTINUITY_REASON_SKIP -> "salto"
                else -> "motivo=$reason"
            }
            log("posición movida a ${newPosition.positionMs} ms ($name)")
        }

        override fun onAudioTrackInitialized(
            eventTime: AnalyticsListener.EventTime,
            audioTrackConfig: AudioSink.AudioTrackConfig
        ) {
            log(
                "AudioTrack creado: ${audioTrackConfig.sampleRate} Hz, " +
                    "encoding=${encodingName(audioTrackConfig.encoding)}, " +
                    "búfer=${audioTrackConfig.bufferSize} bytes, " +
                    "offload=${audioTrackConfig.offload}"
            )
        }

        override fun onAudioTrackReleased(
            eventTime: AnalyticsListener.EventTime,
            audioTrackConfig: AudioSink.AudioTrackConfig
        ) {
            log("AudioTrack liberado")
        }

        override fun onAudioInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: DecoderReuseEvaluation?
        ) {
            val bitrate = if (format.bitrate > 0) " ${format.bitrate / 1000} kbps" else ""
            val reuso = decoderReuseEvaluation
                ?.let { " | reuse=${it.result} descartes=${it.discardReasons}" }
                .orEmpty()
            log(
                "formato de entrada: ${format.sampleMimeType} " +
                    "${format.sampleRate} Hz x ${format.channelCount} canales$bitrate$reuso"
            )
        }

        override fun onAudioDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            log("decodificador '$decoderName' inicializado en $initializationDurationMs ms")
        }

        override fun onAudioDecoderReleased(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String
        ) {
            log("decodificador '$decoderName' liberado")
        }

        override fun onAudioPositionAdvancing(
            eventTime: AnalyticsListener.EventTime,
            playoutStartSystemTimeMs: Long
        ) {
            val playout = System.currentTimeMillis() - playoutStartSystemTimeMs
            log(">>> AUDIO SONANDO (el AudioTrack empezó su reloj hace $playout ms)")
        }

        override fun onAudioUnderrun(
            eventTime: AnalyticsListener.EventTime,
            bufferSize: Int,
            bufferSizeMs: Long,
            elapsedSinceLastFeedMs: Long
        ) {
            log(
                "UNDERRUN (el AudioTrack se quedó sin datos): " +
                    "búfer=$bufferSizeMs ms, último llenado hace $elapsedSinceLastFeedMs ms"
            )
        }

        override fun onAudioSinkError(
            eventTime: AnalyticsListener.EventTime,
            audioSinkError: Exception
        ) {
            log("ERROR del audio sink: $audioSinkError")
        }

        override fun onAudioCodecError(
            eventTime: AnalyticsListener.EventTime,
            audioCodecError: Exception
        ) {
            log("ERROR del códec: $audioCodecError")
        }

        override fun onPlayerError(
            eventTime: AnalyticsListener.EventTime,
            error: PlaybackException
        ) {
            log("ERROR del reproductor: ${error.errorCodeName} - ${error.message}")
        }

        /**
         * Desde dónde pide el reproductor la siguiente carga durante un *seek*.
         *
         * Ojo: es la posición **solicitada**, no el offset de bytes en el que el
         * extractor empieza a leer de verdad. ExoPlayer rellena el `dataSpec` del
         * evento *antes* de abrir la fuente, así que su `position` siempre vale 0
         * y no sirve como diagnóstico (se comprobó en
         * `ProgressiveMediaPeriod.ExtractingLoadable.load()`).
         */
        override fun onLoadStarted(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData
        ) {
            if (inSeekWindow()) {
                val startTimeMs = mediaLoadData.mediaStartTimeMs
                val desde = if (startTimeMs == C.TIME_UNSET) {
                    "posición desconocida"
                } else {
                    "$startTimeMs ms"
                }
                log("carga solicitada desde $desde (tarea ${loadEventInfo.loadTaskId})")
            }
        }

        override fun onLoadCompleted(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData
        ) {
            if (loadEventInfo.loadDurationMs >= SLOW_LOAD_THRESHOLD_MS) {
                log(
                    "lectura que tardó ${loadEventInfo.loadDurationMs} ms " +
                        "(${loadEventInfo.bytesLoaded} bytes)"
                )
            }
        }

        override fun onLoadCanceled(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData
        ) {
            log("lectura CANCELADA tras ${loadEventInfo.loadDurationMs} ms (normal al hacer seek)")
        }

        override fun onLoadError(
            eventTime: AnalyticsListener.EventTime,
            loadEventInfo: LoadEventInfo,
            mediaLoadData: MediaLoadData,
            error: IOException,
            wasCanceled: Boolean
        ) {
            log("ERROR de lectura tras ${loadEventInfo.loadDurationMs} ms: ${error.message}")
        }
    }
}
