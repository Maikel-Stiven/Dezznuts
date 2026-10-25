// Procesador de audio "espía" para el visualizador: mira el PCM que pasa y lo deja EXACTAMENTE igual.
//
// Solo se activa si el visualizador está encendido (así, con todo apagado, la cadena no cambia). Saca un espectro
// ~40 veces por segundo y lo deja en [SpectrumTap] para que la pantalla lo dibuje. No necesita permisos y también ve
// lo que suena desde YouTube Music. Con audio offload el DSP salta los procesadores: ahí no hay datos.

package com.example.creproductor.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.creproductor.data.Spectrum
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Último espectro calculado (mismo proceso que la interfaz). Cada fotograma lleva la hora en que se calculó. */
object SpectrumTap {
    class Frame(val atNanos: Long, val sampleRate: Int, val magnitudes: FloatArray)

    private const val KEEP = 24
    private val ring = arrayOfNulls<Frame>(KEEP)
    @Volatile private var head = 0
    /** Hora del último fotograma (0 = nunca llegó audio al espía): la interfaz la usa para saber si hay datos. */
    @Volatile var lastAtNanos = 0L
        private set

    fun push(f: Frame) { synchronized(ring) { ring[head] = f; head = (head + 1) % KEEP; lastAtNanos = f.atNanos } }

    /** El fotograma más reciente calculado hasta [beforeNanos] (el audio sale con retraso por el búfer de salida). */
    fun latest(beforeNanos: Long): Frame? = synchronized(ring) {
        var best: Frame? = null
        for (f in ring) if (f != null && f.atNanos <= beforeNanos && (best == null || f.atNanos > best.atNanos)) best = f
        best
    }

    fun clear() { synchronized(ring) { ring.fill(null); lastAtNanos = 0L } }
}

@OptIn(UnstableApi::class)
class SpectrumTapProcessor(private val enabledAtBuild: Boolean) : BaseAudioProcessor() {
    private val spectrum = Spectrum(2048)
    private val ring = FloatArray(2048)
    private var write = 0
    private var filled = 0
    private var sinceHop = 0
    private var hop = 1102
    private val scratch = FloatArray(2048)

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (!enabledAtBuild) return AudioFormat.NOT_SET
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> {
                hop = inputAudioFormat.sampleRate / 40
                write = 0; filled = 0; sinceHop = 0
                inputAudioFormat
            }
            else -> AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val ch = inputAudioFormat.channelCount
        val enc = inputAudioFormat.encoding
        val bps = when (enc) { C.ENCODING_PCM_16BIT -> 2; C.ENCODING_PCM_24BIT -> 3; else -> 4 }
        val d = inputBuffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val frames = size / (bps * ch)
        for (f in 0 until frames) {
            var sum = 0f
            for (c in 0 until ch) {
                sum += when (enc) {
                    C.ENCODING_PCM_16BIT -> d.short / 32768f
                    C.ENCODING_PCM_24BIT -> (((d.get().toInt() and 0xFF) or ((d.get().toInt() and 0xFF) shl 8) or (d.get().toInt() shl 16)) / 8388608f)
                    C.ENCODING_PCM_32BIT -> d.int / 2147483648f
                    else -> d.float
                }
            }
            ring[write] = sum / ch
            write = (write + 1) and 2047
            if (filled < 2048) filled++
            if (++sinceHop >= hop && filled >= 2048) { sinceHop = 0; publish() }
        }
        // Pasa tal cual: misma señal, mismo formato.
        val out = replaceOutputBuffer(size)
        out.put(inputBuffer)
        out.flip()
    }

    private fun publish() {
        // El más viejo primero: desde `write` (la próxima a sobrescribir) dando la vuelta.
        for (i in 0 until 2048) scratch[i] = ring[(write + i) and 2047]
        spectrum.analyze(scratch)
        SpectrumTap.push(SpectrumTap.Frame(System.nanoTime(), inputAudioFormat.sampleRate, spectrum.magnitudes.copyOf()))
    }

    override fun onReset() { write = 0; filled = 0; sinceHop = 0 }
}
