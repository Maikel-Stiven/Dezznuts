package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimatedCoverTest {
    @Test fun tomaLaListaHls() = assertEquals(
        "https://mvod.itunes.apple.com/x/P1_default.m3u8",
        AnimatedCover.parse("""{"name":"a","animated":"https://mvod.itunes.apple.com/x/P1_default.m3u8","videoUrl":"https://x/y.mp4"}""")
    )

    @Test fun albumSinBucle() = assertNull(AnimatedCover.parse("""{"name":"a","animated":null,"videoUrl":null}"""))

    @Test fun respuestaConError() = assertNull(AnimatedCover.parse("""{"error":"No matching tracks found"}"""))

    @Test fun jsonRoto() = assertNull(AnimatedCover.parse("<html>"))

    @Test fun noAceptaHttpPlano() = assertNull(AnimatedCover.parse("""{"animated":"http://x/y.m3u8"}"""))
}
