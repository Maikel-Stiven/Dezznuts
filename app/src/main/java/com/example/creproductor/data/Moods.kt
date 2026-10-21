package com.example.creproductor.data

/**
 * Estados de ánimo ("moods") de Selección rápida. La biblioteca no trae un dato de ánimo, así que se deducen del
 * género con palabras clave (una canción puede caer en varios). Las que no tienen género reconocible no aparecen
 * en ningún mood: es una aproximación, no un análisis del audio.
 */
enum class Mood(val label: String, internal val keywords: List<String>) {
    ENERGY("Energía", listOf("rock", "metal", "punk", "hard", "techno", "trap", "hip hop", "hip-hop", "rap", "edm", "drum")),
    HAPPY("Alegre", listOf("pop", "reggae", "funk", "disco", "ska", "latin", "salsa", "cumbia", "merengue", "bachata")),
    PARTY("Fiesta", listOf("dance", "house", "reggaeton", "electr", "trance", "latin", "hip hop", "hip-hop")),
    CHILL("Relax", listOf("ambient", "chill", "acoustic", "acústic", "acustic", "jazz", "lounge", "folk", "new age", "bossa", "lofi", "lo-fi")),
    MELANCHOLY("Melancólico", listOf("blues", "soul", "indie", "ballad", "balada", "bolero", "tango", "fado")),
    FOCUS("Enfoque", listOf("instrumental", "classical", "clásic", "clasic", "ambient", "soundtrack", "score", "piano"))
}

object Moods {
    fun matches(mood: Mood, genre: String): Boolean {
        val g = genre.lowercase()
        return g.isNotBlank() && mood.keywords.any { g.contains(it) }
    }

    /** Moods que tienen al menos una canción, con sus canciones (en el orden de [Mood]). */
    fun <T> group(items: List<T>, genreOf: (T) -> String): Map<Mood, List<T>> =
        Mood.entries.associateWith { m -> items.filter { matches(m, genreOf(it)) } }.filterValues { it.isNotEmpty() }
}
