package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodsTest {
    @Test fun `reconoce el genero sin importar mayusculas ni etiquetas compuestas`() {
        assertTrue(Moods.matches(Mood.ENERGY, "Hard Rock"))
        assertTrue(Moods.matches(Mood.CHILL, "Jazz;Lounge"))
        assertTrue(Moods.matches(Mood.PARTY, "Electrónica".replace("ó", "o")))
    }

    @Test fun `sin genero no cae en ningun mood`() {
        assertFalse(Mood.entries.any { Moods.matches(it, "") })
        assertFalse(Mood.entries.any { Moods.matches(it, "Desconocido") })
    }

    @Test fun `agrupa, repite canciones entre moods y omite los vacios`() {
        val g = Moods.group(listOf("Dance", "Blues", "Desconocido"), { it })
        assertEquals(listOf("Dance"), g[Mood.PARTY])
        assertEquals(listOf("Blues"), g[Mood.MELANCHOLY])
        assertFalse(g.containsKey(Mood.FOCUS))
        assertEquals(listOf("Desconocido"), g[Mood.OTHER])      // sin pistas: va a «Otras», no se pierde
    }

    private data class S(val title: String, val genre: String, val artist: String = "", val path: String = "")

    private fun mood(vararg songs: S) = Moods.group(songs.toList(), { it.genre }, { it.title + " " + it.path }, { it.artist })

    @Test fun `una descarga sin genero entra por las palabras del titulo`() {
        val g = mood(S("Canción triste (Official Audio)", "Desconocido"), S("Beat lofi para estudiar", ""))
        assertEquals(listOf("Canción triste (Official Audio)"), g[Mood.MELANCHOLY]?.map { it.title })
        assertTrue(g[Mood.CHILL]?.any { it.title.startsWith("Beat lofi") } == true)
    }

    @Test fun `hereda el genero de otras canciones del mismo artista`() {
        val g = mood(S("Uno", "Rock", "Los Cuervos"), S("Dos sin etiquetar", "Desconocido", "los cuervos"))
        assertEquals(2, g[Mood.ENERGY]?.size)
    }

    @Test fun `una palabra corta solo cuenta entera`() {
        val g = mood(S("Rocket Man", ""))      // "rock" dentro de "rocket" no es rock
        assertFalse(g.containsKey(Mood.ENERGY))
        assertEquals(1, g[Mood.OTHER]?.size)
    }

    @Test fun `los acentos no importan`() {
        assertTrue(Moods.matchesText(Mood.MELANCHOLY, "Adiós"))
        assertTrue(Moods.matches(Mood.CHILL, "Acústico"))
    }
}
