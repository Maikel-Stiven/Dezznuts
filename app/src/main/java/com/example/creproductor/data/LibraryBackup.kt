package com.example.creproductor.data

/**
 * Copia de seguridad de favoritos, reproducciones y listas en texto plano (una entrada por línea, campos separados
 * por tabulador). Guarda **rutas**, no ids de MediaStore: los ids cambian al reinstalar o cambiar de teléfono y las
 * rutas no (y si cambian, [SongMatcher] cae al nombre del archivo).
 */
data class LibraryBackup(
    val favorites: List<String> = emptyList(),
    val playCounts: Map<String, Int> = emptyMap(),
    val playlists: List<Pair<String, List<String>>> = emptyList()
) {
    fun encode(): String = buildString {
        appendLine(HEADER)
        favorites.forEach { appendLine("FAV\t${clean(it)}") }
        playCounts.forEach { (path, n) -> appendLine("COUNT\t$n\t${clean(path)}") }
        playlists.forEach { (name, paths) ->
            appendLine("LIST\t${clean(name)}")
            paths.forEach { appendLine("SONG\t${clean(it)}") }
        }
    }

    companion object {
        private const val HEADER = "DEZZNUTS-BACKUP 1"
        private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')

        /** null si el texto no es una copia de DEZZNUTS. */
        fun decode(text: String): LibraryBackup? {
            val lines = text.lineSequence().map { it.trimEnd('\r') }.toList()
            if (lines.firstOrNull() != HEADER) return null
            val favorites = mutableListOf<String>()
            val counts = mutableMapOf<String, Int>()
            val lists = mutableListOf<Pair<String, MutableList<String>>>()
            for (line in lines.drop(1)) {
                val f = line.split('\t')
                when (f[0]) {
                    "FAV" -> f.getOrNull(1)?.let(favorites::add)
                    "COUNT" -> { val n = f.getOrNull(1)?.toIntOrNull(); val p = f.getOrNull(2); if (n != null && p != null) counts[p] = n }
                    "LIST" -> f.getOrNull(1)?.let { lists.add(it to mutableListOf()) }
                    "SONG" -> f.getOrNull(1)?.let { lists.lastOrNull()?.second?.add(it) }
                }
            }
            return LibraryBackup(favorites, counts, lists)
        }
    }
}

/** Listas M3U / M3U8: el formato que entienden casi todos los reproductores. */
object M3u {
    fun write(songs: List<Song>): String = buildString {
        appendLine("#EXTM3U")
        songs.forEach {
            appendLine("#EXTINF:${it.duration / 1000},${it.artist} - ${it.title}")
            appendLine(it.path)
        }
    }

    /** Las rutas de la lista (sin comentarios ni líneas vacías). */
    fun parse(text: String): List<String> =
        text.lineSequence().map { it.trim().removePrefix("﻿") }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList()
}

/** Encuentra en la biblioteca las canciones de una lista de rutas: por ruta exacta o, si no, por nombre de archivo. */
class SongMatcher<T>(songs: List<T>, private val pathOf: (T) -> String) {
    private val byPath = songs.associateBy(pathOf)
    private val byName = songs.groupBy { fileName(pathOf(it)) }

    fun find(path: String): T? = byPath[path] ?: byName[fileName(path)]?.firstOrNull()

    fun match(paths: List<String>): List<T> = paths.mapNotNull(::find).distinct()

    private fun fileName(path: String) = path.substringAfterLast('/').substringAfterLast('\\').lowercase()
}
