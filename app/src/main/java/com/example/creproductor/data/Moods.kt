package com.example.creproductor.data

import java.text.Normalizer

/**
 * Estados de ánimo ("moods") de Selección rápida. La biblioteca no trae un dato de ánimo, así que se deducen; antes solo
 * del género, y las canciones mal etiquetadas (descargas de Snaptube y similares: sin género, a veces sin artista) no
 * caían en ninguno. Ahora se usan, en este orden, tres señales:
 *  1. el **género** de la etiqueta,
 *  2. el **género heredado del artista** (si otras canciones suyas sí lo tienen, gana el más repetido),
 *  3. **palabras del título, álbum y ruta** ("lofi", "remix", "triste", "instrumental"…).
 * Lo que no encaja en nada va a [OTHER] para que ninguna canción quede fuera de alcance. Es una aproximación por texto,
 * no un análisis del audio.
 */
enum class Mood(val label: String, internal val keywords: List<String>, internal val words: List<String> = emptyList()) {
    ENERGY("Energía", listOf("rock", "metal", "punk", "hard", "techno", "trap", "hip hop", "hip-hop", "rap", "edm", "drum"),
        listOf("rock", "metal", "punk", "trap", "drill", "phonk", "hardstyle", "gym", "workout", "freestyle", "rap", "energia", "power")),
    HAPPY("Alegre", listOf("pop", "reggae", "funk", "disco", "ska", "latin", "salsa", "cumbia", "merengue", "bachata"),
        listOf("feliz", "happy", "verano", "summer", "alegria", "sunshine", "cumbia", "salsa", "bachata", "merengue", "reggae", "pop")),
    PARTY("Fiesta", listOf("dance", "house", "reggaeton", "electr", "trance", "latin", "hip hop", "hip-hop"),
        listOf("party", "fiesta", "perreo", "reggaeton", "remix", "mix", "dance", "club", "dj", "guaracha", "electro", "rumba", "bailar", "baile")),
    CHILL("Relax", listOf("ambient", "chill", "acoustic", "acústic", "acustic", "jazz", "lounge", "folk", "new age", "bossa", "lofi", "lo-fi"),
        listOf("lofi", "chill", "acoustic", "acustico", "relax", "sleep", "dormir", "slowed", "reverb", "jazz", "bossa", "unplugged", "calma", "suave")),
    MELANCHOLY("Melancólico", listOf("blues", "soul", "indie", "ballad", "balada", "bolero", "tango", "fado"),
        listOf("triste", "sad", "llorar", "lloro", "lagrimas", "dolor", "adios", "extrano", "despecho", "soledad", "ausencia", "ballad", "balada", "bolero", "tango", "blues", "heartbreak", "nostalgia", "recuerdo")),
    FOCUS("Enfoque", listOf("instrumental", "classical", "clásic", "clasic", "ambient", "soundtrack", "score", "piano"),
        listOf("instrumental", "study", "estudio", "focus", "concentracion", "ost", "soundtrack", "score", "classical", "clasica", "beethoven", "mozart", "bach", "ambient", "piano")),
    /** Lo que no encaja en ningún otro: así toda canción es alcanzable desde los moods. */
    OTHER("Otras", emptyList())
}

object Moods {
    private fun norm(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    private fun isKnownGenre(g: String): Boolean {
        val n = norm(g).trim()
        return n.isNotEmpty() && n != "desconocido" && n != "unknown" && n != "other" && n != "otro" && !n.startsWith("<") && n != "youtube music"
    }

    /** ¿El género de la etiqueta casa con este mood? (solo género: ver [group] para las demás señales). */
    fun matches(mood: Mood, genre: String): Boolean {
        val g = norm(genre)
        return isKnownGenre(genre) && mood.keywords.any { g.contains(norm(it)) }
    }

    /** ¿Alguna palabra del texto casa con este mood? Las palabras cortas (≤ 4) deben ser enteras; las largas, por prefijo. */
    fun matchesText(mood: Mood, text: String): Boolean {
        val tokens = norm(text).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        return mood.words.any { w -> tokens.any { t -> if (w.length <= 4) t == w else t.startsWith(w) } }
    }

    /**
     * Moods con sus canciones (en el orden de [Mood]; los vacíos se omiten y una canción puede estar en varios).
     * [genreOf] es el género de la etiqueta, [textOf] el texto donde buscar palabras (título, álbum, ruta) y [artistOf] el
     * artista, que sirve para heredar el género de sus otras canciones.
     */
    fun <T> group(
        items: List<T>, genreOf: (T) -> String,
        textOf: (T) -> String = { "" }, artistOf: (T) -> String = { "" }
    ): Map<Mood, List<T>> {
        // Género más repetido de cada artista, entre sus canciones que sí lo tienen.
        val artistGenre: Map<String, String> = items.filter { isKnownGenre(genreOf(it)) && artistOf(it).isNotBlank() }
            .groupBy { norm(artistOf(it)).trim() }
            .mapValues { (_, songs) -> songs.groupingBy { norm(genreOf(it)).trim() }.eachCount().maxBy { it.value }.key }
        val result = LinkedHashMap<Mood, MutableList<T>>()
        for (item in items) {
            val own = genreOf(item)
            val genre = if (isKnownGenre(own)) own else artistGenre[norm(artistOf(item)).trim()].orEmpty()
            val text = textOf(item)
            val hits = Mood.entries.filter { it != Mood.OTHER && (matches(it, genre) || matchesText(it, text)) }
            if (hits.isEmpty()) result.getOrPut(Mood.OTHER) { mutableListOf() }.add(item)
            else hits.forEach { result.getOrPut(it) { mutableListOf() }.add(item) }
        }
        return Mood.entries.mapNotNull { m -> result[m]?.let { m to it.toList() } }.toMap()
    }
}
