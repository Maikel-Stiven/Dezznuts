package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagSuggestionsTest {
    private fun s(title: String, artist: String = "<unknown>", file: String = "x.mp3") = TagSuggestions.suggest(title, artist, file)

    @Test fun separaArtistaYTituloDelNombre() {
        assertEquals(TagSuggestion("Loser", "Beck"), s("Beck - Loser"))
        assertEquals(TagSuggestion("Loser", "Beck"), s("Beck_-_Loser"))
        assertEquals(TagSuggestion("Loser", "Beck"), s("01. Beck - Loser"))
        assertEquals(TagSuggestion("Loser", "Beck"), s("01 - Beck - Loser"))
    }

    @Test fun noConfundeNumerosQueSonParteDelNombre() {
        assertEquals(TagSuggestion("21 Guns", "Green Day"), s("Green Day - 21 Guns"))
        assertEquals(TagSuggestion("Lukas Graham", "7 Years"), s("7 Years - Lukas Graham"))   // "7 Years" no es número de pista
    }

    @Test fun usaElNombreDelArchivoSiElTituloNoAyuda() {
        assertEquals(TagSuggestion("Loser", "Beck"), s("track", file = "Beck - Loser.flac"))
    }

    @Test fun quitaRuidoDeYoutube() {
        assertEquals(TagSuggestion("Loser", "Beck"), s("Beck - Loser (Official Video)"))
    }

    @Test fun sinSugerenciaSiElArtistaYaEsConocidoOSinSeparador() {
        assertNull(s("Beck - Loser", artist = "Beck"))
        assertNull(s("Nota de voz"))
        assertEquals(true, TagSuggestions.isUnknown("Artista desconocido"))
        assertEquals(true, TagSuggestions.isUnknown(""))
        assertEquals(false, TagSuggestions.isUnknown("Beck"))
    }
}
