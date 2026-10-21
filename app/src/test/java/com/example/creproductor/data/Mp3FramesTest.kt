package com.example.creproductor.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class Mp3FramesTest {
    /** MPEG1 Layer III, 128 kbps, 44100 Hz, sin padding: trama de 417 bytes y 26 122 µs. */
    private fun frame(fill: Int): ByteArray =
        ByteArray(417) { fill.toByte() }.also { it[0] = 0xFF.toByte(); it[1] = 0xFB.toByte(); it[2] = 0x90.toByte(); it[3] = 0x00 }

    private fun mp3(frames: Int, id3Payload: Int = 0): ByteArray {
        val id3 = if (id3Payload == 0) ByteArray(0) else byteArrayOf(
            'I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte(), 3, 0, 0, 0, 0, 0, id3Payload.toByte()
        ) + ByteArray(id3Payload)
        return id3 + (0 until frames).map { frame(it + 1) }.reduce { a, b -> a + b }
    }

    @Test fun `cuenta tramas, longitud y tiempos`() {
        val f = Mp3Frames.parse(mp3(10))
        assertEquals(10, f.size)
        assertEquals(417, f[0].length)
        assertEquals(26_122L, f[0].durationUs)
        assertEquals(3 * 26_122L, f[3].startUs)
    }

    @Test fun `salta la etiqueta ID3v2`() {
        val f = Mp3Frames.parse(mp3(4, id3Payload = 20))
        assertEquals(4, f.size)
        assertEquals(30, f[0].offset)   // 10 de cabecera + 20 de contenido
    }

    @Test fun `el corte devuelve exactamente las tramas del tramo`() {
        val data = mp3(10)
        val f = Mp3Frames.parse(data)
        val cut = Mp3Frames.cut(data, f, startUs = f[2].startUs, endUs = f[5].startUs)!!   // tramas 2, 3 y 4
        assertEquals(3 * 417, cut.size)
        assertArrayEquals(data.copyOfRange(2 * 417, 5 * 417), cut)
    }

    @Test fun `tramo vacio o fuera de rango da null y basura no da tramas`() {
        val data = mp3(3)
        assertNull(Mp3Frames.cut(data, Mp3Frames.parse(data), 10_000_000L, 20_000_000L))
        assertEquals(0, Mp3Frames.parse(ByteArray(100)).size)
    }
}
