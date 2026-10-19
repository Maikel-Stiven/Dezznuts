// Sugerencias de etiquetas para canciones con artista desconocido.
//
// Muchos archivos (descargas, grabaciones) no traen etiquetas pero sí un nombre como "Artista - Título".
// Lógica pura, reutiliza [YoutubeTitleCleaner] y se prueba con tests; no usa internet.

package com.example.creproductor.data

data class TagSuggestion(val title: String, val artist: String)

object TagSuggestions {

    // Número de pista al principio solo si lleva signo ("01. ", "02) ", "03 - "): "21 Guns" o "7 Years" no se tocan.
    private val TRACK_NUMBER = Regex("""^\s*\d{1,3}\s*(?:[.)_]\s+|-\s+|_-_)""")

    fun isUnknown(artist: String): Boolean {
        val a = artist.lowercase()
        return a.isBlank() || "unknown" in a || "desconocid" in a
    }

    /**
     * Artista y título deducidos del título guardado o del nombre del archivo; `null` si el artista ya es conocido
     * o no hay forma de separar "Artista - Título".
     */
    fun suggest(title: String, artist: String, fileName: String): TagSuggestion? {
        if (!isUnknown(artist)) return null
        for (source in listOf(title, fileName.substringBeforeLast('.'))) {
            val cleaned = source.replace('_', ' ').replace(TRACK_NUMBER, "").trim()
            val parsed = YoutubeTitleCleaner.parseFileName(cleaned)
            if (parsed.cleanArtist != "Artista desconocido" && parsed.cleanArtist.isNotBlank() && parsed.cleanTitle.isNotBlank()) {
                return TagSuggestion(title = parsed.cleanTitle, artist = parsed.cleanArtist)
            }
        }
        return null
    }
}
