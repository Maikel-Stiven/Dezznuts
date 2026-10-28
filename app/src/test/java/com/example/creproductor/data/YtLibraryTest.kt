package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YtLibraryTest {
    // Mismo esqueleto que devuelve InnerTube para una lista (reducido a lo que se lee).
    private val sample = """{"contents":{"twoColumn":{"shelf":{"musicShelfRenderer":{"contents":[
      {"musicResponsiveListItemRenderer":{
        "playlistItemData":{"videoId":"kPa7bsKwL-c"},
        "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"https://i/small.jpg"},{"url":"https://i/big.jpg"}]}}},
        "flexColumns":[
          {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Die With A Smile"}]}}},
          {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Lady Gaga"},{"text":" y "},{"text":"Bruno Mars"}]}}}],
        "fixedColumns":[{"musicResponsiveListItemFixedColumnRenderer":{"text":{"runs":[{"text":"4:13"}]}}}]}},
      {"musicResponsiveListItemRenderer":{"flexColumns":[]}}
    ]}}}}}"""

    @Test fun leeLaCancionYIgnoraFilasSinVideo() {
        val t = YtLibrary.parse(sample).single()
        assertEquals("https://music.youtube.com/watch?v=kPa7bsKwL-c", t.url)
        assertEquals("Die With A Smile", t.title)
        assertEquals("Lady Gaga y Bruno Mars", t.artist)
        assertEquals(253L, t.durationSec)
        assertEquals("https://i/big.jpg", t.thumbnail)
    }

    @Test fun jsonRotoDaListaVacia() = assertEquals(0, YtLibrary.parse("<html>").size)

    @Test fun duraciones() {
        assertEquals(253L, YtLibrary.parseDuration("4:13"))
        assertEquals(3723L, YtLibrary.parseDuration("1:02:03"))
        assertEquals(0L, YtLibrary.parseDuration(""))
    }

    @Test fun cabeceraSapisidhash() {
        // sha1("1700000000 abc https://music.youtube.com") calculado aparte
        assertEquals("SAPISIDHASH 1700000000_" + java.security.MessageDigest.getInstance("SHA-1")
            .digest("1700000000 abc https://music.youtube.com".toByteArray()).joinToString("") { "%02x".format(it) },
            YtLibrary.authHeader("abc", 1700000000))
    }

    @Test fun sapisidDeLaCookie() {
        assertEquals("XYZ", YtLibrary.sapisid("a=1; SAPISID=XYZ; b=2"))
        assertNull(YtLibrary.sapisid("a=1; b=2"))
    }
}
