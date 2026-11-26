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

    private const val PREFS_NAME = "artist_photos_cache_v2"   // v2: las fotos de v1 podían ser de otro artista

    /** Carpeta (dentro de `cacheDir`) donde Coil guarda las imágenes; la comparten el cargador y el diálogo de borrado. */
    const val IMAGE_CACHE_DIR = "artist_images_cache"

    /** Artistas sin foto en esta sesión: evita repetir la consulta de red en cada fila/scroll. */
    private val noPhoto = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    /** Deezer limita a ~50 peticiones/5 s: al hacer scroll rápido se piden de dos en dos y no decenas a la vez. */
    private val gate = kotlinx.coroutines.sync.Semaphore(2)
    @Volatile private var blockedUntil = 0L

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
        return prefs.getString(cleanArtist, null)?.ifEmpty { null } // "" = Deezer no la tiene
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
            return@withContext prefs.getString(cleanArtist, null)?.ifEmpty { null }
        }

        if (cleanArtist in noPhoto || !isOnline(context) || android.os.SystemClock.elapsedRealtime() < blockedUntil){
            return@withContext null
        }
        gate.acquire()
        // Mientras esperaba turno otra fila pudo haberla traído ya.
        if (prefs.contains(cleanArtist)) { gate.release(); return@withContext prefs.getString(cleanArtist, null)?.ifEmpty { null } }

        var connection: HttpURLConnection? = null
        try {
            val encodedQuery = URLEncoder.encode(cleanArtist, "UTF-8")
            val urlString = "https://api.deezer.com/search/artist?q=$encodedQuery"

            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            val code = connection.responseCode
            if (code == 429 || code >= 500) {
                // Límite o caída de Deezer: pausa general, sin marcar a nadie como "sin foto".
                blockedUntil = android.os.SystemClock.elapsedRealtime() + 60_000
                return@withContext null
            }
            if (code == HttpURLConnection.HTTP_OK){
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val results = json.optJSONArray("data")

                // Solo vale un resultado que se llame igual: el primero de Deezer suele ser otro artista parecido
                // ("Aurora Vega" → "aurorawave"), y mostrar la cara de otro es peor que no mostrar nada.
                val match = (0 until (results?.length() ?: 0)).map { results!!.getJSONObject(it) }
                    .firstOrNull { CoverFetcher.sameName(cleanArtist, it.optString("name")) }
                if (match != null){
                    val photoUrl = match.optString("picture_big")

                    if (!photoUrl.isNullOrBlank() && !photoUrl.contains("/artist//")){
                        prefs.edit { putString(cleanArtist, photoUrl) }
                        return@withContext photoUrl
                    }
                }
            }
            // Respuesta válida sin foto: se recuerda también entre sesiones para no volver a preguntar.
            if (code == HttpURLConnection.HTTP_OK) prefs.edit { putString(cleanArtist, "") }
            noPhoto.add(cleanArtist)
        }catch (e: Exception){
            // Sin red real (Wi-Fi de portal, túnel): esta sesión no se vuelve a intentar con ese artista.
            if (e !is kotlinx.coroutines.CancellationException) noPhoto.add(cleanArtist) else throw e
        }finally {
            connection?.disconnect()
            gate.release()
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