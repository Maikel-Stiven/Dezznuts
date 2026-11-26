package com.example.creproductor.data

import android.content.Context
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cuenta y biblioteca de YouTube Music: el usuario inicia sesión en una ventana web y la app guarda la cookie de su
 * sesión solo en este teléfono (archivo propio, excluido de las copias de seguridad). Con ella pide a InnerTube
 * (la misma API que usa la web de YouTube Music) la lista de "Me gusta". Es no oficial y puede dejar de funcionar.
 */
object YtLibrary {
    private const val ORIGIN = "https://music.youtube.com"
    private const val PREFS = "yt_auth"
    private const val KEY_COOKIE = "cookie"

    /** Navegador de escritorio/móvil "normal": Google no deja iniciar sesión desde un WebView que se anuncia como tal. */
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

    fun isLoggedIn(context: Context) = cookie(context)?.let { sapisid(it) != null } == true
    fun cookie(context: Context): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_COOKIE, null)
    fun saveCookie(context: Context, cookie: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_COOKIE, cookie).apply()
    fun logout(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()

    internal fun sapisid(cookie: String): String? =
        cookie.split(";").map { it.trim() }.firstOrNull { it.startsWith("SAPISID=") || it.startsWith("__Secure-3PAPISID=") }?.substringAfter("=")

    /** Cabecera `Authorization` que YouTube exige junto a la cookie: SAPISIDHASH `<hora>_<sha1("<hora> <SAPISID> <origen>")>`. */
    internal fun authHeader(sapisid: String, nowSec: Long): String {
        val digest = MessageDigest.getInstance("SHA-1").digest("$nowSec $sapisid $ORIGIN".toByteArray())
        return "SAPISIDHASH ${nowSec}_${digest.joinToString("") { "%02x".format(it) }}"
    }

    /** Canciones que te gustan (lista "LM"). Lanza [IOException] si no hay red o la sesión caducó (401/403). */
    fun liked(context: Context): List<YtTrack> {
        val cookie = cookie(context)?.takeIf { sapisid(it) != null } ?: throw IOException("Sin sesión")
        var page = browse("VLLM", cookie)
        val out = ArrayList(parse(page))
        // La web entrega ~100 por página; el resto llega con "continuaciones". ponytail: tope de 30 páginas (~3000).
        repeat(30) {
            val token = continuation(page) ?: return out.distinctBy { it.url }
            page = runCatching { browse(null, cookie, continuation = token) }.getOrNull() ?: return out.distinctBy { it.url }
            out += parse(page)
        }
        return out.distinctBy { it.url }
    }

    /** Ficha para pedir la página siguiente de una lista (formato nuevo y antiguo de InnerTube), o null si no hay más. */
    internal fun continuation(json: String): String? {
        fun walk(o: Any?): String? = when (o) {
            is JSONObject -> o.optJSONObject("continuationCommand")?.optString("token")?.takeIf { it.isNotEmpty() }
                ?: o.optJSONObject("nextContinuationData")?.optString("continuation")?.takeIf { it.isNotEmpty() }
                ?: o.keys().asSequence().firstNotNullOfOrNull { walk(o.opt(it)) }
            is JSONArray -> (0 until o.length()).firstNotNullOfOrNull { walk(o.opt(it)) }
            else -> null
        }
        return runCatching { walk(JSONObject(json)) }.getOrNull()
    }

    /** Pide una página de InnerTube; [params] filtra el inicio (por ejemplo por estado de ánimo). Con [cookie] es personalizada. */
    internal fun browse(browseId: String?, cookie: String?, params: String? = null, continuation: String? = null): String {
        val lang = java.util.Locale.getDefault()
        val body = JSONObject()
            .put("context", JSONObject().put("client", JSONObject().put("clientName", "WEB_REMIX").put("clientVersion", "1.20241023.01.00")
                .put("hl", lang.language.ifBlank { "es" }).apply { if (lang.country.isNotBlank()) put("gl", lang.country) }))
            .apply { if (browseId != null) put("browseId", browseId); if (params != null) put("params", params); if (continuation != null) put("continuation", continuation) }
            .toString()
        val extra = continuation?.let { java.net.URLEncoder.encode(it, "UTF-8") }?.let { "&ctoken=$it&continuation=$it&type=next" }.orEmpty()
        val c = URL("$ORIGIN/youtubei/v1/browse?prettyPrint=false$extra").openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10000; c.readTimeout = 15000
            c.requestMethod = "POST"; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.setRequestProperty("User-Agent", USER_AGENT)
            c.setRequestProperty("Origin", ORIGIN)
            c.setRequestProperty("X-Origin", ORIGIN)
            c.setRequestProperty("Referer", "$ORIGIN/")
            if (cookie != null) {
                c.setRequestProperty("Cookie", cookie)
                c.setRequestProperty("X-Goog-AuthUser", "0")
                sapisid(cookie)?.let { c.setRequestProperty("Authorization", authHeader(it, System.currentTimeMillis() / 1000)) }
            }
            c.outputStream.use { it.write(body.toByteArray()) }
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }

    /** Todas las filas `musicResponsiveListItemRenderer` de una respuesta de lista (en cualquier nivel del JSON). */
    internal fun parse(json: String): List<YtTrack> {
        val out = ArrayList<YtTrack>()
        fun walk(o: Any?) {
            when (o) {
                is JSONObject -> o.keys().forEach { k ->
                    if (k == "musicResponsiveListItemRenderer") toTrack(o.getJSONObject(k))?.let(out::add) else walk(o.opt(k))
                }
                is JSONArray -> for (i in 0 until o.length()) walk(o.opt(i))
            }
        }
        runCatching { walk(JSONObject(json)) }
        return out
    }

    internal fun runsText(column: JSONObject?): String =
        column?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs")
            ?.let { r -> (0 until r.length()).joinToString("") { r.getJSONObject(it).optString("text") } }.orEmpty()

    internal fun toTrack(item: JSONObject): YtTrack? {
        val videoId = item.optJSONObject("playlistItemData")?.optString("videoId")?.takeIf { it.isNotEmpty() } ?: return null
        val cols = item.optJSONArray("flexColumns") ?: return null
        val title = runsText(cols.optJSONObject(0)).ifBlank { return null }
        val duration = item.optJSONArray("fixedColumns")?.optJSONObject(0)
            ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs")
            ?.optJSONObject(0)?.optString("text").orEmpty()
        val thumbs = item.optJSONObject("thumbnail")?.optJSONObject("musicThumbnailRenderer")?.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
        return YtTrack(
            url = "$ORIGIN/watch?v=$videoId", title = title, artist = runsText(cols.optJSONObject(1)),
            durationSec = parseDuration(duration), thumbnail = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url")
        )
    }

    /** "4:13" → 253, "1:02:03" → 3723, otro texto → 0. */
    internal fun parseDuration(text: String): Long {
        val parts = text.split(":").map { it.trim().toLongOrNull() ?: return 0 }
        return parts.fold(0L) { acc, p -> acc * 60 + p }
    }
}
