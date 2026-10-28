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

/** Canción que viene de YouTube Music (id sintético, ver `MusicViewModel.playYt`): no existe en MediaStore. */
val Song.isYoutube: Boolean get() = id <= -(1L shl 61)


/** Lo que hace falta de una canción para reconocer a sus gemelas rotas (así se prueba sin `Uri`). */
internal data class TwinKey(val path: String, val title: String, val artist: String, val durationMs: Long, val exempt: Boolean = false)

/**
 * Quita los gemelos rotos que dejan los descargadores (Snaptube y similares): el mismo archivo indexado dos veces o una
 * entrada a 0:00 junto a la buena. Una canción sin duración que no tiene gemela se conserva (puede ser un formato pobre).
 */
internal fun <T> List<T>.withoutBrokenTwinsBy(key: (T) -> TwinKey): List<T> {
    fun stem(k: TwinKey) = k.path.substringBeforeLast('.').lowercase()
    fun name(k: TwinKey) = "${k.title.trim().lowercase()}|${k.artist.trim().lowercase()}"
    val good = map(key).filter { it.durationMs > 0 }
    val goodStems = good.mapNotNullTo(HashSet()) { stem(it).takeIf { s -> s.isNotEmpty() } }
    val goodNames = good.mapTo(HashSet()) { name(it) }
    val seenPaths = HashSet<String>()
    return filter { item ->
        val k = key(item)
        if (k.exempt) return@filter true
        if (k.durationMs <= 0 && (stem(k) in goodStems || name(k) in goodNames)) return@filter false
        // La misma ruta dos veces: se queda la primera.
        k.path.isEmpty() || seenPaths.add(k.path)
    }
}

fun List<Song>.withoutBrokenTwins(): List<Song> =
    withoutBrokenTwinsBy { TwinKey(it.path, it.title, it.artist, it.duration, exempt = it.cueStartMs >= 0 || it.isYoutube) }
