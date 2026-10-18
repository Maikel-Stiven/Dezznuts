package com.example.creproductor.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InterruptedIOException
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

    suspend fun fetchOfficialCover(title: String, artist: String): RemoteMetaDataResult? = withContext(Dispatchers.IO){
        try {
            val query = "$title $artist".trim()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=1"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK){
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val results = json.getJSONArray("results")

                if (results.length() > 0){
                    val firstItem = results.getJSONObject(0)
                    val foundTitle = firstItem.optString("trackName", title)
                    val foundArtist = firstItem.optString("artistName", artist)
                    val foundAlbum = firstItem.optString("collectionName", "")

                    val artwork100 = firstItem.optString("artworkUrl100", null)
                    val hdCover = artwork100?.replace("100x100bb", "1000x1000bb")

                    return@withContext RemoteMetaDataResult(
                        title = foundTitle,
                        artist = foundArtist,
                        album = foundAlbum,
                        highResCoverUrl = hdCover
                    )
                }
            }
        }catch (e: InterruptedIOException){
        }catch (e: CancellationException){
            throw e
        }
        catch (e: Exception){
            e.printStackTrace()
        }
        return@withContext null
    }
}