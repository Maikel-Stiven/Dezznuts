package com.example.creproductor.data

import android.content.Context
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.json.JSONArray
import org.json.JSONObject

/**
 * De dónde sale la letra, en este orden y sin tocar internet salvo que el usuario lo pida:
 * 1. la ya guardada de una búsqueda anterior (`filesDir/lyrics/<id>.lrc`: después funciona sin conexión),
 * 2. la incrustada en el archivo (etiquetas del FLAC, USLT/LYRICS de MP3/M4A) y un `.lrc` junto al archivo,
 * 3. [LRCLIB](https://lrclib.net): API abierta y gratuita, sin clave, con letras sincronizadas. Solo se le envían
 *    artista, título, álbum y duración, y solo cuando el usuario pulsa "Buscar en internet".
 */
object LyricsRepository {
    private const val BASE = "https://lrclib.net/api"
    private const val USER_AGENT = "DEZZNUTS (https://github.com/Maikel-Stiven/Dezznuts)"

    private fun cacheFile(context: Context, song: Song) = File(File(context.filesDir, "lyrics"), "${song.id}.lrc")

    /** Letra disponible sin red (guardada, incrustada o `.lrc` vecino). Bloquea: llamar fuera del hilo principal. */
    fun local(context: Context, song: Song): Lyrics? {
        cacheFile(context, song).takeIf { it.isFile }?.let { f ->
            runCatching { Lrc.fromText(f.readText(), "Guardada") }.getOrNull()?.let { return it }
        }
        val path = song.path
        if (path.isEmpty()) return null
        embedded(path)?.let { Lrc.fromText(it, "Archivo") }?.let { return it }
        runCatching { File(path.substringBeforeLast('.') + ".lrc").takeIf { it.isFile }?.readText() }.getOrNull()
            ?.let { Lrc.fromText(it, "Archivo .lrc") }?.let { return it }
        return null
    }

    private fun embedded(path: String): String? {
        if (path.endsWith(".flac", ignoreCase = true)) {
            for (key in listOf("SYNCEDLYRICS", "LYRICS", "UNSYNCEDLYRICS")) FlacTags.read(path, key)?.let { return it }
            return null
        }
        return runCatching { AudioFileIO.read(File(path)).tag?.getFirst(FieldKey.LYRICS) }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    /**
     * Busca en LRCLIB y, si encuentra, la guarda. `null` = no la tienen. Lanza [IOException] si no hay conexión
     * (así la pantalla distingue "sin internet" de "sin letra"). Bloquea: fuera del hilo principal.
     */
    fun online(context: Context, song: Song): Lyrics? {
        val artist = song.artist.takeUnless { it.isBlank() || it.startsWith("<") }
        val title = enc(song.title)
        val seconds = song.duration / 1000
        // El álbum solo ayuda si es real: las canciones de YouTube Music llevan uno genérico y LRCLIB responde 503 con él.
        val album = song.album.takeUnless { it.isBlank() || it.startsWith("<") || song.isYoutube }
        // La búsqueda exacta es un atajo: si falla (404, 503, red), se sigue con la búsqueda general en vez de rendirse.
        val exact = if (artist != null && album != null)
            runCatching { get("$BASE/get?track_name=$title&artist_name=${enc(artist)}&album_name=${enc(album)}&duration=$seconds") }.getOrNull()
                ?.let { runCatching { fromJson(JSONObject(it)) }.getOrNull() }
        else null
        val found = exact ?: get("$BASE/search?track_name=$title" + (artist?.let { "&artist_name=${enc(it)}" } ?: ""))
            ?.let { runCatching { JSONArray(it) }.getOrNull() }
            ?.let { arr -> bestMatch((0 until arr.length()).mapNotNull { runCatching { arr.getJSONObject(it) }.getOrNull() }, seconds) }
            ?: return null
        runCatching {
            cacheFile(context, song).apply { parentFile?.mkdirs() }.writeText(
                if (found.synced) found.lines.joinToString("\n") { "[%02d:%02d.%02d]%s".format(it.timeMs / 60000, it.timeMs / 1000 % 60, it.timeMs % 1000 / 10, it.text) }
                else found.lines.joinToString("\n") { it.text }
            )
        }
        return found
    }

    /** Entre varias versiones, la sincronizada con la duración más parecida (la misma canción en otro disco dura distinto). */
    private fun bestMatch(candidates: List<JSONObject>, seconds: Long): Lyrics? =
        candidates.sortedWith(
            compareByDescending<JSONObject> { !it.isNull("syncedLyrics") && it.optString("syncedLyrics").isNotBlank() }
                .thenBy { if (seconds > 0) kotlin.math.abs(it.optDouble("duration", 0.0) - seconds) else 0.0 }
        ).firstNotNullOfOrNull { runCatching { fromJson(it) }.getOrNull() }

    /** Preferir la sincronizada; si solo hay texto plano, esa. */
    private fun fromJson(o: JSONObject): Lyrics? {
        fun str(k: String) = if (o.isNull(k)) "" else o.optString(k)
        return Lrc.fromText(str("syncedLyrics"), "LRCLIB") ?: Lrc.fromText(str("plainLyrics"), "LRCLIB")
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** GET con tiempos de espera cortos; `null` en 404, [IOException] si no se puede conectar. */
    private fun get(url: String): String? {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 8000; c.readTimeout = 8000
            c.setRequestProperty("User-Agent", USER_AGENT)
            return when (c.responseCode) {
                200 -> c.inputStream.bufferedReader().use { it.readText() }
                404 -> null
                else -> throw IOException("HTTP ${c.responseCode}")
            }
        } finally { c.disconnect() }
    }
}
