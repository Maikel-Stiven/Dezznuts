package com.example.creproductor.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.core.content.edit

object ArtistImageFetcher {

    private const val PREFS_NAME = "artist_photos_cache"

    private fun isOnline (context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    suspend fun fetchArtistImageUrl(context: Context, artist: String): String? = withContext(Dispatchers.IO) {
        val cleanArtist = artist.trim()
        if (cleanArtist.isBlank() ||
            cleanArtist.equals("Artista desconocido", ignoreCase = true) ||
            cleanArtist.equals("<unknown>", ignoreCase = true)
        ) {
            return@withContext null
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.contains(cleanArtist)){
            return@withContext prefs.getString(cleanArtist, null)
        }

        if (!isOnline(context)){
            return@withContext null
        }

        try {
            val encodedQuery = URLEncoder.encode(cleanArtist, "UTF-8")
            val urlString = "https://api.deezer.com/search/artist?q=$encodedQuery"

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == HttpURLConnection.HTTP_OK){
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val results = json.optJSONArray("data")

                if (results != null && results.length() > 0){
                    val firstItem = results.getJSONObject(0)
                    val photoUrl = firstItem.optString("picture_big", null)

                    if (!photoUrl.isNullOrBlank()){
                        prefs.edit { putString(cleanArtist, photoUrl) }
                        return@withContext photoUrl
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        return@withContext null
    }
}