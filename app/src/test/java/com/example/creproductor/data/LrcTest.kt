package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcTest {
    @Test fun `lee tiempos con distintas fracciones y descarta metadatos`() {
        val lines = Lrc.parse("[ar:Alguien]\n[ti:Algo]\n[00:12.5]uno\n[01:02.50]dos\n[00:03]tres\n[02:00.123]cuatro")!!
        assertEquals(listOf("tres", "uno", "dos", "cuatro"), lines.map { it.text })
        assertEquals(listOf(3_000L, 12_500L, 62_500L, 120_123L), lines.map { it.timeMs })
    }

    @Test fun `varias marcas en una linea y marcas de palabra`() {
        val lines = Lrc.parse("[00:10.00][00:50.00]<00:10.00>es <00:10.50>estribillo")!!
        assertEquals(2, lines.size)
        assertEquals("es estribillo", lines[0].text)
        assertEquals(50_000L, lines[1].timeMs)
    }

    @Test fun `offset positivo adelanta la letra`() {
        assertEquals(9_500L, Lrc.parse("[offset:500]\n[00:10.00]x")!![0].timeMs)
        assertEquals(0L, Lrc.parse("[offset:99999]\n[00:10.00]x")!![0].timeMs)   // nunca negativo
    }

    @Test fun `linea activa por busqueda binaria`() {
        val l = Lrc.parse("[00:05.00]a\n[00:10.00]b\n[00:20.00]c")!!
        assertEquals(-1, Lrc.activeIndex(l, 4_999))
        assertEquals(0, Lrc.activeIndex(l, 5_000))
        assertEquals(1, Lrc.activeIndex(l, 19_999))
        assertEquals(2, Lrc.activeIndex(l, 999_999))
        assertEquals(-1, Lrc.activeIndex(emptyList(), 100))
    }

    @Test fun `texto sin marcas es plano y una letra vacia es nula`() {
        val ly = Lrc.fromText("hola\nmundo\n\n\n\nadios\n", "archivo")!!
        assertFalse(ly.synced)
        assertEquals(listOf("hola", "mundo", "", "adios"), ly.lines.map { it.text })
        assertTrue(Lrc.fromText("[00:01.00]x", "red")!!.synced)
        assertNull(Lrc.fromText("   \n ", "red"))
        assertNull(Lrc.parse("sin tiempos"))
    }
}
