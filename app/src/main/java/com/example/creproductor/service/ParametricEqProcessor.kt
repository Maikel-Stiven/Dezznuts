// Procesador de audio de Media3 que aplica el ecualizador paramétrico (y la subida de ReplayGain).
//
// Va dentro de la cadena de DefaultAudioSink, antes del AudioTrack: trabaja en float de 32 bits, así que no
// añade ruido de cuantización y funciona con cualquier dispositivo (no depende de efectos del sistema).
// Solo está "activo" si se creó habilitado; así, con todo apagado no toca la señal (bit-perfect).
// Con audio offload el DSP salta los procesadores, por lo que no tiene efecto en ese modo.

package com.example.creproductor.service

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.creproductor.data.EqBand
import com.example.creproductor.data.EqBands
import com.example.creproductor.data.ParametricEq
import java.nio.ByteBuffer
import java.nio.ByteOrder

@OptIn(UnstableApi::class)
class ParametricEqProcessor(private val enabledAtBuild: Boolean) : BaseAudioProcessor() {

    @Volatile private var bands: List<EqBand> = EqBands.DEFAULT
    @Volatile private var eqOn = false
    @Volatile private var boostLinear = 1f
    private var eq: ParametricEq? = null
    private var scratch = FloatArray(0)

    /** Aplica bandas / interruptor del EQ / subida (dB) en vivo, sin reconfigurar la cadena. */
    fun update(bands: List<EqBand>, eqEnabled: Boolean, boostDb: Float) {
        this.bands = bands
        eqOn = eqEnabled
        boostLinear = Math.pow(10.0, boostDb / 20.0).toFloat()
        eq?.let { apply(it) }
    }

    private fun apply(e: ParametricEq) {
        e.setBands(if (eqOn) bands else emptyList())
        e.extraGain = boostLinear
    }

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        if (!enabledAtBuild) return AudioFormat.NOT_SET
        return when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_24BIT, C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> {
                eq = ParametricEq(inputAudioFormat.sampleRate, inputAudioFormat.channelCount).also { apply(it) }
                AudioFormat(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, C.ENCODING_PCM_FLOAT)
            }
            else -> AudioFormat.NOT_SET // formato raro: se deja pasar sin tocar
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val e = eq ?: return
        val enc = inputAudioFormat.encoding
        val bytesPerSample = when (enc) { C.ENCODING_PCM_16BIT -> 2; C.ENCODING_PCM_24BIT -> 3; else -> 4 }
        val samples = inputBuffer.remaining() / bytesPerSample
        val frames = samples / inputAudioFormat.channelCount
        if (scratch.size < samples) scratch = FloatArray(samples)
        val order = inputBuffer.order()
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until samples) {
            scratch[i] = when (enc) {
                C.ENCODING_PCM_16BIT -> inputBuffer.short / 32768f
                C.ENCODING_PCM_24BIT -> {
                    val v = (inputBuffer.get().toInt() and 0xFF) or ((inputBuffer.get().toInt() and 0xFF) shl 8) or (inputBuffer.get().toInt() shl 16)
                    v / 8388608f
                }
                C.ENCODING_PCM_32BIT -> inputBuffer.int / 2147483648f
                else -> inputBuffer.float
            }
        }
        inputBuffer.order(order)
        e.process(scratch, frames)
        val out = replaceOutputBuffer(samples * 4).order(ByteOrder.nativeOrder())
        for (i in 0 until samples) out.putFloat(scratch[i])
        out.flip()
    }

    override fun onFlush() { eq?.reset() }

    override fun onReset() { eq = null }
}
