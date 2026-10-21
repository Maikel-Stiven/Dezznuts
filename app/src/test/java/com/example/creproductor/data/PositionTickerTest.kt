package com.example.creproductor.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del bucle de posición (ver [trackPlaybackPosition]).
 *
 * Con estas tres se protege el ahorro real: sin colectores no hay ticks, en pausa tampoco y,
 * al reanudar, vuelven cada 500 ms. Si alguien añade un colector nuevo de la posición, la
 * primera prueba deja de cumplirse.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PositionTickerTest {

    @Test
    fun `sin suscriptores no hay ticks`() = runTest {
        val posicion = MutableStateFlow(0L)
        val reproduciendo = MutableStateFlow(true)
        var lecturas = 0

        backgroundScope.trackPlaybackPosition(posicion, reproduciendo) { lecturas++; 10L }

        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()

        // Solo la lectura única del bloque inactivo: nadie observa la posición.
        assertEquals(1, lecturas)
    }

    @Test
    fun `en pausa no hay ticks`() = runTest {
        val posicion = MutableStateFlow(0L)
        val reproduciendo = MutableStateFlow(false)
        var lecturas = 0

        backgroundScope.launch { posicion.collect {} }
        backgroundScope.trackPlaybackPosition(posicion, reproduciendo) { lecturas++; 10L }

        runCurrent()
        val iniciales = lecturas
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(iniciales, lecturas)
    }

    @Test
    fun `al reanudar vuelven los ticks cada 500 ms`() = runTest {
        val posicion = MutableStateFlow(0L)
        val reproduciendo = MutableStateFlow(false)
        var lecturas = 0

        backgroundScope.launch { posicion.collect {} }
        backgroundScope.trackPlaybackPosition(posicion, reproduciendo) { lecturas++; 42L }

        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        val enPausa = lecturas

        reproduciendo.value = true
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()

        // Lectura inmediata + un tick cada 500 ms durante 2 s.
        assertTrue("lecturas tras reanudar: ${lecturas - enPausa}", lecturas - enPausa >= 4)
        assertEquals(42L, posicion.value)
    }
}
