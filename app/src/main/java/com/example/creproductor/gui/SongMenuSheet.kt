package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.Playlist
import com.example.creproductor.data.Song
import com.example.creproductor.data.isYoutube

/** Acciones del menú (⋮) de una canción, tal como las define la maqueta. */
class SongMenuActions(
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onAddToPlaylist: (playlistId: String) -> Unit,
    val onGoToAlbum: () -> Unit,
    val onGoToArtist: () -> Unit,
    val onEditTags: () -> Unit,
    val onAbout: () -> Unit,
    val onAudioCutter: () -> Unit,
    val onDelete: () -> Unit
)

/**
 * Menú de una canción: cola, lista, álbum, artista, etiquetas, ficha, cortador y borrado.
 * Las pistas de una hoja CUE comparten archivo, así que no ofrecen editar ni borrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuSheet(
    song: Song,
    playlists: List<Playlist>,
    actions: SongMenuActions,
    onDismiss: () -> Unit
) {
    var pickingPlaylist by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val isCueTrack = song.cueStartMs >= 0
    // Una canción de YouTube Music no es un archivo: nada de álbum local, etiquetas, cortador ni borrado.
    val isYt = song.isYoutube

    // Cada acción cierra el menú y luego ejecuta.
    fun run(block: () -> Unit): () -> Unit = { onDismiss(); block() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.navigationBarsPadding().verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                supportingContent = {
                    val artist = if (song.artist.isBlank() || song.artist == "<unknown>") "Artista desconocido" else song.artist
                    Text("$artist • ${com.example.creproductor.formatDuration(song.duration)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
            HorizontalDivider()

            if (pickingPlaylist) {
                if (playlists.isEmpty()) {
                    Text(
                        tr("Aún no has creado una lista de reproducción"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                playlists.forEach { p ->
                    MenuRow(Icons.AutoMirrored.Filled.QueueMusic, p.name, { onDismiss(); actions.onAddToPlaylist(p.id) })
                }
            } else {
                MenuRow(Icons.Default.SkipNext, "Reproducir a continuación", run(actions.onPlayNext))
                MenuRow(Icons.AutoMirrored.Filled.QueueMusic, "Agregar al final de la cola", run(actions.onAddToQueue))
                if (!isYt) MenuRow(Icons.AutoMirrored.Filled.PlaylistAdd, "Agregar a una playlist", { pickingPlaylist = true })
                if (!isYt) MenuRow(Icons.Default.Album, "Ir al álbum", run(actions.onGoToAlbum))
                MenuRow(Icons.Default.Person, "Ir al artista", run(actions.onGoToArtist))
                if (!isCueTrack && !isYt) MenuRow(Icons.Default.Edit, "Editor de etiquetas", run(actions.onEditTags))
                MenuRow(Icons.Default.Info, "Acerca de", run(actions.onAbout))
                if (!isYt) MenuRow(Icons.Default.ContentCut, "Cortador de audio", run(actions.onAudioCutter))
                if (!isCueTrack && !isYt) MenuRow(Icons.Default.DeleteForever, "Eliminar del dispositivo", { confirmDelete = true }, tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("¿Eliminar del dispositivo?")) },
            text = { Text("\"${song.title}\" se borrará del almacenamiento. No se puede deshacer.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDismiss(); actions.onDelete() }) { Text(tr("Eliminar")) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Cancelar")) } }
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, text: String, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified) {
    ListItem(
        headlineContent = { Text(tr(text), color = tint) },
        leadingContent = { Icon(icon, contentDescription = null, tint = if (tint == androidx.compose.ui.graphics.Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}
