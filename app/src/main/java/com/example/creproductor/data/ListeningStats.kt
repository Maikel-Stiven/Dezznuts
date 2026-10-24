package com.example.creproductor.data

/** Una canción reducida a lo que cuentan las estadísticas (así se prueba sin tocar clases de Android). */
data class PlayedEntry(val id: Long, val artist: String, val plays: Int, val durationMs: Long)

/** Resumen de lo escuchado, calculado de los contadores de reproducción que ya se guardan en cada [Song]. */
data class ListeningStats(
    val plays: Int,
    val listenedMs: Long,
    /** (id de canción, reproducciones), de más a menos. */
    val topSongs: List<Pair<Long, Int>>,
    val topArtists: List<Pair<String, Int>>
) {
    companion object {
        fun of(songs: List<Song>, top: Int = 5) =
            compute(songs.map { PlayedEntry(it.id, it.artist, it.playCount, it.duration) }, top)

        /** Las canciones sin reproducciones no cuentan. Los artistas desconocidos se omiten del ranking. */
        fun compute(entries: List<PlayedEntry>, top: Int = 5): ListeningStats {
            val played = entries.filter { it.plays > 0 }
            val byArtist = played
                .filter { it.artist.isNotBlank() && !it.artist.startsWith("<") }
                .groupBy { it.artist }
                .mapValues { (_, list) -> list.sumOf { it.plays } }
            return ListeningStats(
                plays = played.sumOf { it.plays },
                listenedMs = played.sumOf { it.plays.toLong() * it.durationMs },
                topSongs = played.sortedByDescending { it.plays }.take(top).map { it.id to it.plays },
                topArtists = byArtist.entries.sortedByDescending { it.value }.take(top).map { it.key to it.value }
            )
        }
    }
}
