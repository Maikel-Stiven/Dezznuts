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
        val g = Moods.group(listOf("Dance", "Blues", "Desconocido")) { it }
        assertEquals(listOf("Dance"), g[Mood.PARTY])
        assertEquals(listOf("Blues"), g[Mood.MELANCHOLY])
        assertFalse(g.containsKey(Mood.FOCUS))
    }
}
