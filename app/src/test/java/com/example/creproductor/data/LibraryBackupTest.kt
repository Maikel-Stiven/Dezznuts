package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryBackupTest {
    @Test
    fun `la copia se lee igual que se escribió`() {
        val backup = LibraryBackup(
            favorites = listOf("/m/a.flac"),
            playCounts = mapOf("/m/a.flac" to 7, "/m/b.mp3" to 2),
            playlists = listOf("Fiesta\tloca" to listOf("/m/a.flac", "/m/b.mp3"), "Vacía" to emptyList())
        )
        val read = LibraryBackup.decode(backup.encode())!!
        assertEquals(backup.favorites, read.favorites)
        assertEquals(backup.playCounts, read.playCounts)
        assertEquals(listOf("Fiesta loca" to listOf("/m/a.flac", "/m/b.mp3"), "Vacía" to emptyList<String>()), read.playlists)
    }

    @Test
    fun `un archivo que no es copia se rechaza`() {
        assertNull(LibraryBackup.decode("#EXTM3U\n/m/a.mp3"))
    }

    @Test
    fun `m3u ignora comentarios, líneas vacías y el BOM`() {
        assertEquals(listOf("/m/a.mp3", "b.flac"), M3u.parse("﻿#EXTM3U\n#EXTINF:3,x\n/m/a.mp3\n\n  b.flac \r\n"))
    }

    @Test
    fun `se encuentra por ruta y, si cambió, por nombre de archivo`() {
        val songs = listOf("/storage/emulated/0/Music/Creep.mp3", "/sdcard/Music/Loser.flac")
        val found = SongMatcher(songs) { it }.match(listOf("/otro/telefono/LOSER.flac", "/storage/emulated/0/Music/Creep.mp3", "/no/existe.mp3"))
        assertEquals(listOf("/sdcard/Music/Loser.flac", "/storage/emulated/0/Music/Creep.mp3"), found)
    }
}
