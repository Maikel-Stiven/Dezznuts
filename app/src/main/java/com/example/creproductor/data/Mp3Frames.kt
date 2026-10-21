package com.example.creproductor.data

/**
 * Recorre las tramas de un MP3 (Layer III) para poder cortarlo por bytes, sin recodificar.
 *
 * Una trama MP3 empieza por una cabecera de 4 bytes con la sincronía `0xFFE…`; de ella salen su longitud y su
 * duración. Se salta la etiqueta ID3v2 inicial. Lógica pura (sin Android) para probarla con tramas sintéticas.
 *
 * Limitación conocida: el "depósito de bits" hace que las 1–2 primeras tramas de un corte puedan referirse a
 * datos de la trama anterior; los decodificadores las descartan, así que a lo sumo se pierde una fracción de
 * segundo al empezar, no hay corrupción.
 */
object Mp3Frames {
    class Frame(val offset: Int, val length: Int, val startUs: Long, val durationUs: Long)

    private val BITRATE_V1 = intArrayOf(0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320)
    private val BITRATE_V2 = intArrayOf(0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160)
    private val RATE_V1 = intArrayOf(44100, 48000, 32000)
    private val RATE_V2 = intArrayOf(22050, 24000, 16000)
    private val RATE_V25 = intArrayOf(11025, 12000, 8000)

    /** Tramas del MP3 en `data`; vacío si no es Layer III reconocible (entonces se corta decodificando). */
    fun parse(data: ByteArray): List<Frame> {
        var pos = id3Size(data)
        val frames = ArrayList<Frame>()
        var timeUs = 0L
        while (pos + 4 <= data.size) {
            val b1 = data[pos + 1].toInt() and 0xFF
            val b2 = data[pos + 2].toInt() and 0xFF
            if (data[pos].toInt() and 0xFF != 0xFF || b1 and 0xE0 != 0xE0) { pos++; continue }   // sin sincronía: buscar
            val version = (b1 shr 3) and 3        // 0 = 2.5, 2 = 2, 3 = 1
            val layer = (b1 shr 1) and 3          // 1 = Layer III
            val bitrateIdx = b2 shr 4
            val rateIdx = (b2 shr 2) and 3
            if (version == 1 || layer != 1 || bitrateIdx == 0 || bitrateIdx == 15 || rateIdx == 3) { pos++; continue }
            val v1 = version == 3
            val bitrate = (if (v1) BITRATE_V1 else BITRATE_V2)[bitrateIdx] * 1000
            val rate = (if (v1) RATE_V1 else if (version == 2) RATE_V2 else RATE_V25)[rateIdx]
            val padding = (b2 shr 1) and 1
            val length = (if (v1) 144 else 72) * bitrate / rate + padding
            if (length < 4 || pos + length > data.size) break
            val durationUs = (if (v1) 1152L else 576L) * 1_000_000L / rate
            frames += Frame(pos, length, timeUs, durationUs)
            timeUs += durationUs
            pos += length
        }
        return frames
    }

    /** Bytes del MP3 desde la primera trama que empieza en `[startUs, endUs)` hasta la última de ese tramo. */
    fun cut(data: ByteArray, frames: List<Frame>, startUs: Long, endUs: Long): ByteArray? {
        val chosen = frames.filter { it.startUs >= startUs && it.startUs < endUs }
        if (chosen.isEmpty()) return null
        return data.copyOfRange(chosen.first().offset, chosen.last().let { it.offset + it.length })
    }

    private fun id3Size(d: ByteArray): Int {
        if (d.size < 10 || d[0] != 'I'.code.toByte() || d[1] != 'D'.code.toByte() || d[2] != '3'.code.toByte()) return 0
        val size = ((d[6].toInt() and 0x7F) shl 21) or ((d[7].toInt() and 0x7F) shl 14) or
            ((d[8].toInt() and 0x7F) shl 7) or (d[9].toInt() and 0x7F)
        return 10 + size
    }
}
