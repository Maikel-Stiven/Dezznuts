package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.AudioQuality
import com.example.creproductor.data.QualityTier
import com.example.creproductor.data.Song
import com.example.creproductor.data.isYoutube

/** Ficha técnica de la canción: archivo, calidad y salida real. Se abre desde el menú (⋮) del reproductor. */
@Composable
fun TrackInfoDialog(
    song: Song,
    audioInfo: String,
    onEdit: () -> Unit = {},
    onGoToArtist: () -> Unit = {},
    onGoToAlbum: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onDismiss: () -> Unit
) {

    if (song.isYoutube){
        AlertDialog(
            onDismissRequest =  onDismiss,
            icon = {Icon(Icons.Default.MusicNote, contentDescription = null)},
            title = {Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)},
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "${song.artist} • YouTube Music",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    TextButton(
                        onClick = {onDismiss(); onGoToArtist()},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                            Text(tr("Ir al artista"))
                        }
                    }
                    TextButton(
                        onClick = {onDismiss(); onGoToAlbum()},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Album, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                            Text(tr("Ir al álbum"))
                        }
                    }
                    TextButton(
                        onClick = {onDismiss(); onAddToPlaylist()},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.padding(end = 12.dp))
                            Text(tr("Agregar a playlist"))
                        }
                    }
                }
            },
            confirmButton = {
              TextButton(onClick = onDismiss) { Text(tr("Cerrar"))}
            }
        )
        return
    }
    val tier = when (song.quality) {
        QualityTier.HIRES -> "Hi-Res"
        QualityTier.LOSSLESS -> "Lossless"
        QualityTier.LOSSY -> tr("Con pérdida")
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
        title = { Text(tr("Información técnica")) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { (k, v) ->
                    Column {
                        Text(tr(k), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text(v, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
        dismissButton = { TextButton(onClick = { onDismiss(); onEdit() }) { Text(tr("Editar etiquetas")) } }
    )
}
