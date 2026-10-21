package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverAccentTest {
    private fun hue(argb: Int): Int {
        val r = argb shr 16 and 0xFF; val g = argb shr 8 and 0xFF; val b = argb and 0xFF
        return when (maxOf(r, g, b)) { r -> 0; g -> 120; else -> 240 }
    }

    @Test
    fun `carátula gris no da acento`() {
        assertNull(CoverAccent.fromPixels(IntArray(576) { 0xFF808080.toInt() }))
    }

    @Test
    fun `gana el tono dominante aunque haya otro`() {
        val px = IntArray(576) { if (it < 400) 0xFF1E3CC8.toInt() else 0xFFC81E1E.toInt() }
        assertEquals(240, hue(CoverAccent.fromPixels(px)!!))
    }

    @Test
    fun `el rojo a ambos lados de 0 grados no se parte`() {
        val px = IntArray(576) { if (it % 2 == 0) 0xFFE60A1E.toInt() else 0xFFE61E0A.toInt() }
        assertEquals(0, hue(CoverAccent.fromPixels(px)!!))
    }

    @Test
    fun `el acento es brillante para leerse sobre fondo oscuro`() {
        val a = CoverAccent.fromPixels(IntArray(576) { 0xFF2A0F40.toInt() })!!
        assertTrue(maxOf(a shr 16 and 0xFF, a shr 8 and 0xFF, a and 0xFF) > 220)
    }
}
