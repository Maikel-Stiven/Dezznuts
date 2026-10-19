// Detecta canciones repetidas (misma obra en distintos archivos) y audios de WhatsApp.
//
// Lógica pura (sin Android) para probarla. Criterio: mismo título y artista normalizados y duración
// casi igual. De cada grupo se conserva la de mejor calidad y el resto se ofrece como copias.

package com.example.creproductor.data

import java.text.Normalizer

object DuplicateFinder {

    /** Diferencia máxima de duración (ms) para considerar que dos archivos son la misma canción. */
    private const val DURATION_TOLERANCE_MS = 3_000L

    private val BRACKETS = Regex("""[(\[].*?[)\]]""")
    private val NOT_ALNUM = Regex("""[^\p{L}\p{N}]+""")
    private val ACCENTS = Regex("""\p{M}+""")

    /** Minúsculas, sin acentos, sin "(Remastered)"/"[Live]" ni signos: "Señor (Remix)!" → "senor". */
    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase().replace(BRACKETS, " "), Normalizer.Form.NFD)
            .replace(ACCENTS, "").replace(NOT_ALNUM, "")

    data class Group<T>(val keep: T, val copies: List<T>)

    /** Mejor primero: mayor nivel (Hi-Res > Lossless > con pérdida), luego bits, frecuencia y bitrate. */
    private val SONG_BEST_FIRST = compareByDescending<Song> { it.quality.ordinal }
        .thenByDescending { it.bitsPerSample }
        .thenByDescending { it.sampleRate }
        .thenByDescending { it.bitrate }
        .thenBy { it.id }

    /** Duplicados de la biblioteca. Las pistas CUE no se cuentan: no son archivos propios. */
    fun findSongs(songs: List<Song>): List<Group<Song>> =
        find(songs.filter { it.cueStartMs < 0 }, { it.title }, { it.artist }, { it.duration }, SONG_BEST_FIRST)
            .sortedBy { it.keep.title.lowercase() }

    /** Versión genérica (como [SongOrganizer]): sirve con cualquier tipo y se prueba sin Android. */
    fun <T> find(
        items: List<T>,
        titleOf: (T) -> String,
        artistOf: (T) -> String,
        durationOf: (T) -> Long,
        bestFirst: Comparator<T>
    ): List<Group<T>> =
        items.filter { normalize(titleOf(it)).isNotEmpty() }
            .groupBy { normalize(titleOf(it)) + "|" + normalize(artistOf(it)) }
            .values.filter { it.size > 1 }
            .flatMap { sameName -> clusterByDuration(sameName, durationOf) }
            .filter { it.size > 1 }
            .map { cluster -> cluster.sortedWith(bestFirst).let { Group(it.first(), it.drop(1)) } }

    /** Agrupa por duración encadenando archivos cuya duración difiere menos que la tolerancia. */
    private fun <T> clusterByDuration(items: List<T>, durationOf: (T) -> Long): List<List<T>> {
        val clusters = mutableListOf<MutableList<T>>()
        for (item in items.sortedBy(durationOf)) {
            val last = clusters.lastOrNull()
            if (last != null && durationOf(item) - durationOf(last.last()) <= DURATION_TOLERANCE_MS) last += item else clusters += mutableListOf(item)
        }
        return clusters
    }

    /** Audios recibidos por WhatsApp (por la carpeta donde se guardan). */
    fun whatsAppAudios(songs: List<Song>): List<Song> =
        songs.filter { it.path.contains("whatsapp", ignoreCase = true) }
}
