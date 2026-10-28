package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class YtMusicTest {
    @Test fun cleanArtistQuitaTopicYVevo() {
        assertEquals("Moe Shop", YtMusic.cleanArtist("Moe Shop - Topic"))
        assertEquals("Adele", YtMusic.cleanArtist("AdeleVEVO"))
        assertEquals("Los Cuervos del Sur", YtMusic.cleanArtist("Los Cuervos del Sur"))
    }
}

class YtMusicCleanTitleTest {
    @org.junit.Test fun quitaElArtistaYElRuidoSoloSiElPrefijoEsElArtista() {
        org.junit.Assert.assertEquals("Canción", YtMusic.cleanTitle("Banda - Canción (Official Video)", "Banda - Topic"))
        org.junit.Assert.assertEquals("Remastered 2011 - Canción", YtMusic.cleanTitle("Remastered 2011 - Canción", "Banda"))
        org.junit.Assert.assertEquals("Canción", YtMusic.cleanTitle("Canción (Official Video)", "Banda"))
    }
}
