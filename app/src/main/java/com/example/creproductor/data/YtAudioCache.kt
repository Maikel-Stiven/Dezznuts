package com.example.creproductor.data

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Lo que ya sonó de YouTube Music se queda en disco (LRU): repetirlo no gasta datos y, sin internet, una canción
 * escuchada entera vuelve a sonar. Solo pasa por aquí lo que viene de http(s); los archivos locales van directo.
 * La clave es la página de la canción, no la dirección de audio (esa caduca en horas).
 */
@OptIn(UnstableApi::class)
object YtAudioCache {
    @Volatile private var cache: SimpleCache? = null
    @Volatile private var db: StandaloneDatabaseProvider? = null

    /** Una sola base de datos para las dos cachés (esta y [YtOffline]): dos conexiones al mismo archivo se bloquean. */
    internal fun database(context: Context): StandaloneDatabaseProvider = db ?: synchronized(this) {
        db ?: StandaloneDatabaseProvider(context.applicationContext).also { db = it }
    }

    fun get(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: run {
            val dir = File(context.applicationContext.cacheDir, "yt_audio")
            // ponytail: tope fijo según espacio libre al crear; ajustable desde Opciones si alguien lo pide.
            val max = (dir.parentFile?.usableSpace ?: 0L).div(10).coerceIn(64L shl 20, 384L shl 20)
            SimpleCache(dir, LeastRecentlyUsedCacheEvictor(max), database(context))
                .also { cache = it }
        }
    }

    /**
     * Fuente de datos del reproductor: archivos/content:// directos y http(s) a través de la caché. Una canción de
     * YouTube entra al reproductor con la dirección de su página; la de audio (que caduca) se pide aquí, justo al
     * cargarla, así que la cola entera vive en el reproductor (siguiente/anterior desde la notificación, los
     * auriculares o Android Auto, y sigue sola con la app cerrada). Si ya está entera en disco ni se pide.
     */
    fun dataSourceFactory(context: Context): DataSource.Factory {
        val app = context.applicationContext
        // Primero lo descargado a propósito (solo lectura: lo que suene no se guarda ahí), luego la caché LRU.
        val offline = CacheDataSource.Factory()
            .setCache(YtOffline.cache(app))
            .setUpstreamDataSourceFactory(lruFactory(app))
            .setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return DefaultDataSource.Factory(app, offline)
    }

    /** http(s) a través de la caché LRU; la página de YouTube se cambia por su audio al abrirla. */
    internal fun lruFactory(context: Context): CacheDataSource.Factory {
        val app = context.applicationContext
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(10_000).setReadTimeoutMs(15_000)
            .setAllowCrossProtocolRedirects(true)
        val resolving = ResolvingDataSource.Factory(http) { spec ->
            val page = spec.uri.toString()
            if (!isYtPage(page)) return@Factory spec
            val metered = YtMusic.saveData(app)
            val cachedLen = ContentMetadata.getContentLength(get(app).getContentMetadata(page))
            val r = try { YtMusic.resolve(page, metered, preferLength = cachedLen.takeIf { it > 0 }) }
                catch (e: java.io.IOException) { throw e } catch (e: Exception) { throw java.io.IOException(e) }
            spec.withUri(android.net.Uri.parse(r.audioUrl))
        }
        return CacheDataSource.Factory()
            .setCache(get(app))
            .setUpstreamDataSourceFactory(resolving)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun isYtPage(url: String) = url.startsWith("https://music.youtube.com/watch") || url.startsWith("https://www.youtube.com/watch")
}
