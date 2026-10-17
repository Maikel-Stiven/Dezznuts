package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSerializerTest {

    @Test
    fun `round trip conserva los datos basicos`() {
        val original = listOf(
            Playlist(
                id = "id-1",
                name = "Rock clásico",
                coverUri = "content://cover/1",
                songIds = listOf(1L, 2L, 3L)
            ),
            Playlist(id = "id-2", name = "Chill", coverUri = null, songIds = emptyList())
        )

        val decoded = PlaylistSerializer.decode(PlaylistSerializer.encode(original))

        assertEquals(2, decoded.size)
        assertEquals("id-1", decoded[0].id)
        assertEquals("Rock clásico", decoded[0].name)
        assertEquals("content://cover/1", decoded[0].coverUri)
        assertEquals(listOf(1L, 2L, 3L), decoded[0].songIds)
        assertEquals(null, decoded[1].coverUri)
        assertTrue(decoded[1].songIds.isEmpty())
    }

    @Test
    fun `nombres con separadores no corrompen los datos`() {
        val tricky = listOf(
            Playlist(id = "a", name = "Rock; Pop | Jazz, Blues", coverUri = null, songIds = listOf(7L))
        )

        val decoded = PlaylistSerializer.decode(PlaylistSerializer.encode(tricky))

        assertEquals(1, decoded.size)
        assertEquals("Rock; Pop | Jazz, Blues", decoded[0].name)
        assertEquals(listOf(7L), decoded[0].songIds)
    }

    @Test
    fun `nombres con espacios acentos y simbolos sobreviven`() {
        val name = "Ñoño + 50% café & té"
        val decoded = PlaylistSerializer.decode(
            PlaylistSerializer.encode(listOf(Playlist(name = name)))
        )

        assertEquals(name, decoded.single().name)
    }

    @Test
    fun `lista vacia se codifica y decodifica como vacia`() {
        assertEquals("", PlaylistSerializer.encode(emptyList()))
        assertTrue(PlaylistSerializer.decode("").isEmpty())
        assertTrue(PlaylistSerializer.decode("   ").isEmpty())
    }

    @Test
    fun `ids invalidos se descartan`() {
        val decoded = PlaylistSerializer.decode("p1|Lista||1,2,abc,3")

        assertEquals(listOf(1L, 2L, 3L), decoded.single().songIds)
    }
}
