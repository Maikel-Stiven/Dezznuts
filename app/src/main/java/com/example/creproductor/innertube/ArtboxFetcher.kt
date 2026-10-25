package com.example.creproductor.innertube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object ArtboxFetcher {
    private val client = OkHttpClient()

    /**
     * Busca la portada en alta resolución (1000x1000) via iTunes search API,
     * Si no la encuentra, retorna enlace original de Yt Music
     */

    suspend fun getHdCoverUrl(artist: String, title: String, fallbackUrl: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val query = URLEncoder.encode("$artist $title", "UTF-8")
            val url = "https://itunes.apple.com/search?term=$query&entity=song&limit=1"
            val request = Request.Builder().url(url).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use fallbackUrl
                val body = response.body?.string() ?: return@use fallbackUrl
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return@use fallbackUrl
                if (results.length() > 0) {
                    val artworkUrl = results.getJSONObject(0).optString("artworkUrl100", "")
                    if (artworkUrl.isNotBlank()) {
                        return@use artworkUrl.replace("100x100bb.jpg", "1000x1000bb.jpg")
                    }
                }
                fallbackUrl
            }
        }.getOrDefault(fallbackUrl)
    }
}