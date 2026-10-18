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
import java.io.File

object ArtistImageFetcher {

    private const val PREFS_NAME = "artist_photos_cache"

    /** Carpeta (dentro de `cacheDir`) donde Coil guarda las imágenes; la comparten el cargador y el diálogo de borrado. */
    const val IMAGE_CACHE_DIR = "artist_images_cache"

    /** Artistas sin foto en esta sesión: evita repetir la consulta de red en cada fila/scroll. */
    private val noPhoto = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private fun isOnline (context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun cachedArtistImage(context: Context, artist: String): String?{
        val cleanArtist = artist.trim()
        if (cleanArtist.isBlank() ||
            cleanArtist.equals("Artista desconocido", ignoreCase = true) ||
            cleanArtist.equals("<unknown>", ignoreCase = true)
            ){
            return null
        }
        val  prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(cleanArtist, null)
    }

    suspend fun fetchArtistImage(context: Context, artist: String): String?{
        return fetchArtistImageUrl(context, artist)
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

        if (cleanArtist in noPhoto || !isOnline(context)){
            return@withContext null
        }

        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(cleanArtist, "UTF-8")
            val urlString = "https://api.deezer.com/search/artist?q=$encodedQuery"

            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
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
            noPhoto.add(cleanArtist)
        }catch (e: Exception){
            e.printStackTrace()
        }finally {
            connection?.disconnect()
        }
        return@withContext null
    }

    fun artistImageCacheSize(context: Context): Long {
        val cacheDir = File(context.cacheDir, IMAGE_CACHE_DIR)
        if (!cacheDir.exists()) return 0L
        return cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    fun clearArtistImageCache(context: Context){
        noPhoto.clear()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit{clear()}

        val cacheDir = File(context.cacheDir, IMAGE_CACHE_DIR)
        if (cacheDir.exists()){
            cacheDir.deleteRecursively()
        }
    }
}