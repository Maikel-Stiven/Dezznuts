// Lee etiquetas Vorbis (p. ej. `CUESHEET`) de un FLAC recorriendo sus bloques de metadatos.
// Es la vía para los CUE sheets en Android 11+, donde un `.cue` suelto no se puede leer (almacenamiento
// con alcance) pero el audio sí. Lógica sobre un `InputStream` para probarla con bytes sintéticos.

package com.example.creproductor.data

import java.io.DataInputStream
import java.io.File
import java.io.InputStream

object FlacTags {

    /** Tamaño máximo que se acepta para un bloque de comentarios (una carátula no va aquí; esto es texto). */
    private const val MAX_COMMENT_BLOCK = 2 * 1024 * 1024

    /** Valor de la etiqueta `key` (sin distinguir mayúsculas) del FLAC en `path`; `null` si no existe o no es FLAC. */
    fun read(path: String, key: String): String? = try {
        File(path).inputStream().buffered().use { read(it, key) }
    } catch (_: Exception) { null }

    fun read(input: InputStream, key: String): String? {
        val data = DataInputStream(input)
        val magic = ByteArray(4).also { data.readFully(it) }
        if (String(magic, Charsets.US_ASCII) != "fLaC") return null
        while (true) {
            val header = data.readInt()
            val last = header ushr 31 == 1
            val type = (header ushr 24) and 0x7F
            val length = header and 0xFFFFFF
            if (type == 4 && length <= MAX_COMMENT_BLOCK) {
                return parseComments(ByteArray(length).also { data.readFully(it) }, key)
            }
            data.skipNBytes(length.toLong())   // STREAMINFO, SEEKTABLE, PICTURE…: se salta sin leerlo
            if (last) return null
        }
    }

    private fun parseComments(b: ByteArray, key: String): String? {
        fun le32(o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or
            ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)
        var o = 4 + le32(0)                  // salta el "vendor"
        val count = le32(o); o += 4
        repeat(count) {
            if (o + 4 > b.size) return null
            val len = le32(o); o += 4
            if (len < 0 || o + len > b.size) return null
            val entry = String(b, o, len, Charsets.UTF_8); o += len
            val eq = entry.indexOf('=')
            if (eq > 0 && entry.substring(0, eq).equals(key, ignoreCase = true)) return entry.substring(eq + 1)
        }
        return null
    }
}
