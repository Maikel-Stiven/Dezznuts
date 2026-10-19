package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BitPerfectTest {
    @Test fun formatoDelAudioTrackSegunSalidaFloatYFuente() {
        // 16 bits: siempre 16 bits
        assertEquals(MixerFormat(44_100, 16, false, 2), BitPerfect.trackFormat(44_100, 2, 16, floatOutput = true))
        // alta resolución sin salida float: ExoPlayer lo baja a 16 bits
        assertEquals(MixerFormat(96_000, 16, false, 2), BitPerfect.trackFormat(96_000, 2, 24, floatOutput = false))
        // alta resolución con salida float: float de 32 bits
        assertEquals(MixerFormat(96_000, 32, true, 2), BitPerfect.trackFormat(96_000, 2, 24, floatOutput = true))
        // profundidad desconocida: se infiere por la frecuencia
        assertEquals(MixerFormat(96_000, 32, true, 2), BitPerfect.trackFormat(96_000, 2, 0, floatOutput = true))
        assertEquals(MixerFormat(44_100, 16, false, 2), BitPerfect.trackFormat(44_100, 2, 0, floatOutput = true))
    }

    @Test fun eligeSoloCoincidenciaExacta() {
        val dac = listOf(
            MixerFormat(44_100, 16, false, 2), MixerFormat(44_100, 24, false, 2),
            MixerFormat(96_000, 24, false, 2), MixerFormat(96_000, 32, false, 2)
        )
        assertEquals(0, BitPerfect.choose(dac, MixerFormat(44_100, 16, false, 2)))
        assertEquals(2, BitPerfect.choose(dac, MixerFormat(96_000, 24, false, 2)))
        // frecuencia que el DAC no ofrece, canales distintos o float que no ofrece: sin bit-perfect
        assertNull(BitPerfect.choose(dac, MixerFormat(48_000, 16, false, 2)))
        assertNull(BitPerfect.choose(dac, MixerFormat(44_100, 16, false, 1)))
        assertNull(BitPerfect.choose(dac, MixerFormat(96_000, 32, true, 2)))
        assertNull(BitPerfect.choose(emptyList(), MixerFormat(44_100, 16, false, 2)))
    }

    @Test fun soloSeIntentaConLaSenalSinTocar() {
        assertTrue(BitPerfect.allowed(34, enabled = true, usbConnected = true, processorActive = false, offload = false))
        assertFalse(BitPerfect.allowed(33, true, true, false, false))   // Android 13
        assertFalse(BitPerfect.allowed(34, false, true, false, false))  // apagado
        assertFalse(BitPerfect.allowed(34, true, false, false, false))  // sin DAC USB
        assertFalse(BitPerfect.allowed(34, true, true, true, false))    // EQ / subida de ReplayGain
        assertFalse(BitPerfect.allowed(34, true, true, false, true))    // offload
    }

    @Test fun rutaRealIndicaBitPerfect() {
        assertEquals("Bit-perfect 96 kHz / 16 bit", AudioQuality.route(false, 16, 96_000, 48_000, bitPerfect = true))
        assertEquals("Offload", AudioQuality.route(true, 16, 96_000, 48_000, bitPerfect = true))
    }
}
