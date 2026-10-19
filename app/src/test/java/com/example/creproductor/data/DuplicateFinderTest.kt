package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DuplicateFinderTest {
    private data class Item(val id: Int, val title: String, val artist: String, val durMs: Long, val rank: Int)

    private fun dups(vararg items: Item) =
        DuplicateFinder.find(items.toList(), { it.title }, { it.artist }, { it.durMs }, compareByDescending<Item> { it.rank })

    @Test fun normalizaTituloSinAcentosNiParentesis() {
        assertEquals("senor", DuplicateFinder.normalize("Señor (Remastered 2011)!"))
        assertEquals("loser", DuplicateFinder.normalize("  LOSER [Live] "))
    }

    @Test fun agrupaPorTituloArtistaYDuracionYConservaLaMejor() {
        val g = dups(
            Item(1, "Loser", "Beck", 233_000, rank = 1),
            Item(2, "Loser (Remastered)", "BECK", 233_800, rank = 5),
            Item(3, "Loser", "Beck", 400_000, rank = 9),   // misma etiqueta, otra duración: no es copia
            Item(4, "Otra", "Beck", 233_000, rank = 9)
        )
        assertEquals(1, g.size)
        assertEquals(2, g[0].keep.id)
        assertEquals(listOf(1), g[0].copies.map { it.id })
    }

    @Test fun sinRepetidosNoDevuelveNada() {
        assertEquals(0, dups(Item(1, "A", "x", 1000, 1), Item(2, "B", "x", 1000, 1)).size)
    }
}
