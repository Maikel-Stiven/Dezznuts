// Lee frecuencia y profundidad de la cabecera de un FLAC o WAV. Solo se usa en Android < 11,
// donde MediaStore no las informa. Lógica pura sobre los primeros bytes del archivo (probada con tests).

package com.example.creproductor.data

import java.io.File

object AudioHeader {

    /** Frecuencia (Hz) y profundidad (bits); `null` si no es un FLAC/WAV reconocible. */
    data class Info(val sampleRate: Int, val bitsPerSample: Int)

    /** Con 64 bytes basta: STREAMINFO de FLAC empieza en el byte 8 y el `fmt ` de un WAV estándar en el 12. */
    private const val HEADER_BYTES = 64

    fun read(path: String): Info? = try {
        File(path).inputStream().use { input ->
            val buf = ByteArray(HEADER_BYTES)
            val n = input.read(buf)
            if (n > 0) parse(buf.copyOf(n)) else null
        }
    } catch (_: Exception) { null }

    fun parse(b: ByteArray): Info? = when {
        b.size >= 26 && b.startsWithAscii("fLaC") -> {
            // STREAMINFO: tras la cabecera de bloque (4 bytes) vienen 10 bytes de tamaños; luego
            // frecuencia (20 bits), canales (3), bits-1 (5).
            val o = 8 + 10
            val rate = ((b[o].toInt() and 0xFF) shl 12) or ((b[o + 1].toInt() and 0xFF) shl 4) or ((b[o + 2].toInt() and 0xFF) shr 4)
            val bits = ((((b[o + 2].toInt() and 0x01) shl 4) or ((b[o + 3].toInt() and 0xFF) shr 4)) + 1)
            Info(rate, bits)
        }
        b.size >= 36 && b.startsWithAscii("RIFF") && String(b, 8, 4, Charsets.US_ASCII) == "WAVE" &&
            String(b, 12, 4, Charsets.US_ASCII) == "fmt " -> {
            val rate = le32(b, 24)
            val bits = (b[34].toInt() and 0xFF) or ((b[35].toInt() and 0xFF) shl 8)
            Info(rate, bits)
        }
        else -> null
    }

    private fun ByteArray.startsWithAscii(s: String) = s.indices.all { this[it] == s[it].code.toByte() }
    private fun le32(b: ByteArray, o: Int) =
        (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)
}
