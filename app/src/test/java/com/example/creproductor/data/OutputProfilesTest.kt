package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutputProfilesTest {
    @Test fun mandaLaSalidaMasEspecifica() {
        assertEquals(OutputClass.SPEAKER, OutputProfiles.classify(listOf(2)))
        assertEquals(OutputClass.BLUETOOTH, OutputProfiles.classify(listOf(2, 8)))
        assertEquals(OutputClass.WIRED, OutputProfiles.classify(listOf(2, 8, 3)))
        assertEquals(OutputClass.USB, OutputProfiles.classify(listOf(2, 3, 22)))
        assertEquals(OutputClass.SPEAKER, OutputProfiles.classify(emptyList()))
    }

    @Test fun ajustesDeUnPerfilSeGuardanYSeLeen() {
        val p = ProfileSettings(true, false, true, true, "60.0:3.0:1.0;230.0:0.0:1.0")
        assertEquals(p, ProfileSettings.decode(p.encode()))
        assertNull(ProfileSettings.decode("basura"))
        assertNull(ProfileSettings.decode(null))
    }
}
