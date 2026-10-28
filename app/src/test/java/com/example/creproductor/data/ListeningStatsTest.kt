package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ListeningStatsTest {
    private fun e(id: Long, artist: String, plays: Int, durMs: Long = 60_000) = PlayedEntry(id, artist, plays, durMs)

    @Test fun sinReproduccionesEstaVacio() {
        val s = ListeningStats.compute(listOf(e(1, "A", 0)))
        assertEquals(0, s.plays); assertEquals(0L, s.listenedMs); assertEquals(emptyList<Any>(), s.topSongs)
    }

    @Test fun sumaTiempoYOrdenaRankings() {
        val s = ListeningStats.compute(listOf(e(1, "A", 2), e(2, "B", 5), e(3, "A", 4, durMs = 120_000), e(4, "<unknown>", 9)))
        assertEquals(20, s.plays)
        assertEquals(2 * 60_000L + 5 * 60_000L + 4 * 120_000L + 9 * 60_000L, s.listenedMs)
        assertEquals(listOf(4L to 9, 2L to 5, 3L to 4, 1L to 2), s.topSongs)
        assertEquals(listOf("A" to 6, "B" to 5), s.topArtists)   // "<unknown>" no entra
    }
}
