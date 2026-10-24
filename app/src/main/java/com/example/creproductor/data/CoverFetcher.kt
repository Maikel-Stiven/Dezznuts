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

    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

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
    suspend fun albumCandidates(title: String, artist: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode("$title $artist".trim(), "UTF-8")
            val c = URL("https://itunes.apple.com/search?term=$q&media=music&entity=song&limit=10").openConnection() as HttpURLConnection
            c.connectTimeout = 5000; c.readTimeout = 5000
            if (c.responseCode != HttpURLConnection.HTTP_OK) return@withContext emptyList()
            val results = JSONObject(c.inputStream.bufferedReader().use { it.readText() }).getJSONArray("results")
            (0 until results.length()).map { results.getJSONObject(it) }
                .filter { isSameSong(title, artist, it.optString("trackName"), it.optString("artistName")) }
                .map { it.optString("collectionName") }.filter { it.isNotBlank() }.distinct()
        } catch (e: CancellationException) { throw e } catch (_: Exception) { emptyList() }
    }

    suspend fun fetchOfficialCover(title: String, artist: String): RemoteMetaDataResult? = withContext(Dispatchers.IO){
        try {
            val query = "$title $artist".trim()
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=5"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK){
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val results = json.getJSONArray("results")

                for (i in 0 until results.length()){
                    val item = results.getJSONObject(i)
                    val foundTitle = item.optString("trackName", title)
                    val foundArtist = item.optString("artistName", artist)
                    if (!isSameSong(title, artist, foundTitle, foundArtist)) continue

                    val artwork100 = item.optString("artworkUrl100", null)
                    return@withContext RemoteMetaDataResult(
                        title = foundTitle,
                        artist = foundArtist,
                        album = item.optString("collectionName", ""),
                        highResCoverUrl = artwork100?.replace("100x100bb", "1000x1000bb")
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