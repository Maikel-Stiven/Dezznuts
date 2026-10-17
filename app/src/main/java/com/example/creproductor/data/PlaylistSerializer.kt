package com.example.creproductor.data

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Serializa y deserializa las listas de reproducción para guardarlas en
 * SharedPreferences.
 *
 * El formato es: `id|nombre|portada|id1,id2,id3` separado por `;`.
 * Los campos de texto se codifican con URL-encoding para que nombres que
 * contengan los separadores (`;`, `|`, `,`) no corrompan los datos.
 */
object PlaylistSerializer {

    private const val LIST_SEPARATOR = ";"
    private const val FIELD_SEPARATOR = "|"
    private const val ID_SEPARATOR = ","
    private const val CHARSET = "UTF-8"

    fun encode(playlists: List<Playlist>): String =
        playlists.joinToString(LIST_SEPARATOR) { playlist ->
            listOf(
                encodeField(playlist.id),
                encodeField(playlist.name),
                encodeField(playlist.coverUri.orEmpty()),
                playlist.songIds.joinToString(ID_SEPARATOR)
            ).joinToString(FIELD_SEPARATOR)
        }

    fun decode(raw: String): List<Playlist> {
        if (raw.isBlank()) return emptyList()
        return raw.split(LIST_SEPARATOR).mapNotNull { line ->
            val parts = line.split(FIELD_SEPARATOR)
            if (parts.size >= 4) {
                Playlist(
                    id = decodeField(parts[0]),
                    name = decodeField(parts[1]),
                    coverUri = decodeField(parts[2]).ifBlank { null },
                    songIds = parts[3].split(ID_SEPARATOR).mapNotNull { it.toLongOrNull() }
                )
            } else {
                null
            }
        }
    }

    private fun encodeField(value: String): String =
        URLEncoder.encode(value, CHARSET)

    private fun decodeField(value: String): String = try {
        URLDecoder.decode(value, CHARSET)
    } catch (e: IllegalArgumentException) {
        // Si el valor no está codificado (datos antiguos), se devuelve tal cual.
        value
    }
}
