package com.example.creproductor.data

/**
 * Agrupaciones y filtros de canciones, en funciones puras y genéricas para
 * poder reutilizarlas en la UI y probarlas con JUnit sin depender de Android.
 *
 * Son genéricas (`<T>` + selector) para que las pruebas puedan usar cualquier
 * tipo ligero en lugar de [Song], que depende de `android.net.Uri`.
 */
object SongOrganizer {

    const val UNKNOWN_ARTIST = "Artista desconocido"
    const val UNKNOWN_ALBUM = "Álbum desconocido"
    const val UNKNOWN_GENRE = "Desconocido"
    const val NO_FOLDER = "Sin carpeta"

    fun <T> groupByAlbum(
        items: List<T>,
        albumOf: (T) -> String,
        trackNumberOf: ((T) -> Int)? = null,
        titleOf: ((T) -> String)?= null
    ): Map<String, List<T>> {
        val grouped = items
            .filter {
                val album = albumOf(it)
                album.isNotBlank() &&
                        !album.equals("<unknown>", ignoreCase = true) &&
                        !album.equals("unknown", ignoreCase = true)
            }
            .groupBy(albumOf)

        if (trackNumberOf == null) return grouped

        return grouped.mapValues { (_, albumsSongs) ->
            albumsSongs.sortedWith(
                compareBy<T> {
                    val track = trackNumberOf(it)
                    if (track > 0) track else Int.MAX_VALUE
                }.thenBy {
                    titleOf?.invoke(it) ?: ""
                }
            )
        }
    }

    fun <T> groupByArtist(items: List<T>, artistOf: (T) -> String): Map<String, List<T>> =
        items.groupBy { item ->
            val artist = artistOf(item)
            if (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)) {
                UNKNOWN_ARTIST
            } else {
                artist
            }
        }

    fun <T> groupByGenre(items: List<T>, genreOf: (T) -> String): Map<String, List<T>> =
        items.groupBy { item ->
            val genre = genreOf(item)
            if (genre.isBlank() || genre.equals("<unknown>", ignoreCase = true)) {
                UNKNOWN_GENRE
            } else {
                genre
            }
        }

    fun <T> groupByFolder(items: List<T>, pathOf: (T) -> String): Map<String, List<T>> =
        items.groupBy { item ->
            val path = pathOf(item)
            if (path.isBlank()) {
                NO_FOLDER
            } else {
                path.substringBeforeLast('/', missingDelimiterValue = NO_FOLDER).ifBlank { "/" }
            }
        }

    fun matchesQuery(title: String, artist: String, album: String, query: String): Boolean {
        if (query.isBlank()) return true
        return title.contains(query, ignoreCase = true) ||
            artist.contains(query, ignoreCase = true) ||
            album.contains(query, ignoreCase = true)
    }

    fun getAlbumSongsSorted(albumSongs: List<Song>): List<Song>{
        return albumSongs.sortedWith(
            compareBy<Song> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title }
        )
    }

    fun getArtisSorted(artistSong: List<Song>): List<Song>{
        return artistSong.sortedWith(
            compareBy<Song> { it.album }
                .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                .thenBy { it.title }
        )
    }
}
