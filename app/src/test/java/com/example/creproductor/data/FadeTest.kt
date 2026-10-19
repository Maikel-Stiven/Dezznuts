package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FadeTest {
    private val never = Long.MAX_VALUE

    @Test fun sinFundidoNoCambiaNada() {
        assertEquals(1f, Fade.factor(0, 1000, 0, 0, 0), 0f)
    }

    @Test fun entradaAlInicioDeLaPista() {
        assertEquals(0f, Fade.factor(0, 60_000, 2000, never, -1), 0.0001f)
        assertEquals(0.25f, Fade.factor(1000, 60_000, 2000, never, -1), 0.0001f)   // (1/2)^2
        assertEquals(1f, Fade.factor(2000, 60_000, 2000, never, -1), 0.0001f)
    }

    @Test fun salidaAlFinalDeLaPista() {
        assertEquals(1f, Fade.factor(50_000, 60_000, 2000, never, -1), 0.0001f)
        assertEquals(0.25f, Fade.factor(59_000, 60_000, 2000, never, -1), 0.0001f)
        assertEquals(0f, Fade.factor(60_000, 60_000, 2000, never, -1), 0.0001f)
    }

    @Test fun duracionDesconocidaNoHaceFundidoDeSalida() {
        assertEquals(1f, Fade.factor(59_900, -1, 2000, never, -1), 0.0001f)
    }

    @Test fun entradaAlReanudarYSalidaAlPausar() {
        assertEquals(0.25f, Fade.factor(30_000, 60_000, 2000, 1000, -1), 0.0001f)   // reanuda a mitad de pista
        assertEquals(0.25f, Fade.factor(30_000, 60_000, 2000, never, 1000), 0.0001f) // pausando: a mitad de camino
        assertEquals(0f, Fade.factor(30_000, 60_000, 2000, never, 2000), 0.0001f)    // pausa completada
    }
}
