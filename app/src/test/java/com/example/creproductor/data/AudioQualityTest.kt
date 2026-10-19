package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioQualityTest {
    @Test fun clasificaPorFormatoYResolucion() {
        assertEquals(QualityTier.LOSSY, AudioQuality.tier("audio/mpeg", "/a.mp3", 0, 44100))
        assertEquals(QualityTier.LOSSLESS, AudioQuality.tier("audio/flac", "/a.flac", 16, 44100))
        assertEquals(QualityTier.HIRES, AudioQuality.tier("audio/flac", "/a.flac", 24, 44100))
        assertEquals(QualityTier.HIRES, AudioQuality.tier(null, "/a.WAV", 0, 96000))
    }

    @Test fun etiquetaOmiteLoDesconocido() {
        assertEquals("FLAC · 24 bit · 96 kHz · 2304 kbps", AudioQuality.label("audio/flac", 96000, 24, 2304000))
        assertEquals("MP3 · 44.1 kHz", AudioQuality.label("audio/mpeg", 44100, 0, 0))
    }

    @Test fun replayGainSoloAtenua() {
        assertEquals(-6.5f, ReplayGain.parseDb("-6.50 dB")!!, 0.001f)
        assertEquals(-3f, ReplayGain.parseDb("-3,0 dB")!!, 0.001f)
        assertNull(ReplayGain.parseDb("nada"))
        assertEquals(1f, ReplayGain.volumeFor(null), 0f)
        assertEquals(1f, ReplayGain.volumeFor(+4f), 0f)
        assertEquals(0.5012f, ReplayGain.volumeFor(-6f), 0.001f)
    }
}
