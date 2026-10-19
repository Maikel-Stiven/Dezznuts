package com.example.creproductor.data


data class ParsedMetaData(
    val cleanTitle: String,
    val cleanArtist: String,
    val isTopicDownload: Boolean
)

object YoutubeTitleCleaner {

    // Se compilan una vez (antes, en cada llamada: con miles de canciones sin etiquetas eran decenas de miles).
    private val EXTENSION = Regex("\\.(mp3|m4a|flac|wav|webm)$", RegexOption.IGNORE_CASE)
    private val NOISE_BRACKETS = Regex("(?i)\\[(official|video|lyric|audio|4k|hd|topic).*?]")
    private val NOISE_PARENS = Regex("(?i)\\((official|video|lyric|audio|4k|hd|topic).*?\\)")
    private val NOISE_WORDS = Regex("(?i)\\b(official video|music video|lyric video|audio|full song|release)\\b")
    private val SEPARATORS = listOf(" - ", " – ", " — ", " · ")

    fun parseFileName(rawName: String): ParsedMetaData{
        val isTopic = rawName.contains("Topic", ignoreCase = true)

        val clean = rawName
            .replace(EXTENSION, "")
            .replace(NOISE_BRACKETS, "")
            .replace(NOISE_PARENS, "")
            .replace(NOISE_WORDS, "")
            .trim()
        var artist = "Artista desconocido"
        var title = clean

        for (sep in SEPARATORS){
            if (clean.contains(sep)){
                val parts = clean.split(sep, limit = 2)
                artist = parts[0].trim()
                title = parts[1].trim()
                break
            }
        }

        title = title.removeSurrounding("\"").removeSurrounding("'").trim()
        artist = artist.removeSurrounding("\"").removeSurrounding("'").trim()

        return ParsedMetaData(
            cleanTitle = title,
            cleanArtist = artist,
            isTopicDownload = isTopic
        )
    }
}