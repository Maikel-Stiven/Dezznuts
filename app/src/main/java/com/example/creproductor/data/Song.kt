// Clase que le da los valores
// al archivo o canción como nombre número para organizarlo
// en la lista de reproducción

package com.example.creproductor.data

import android.net.Uri
import java.nio.charset.StandardCharsets

data class Song (
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val year: Int = 0,
    val path: String = "",
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val genre: String = "Desconocido"
)


fun String.fixEncoding(): String{
    return try {
        if (this.contains("â") || this.contains("Ã")){
            String(this.toByteArray(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8)
                .replace("â€™", "")
                .replace("â€“", "")
        }else{
            this
        }
    }catch (_: Exception){
        this
    }
}