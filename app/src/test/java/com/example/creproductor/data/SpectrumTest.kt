package com.example.creproductor.data

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpectrumTest {
    private fun sine(freq: Double, rate: Int, n: Int, amp: Double = 1.0) = FloatArray(n) { (amp * sin(2 * PI * freq * it / rate)).toFloat() }

    @Test fun unaSenoidalCaeEnSuBinConSuAmplitud() {
        val s = Spectrum(2048)
        s.analyze(sine(1000.0, 48000, 2048, 0.5))
        val peak = s.magnitudes.indices.maxBy { s.magnitudes[it] }
        assertEquals(1000.0, peak * 48000.0 / 2048, 48000.0 / 2048)      // dentro de un bin
        assertEquals(0.5f, s.magnitudes[peak], 0.1f)
    }

    @Test fun elSilencioDaBarrasA0() {
        val s = Spectrum(1024); s.analyze(FloatArray(1024))
        val out = FloatArray(32); s.bars(44100, out)
        assertTrue(out.all { it == 0f })
    }

    @Test fun lasBarrasSubenDondeEstaLaFrecuencia() {
        val s = Spectrum(2048); s.analyze(sine(440.0, 44100, 2048))
        val out = FloatArray(40); s.bars(44100, out)
        val top = out.indices.maxBy { out[it] }
        // 440 Hz en escala log entre 40 y 16000 Hz: ≈ 40 % del recorrido
        assertTrue("barra $top", top in 12..20)
        assertTrue(out[top] > 0.8f)
        assertTrue(out[35] < 0.2f)
    }

    @Test fun lasBarrasBajanDespacioYSuben() {
        val sm = CavaSmoother(8)
        sm.step(FloatArray(8) { 1f }, 0.016f)
        assertEquals(1f, sm.shown[3], 0.001f)
        repeat(3) { sm.step(FloatArray(8), 0.016f) }
        assertTrue(sm.shown[3] in 0.5f..0.999f)      // cae, pero no de golpe
    }

    @Test(expected = IllegalArgumentException::class) fun tamanoNoPotenciaDe2() { Spectrum(1000) }
}
