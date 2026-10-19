package com.example.creproductor.gui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.AudioQuality
import com.example.creproductor.data.QualityTier
import com.example.creproductor.data.Song

/** Ficha técnica de la canción: archivo, calidad y salida real. Se abre desde el menú (⋮) del reproductor. */
@Composable
fun TrackInfoDialog(song: Song, audioInfo: String, onDismiss: () -> Unit) {
    val tier = when (song.quality) {
        QualityTier.HIRES -> "Hi-Res"
        QualityTier.LOSSLESS -> "Lossless"
        QualityTier.LOSSY -> "Con pérdida"
    }
    val file = AudioQuality.label(song.mimeType, song.sampleRate, song.bitsPerSample, song.bitrate)
    val rows = listOf(
        "Título" to song.title,
        "Artista" to song.artist,
        "Álbum" to song.album,
        "Calidad" to tier,
        "Archivo" to file.ifEmpty { "—" },
        "Reproduciendo" to audioInfo.ifEmpty { "—" },
        "Ruta" to song.path.ifEmpty { "—" }
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("Información técnica") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (k, v) ->
                    Column {
                        Text(k, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text(v, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
