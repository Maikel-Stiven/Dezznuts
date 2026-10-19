// Clase que le da los valores
// al archivo o canción como nombre número para organizarlo
// en la lista de reproducción

package com.example.creproductor.data

import android.net.Uri
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class Song (
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val year: Int = 0,
    val trackNumber: Int = 0,
    val path: String = "",
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val genre: String = "Desconocido",
    /** Datos técnicos del archivo (0 / "" si el sistema no los informa). */
    val mimeType: String = "",
    val sampleRate: Int = 0,
    val bitsPerSample: Int = 0,
    val bitrate: Int = 0,
    val quality: QualityTier = QualityTier.LOSSY,
    /** Pista de una hoja CUE: tramo [inicio, fin) en ms del archivo largo (−1 = canción normal). */
    val cueStartMs: Long = -1L,
    val cueEndMs: Long = -1L
)


fun String.fixEncoding(): String{
    if (!(contains("â") || contains("Ã"))) return this
    // Decodificación estricta: si los bytes no forman UTF-8 válido, el texto ya
    // estaba bien ("NÃO", "pâte") y se deja como está en vez de corromperlo.
    return try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(toByteArray(StandardCharsets.ISO_8859_1)))
            .toString()
            .replace("â€™", "")
            .replace("â€“", "")
    }catch (_: CharacterCodingException){
        this
    }
}
