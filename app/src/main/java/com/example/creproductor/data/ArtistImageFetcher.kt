package com.example.creproductor.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Obtiene la foto de un artista desde la API pública de Deezer y **descarga la imagen**
 * para guardarla en el almacenamiento interno de la app, de forma que solo se pida una vez.
 *
 * Funcionamiento (para evitar peticiones repetidas):
 *  1. Si la imagen ya está descargada en disco (`filesDir/artist_images`), se devuelve ese
 *     archivo y **no se toca la red**.
 *  2. Si no está, se resuelve la URL del artista (caché en memoria → `SharedPreferences` →
 *     API de Deezer) y se descarga el archivo.
 *  3. Cuando Deezer responde que el artista no tiene foto, se recuerda con [NO_PHOTO] para
 *     no volver a preguntar.
 *
 * La caché en disco se puede vaciar con [clearArtistImageCache] (pensado para una futura
 * opción de "borrar caché" si ocupa demasiado).
 */
object ArtistImageFetcher {

    private const val PREFS_NAME = "artist_photos_cache"

    /** Centinela que marca "consultado, sin foto". */
    private const val NO_PHOTO = ""

    /** Carpeta (dentro de filesDir) donde se guardan las imágenes descargadas. */
    private const val IMAGE_DIR = "artist_images"

    /** URL de la foto por artista (sesión). */
    private val urlMemoryCache = ConcurrentHashMap<String, String>()

    /** Archivo ya descargado por artista (sesión). */
    private val fileMemoryCache = ConcurrentHashMap<String, File>()

    private fun isOnline (context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun isUnknown(artist: String): Boolean =
        artist.isBlank() ||
        artist.equals("Artista desconocido", ignoreCase = true) ||
        artist.equals("<unknown>", ignoreCase = true)

    /** Nombre de archivo seguro (hash) para el artista. */
    private fun imageFileFor(context: Context, artist: String): File {
        val dir = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }
        val digest = MessageDigest.getInstance("MD5").digest(artist.lowercase().trim().toByteArray())
        val safeName = digest.joinToString("") { "%02x".format(it) }
        return File(dir, "$safeName.jpg")
    }

    /**
     * Devuelve el archivo de la foto **si ya está descargado**, sin tocar la red.
     *
     * Es una lectura síncrona y barata (`File.exists()`), pensada para pintar la foto desde
     * el primer fotograma y así evitar el "parpadeo" a la carátula al reabrir un artista.
     */
    fun cachedArtistImage(context: Context, artist: String): File? {
        val cleanArtist = artist.trim()
        if (isUnknown(cleanArtist)) return null

        fileMemoryCache[cleanArtist]?.takeIf { it.exists() }?.let { return it }

        val file = imageFileFor(context, cleanArtist)
        if (file.exists() && file.length() > 0) {
            fileMemoryCache[cleanArtist] = file
            return file
        }
        return null
    }

    /**
     * Devuelve el archivo con la foto del artista, **descargándola y guardándola la primera
     * vez**. Las siguientes llamadas leen directamente del disco.
     *
     * @return el archivo de imagen, o `null` si no existe foto / no hay conexión / falló la descarga.
     */
    suspend fun fetchArtistImage(context: Context, artist: String): File? = withContext(Dispatchers.IO) {
        val cleanArtist = artist.trim()
        if (isUnknown(cleanArtist)) return@withContext null

        // 1) Ya descargada en esta sesión.
        fileMemoryCache[cleanArtist]?.takeIf { it.exists() }?.let { return@withContext it }

        // 2) Ya descargada en disco -> no se toca la red.
        val target = imageFileFor(context, cleanArtist)
        if (target.exists() && target.length() > 0) {
            fileMemoryCache[cleanArtist] = target
            return@withContext target
        }

        // 3) Resolver la URL (memoria/prefs/Deezer) y descargar la imagen.
        val url = resolveImageUrl(context, cleanArtist) ?: return@withContext null

        if (!isOnline(context)) return@withContext null

        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 6000
            connection.readTimeout = 6000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                if (target.length() > 0) {
                    fileMemoryCache[cleanArtist] = target
                    return@withContext target
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()   // fallo de red: no se cachea nada para poder reintentar.
        }

        target.delete()   // descarga incompleta -> limpiar el archivo a medias.
        return@withContext null
    }

    /**
     * Resuelve la URL de la foto del artista: memoria → `SharedPreferences` → API de Deezer.
     * No descarga la imagen; solo averigua dónde está.
     */
    private fun resolveImageUrl(context: Context, artist: String): String? {
        val cleanArtist = artist.trim()
        if (isUnknown(cleanArtist)) return null

        // 1) Caché en memoria.
        urlMemoryCache[cleanArtist]?.let { return it.ifBlank { null } }

        // 2) Caché persistente (disco).
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(cleanArtist)) {
            val value = prefs.getString(cleanArtist, null) ?: NO_PHOTO
            urlMemoryCache[cleanArtist] = value
            return value.ifBlank { null }
        }

        // 3) Red.
        if (!isOnline(context)) return null

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

                // Deezer a veces responde 200 con un objeto "error"; en ese caso NO cacheamos.
                if (!json.has("error")) {
                    val results = json.optJSONArray("data")

                    val photoUrl = results
                        ?.optJSONObject(0)
                        ?.optString("picture_big")
                        ?.takeIf { it.isNotBlank() && it != "null" }

                    if (!photoUrl.isNullOrBlank()) {
                        urlMemoryCache[cleanArtist] = photoUrl
                        prefs.edit().putString(cleanArtist, photoUrl).apply()
                        return photoUrl
                    }

                    // Respondió bien pero el artista no tiene foto: lo recordamos.
                    urlMemoryCache[cleanArtist] = NO_PHOTO
                    prefs.edit().putString(cleanArtist, NO_PHOTO).apply()
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        return null
    }

    /**
     * Tamaño en bytes que ocupan las imágenes de artistas ya descargadas.
     * Se usa para mostrarlo en el diálogo de "borrar caché".
     */
    fun artistImageCacheSize(context: Context): Long {
        val dir = File(context.filesDir, IMAGE_DIR)
        val files = dir.listFiles() ?: return 0L
        return files.sumOf { if (it.isFile) it.length() else 0L }
    }

    /**
     * Borra la caché de imágenes de artistas (archivos descargados + URLs recordadas).
     * Pensado para la futura opción de "limpiar caché" desde los ajustes.
     */
    fun clearArtistImageCache(context: Context) {
        fileMemoryCache.clear()
        urlMemoryCache.clear()

        File(context.filesDir, IMAGE_DIR).deleteRecursively()

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
