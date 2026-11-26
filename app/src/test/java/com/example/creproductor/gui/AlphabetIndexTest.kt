package com.example.creproductor.gui

import org.junit.Assert.assertEquals
import org.junit.Test

class AlphabetIndexTest {
    @Test fun inicialSinTildesNiSimbolos() {
        assertEquals("E", indexInitial("élan"))
        assertEquals("N", indexInitial("Ñandú"))
        assertEquals("#", indexInitial("2Pac"))
        assertEquals("#", indexInitial(""))
        assertEquals("B", indexInitial("  bad guy"))
    }

    @Test fun primeraPosicionDeCadaLetra() {
        assertEquals(listOf("#" to 0, "A" to 1, "B" to 3), indexPositions(listOf("1999", "Abba", "ánimo", "Beat", "bossa")))
    }
}
