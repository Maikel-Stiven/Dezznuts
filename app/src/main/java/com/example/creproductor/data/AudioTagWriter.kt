package com.example.creproductor.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.File
import java.io.InputStream
import java.net.URL


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
        try {

            tempFile = File.createTempFile("edit_song_", ".tmp", context.cacheDir)

            context.contentResolver.openInputStream(song.contentUri)?.use{input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext false

            val audioFile = AudioFileIO.read(tempFile)
            val tag = audioFile.tagOrCreateAndSetDefault

            tag.setField(FieldKey.TITLE, newTitle)
            tag.setField(FieldKey.ARTIST, newArtist)
            tag.setField(FieldKey.ALBUM, newAlbum)

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

            context.contentResolver.openOutputStream(song.contentUri, "rwt")?.use { output ->
                tempFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }?: return@withContext false

            true
        }catch (e: Exception){
            e.printStackTrace()
            false
        }finally {
            tempFile?.delete()
        }
    }

    private fun fetchImageBytes(context: Context, urlOrUri: String): ByteArray?{
        return try {
            val inputStream: InputStream? = if (urlOrUri.startsWith("http")){
                URL(urlOrUri).openStream()
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