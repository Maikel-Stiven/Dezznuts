package com.example.creproductor.data

import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Test

class ParametricEqTest {
    private val fs = 48_000

    /** Ganancia medida (dB) de un tono puro al pasar por el ecualizador, descartando el transitorio. */
    private fun measureDb(bands: List<EqBand>, toneHz: Double, extra: Float = 1f): Double {
        val eq = ParametricEq(fs, 1).apply { setBands(bands); extraGain = extra }
        val n = fs
        val buf = FloatArray(n) { (0.1 * sin(2 * PI * toneHz * it / fs)).toFloat() }
        eq.process(buf, n)
        fun rms(a: FloatArray, from: Int) = sqrt(a.drop(from).map { it.toDouble() * it }.average())
        val ref = FloatArray(n) { (0.1 * sin(2 * PI * toneHz * it / fs)).toFloat() }
        return 20 * log10(rms(buf, n / 2) / rms(ref, n / 2))
    }

    @Test fun subeLaBandaEnSuFrecuenciaCentral() {
        // +6 dB a 1 kHz; el preamp automático resta 6 dB → neto ≈ 0 dB en el centro
        val bands = listOf(EqBand(1000f, 6f, 1f))
        assertEquals(0.0, measureDb(bands, 1000.0), 0.15)
        // lejos de la banda (100 Hz) solo actúa el preamp: ≈ −6 dB
        assertEquals(-6.0, measureDb(bands, 100.0), 0.3)
    }

    @Test fun recortaLaBandaSinPreamp() {
        // −6 dB: no hay subida, así que no hay preamp; en el centro baja ~6 dB y fuera no cambia
        val bands = listOf(EqBand(1000f, -6f, 1f))
        assertEquals(-6.0, measureDb(bands, 1000.0), 0.15)
        assertEquals(0.0, measureDb(bands, 100.0), 0.3)
    }

    @Test fun planoEsTransparente() {
        assertEquals(0.0, measureDb(EqBands.DEFAULT, 440.0), 0.001)
    }

    @Test fun gananciaExtraSeAplica() {
        assertEquals(6.0, measureDb(EqBands.DEFAULT, 440.0, extra = 1.9953f), 0.01)
    }

    @Test fun codificaYDecodificaConLimites() {
        val b = listOf(EqBand(60f, 3.5f, 1.2f), EqBand(14000f, -12f, 4f))
        assertEquals(b, EqBands.decode(EqBands.encode(b)))
        assertEquals(EqBands.DEFAULT, EqBands.decode("basura"))
        assertEquals(12f, EqBands.decode("1000:99:1").first().gainDb, 0f)
    }
}

class ParametricEqRobustnessTest {
    /** El hilo de audio procesa mientras la interfaz cambia las bandas (activar/desactivar bandas incluido). */
    @Test fun cambiarBandasMientrasSeProcesaNoFalla() {
        val eq = ParametricEq(48_000, 2).apply { setBands(EqBands.DEFAULT) }
        val error = java.util.concurrent.atomic.AtomicReference<Throwable?>(null)
        val stop = java.util.concurrent.atomic.AtomicBoolean(false)
        val ui = Thread {
            var n = 0
            while (!stop.get()) {
                // alterna entre 0, 3 y 5 bandas activas
                val active = intArrayOf(0, 3, 5)[n++ % 3]
                eq.setBands(EqBands.DEFAULT.mapIndexed { i, b -> if (i < active) b.copy(gainDb = 4f) else b })
            }
        }
        val audio = Thread {
            try {
                val buf = FloatArray(2 * 480) { 0.1f }
                repeat(20_000) { eq.process(buf, 480) }
            } catch (t: Throwable) { error.set(t) }
        }
        ui.start(); audio.start(); audio.join(); stop.set(true); ui.join()
        assertEquals(null, error.get())
    }

    /** Presupuesto de tiempo real: 1 s de audio estéreo a 96 kHz con 5 bandas debe procesarse en mucho menos de 1 s. */
    @Test fun procesaMuchoMasRapidoQueTiempoReal() {
        val eq = ParametricEq(96_000, 2).apply { setBands(EqBands.DEFAULT.map { it.copy(gainDb = 3f) }) }
        val buf = FloatArray(2 * 96_000) { (it % 100) / 1000f }
        eq.process(buf, 96_000) // calentamiento de la JVM
        val t0 = System.nanoTime()
        repeat(5) { eq.process(buf, 96_000) }
        val msPerSecondOfAudio = (System.nanoTime() - t0) / 5 / 1_000_000
        println("EQ 5 bandas, 96 kHz estéreo: $msPerSecondOfAudio ms por segundo de audio")
        org.junit.Assert.assertTrue("demasiado lento: $msPerSecondOfAudio ms", msPerSecondOfAudio < 250)
    }
}

class EqResponseTest {
    @Test fun lasCurvaDibujadaCoincideConLaGananciaDeLaBanda() {
        val bands = listOf(EqBand(1000f, 6f, 1f))
        // en la frecuencia central, la respuesta es exactamente la ganancia de la banda
        assertEquals(6f, PeakingFilter.responseDb(bands, 1000f), 0.05f)
        // muy lejos de la banda, ≈ 0 dB
        assertEquals(0f, PeakingFilter.responseDb(bands, 30f), 0.2f)
        assertEquals(0f, PeakingFilter.responseDb(bands, 18000f), 0.5f)
        // plano = 0 dB en todas partes
        assertEquals(0f, PeakingFilter.responseDb(EqBands.DEFAULT, 440f), 0.001f)
    }

    @Test fun lasBandasSeSumanEnDb() {
        val two = listOf(EqBand(1000f, 3f, 1f), EqBand(1000f, 3f, 1f))
        assertEquals(6f, PeakingFilter.responseDb(two, 1000f), 0.1f)
    }

    @Test fun reconocePresets() {
        assertEquals("Plano", EqBands.presetNameOf(EqBands.DEFAULT))
        assertEquals("Graves", EqBands.presetNameOf(EqBands.PRESETS[1].second))
        assertEquals(null, EqBands.presetNameOf(EqBands.DEFAULT.map { it.copy(gainDb = 1f) }))
    }

    /** La curva dibujada debe coincidir con lo que mide el filtro real sobre un tono (misma fórmula). */
    @Test fun graficaYAudioCoinciden() {
        val bands = listOf(EqBand(2000f, 5f, 1.5f))
        val fs = 48_000
        val eq = ParametricEq(fs, 1).apply { setBands(bands); extraGain = 10f.let { Math.pow(10.0, 5.0 / 20).toFloat() } } // anula el preamp
        val tone = 2000.0
        val buf = FloatArray(fs) { (0.1 * kotlin.math.sin(2 * Math.PI * tone * it / fs)).toFloat() }
        eq.process(buf, fs)
        val rms = kotlin.math.sqrt(buf.drop(fs / 2).map { it.toDouble() * it }.average())
        val ref = 0.1 / kotlin.math.sqrt(2.0)
        assertEquals(PeakingFilter.responseDb(bands, 2000f).toDouble(), 20 * kotlin.math.log10(rms / ref), 0.1)
    }
}
