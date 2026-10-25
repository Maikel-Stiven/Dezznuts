package com.example.creproductor.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverFetcherTest {

    @Test
    fun `acepta la misma cancion aunque cambie el formato`() {
        assertTrue(CoverFetcher.isSameSong("Loser", "Tame Impala", "Loser (Remastered)", "Tame Impala"))
    }

    @Test
    fun `rechaza una cancion distinta`() {
        assertFalse(CoverFetcher.isSameSong("Loser", "Tame Impala", "Creep", "Radiohead"))
        assertFalse(CoverFetcher.isSameSong("Loser", "Tame Impala", "Loser", "Beck"))
    }

    @Test
    fun `con artista desconocido solo compara el titulo`() {
        assertTrue(CoverFetcher.isSameSong("Loser", "<Artista desconocido>", "Loser", "Beck"))
    }

    @Test fun ignoraAcentosEnElArtista() =
        org.junit.Assert.assertTrue(CoverFetcher.isSameSong("Todo Lo Fue", "Lenin Ramirez", "Todo Lo Fue", "Lenin Ramírez"))
}
