package com.example.creproductor.data

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.InputStream
import java.net.URL
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine


object AudioTagWriter {

    suspend fun writeMetaData(
        context: Context,
        song: Song,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        coverUrlOrUri: String? = null
    ): Boolean = withContext(Dispatchers.IO){
        var tempFile: File? = null
        var backupFile: File? = null
        try {

            // jaudiotagger elige el lector por la extensión: con ".tmp" falla en
            // cualquier formato (FLAC incluido), así que se conserva la real.
            tempFile = File.createTempFile("edit_song_", ".${audioExtension(context, song)}", context.cacheDir)

            context.contentResolver.openInputStream(song.contentUri)?.use{input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            // Copia del original: la escritura final trunca el archivo (`rwt`) antes de copiar,
            // así que si falla a medias se restaura desde aquí en vez de dejar la canción rota.
            backupFile = File.createTempFile("backup_song_", ".${tempFile.extension}", context.cacheDir)
            tempFile.copyTo(backupFile, overwrite = true)

            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tagOrCreateAndSetDefault

            // Un campo vacío significa "no tocar": así no se escribe un marcador como "<unknown>" dentro del archivo.
            if (newTitle.isNotBlank()) tag.setField(FieldKey.TITLE, newTitle)
            if (newArtist.isNotBlank()) tag.setField(FieldKey.ARTIST, newArtist)
            if (newAlbum.isNotBlank()) tag.setField(FieldKey.ALBUM, newAlbum)

            if (!coverUrlOrUri.isNullOrBlank()){
                val imageBytes = fetchImageBytes(context, coverUrlOrUri)
                imageBytes?.let { bytes ->
                    tag.deleteArtworkField()
                    val artwork= ArtworkFactory.getNew()
                    artwork.binaryData = bytes
                    tag.setField(artwork)
                }
            }
            audioFile.commit()

            try {
                context.contentResolver.openOutputStream(song.contentUri, "rwt")?.use { output ->
                    tempFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: return@withContext false
            } catch (e: Exception) {
                restoreOriginal(context, song, backupFile)
                throw e
            }

            rescan(context, song)
            true
        }catch (e: Exception){
            e.printStackTrace()
            false
        }finally {
            tempFile?.delete()
            backupFile?.delete()
        }
    }

    /** Devuelve al archivo su contenido original tras una escritura fallida. */
    private fun restoreOriginal(context: Context, song: Song, backup: File) {
        try {
            context.contentResolver.openOutputStream(song.contentUri, "rwt")?.use { output ->
                backup.inputStream().use { it.copyTo(output) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** MediaStore no relee los tags solo: sin esto `loadSongs()` devuelve los datos viejos. */
    private suspend fun rescan(context: Context, song: Song) {
        if (song.path.isEmpty()) return
        suspendCancellableCoroutine { cont ->
            MediaScannerConnection.scanFile(context, arrayOf(song.path), null) { _, _ ->
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }

    private fun audioExtension(context: Context, song: Song): String {
        val fromPath = File(song.path).extension
        if (fromPath.isNotEmpty()) return fromPath
        val mime = context.contentResolver.getType(song.contentUri)
        return android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "mp3"
    }

    private fun fetchImageBytes(context: Context, urlOrUri: String): ByteArray?{
        return try {
            val inputStream: InputStream? = if (urlOrUri.startsWith("http")){
                URL(urlOrUri).openConnection().apply {
                    connectTimeout = 5000
                    readTimeout = 10000
                }.getInputStream()
            }else{
                context.contentResolver.openInputStream(Uri.parse(urlOrUri))
            }
            inputStream?.use { it.buffered().readBytes() }
        }catch (e: Exception){
            e.printStackTrace()
            null
        }
    }

}