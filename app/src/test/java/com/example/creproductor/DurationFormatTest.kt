package com.example.creproductor

import com.example.creproductor.gui.formatDuration

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun `formatea minutos y segundos`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:59", formatDuration(59_999))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("60:00", formatDuration(3_600_000))
    }
}
