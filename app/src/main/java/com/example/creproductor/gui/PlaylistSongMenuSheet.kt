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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.SkipNext
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.Playlist
import com.example.creproductor.data.Song
import com.example.creproductor.formatDuration

class PlaylistSongMenuActions(
    val onPlayNext: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onAddToPlaylist: (targetPlaylistId: String) -> Unit,
    val onRemoveFromPlaylist: () -> Unit,
    val onGoToAlbum: () -> Unit,
    val onGoToArtist: () -> Unit,
    val onEditTags: () -> Unit,
    val onAbout: () -> Unit,
    val onAudioCutter: () -> Unit,
    val onDeleteFromDevice: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSongMenuSheet(
    song: Song,
    playlist: List<Playlist>,
    actions: PlaylistSongMenuActions,
    onDismiss: () -> Unit
){
    var pickingPlaylist by remember { mutableStateOf(false) }
    var confirmDelete by remember {mutableStateOf(false)}
    val isCueTrack = song.cueStartMs >= 0

    fun run(block: () -> Unit): () -> Unit = {onDismiss(); block() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState (skipPartiallyExpanded = true)
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent = {Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)},
                supportingContent = {
                    val artist = if (song.artist.isBlank() || song.artist == "<unknown>") "Artista desconocido" else song.artist
                    Text("$artist • ${formatDuration(song.duration)}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            HorizontalDivider()

            if (pickingPlaylist){
                if (playlist.isEmpty()){
                    Text(
                        "Aún no haz creado una lista de reproducción",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                playlist.forEach { p ->
                    MenuRow(Icons.AutoMirrored.Filled.QueueMusic, p.name, {onDismiss(); actions.onAddToPlaylist(p.id) })

                }
            } else{
                MenuRow(Icons.Default.SkipNext, "Reproducir a continuación", run(actions.onPlayNext))
                MenuRow(Icons.AutoMirrored.Filled.QueueMusic, "Agregar al final de la cola", run(actions.onAddToQueue))
                MenuRow(Icons.AutoMirrored.Filled.PlaylistAdd, "Agregar a otra playlist", {pickingPlaylist = true})
                MenuRow(Icons.Default.RemoveCircleOutline, "Quitar de esta lista de reproducción", run(actions.onRemoveFromPlaylist))
                MenuRow(Icons.Default.Album, "Ir al álbum", run(actions.onGoToAlbum))
                MenuRow(Icons.Default.Person, "Ir al artista", run(actions.onGoToArtist))
                if (!isCueTrack) MenuRow(Icons.Default.Edit, "Editor de etiquetas", run(actions.onEditTags))
                MenuRow(Icons.Default.Info, "Acerca de", run(actions.onAbout))
                MenuRow(Icons.Default.ContentCut, "Cortador de audio", run(actions.onAudioCutter))
                if (!isCueTrack) MenuRow(Icons.Default.Delete, "Eliminar del dispositivo", {confirmDelete = true}, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (confirmDelete){
        AlertDialog(
            onDismissRequest = {confirmDelete = false},
            title = {Text(tr("¿Desea eliminar del dispositivo?"))},
            text = {Text("\"${song.title}\" se borrará del dispositivo. No se puede deshacer esta acción ")},
            confirmButton = {TextButton(onClick = {confirmDelete = false; onDismiss(); actions.onDeleteFromDevice()}) { Text(tr("Eliminar"))}},
            dismissButton = {TextButton(onClick = {confirmDelete = false}) {Text(tr("Cancelar")) }}
        )
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: Color = Color.Unspecified
){
    ListItem(
        headlineContent = {Text(text, color = tint)},
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}