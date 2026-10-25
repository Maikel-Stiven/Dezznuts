package com.example.creproductor.innertube

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class InnerTubeClient {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    companion object {
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1"
        private const val CLIENT_NAME = "WEB_REMIX"
        private const val CLIENT_VERSION = "1.20240101.01.00"
    }

    private fun buildContextPayload(): JSONObject {
        return JSONObject().apply {
            put("context", JSONObject().apply {
                put("client", JSONObject().apply {
                    put("clientName", CLIENT_NAME)
                    put("clientVersion", CLIENT_VERSION)
                    put("hl", "es")
                    put("gl", "CO")
                })
            })
        }
    }

    /** Consulta la pantalla principal de InnerTube (FEmusic_home) */
    suspend fun fetchHome(): List<YtOnlineTrack> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/browse"
        val payload = buildContextPayload().apply {
            put("browseId", "FEmusic_home")
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use emptyList()
            val responseBody = response.body?.string() ?: return@use emptyList()
            parseTracksFromResponse(JSONObject(responseBody))
        }
    }

    private fun parseTracksFromResponse(json: JSONObject): List<YtOnlineTrack> {
        val tracks = mutableListOf<YtOnlineTrack>()
        runCatching {
            val contents = json.optJSONObject("contents")
                ?.optJSONObject("singleColumnBrowseResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("sections") ?: return emptyList()

            for (i in 0 until contents.length()) {
                val section = contents.optJSONObject(i)
                val shelf = section?.optJSONObject("musicShelfRenderer")
                    ?: section?.optJSONObject("musicCarouselShelfRenderer") ?: continue

                val items = shelf.optJSONArray("contents") ?: continue
                for (j in 0 until items.length()) {
                    val item = items.optJSONObject(j)
                    val responsiveItem = item?.optJSONObject("musicResponsiveListItemRenderer")
                        ?: item?.optJSONObject("musicTwoRowItemRenderer") ?: continue

                    val videoId = responsiveItem.optString("navigationEndpoint")
                        .let { JSONObject(it).optJSONObject("watchEndpoint")?.optString("videoId") }
                        ?: continue

                    val title = parseText(responsiveItem, "flexColumns", 0)
                    val artist = parseText(responsiveItem, "flexColumns", 1)
                    val thumbUrl = parseThumbnail(responsiveItem)

                    if (title.isNotBlank()) {
                        tracks.add(YtOnlineTrack(videoId, title, artist, thumbUrl))
                    }
                }
            }
        }
        return tracks
    }

    private fun parseText(item: JSONObject, columnKey: String, colIndex: Int): String {
        return runCatching {
            val cols = item.optJSONArray(columnKey) ?: return ""
            val col = cols.optJSONObject(colIndex)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer") ?: return ""
            val textObj = col.optJSONObject("text") ?: return ""
            val runs = textObj.optJSONArray("runs") ?: return ""
            runs.optJSONObject(0)?.optString("text") ?: ""
        }.getOrDefault("")
    }

    private fun parseThumbnail(item: JSONObject): String {
        return runCatching {
            val thumbs = item.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails") ?: return ""
            thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
        }.getOrDefault("")
    }
}