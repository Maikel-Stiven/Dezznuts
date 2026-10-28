package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BrokenTwinsTest {
    private fun k(path: String, title: String, ms: Long, artist: String = "A", exempt: Boolean = false) = TwinKey(path, title, artist, ms, exempt)
    private fun clean(vararg items: TwinKey) = items.toList().withoutBrokenTwinsBy { it }.map { it.path }

    @Test fun quitaLaGemelaA0ConElMismoTituloYArtista() =
        assertEquals(listOf("/s/a.m4a"), clean(k("/s/a.m4a", "Canción", 200_000), k("/s/.tmp/b.mp4", "canción", 0)))

    @Test fun quitaLaGemelaA0ConLaMismaRutaSinExtension() =
        assertEquals(listOf("/s/x.m4a"), clean(k("/s/x.m4a", "Uno", 90_000), k("/s/x.mp4", "Otro", 0)))

    @Test fun conservaUnaSinDuracionQueNoTieneGemela() =
        assertEquals(listOf("/s/raro.webm"), clean(k("/s/raro.webm", "Raro", 0)))

    @Test fun lamismaRutaDosVecesQuedaUnaVez() =
        assertEquals(listOf("/s/a.m4a"), clean(k("/s/a.m4a", "A", 5_000), k("/s/a.m4a", "A", 5_000)))

    @Test fun dosCancionesDistintasConIgualTituloEnOtroArtistaSeConservan() =
        assertEquals(listOf("/a.mp3", "/b.mp3"), clean(k("/a.mp3", "Intro", 60_000, "X"), k("/b.mp3", "Intro", 0, "Y")))

    @Test fun lasExentasNoSeTocan() =
        assertEquals(listOf("/a.flac", "/a.flac"), clean(k("/a.flac", "P1", 1_000, exempt = true), k("/a.flac", "P2", 1_000, exempt = true)))
}
