package com.example.creproductor.data

import android.content.Context
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
    fun parse(json: String): String? = runCatching {
        val o = JSONObject(json)
        if (o.has("error")) null else o.optString("animated").takeIf { it.startsWith("https://") }
    }.getOrNull()

    private fun key(song: Song) = "${song.artist.trim().lowercase()}|${song.album.trim().lowercase()}"

    private fun cacheFile(context: Context, song: Song) =
        File(File(context.cacheDir, "animated_cover"), "${key(song).hashCode().toUInt().toString(16)}.txt")

    /** ¿Tiene sentido preguntar? Sin álbum o con artista desconocido el servicio no acierta. */
    fun eligible(song: Song) =
        song.album.isNotBlank() && song.artist.isNotBlank() && !song.artist.startsWith("<") && !song.album.startsWith("<")

    /**
     * URL del bucle o `null`. Lanza [IOException] si no hay conexión o el servicio está saturado (429/5xx): eso no se
     * guarda para reintentar luego. Bloquea: llamar fuera del hilo principal.
     */
    fun find(context: Context, song: Song): String? {
        if (!eligible(song)) return null
        val f = cacheFile(context, song)
        if (f.isFile && System.currentTimeMillis() - f.lastModified() < TTL_MS) {
            return f.readText().takeIf { it != NONE }
        }
        val url = "$BASE?s=${enc(song.title)}&a=${enc(song.artist)}&al=${enc(song.album)}&d=${song.duration / 1000}"
        val c = URL(url).openConnection() as HttpURLConnection
        val body = try {
            c.connectTimeout = 8000; c.readTimeout = 8000
            c.setRequestProperty("User-Agent", USER_AGENT)
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
        val result = parse(body)
        runCatching { f.apply { parentFile?.mkdirs() }.writeText(result ?: NONE) }
        return result
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
