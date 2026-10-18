package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SongEncodingTest {

    @Test
    fun `repara mojibake de acentos`() {
        assertEquals("café", "cafÃ©".fixEncoding())
    }

    @Test
    fun `repara mojibake de enie`() {
        assertEquals("ñoño", "Ã±oÃ±o".fixEncoding())
    }

    @Test
    fun `deja intacto el texto normal`() {
        assertEquals("Hello World", "Hello World".fixEncoding())
        assertEquals("<unknown>", "<unknown>".fixEncoding())
        assertEquals("", "".fixEncoding())
    }

    @Test
    fun `no corrompe texto valido con A tilde o a circunfleja`() {
        assertEquals("NÃO", "NÃO".fixEncoding())
        assertEquals("pâte", "pâte".fixEncoding())
    }
}
