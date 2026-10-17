package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeTitleCleanerTest {

    @Test
    fun `separa artista y titulo con guion`() {
        val result = YoutubeTitleCleaner.parseFileName("Daft Punk - Get Lucky.mp3")
        assertEquals("Daft Punk", result.cleanArtist)
        assertEquals("Get Lucky", result.cleanTitle)
    }

    @Test
    fun `quita etiquetas entre parentesis y la extension`() {
        val result = YoutubeTitleCleaner.parseFileName("Daft Punk - Get Lucky (Official Video).mp3")
        assertEquals("Daft Punk", result.cleanArtist)
        assertEquals("Get Lucky", result.cleanTitle)
    }

    @Test
    fun `quita etiquetas entre corchetes`() {
        val result = YoutubeTitleCleaner.parseFileName("Some Artist - Song [4K]")
        assertEquals("Some Artist", result.cleanArtist)
        assertEquals("Song", result.cleanTitle)
    }

    @Test
    fun `detecta descargas de topic`() {
        val result = YoutubeTitleCleaner.parseFileName("Some Artist - Song [Topic]")
        assertTrue(result.isTopicDownload)
        assertEquals("Some Artist", result.cleanArtist)
        assertEquals("Song", result.cleanTitle)
    }

    @Test
    fun `sin separador deja artista desconocido`() {
        val result = YoutubeTitleCleaner.parseFileName("Just A Title.mp3")
        assertEquals("Artista desconocido", result.cleanArtist)
        assertEquals("Just A Title", result.cleanTitle)
        assertFalse(result.isTopicDownload)
    }

    @Test
    fun `soporta guion largo`() {
        val result = YoutubeTitleCleaner.parseFileName("Artista – Título")
        assertEquals("Artista", result.cleanArtist)
        assertEquals("Título", result.cleanTitle)
    }

    @Test
    fun `quita comillas sobrantes`() {
        val result = YoutubeTitleCleaner.parseFileName("\"Artist\" - \"Title\"")
        assertEquals("Artist", result.cleanArtist)
        assertEquals("Title", result.cleanTitle)
    }
}
