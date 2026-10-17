package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RepeatModeTest {

    @Test
    fun `ciclo completo de modos`() {
        assertEquals(RepeatMode.ALL, RepeatMode.OFF.next())
        assertEquals(RepeatMode.ONE, RepeatMode.ALL.next())
        assertEquals(RepeatMode.STOP_AFTER_CURRENT, RepeatMode.ONE.next())
        assertEquals(RepeatMode.OFF, RepeatMode.STOP_AFTER_CURRENT.next())
    }
}
