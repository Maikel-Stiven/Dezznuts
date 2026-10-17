package com.example.creproductor.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MediaStoreScanner (private  val context: Context) {
    fun scanAudioFiles(chunkSize: Int = 100): Flow<List<Song>> = flow {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else{
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TRACK
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                add(MediaStore.Audio.Media.GENRE)
            }
        }.toTypedArray()

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projectionList,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
            }else -1

            val buffer = mutableListOf<Song>()

            while (cursor.moveToNext()){
                val id = cursor.getLong(idCol)
                val title = (cursor.getString(titleCol) ?: "Sin título").fixEncoding()
                val artist = (cursor.getString(artistCol) ?: "<Artista desconocido>").fixEncoding()
                val album = (cursor.getString(albumCol) ?: "<Álbum desconocido>").fixEncoding()
                val duration = cursor.getLong(durationCol)
                val albumId = cursor.getLong(albumIdCol)
                val year = cursor.getInt(yearColumn)
                val path = cursor.getString(dataCol) ?: ""
                val rawTrack = cursor.getInt(trackCol)
                val trackNumber = if (rawTrack >= 1000) rawTrack % 1000 else rawTrack
                val rawGenre = if (genreCol != -1) cursor.getString(genreCol) else null
                val genre = if (!rawGenre.isNullOrBlank()) rawGenre.fixEncoding() else "Desconocido"


                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )

                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                buffer.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        contentUri = contentUri,
                        albumArtUri = albumArtUri,
                        year = year,
                        trackNumber = trackNumber,
                        path = path,
                        genre = genre
                    )
                )

                if (buffer.size >= chunkSize){
                    emit(buffer.toList())
                    buffer.clear()
                }
            }

            if (buffer.isNotEmpty()){
                emit(buffer.toList())
            }
        }
    }.flowOn(Dispatchers.IO)
}