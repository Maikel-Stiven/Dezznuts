package com.example.creproductor.data


data class ParsedMetaData(
    val cleanTitle: String,
    val cleanArtist: String,
    val isTopicDownload: Boolean
)

object YoutubeTitleCleaner {

    fun parseFileName(rawName: String): ParsedMetaData{
        val isTopic = rawName.contains("Topic", ignoreCase = true)

        val clean = rawName
            .replace(Regex("\\.(mp3|m4a|flac|wav|webm)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("(?i)\\[(official|video|lyric|audio|4k|hd|topic).*?]"), "")
            .replace(Regex("(?i)\\((official|video|lyric|audio|4k|hd|topic).*?\\)"), "")
            .replace(Regex("(?i)\\b(official video|music video|lyric video|audio|full song|release)\\b"), "")
            .trim()
        var artist = "Artista desconocido"
        var title = clean

        val separators = listOf(" - ", " – ", " — ", " · ")
        for (sep in separators){
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