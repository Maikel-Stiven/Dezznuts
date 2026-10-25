package com.example.creproductor.data

import android.content.Context
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

/** Una tarjeta del inicio de YouTube Music: una canción (se toca y suena) o un álbum/lista (se abre para ver sus canciones). */
data class YtItem(val title: String, val subtitle: String, val thumbnail: String?, val track: YtTrack?, val browseId: String?)
data class YtShelf(val title: String, val items: List<YtItem>)
/** Un estado de ánimo del inicio ("Relax", "Entrenar"…); [params] es lo que YouTube pide para filtrar. */
data class YtMood(val label: String, val params: String)
data class YtHomeFeed(val moods: List<YtMood>, val shelves: List<YtShelf>)

/**
 * Inicio de YouTube Music (`FEmusic_home`): estantes y estados de ánimo. Sin sesión es el inicio genérico; con la
 * cookie de la cuenta (ver [YtLibrary]) es el personalizado. Idea de la pantalla de Maikel (rama temas/interfaces),
 * montada sobre el mismo cliente InnerTube que ya pide los "Me gusta". Se guarda 10 min para no repetir peticiones.
 */
object YtHome {
    private const val TTL_MS = 10 * 60_000L
    private val cache = object : LinkedHashMap<String, Pair<Long, YtHomeFeed>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, YtHomeFeed>>) = size > 8
    }

    /** Lanza [IOException] si no hay red o YouTube no responde. Bloquea: llamar fuera del hilo principal. */
    fun load(context: Context, params: String? = null, force: Boolean = false): YtHomeFeed {
        val cookie = YtLibrary.cookie(context)?.takeIf { YtLibrary.sapisid(it) != null }
        val key = "${cookie != null}|${java.util.Locale.getDefault().language}|${params.orEmpty()}"
        if (!force) synchronized(cache) { cache[key] }?.let { (at, feed) ->
            if (System.currentTimeMillis() - at < TTL_MS) return feed
        }
        val feed = parse(YtLibrary.browse("FEmusic_home", cookie, params))
        if (feed.shelves.isEmpty()) throw IOException("Inicio vacío")
        synchronized(cache) { cache[key] = System.currentTimeMillis() to feed }
        return feed
    }

    /** Canciones de un álbum o lista del inicio. El artista que falte en cada fila se toma del subtítulo de la tarjeta. */
    fun tracksOf(context: Context, item: YtItem): List<YtTrack> {
        val id = item.browseId ?: return emptyList()
        val cookie = YtLibrary.cookie(context)?.takeIf { YtLibrary.sapisid(it) != null }
        val artist = artistOf(item.subtitle)
        return YtLibrary.parse(YtLibrary.browse(id, cookie)).map {
            it.copy(artist = it.artist.ifBlank { artist }, thumbnail = it.thumbnail ?: item.thumbnail)
        }
    }

    internal fun parse(json: String): YtHomeFeed {
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return YtHomeFeed(emptyList(), emptyList())
        val list = findSectionList(root) ?: return YtHomeFeed(emptyList(), emptyList())
        val moods = list.optJSONObject("header")?.optJSONObject("chipCloudRenderer")?.optJSONArray("chips").objects().mapNotNull { c ->
            val chip = c.optJSONObject("chipCloudChipRenderer") ?: return@mapNotNull null
            val label = chip.optJSONObject("text")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
            val params = chip.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optString("params").orEmpty()
            if (label.isBlank() || params.isBlank()) null else YtMood(label, params)
        }
        val shelves = list.optJSONArray("contents").objects().mapNotNull { section ->
            val shelf = section.optJSONObject("musicCarouselShelfRenderer") ?: return@mapNotNull null
            val title = shelf.optJSONObject("header")?.optJSONObject("musicCarouselShelfBasicHeaderRenderer")
                ?.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty()
            val items = shelf.optJSONArray("contents").objects().mapNotNull(::toItem).take(20)
            if (title.isBlank() || items.isEmpty()) null else YtShelf(title, items)
        }
        return YtHomeFeed(moods, shelves)
    }

    /** El `sectionListRenderer` de la primera pestaña (la ruta puede variar un poco: se busca en profundidad). */
    private fun findSectionList(o: Any?): JSONObject? = when (o) {
        is JSONObject -> o.optJSONObject("sectionListRenderer") ?: o.keys().asSequence().firstNotNullOfOrNull { findSectionList(o.opt(it)) }
        is JSONArray -> (0 until o.length()).firstNotNullOfOrNull { findSectionList(o.opt(it)) }
        else -> null
    }

    private fun toItem(entry: JSONObject): YtItem? {
        entry.optJSONObject("musicResponsiveListItemRenderer")?.let { r ->
            // La segunda columna del inicio trae "Artista • 476 M reproducciones": solo interesa el artista.
            val t = (YtLibrary.toTrack(r) ?: return null).let { it.copy(artist = it.artist.substringBefore(" • ").trim()) }
            return YtItem(t.title, t.artist, t.thumbnail, t, null)
        }
        val r = entry.optJSONObject("musicTwoRowItemRenderer") ?: return null
        val title = r.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text").orEmpty().ifBlank { return null }
        val subtitle = r.optJSONObject("subtitle")?.optJSONArray("runs").objects().joinToString("") { it.optString("text") }
        val thumbs = r.optJSONObject("thumbnailRenderer")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        val thumb = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url")
        val nav = r.optJSONObject("navigationEndpoint")
        val videoId = nav?.optJSONObject("watchEndpoint")?.optString("videoId")?.takeIf { it.isNotBlank() }
        if (videoId != null) {
            val track = YtTrack("https://music.youtube.com/watch?v=$videoId", title, artistOf(subtitle), 0L, thumb)
            return YtItem(title, subtitle, thumb, track, null)
        }
        val browseId = nav?.optJSONObject("browseEndpoint")?.optString("browseId")?.takeIf { it.startsWith("MPRE") || it.startsWith("VL") }
        return browseId?.let { YtItem(title, subtitle, thumb, null, it) }
    }

    private val KIND = setOf("canción", "cancion", "song", "video", "álbum", "album", "sencillo", "single", "ep", "lista", "playlist", "podcast", "episodio", "episode")

    /** "Canción • Artista • 1,2 M" → "Artista" (se salta la etiqueta del tipo). */
    internal fun artistOf(subtitle: String) =
        subtitle.split("•", "·").map { it.trim() }.firstOrNull { it.isNotBlank() && it.lowercase() !in KIND }.orEmpty()

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
