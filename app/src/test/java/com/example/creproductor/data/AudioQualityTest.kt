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

class AudioRouteTest {
    @Test fun rutaRealDistingueOffloadRemuestreoYDirecto() {
        assertEquals("Offload", AudioQuality.route(true, 0, 44100, 48000))
        assertEquals("PCM 16 bit 44.1 kHz → remuestreado a 48 kHz", AudioQuality.route(false, 16, 44100, 48000))
        assertEquals("PCM 24 bit 96 kHz directo", AudioQuality.route(false, 24, 96000, 96000))
        assertEquals(24, AudioQuality.bitsOfPcmEncoding(21))
    }
}

class AudioHeaderTest {
    @Test fun leeStreamInfoDeFlac() {
        // STREAMINFO: frecuencia (20 bits) | canales-1 (3) | bits-1 (5) empaquetados desde el byte 18
        val b = ByteArray(64)
        "fLaC".forEachIndexed { i, c -> b[i] = c.code.toByte() }
        val rate = 96000; val ch = 2; val bits = 24
        val v = (rate.toLong() shl 44) or ((ch - 1).toLong() shl 41) or ((bits - 1).toLong() shl 36)
        for (i in 0 until 5) b[18 + i] = ((v shr (56 - 8 * i)) and 0xFF).toByte()
        assertEquals(AudioHeader.Info(96000, 24), AudioHeader.parse(b))
    }

    @Test fun leeFmtDeWav() {
        val b = ByteArray(64)
        "RIFF".forEachIndexed { i, c -> b[i] = c.code.toByte() }
        "WAVEfmt ".forEachIndexed { i, c -> b[8 + i] = c.code.toByte() }
        b[24] = 0x80.toByte(); b[25] = 0xBB.toByte() // 48000 = 0xBB80
        b[34] = 24
        assertEquals(AudioHeader.Info(48000, 24), AudioHeader.parse(b))
        assertNull(AudioHeader.parse(ByteArray(64)))
    }
}
