package com.example.creproductor.data

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.ContentMetadataMutations
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Canciones de YouTube Music guardadas a propósito para escucharlas sin internet. A diferencia de [YtAudioCache]
 * (lo último que sonó, se va borrando solo), esto no se borra hasta que el usuario quita la descarga, y vive en
 * `filesDir`: el sistema no lo limpia al faltar espacio. La clave es la página de la canción, igual que en la caché.
 */
@OptIn(UnstableApi::class)
object YtOffline {
    /** Lista de YT donde aparecen las descargadas (se ve en "Tus playlists" y funciona sin conexión). */
    const val PLAYLIST_ID = "dezz_offline"

    @Volatile private var cache: SimpleCache? = null

    fun cache(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(File(context.applicationContext.filesDir, "yt_offline"), NoOpCacheEvictor(),
            YtAudioCache.database(context)).also { cache = it }
    }

    fun isDownloaded(context: Context, url: String): Boolean {
        val c = cache(context)
        val len = ContentMetadata.getContentLength(c.getContentMetadata(url))
        return len > 0 && c.isCached(url, 0, len)
    }

    /**
     * Descarga entera (si ya sonó, la copia de la caché sin gastar datos). Bloquea: llamar en Dispatchers.IO.
     * Por trozos de 1 MB: YouTube sirve rápido los pedidos cortos y frena los de archivo entero (a ritmo de reproducción).
     */
    fun download(context: Context, url: String, onProgress: (Float) -> Unit = {}) {
        val c = cache(context)
        val source = CacheDataSource.Factory().setCache(c)
            .setUpstreamDataSourceFactory(YtAudioCache.lruFactory(context)).createDataSource()
        // El tamaño viene en la dirección de audio (clen): con pedidos por trozos la caché no lo sabría nunca.
        // Mismos argumentos que usa el reproductor, así sale el mismo stream (y lo que ya sonó se reaprovecha).
        val lruLen = ContentMetadata.getContentLength(YtAudioCache.get(context).getContentMetadata(url)).takeIf { it > 0 }
        val total = YtMusic.clen(YtMusic.resolve(url, YtMusic.saveData(context), lruLen).audioUrl)
        if (total == null) {  // sin clen: de una vez (más lento, pero la caché aprende el tamaño al llegar al final)
            CacheWriter(source, DataSpec(Uri.parse(url)), null) { t, done, _ -> if (t > 0) onProgress(done.toFloat() / t) }.cache()
            return
        }
        c.applyContentMetadataMutations(url, ContentMetadataMutations().also { ContentMetadataMutations.setContentLength(it, total) })
        var pos = 0L
        while (pos < total) {
            val len = minOf(CHUNK, total - pos)
            val start = pos
            CacheWriter(source, DataSpec.Builder().setUri(Uri.parse(url)).setPosition(pos).setLength(len).build(), null) { _, done, _ ->
                onProgress((start + done).toFloat() / total)
            }.cache()
            pos += len
        }
    }

    private const val CHUNK = 1L shl 20

    fun remove(context: Context, url: String) = runCatching { cache(context).removeResource(url) }

    fun usedBytes(context: Context): Long = cache(context).cacheSpace
}
