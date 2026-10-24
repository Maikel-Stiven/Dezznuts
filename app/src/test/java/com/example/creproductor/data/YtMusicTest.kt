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
