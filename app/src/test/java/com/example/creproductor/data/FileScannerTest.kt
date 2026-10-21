package com.example.creproductor.data

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileScannerTest {
    private fun temp(name: String, bytes: Int): File =
        File(createTempDir("scan"), name).apply { writeBytes(ByteArray(bytes)) }

    @Test fun `solo acepta extensiones de audio`() {
        assertTrue(FileScanner.isCandidate(temp("a.FLAC", 10), ScanSettings()))
        assertFalse(FileScanner.isCandidate(temp("a.txt", 10), ScanSettings()))
    }

    @Test fun `filtro de tamano minimo en KB`() {
        val f = temp("a.mp3", 2048)
        assertTrue(FileScanner.isCandidate(f, ScanSettings(minKb = 2)))
        assertFalse(FileScanner.isCandidate(f, ScanSettings(minKb = 3)))
    }

    @Test fun `ocultos por archivo o por carpeta`() {
        assertTrue(FileScanner.isHidden(File("/m/.cache/a.mp3")))
        assertTrue(FileScanner.isHidden(File("/m/.a.mp3")))
        assertFalse(FileScanner.isHidden(File("/m/musica/a.mp3")))
        assertFalse(FileScanner.isCandidate(File("/m/.x/a.mp3"), ScanSettings(ignoreHidden = true)))
    }

    @Test fun `porcentaje`() {
        assertTrue(ScanProgress(50, 200, "").percent == 25)
        assertTrue(ScanProgress(0, 0, "", finished = true).percent == 100)
    }
}
