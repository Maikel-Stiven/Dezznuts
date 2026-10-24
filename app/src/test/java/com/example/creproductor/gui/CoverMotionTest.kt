package com.example.creproductor.gui

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverMotionTest {
    @Test fun sinMovimientoNoInclina() = assertEquals(0f, tiltDegrees(0f), 0f)

    @Test fun nuncaSuperaElMaximo() {
        assertEquals(9f, tiltDegrees(50f), 0f)
        assertEquals(-9f, tiltDegrees(-50f), 0f)
    }

    @Test fun esProporcionalEnPequeno() = assertEquals(1.6f, tiltDegrees(1f), 1e-6f)
}
