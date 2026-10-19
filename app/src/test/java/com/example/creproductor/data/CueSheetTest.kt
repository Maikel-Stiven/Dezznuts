package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CueSheetTest {
    private val cue = """
        PERFORMER "Banda"
        TITLE "Disco"
        FILE "disco.flac" WAVE
          TRACK 01 AUDIO
            TITLE "Uno"
            INDEX 01 00:00:00
          TRACK 02 AUDIO
            TITLE "Dos"
            PERFORMER "Invitado"
            INDEX 00 03:59:50
            INDEX 01 04:00:37
          TRACK 03 AUDIO
            INDEX 01 07:30:00
    """.trimIndent()

    @Test fun partePistasConTiemposYMetadatos() {
        val sheet = CueSheet.parse(cue, totalDurationMs = 600_000)!!
        assertEquals("Disco", sheet.album)
        assertEquals("disco.flac", sheet.fileName)
        assertEquals(3, sheet.tracks.size)
        assertEquals(0L, sheet.tracks[0].startMs)
        assertEquals("Banda", sheet.tracks[0].performer)            // hereda el del álbum
        assertEquals(240_000L + 37 * 1000 / 75, sheet.tracks[1].startMs)   // usa INDEX 01, no INDEX 00
        assertEquals("Invitado", sheet.tracks[1].performer)
        assertEquals(sheet.tracks[2].startMs, sheet.tracks[1].endMs)       // el fin es el inicio de la siguiente
        assertEquals("Pista 3", sheet.tracks[2].title)                      // sin título → nombre por defecto
        assertEquals(600_000L, sheet.tracks[2].endMs)                       // la última cierra con la duración
    }

    @Test fun rechazaCuesQueNoSePuedenUsar() {
        assertNull(CueSheet.parse("FILE \"a.flac\" WAVE\nFILE \"b.flac\" WAVE\nTRACK 01 AUDIO\nINDEX 01 00:00:00", 1000))
        assertNull(CueSheet.parse("FILE \"a.flac\" WAVE\nTRACK 01 AUDIO\nINDEX 01 00:00:00", 1000)) // una sola pista
        assertNull(CueSheet.parse("basura", 1000))
    }
}

class FlacTagsTest {
    private fun le32(n: Int) = byteArrayOf(n.toByte(), (n shr 8).toByte(), (n shr 16).toByte(), (n shr 24).toByte())

    /** FLAC mínimo: STREAMINFO (34 bytes, relleno) + VORBIS_COMMENT con las etiquetas dadas. */
    private fun flac(vararg tags: String): java.io.ByteArrayInputStream {
        val vendor = "test".toByteArray()
        var body = le32(vendor.size) + vendor + le32(tags.size)
        tags.forEach { val t = it.toByteArray(); body += le32(t.size) + t }
        fun block(type: Int, last: Boolean, data: ByteArray): ByteArray {
            val h = (if (last) 0x80 else 0) or type
            return byteArrayOf(h.toByte(), (data.size shr 16).toByte(), (data.size shr 8).toByte(), data.size.toByte()) + data
        }
        return java.io.ByteArrayInputStream("fLaC".toByteArray() + block(0, false, ByteArray(34)) + block(4, true, body))
    }

    @Test fun encuentraLaEtiquetaSinImportarMayusculas() {
        assertEquals("a\nb", FlacTags.read(flac("TITLE=x", "cuesheet=a\nb"), "CUESHEET"))
    }

    @Test fun devuelveNullSiNoEstaONoEsFlac() {
        assertNull(FlacTags.read(flac("TITLE=x"), "CUESHEET"))
        assertNull(FlacTags.read(java.io.ByteArrayInputStream("RIFFxxxxxxxx".toByteArray()), "CUESHEET"))
    }
}
