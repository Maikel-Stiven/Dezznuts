package com.example.creproductor.data

import android.content.Context
import android.os.SystemClock
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

/**
 * Portada animada (los "bucles" de video de Apple Music), opcional y apagada por defecto.
 *
 * Es la misma fuente que usa Glassy Music (su extensión Better Lyrics Shaders): el servicio comunitario
 * [artwork.boidu.dev](https://github.com/boidushya/artwork.boidu.dev). No es una API oficial de Apple ni lleva clave;
 * recibe título, artista, álbum y duración, y devuelve la lista HLS del bucle si el álbum tiene uno. Límite público:
 * 60 peticiones por minuto, así que el resultado (también el "no hay") se guarda en disco.
 */
object AnimatedCover {
    private const val BASE = "https://artwork.boidu.dev/"
    private const val USER_AGENT = "DEZZNUTS (https://github.com/Maikel-Stiven/Dezznuts)"
    private const val NONE = "-"
    private const val TTL_MS = 7L * 24 * 3600 * 1000

    /** La lista HLS (se adapta a la conexión y se limita a 720p al reproducir) o `null` si el álbum no tiene bucle. */
    fun parse(json: String, title: String? = null, artist: String? = null): String? = runCatching {
        val o = JSONObject(json)
        // El servicio a veces devuelve OTRA canción con el mismo título (otro artista): ese bucle no es de esta portada.
        val sameSong = title == null || artist == null || !o.has("name") ||
            CoverFetcher.isSameSong(title, artist, o.optString("name"), o.optString("artist"))
        if (o.has("error") || !sameSong) null else o.optString("animated").takeIf { it.startsWith("https://") }
    }.getOrNull()

    private fun key(song: Song) = "${song.artist.trim().lowercase()}|${song.album.trim().lowercase()}"

    private fun cacheFile(context: Context, song: Song) =
        File(File(context.cacheDir, "animated_cover_v2"), "${key(song).hashCode().toUInt().toString(16)}.txt")

    /** Álbum de verdad: la etiqueta provisional "YouTube Music" no es un álbum (iTunes lo averigua por título y artista). */
    private fun realAlbum(song: Song) =
        song.album.takeUnless { it.isBlank() || it.startsWith("<") || (song.isYoutube && it == "YouTube Music") }.orEmpty()

    /** ¿Tiene sentido preguntar? Sin álbum o con artista desconocido el servicio no acierta (una de YouTube se busca por título). */
    fun eligible(song: Song) =
        song.artist.isNotBlank() && !song.artist.startsWith("<") && (song.isYoutube || realAlbum(song).isNotBlank())

    /** "Nevermind (Remastered)" → "Nevermind"; "Rush! - Single" → "Rush!". */
    internal fun baseAlbum(album: String) = album
        .replace(Regex("""\s*[(\[][^)\]]*[)\]]"""), "")
        .replace(Regex("""(?i)\s+-\s+(single|ep|deluxe.*|remaster.*)$"""), "")
        .trim()

    // Como el servicio admite 60 peticiones/min, todas pasan por una sola puerta con un hueco mínimo; si responde 429 se
    // respeta su Retry-After y, mientras tanto, ni se pregunta. Un fallo reciente (sin red) tampoco se repite al instante.
    private const val GAP_MS = 1100L
    private const val RETRY_FAIL_MS = 3 * 60_000L
    private val gate = Mutex()
    private var last = 0L
    private var blockedUntil = 0L
    private val failedAt = ConcurrentHashMap<String, Long>()

    private suspend fun ask(song: Song, album: String): String? = gate.withLock {
        val now = SystemClock.elapsedRealtime()
        if (now < blockedUntil) throw IOException("limitado")
        (last + GAP_MS - now).takeIf { it > 0 }?.let { delay(it) }
        last = SystemClock.elapsedRealtime()
        withContext(Dispatchers.IO) { askNow(song, album) }
    }

    private fun askNow(song: Song, album: String): String? {
        val url = "$BASE?s=${enc(song.title)}&a=${enc(song.artist)}&al=${enc(album)}&d=${song.duration / 1000}"
        val c = URL(url).openConnection() as HttpURLConnection
        val body = try {
            c.connectTimeout = 8000; c.readTimeout = 8000
            c.setRequestProperty("User-Agent", USER_AGENT)
            if (c.responseCode == 429) {
                blockedUntil = SystemClock.elapsedRealtime() + (c.getHeaderField("Retry-After")?.toLongOrNull() ?: 30L).coerceIn(5L, 120L) * 1000
            }
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
        return parse(body, song.title, song.artist)
    }

    /**
     * URL del bucle o `null`. El bucle cuelga de un álbum concreto de Apple, así que se prueba el álbum de tus etiquetas,
     * el mismo sin sufijos de edición y los álbumes en los que iTunes tiene la canción (máx. 4 consultas). Lanza
     * [IOException] si no hay conexión o el servicio está saturado (429/5xx) y no se pudo comprobar ninguno: eso no se
     * guarda, para reintentar luego. Solo se guarda el "no hay" si todo se pudo consultar.
     */
    suspend fun find(context: Context, song: Song): String? {
        if (!eligible(song)) return null
        val f = cacheFile(context, song)
        val k = key(song)
        failedAt[k]?.let { if (SystemClock.elapsedRealtime() - it < RETRY_FAIL_MS) throw IOException("falló hace poco") }
        if (f.isFile && System.currentTimeMillis() - f.lastModified() < TTL_MS) {
            return f.readText().takeIf { it != NONE }
        }
        val album = realAlbum(song)
        val candidates = (listOf(album, baseAlbum(album)) + CoverFetcher.albumCandidates(song.title, song.artist))
            .filter { it.isNotBlank() && !it.startsWith("<") }.distinct().take(4)
        var failed: IOException? = null
        var answered = 0
        for (album in candidates) {
            currentCoroutineContext().ensureActive()   // saltar de canción cancela lo que quedaba por preguntar
            try {
                answered++
                ask(song, album)?.let { hit ->
                    runCatching { f.apply { parentFile?.mkdirs() }.writeText(hit) }
                    return hit
                }
            } catch (e: IOException) { answered--; failed = e }
        }
        // Si alguna consulta falló, el "no hay" no es fiable: no se guarda.
        if (failed != null) {
            if (answered == 0) { failedAt[k] = SystemClock.elapsedRealtime(); throw failed } else return null
        }
        runCatching { f.apply { parentFile?.mkdirs() }.writeText(NONE) }
        return null
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
