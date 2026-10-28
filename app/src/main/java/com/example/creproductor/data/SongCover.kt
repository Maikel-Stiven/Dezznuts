// Carátula de una canción para Coil.
//
// En muchos dispositivos el MediaStore no indexa el arte embebido de los FLAC (en el Huawei Y6p
// con Android 9, `audio/albumart/<id>` existe pero devuelve 0 bytes). En ese caso se extrae la
// imagen del propio archivo con MediaMetadataRetriever y se guarda en cacheDir para no repetirlo.

package com.example.creproductor.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okio.Buffer

/**
 * Modelo de imagen para Coil. Dos canciones del mismo álbum comparten [cacheKey] (el id de álbum),
 * así que comparten entrada de caché y no se extrae la misma carátula dos veces.
 */
class SongCover(
    val cacheKey: String,
    val contentUri: Uri,
    val albumArtUri: Uri?,
    val mimeType: String = "",
    val path: String = ""
) {
    override fun equals(other: Any?) = other is SongCover && other.cacheKey == cacheKey

    override fun hashCode() = cacheKey.hashCode()

    override fun toString() = "SongCover($cacheKey)"
}

/** Se usa para no abrir 10 FLAC grandes a la vez en un móvil lento. */
private val extractionLimit = Semaphore(3)

val Song.coverModel: SongCover
    get() = SongCover(
        cacheKey = if (albumArtUri?.scheme == "https") "web_${albumArtUri.toString().hashCode().toUInt().toString(16)}"
            else albumArtUri?.lastPathSegment?.let { "album_$it" }
            ?: "song_${contentUri.lastPathSegment}",
        contentUri = contentUri,
        albumArtUri = albumArtUri,
        mimeType = mimeType,
        path = path
    )

class SongCoverFetcher(
    private val context: Context,
    private val model: SongCover
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        if (missingMarker().exists()) return@withContext null

        val bytes = readFromCache() ?: readFromWeb() ?: readFromMediaStore() ?: extractAndCache()
        if (bytes == null) {
            // Sin arte: se recuerda para no volver a abrir el archivo en cada aparición.
            runCatching { missingMarker().apply { parentFile?.mkdirs() }.writeText("") }
            return@withContext null
        }
        SourceResult(
            source = ImageSource(Buffer().write(bytes), context),
            mimeType = "image/jpeg",
            dataSource = DataSource.DISK
        )
    }

    private fun readFromCache(): ByteArray? {
        val file = cacheFile()
        if (!file.isFile || file.length() == 0L) return null
        return runCatching { file.readBytes() }.getOrNull()
    }

    /** Miniatura de YouTube Music (https): se descarga una vez y queda en el mismo caché que las demás. */
    private fun readFromWeb(): ByteArray? {
        val uri = model.albumArtUri?.takeIf { it.scheme == "https" } ?: return null
        val bytes = runCatching {
            val c = java.net.URL(uri.toString()).openConnection() as java.net.HttpURLConnection
            try {
                c.connectTimeout = 8000; c.readTimeout = 8000
                if (c.responseCode == 200) c.inputStream.use { it.readBytes() } else null
            } finally { c.disconnect() }
        }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return null
        runCatching { cacheFile().apply { parentFile?.mkdirs() }.writeBytes(bytes) }
        return bytes
    }

    private fun readFromMediaStore(): ByteArray? {
        val uri = model.albumArtUri ?: return null
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private suspend fun extractAndCache(): ByteArray? = extractionLimit.withPermit {
        // Otro hilo pudo extraerla mientras se esperaba el permiso.
        readFromCache()?.let { return@withPermit it }

        val esFlac = model.mimeType.contains("flac", ignoreCase = true) ||
            model.path.endsWith(".flac", ignoreCase = true)
        val bytes = if (esFlac) {
            // Android 9 no saca la imagen de un FLAC por MediaMetadataRetriever; se parsea el bloque.
            readFlacPicture()
        } else {
            readWithRetriever() ?: readFlacPicture()
        }?.takeIf { it.isNotEmpty() } ?: return@withPermit null

        val file = cacheFile()
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeBytes(bytes)
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        }
        bytes
    }

    private fun readWithRetriever(): ByteArray? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, model.contentUri)
            retriever.embeddedPicture
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun readFlacPicture(): ByteArray? = runCatching {
        context.contentResolver.openInputStream(model.contentUri)?.use { FlacTags.picture(it) }
    }.getOrNull()

    private fun cacheFile() = File(context.cacheDir, "covers/${model.cacheKey}.img")

    private fun missingMarker() = File(context.cacheDir, "covers/${model.cacheKey}.none")

    class Factory(private val context: Context) : Fetcher.Factory<SongCover> {
        override fun create(data: SongCover, options: Options, imageLoader: ImageLoader): Fetcher =
            SongCoverFetcher(context, data)
    }
}

/** Tamaño y limpieza del caché de carátulas extraídas (compartido con el diálogo de caché). */
object SongCoverCache {
    private const val DIR = "covers"

    fun size(context: Context): Long {
        val dir = File(context.cacheDir, DIR)
        return if (dir.exists()) dir.walkTopDown().filter { it.isFile }.sumOf { it.length() } else 0L
    }

    fun clear(context: Context) {
        File(context.cacheDir, DIR).deleteRecursively()
    }

    /** Tras editar una carátula: la extraída del archivo ya no vale. */
    fun invalidate(context: Context, song: Song) {
        val key = song.coverModel.cacheKey
        File(context.cacheDir, "$DIR/$key.img").delete()
        File(context.cacheDir, "$DIR/$key.none").delete()
    }
}
