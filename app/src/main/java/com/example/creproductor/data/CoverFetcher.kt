package com.example.creproductor.data

import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class RemoteMetaDataResult(
    val title: String,
    val artist: String,
    val album: String,
    val highResCoverUrl: String?
)

object CoverFetcher {

    /** Sin mayúsculas, acentos ni signos: «Ramírez» y «Ramirez» son el mismo artista para iTunes y para YouTube. */
    private fun norm(s: String) = java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD).filter { it.isLetterOrDigit() }

    /**
     * ¿El resultado remoto es la misma canción? Evita sobrescribir las etiquetas con el primer
     * resultado de iTunes cuando este no tiene nada que ver. El título debe contenerse en un
     * sentido u otro; el artista solo se exige si el local es conocido.
     */
    internal fun isSameSong(title: String, artist: String, foundTitle: String, foundArtist: String): Boolean {
        val t = norm(title); val ft = norm(foundTitle)
        if (t.isEmpty() || ft.isEmpty() || !(t.contains(ft) || ft.contains(t))) return false
        val a = norm(artist); val fa = norm(foundArtist)
        val unknown = a.isEmpty() || "desconocid" in a || "unknown" in a
        return unknown || a.contains(fa) || fa.contains(a)
    }

    /**
     * Álbumes (sin repetir) en los que iTunes tiene esta canción: el álbum de tus etiquetas casi nunca coincide con el
     * de Apple (ediciones, "Remastered", "Deluxe"), y el bucle animado cuelga de un álbum concreto.
     */
    suspend fun albumCandidates(title: String, artist: String): List<String> =
        Itunes.search(title, artist)
            .filter { isSameSong(title, artist, it.track, it.artist) }
            .map { it.album }.filter { it.isNotBlank() }.distinct()

    suspend fun fetchOfficialCover(title: String, artist: String): RemoteMetaDataResult? =
        Itunes.search(title, artist).firstOrNull { isSameSong(title, artist, it.track, it.artist) }?.let {
            RemoteMetaDataResult(
                title = it.track.ifBlank { title }, artist = it.artist.ifBlank { artist }, album = it.album,
                highResCoverUrl = it.art100.takeIf { u -> u.isNotBlank() }?.replace("100x100bb", "1000x1000bb")
            )
        }
}

/** Un resultado de la búsqueda de iTunes. */
data class ItunesHit(val track: String, val artist: String, val album: String, val art100: String)

/**
 * Única puerta a iTunes Search (límite ≈ 20 peticiones/min por IP): resultados en memoria (la misma canción pedida
 * por la portada HD, el bucle animado y el editor cuesta una sola petición), peticiones una tras otra con un hueco
 * mínimo y pausa cuando el servicio responde 403/429. Un fallo de red no se guarda para poder reintentar.
 */
object Itunes {
    private const val GAP_MS = 1500L
    private const val BLOCK_MS = 60_000L
    private val gate = Mutex()
    private var last = 0L
    private var blockedUntil = 0L
    private val cache = object : LinkedHashMap<String, List<ItunesHit>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<ItunesHit>>) = size > 200
    }

    private fun key(title: String, artist: String) =
        "${title.lowercase().filter { it.isLetterOrDigit() }}|${artist.lowercase().filter { it.isLetterOrDigit() }}"

    suspend fun search(title: String, artist: String): List<ItunesHit> {
        val k = key(title, artist)
        synchronized(cache) { cache[k] }?.let { return it }
        return gate.withLock {
            synchronized(cache) { cache[k] }?.let { return@withLock it }
            val now = SystemClock.elapsedRealtime()
            if (now < blockedUntil) return@withLock emptyList()
            (last + GAP_MS - now).takeIf { it > 0 }?.let { delay(it) }
            last = SystemClock.elapsedRealtime()
            val hits = withContext(Dispatchers.IO) { request(title, artist) }
            if (hits != null) synchronized(cache) { cache[k] = hits }
            hits ?: emptyList()
        }
    }

    /** `null` = no se pudo preguntar (red, límite); lista vacía = iTunes no la conoce. */
    private fun request(title: String, artist: String): List<ItunesHit>? = try {
        val q = URLEncoder.encode("$title $artist".trim(), "UTF-8")
        val c = URL("https://itunes.apple.com/search?term=$q&media=music&entity=song&limit=10").openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 5000; c.readTimeout = 5000
            when (c.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val results = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONArray("results")
                    (0 until results.length()).map { results.getJSONObject(it) }.map {
                        ItunesHit(it.optString("trackName"), it.optString("artistName"), it.optString("collectionName"), it.optString("artworkUrl100"))
                    }
                }
                403, 429 -> { blockedUntil = SystemClock.elapsedRealtime() + BLOCK_MS; null }
                else -> null
            }
        } finally { c.disconnect() }
    } catch (e: CancellationException) { throw e } catch (_: Exception) { null }
}