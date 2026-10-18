package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SongOrganizerTest {

    private data class FakeSong(
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val path: String
    )

    private val songs = listOf(
        FakeSong("A", "Daft Punk", "Discovery", "Electrónica", "/Music/Daft Punk/a.mp3"),
        FakeSong("B", "Daft Punk", "Discovery", "Electrónica", "/Music/Daft Punk/b.mp3"),
        FakeSong("C", "Queen", "News of the World", "Rock", "/Music/Queen/c.mp3"),
        FakeSong("D", "<unknown>", "<unknown>", "", "/Music/misc/d.mp3"),
        FakeSong("E", "", "unknown", "Desconocido", "")
    )

    @Test
    fun `agrupa por album y excluye desconocidos`() {
        val albums = SongOrganizer.groupByAlbum(
            items = songs,
            albumOf = {it.album}
        )

        assertEquals(setOf("Discovery", "News of the World"), albums.keys)
        assertEquals(2, albums["Discovery"]?.size)
    }

    @Test
    fun `agrupa por artista mapeando desconocidos`() {
        val artists = SongOrganizer.groupByArtist(songs) { it.artist }

        assertTrue(artists.containsKey("Daft Punk"))
        assertTrue(artists.containsKey("Queen"))
        assertTrue(artists.containsKey(SongOrganizer.UNKNOWN_ARTIST))
        // El artista "<unknown>" y el vacío caen en la misma categoría
        assertEquals(2, artists[SongOrganizer.UNKNOWN_ARTIST]?.size)
    }

    @Test
    fun `agrupa por genero mapeando desconocidos`() {
        val genres = SongOrganizer.groupByGenre(songs) { it.genre }

        assertEquals(2, genres["Electrónica"]?.size)
        assertTrue(genres.containsKey("Rock"))
        assertTrue(genres.containsKey(SongOrganizer.UNKNOWN_GENRE))
    }

    @Test
    fun `agrupa por carpeta`() {
        val folders = SongOrganizer.groupByFolder(songs) { it.path }

        assertEquals(2, folders["/Music/Daft Punk"]?.size)
        assertTrue(folders.containsKey("/Music/Queen"))
        assertTrue(folders.containsKey("/Music/misc"))
        assertTrue(folders.containsKey(SongOrganizer.NO_FOLDER))
    }

    @Test
    fun `busqueda encuentra por titulo artista o album`() {
        assertTrue(SongOrganizer.matchesQuery("Get Lucky", "Daft Punk", "Discovery", "lucky"))
        assertTrue(SongOrganizer.matchesQuery("Get Lucky", "Daft Punk", "Discovery", "daft"))
        assertTrue(SongOrganizer.matchesQuery("Get Lucky", "Daft Punk", "Discovery", "disco"))
        assertFalse(SongOrganizer.matchesQuery("Get Lucky", "Daft Punk", "Discovery", "queen"))
    }

    @Test
    fun `busqueda vacia coincide con todo`() {
        assertTrue(SongOrganizer.matchesQuery("a", "b", "c", ""))
        assertTrue(SongOrganizer.matchesQuery("a", "b", "c", "   "))
    }
}
