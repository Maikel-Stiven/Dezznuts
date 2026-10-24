package com.example.creproductor.service

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.creproductor.data.PlaylistSerializer

/**
 * Árbol de navegación para Android Auto y otros clientes (asistente, Wear): Recientes, Favoritas, Listas y Todas.
 * Lee MediaStore y los datos guardados en preferencias, sin depender de la interfaz. Bloquea: llamar fuera del hilo principal.
 */
object LibraryBrowser {
    const val ROOT = "root"
    private const val RECENT = "recent"
    private const val FAVS = "favs"
    private const val ALL = "all"
    private const val PLAYLISTS = "playlists"
    private const val PLAYLIST_PREFIX = "playlist:"
    private const val ALL_LIMIT = 500   // Android Auto limita las listas largas por seguridad vial

    private val audioUri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    private val ALBUM_ART: Uri = Uri.parse("content://media/external/audio/albumart")

    private fun folder(id: String, title: String) = MediaItem.Builder()
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle(title).setIsBrowsable(true).setIsPlayable(false)
                .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED).build()
        ).build()

    fun root(): MediaItem = folder(ROOT, "DEZZNUTS")

    /** Hijos de [parentId]; `null` si no es una carpeta conocida. */
    fun children(context: Context, parentId: String): List<MediaItem>? {
        val prefs = context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE)
        return when {
            parentId == ROOT -> listOf(
                folder(RECENT, "Recientes"), folder(FAVS, "Favoritas"), folder(PLAYLISTS, "Listas de reproducción"), folder(ALL, "Todas las canciones")
            )
            parentId == RECENT -> songsByIds(context, ids(prefs.getString("recent_ids", "")))
            parentId == FAVS -> songsByIds(context, (prefs.getStringSet("fav_ids", emptySet()) ?: emptySet()).mapNotNull { it.toLongOrNull() })
            parentId == ALL -> allSongs(context)
            parentId == PLAYLISTS -> PlaylistSerializer.decode(prefs.getString("saved_playlist", "") ?: "")
                .map { folder(PLAYLIST_PREFIX + it.id, "${it.name} (${it.songIds.size})") }
            parentId.startsWith(PLAYLIST_PREFIX) -> PlaylistSerializer.decode(prefs.getString("saved_playlist", "") ?: "")
                .firstOrNull { it.id == parentId.removePrefix(PLAYLIST_PREFIX) }
                ?.let { songsByIds(context, it.songIds) }
            else -> null
        }
    }

    /** Una pista suelta (cuando un cliente elige un elemento por su `mediaId`). */
    fun item(context: Context, mediaId: String): MediaItem? {
        if (mediaId == ROOT) return root()
        val id = mediaId.toLongOrNull()?.takeIf { it > 0 } ?: return null
        return songsByIds(context, listOf(id)).firstOrNull()
    }

    private fun ids(csv: String?) = csv.orEmpty().split(",").mapNotNull { it.trim().toLongOrNull() }

    private val PROJECTION = arrayOf(
        MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.DURATION
    )

    private fun allSongs(context: Context): List<MediaItem> = query(context, null, null, "${MediaStore.Audio.Media.TITLE} ASC", ALL_LIMIT)

    /** En el orden pedido; las que ya no existen se omiten. */
    private fun songsByIds(context: Context, wanted: List<Long>): List<MediaItem> {
        val ids = wanted.filter { it > 0 }.take(ALL_LIMIT)
        if (ids.isEmpty()) return emptyList()
        val found = query(context, "${MediaStore.Audio.Media._ID} IN (${ids.joinToString(",")})", null, null).associateBy { it.mediaId }
        return ids.mapNotNull { found[it.toString()] }
    }

    /** [limit]: se corta al llegar (la ordenación con LIMIT en el SQL no la admite MediaStore desde Android 11). */
    private fun query(context: Context, selection: String?, args: Array<String>?, order: String?, limit: Int = Int.MAX_VALUE): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        val where = listOfNotNull("${MediaStore.Audio.Media.IS_MUSIC} != 0", selection).joinToString(" AND ")
        runCatching {
            context.contentResolver.query(audioUri, PROJECTION, where, args, order)?.use { c ->
                while (out.size < limit && c.moveToNext()) {
                    val id = c.getLong(0)
                    out += MediaItem.Builder()
                        .setMediaId(id.toString())
                        .setUri(ContentUris.withAppendedId(audioUri, id))
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(c.getString(1)).setArtist(c.getString(2)).setAlbumTitle(c.getString(3))
                                .setDurationMs(c.getLong(5))
                                .setArtworkUri(ContentUris.withAppendedId(ALBUM_ART, c.getLong(4)))
                                .setIsBrowsable(false).setIsPlayable(true).setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC).build()
                        ).build()
                }
            }
        }
        return out
    }
}
