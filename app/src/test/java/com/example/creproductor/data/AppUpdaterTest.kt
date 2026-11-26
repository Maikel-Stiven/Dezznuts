package com.example.creproductor.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterTest {
    @Test fun comparaVersiones() {
        assertTrue(AppUpdater.isNewer("v1.3.0", "1.2.1"))
        assertTrue(AppUpdater.isNewer("v1.10.0", "1.9.2"))   // numérico, no alfabético
        assertTrue(AppUpdater.isNewer("2.0", "1.9.9"))
        assertFalse(AppUpdater.isNewer("v1.3.0", "1.3.0"))
        assertFalse(AppUpdater.isNewer("v1.2.0", "1.3.0"))
        assertFalse(AppUpdater.isNewer("1.3.0-beta", "1.3.0"))
        assertFalse(AppUpdater.isNewer("", "1.3.0"))
    }

    @Test fun notasSinMarkdown() {
        val md = "### Novedades\r\n- **Interfaz** nueva: barra\r\n  sin menú lateral.\r\n\r\n- Usa `Opus`"
        org.junit.Assert.assertEquals("Novedades\n• Interfaz nueva: barra sin menú lateral.\n\n• Usa Opus", AppUpdater.plainNotes(md))
    }
}
