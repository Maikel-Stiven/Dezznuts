package com.example.creproductor.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Save
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

class PlaylistMenuActions(
    val onPlay: () -> Unit,
    val onAddSongs: () -> Unit,
    val onAddToQueue: () -> Unit,
    val onAddToPlaylist: (targetPlaylistId: String) -> Unit,
    val onSaveToDevice: () -> Unit,
    val onEditTags: () -> Unit,
    val onDeletePlaylist: () -> Unit,
    val onRemoveDuplicates: () -> Unit,
    val onDeleteFromDevice: () -> Unit

)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistMenuSheet(
    playlist: Playlist,
    allSongs: List<Song>,
    allPlaylist: List<Playlist>,
    actions: PlaylistMenuActions,
    onDismiss: () -> Unit
){
    var pickingTargetPlaylist by remember { mutableStateOf(false) }
    var confirmDeletePlaylist by remember { mutableStateOf(false) }
    var confirmDeleteFromDevice by remember { mutableStateOf(false) }

    val isEmpty = playlist.songIds.isEmpty()

    val playlistSongs = remember(playlist.songIds, allSongs) {
        if (playlist.songIds.isEmpty() || allSongs.isEmpty()) emptyList()
        else{
            val songMap = allSongs.associateBy { it.id }
            playlist.songIds.mapNotNull { songMap[it] }
        }
    }

    val totalDurationMs = remember(playlistSongs) {
        playlistSongs.sumOf { it.duration }
    }

    val countLabel = if (playlist.songIds.size == 1) "1 canción" else "${playlist.songIds.size} canciones"
    val durationLabel = formatDuration(totalDurationMs)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = playlist.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                supportingContent = {
                    Text(
                        text = if (isEmpty) countLabel else "$countLabel • $durationLabel",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            HorizontalDivider()

            if (pickingTargetPlaylist){
                val availablePlaylist = remember(allPlaylist, playlist.id) {
                    allPlaylist.filter { it.id != playlist.id }
                }

                if (availablePlaylist.isEmpty()){
                    Text(
                        text = "No hay otras listas de reproducción creadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }else{
                    availablePlaylist.forEach { target ->
                        MenuRow(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            text = target.name,
                            onClick = {onDismiss(); actions.onAddToPlaylist(target.id)}
                        )
                    }
                }
            }else{
                if (isEmpty){
                    MenuRow(
                        icon = Icons.Default.AddCircleOutline,
                        text = "Agregar pistas",
                        onClick = {onDismiss(); actions.onAddSongs()}
                    )
                }else{
                    MenuRow(
                        icon = Icons.Default.PlayArrow,
                        text = "Reproducir",
                        onClick = {onDismiss(); actions.onPlay()}
                    )

                    MenuRow(
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        text = "Agregar a la cola de reproducción actual",
                        onClick = {onDismiss(); actions.onAddToQueue()}
                    )

                    MenuRow(
                        icon = Icons.Default.AddCircle,
                        text = "Agregar pistas",
                        onClick = {onDismiss(); actions.onAddSongs()}
                    )
                }

                MenuRow(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    text = "Agregar a una playlist",
                    onClick = {pickingTargetPlaylist = true}
                )
                MenuRow(
                    icon = Icons.Default.Save,
                    text = "Guardar playlist en el dispositivo",
                    onClick = {onDismiss(); actions.onSaveToDevice()}
                )
                MenuRow(
                    icon = Icons.Default.Edit,
                    text = "Editor de etiquetas",
                    onClick = {onDismiss(); actions.onEditTags()}
                )
                MenuRow(
                    icon = Icons.Default.Close,
                    text = "Eliminar playlist",
                    onClick = {confirmDeletePlaylist = true}
                )

                if (!isEmpty){
                    MenuRow(
                        icon = Icons.Default.RemoveCircleOutline,
                        text = "Eliminar duplicados de la playlist",
                        onClick = {onDismiss(); actions.onRemoveDuplicates()}
                    )
                }

                HorizontalDivider()

                MenuRow(
                    icon = Icons.Default.Delete,
                    text = "Eliminar del dispositivo",
                    onClick = {confirmDeleteFromDevice = true},
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (confirmDeletePlaylist){
        AlertDialog(
            onDismissRequest = {confirmDeletePlaylist = false},
            title = {Text("Se eliminará la playlist \"${playlist.name}\". ¿Desea eliminar la playlist?")},
            confirmButton = {
                TextButton(onClick =  {confirmDeletePlaylist = false; onDismiss();  actions.onDeletePlaylist() }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = {confirmDeletePlaylist = false}) {
                    Text("Cancelar")
                }
            }

        )
    }

    if (confirmDeleteFromDevice){
        AlertDialog(
            onDismissRequest = {confirmDeleteFromDevice = false},
            title = {Text("¿Eliminar del dispositivo?")},
            text = {Text("Se eliminaran las canciones de \"${playlist.name}\" del dispositivo. Esta acción no se puede deshacer")},
            confirmButton =  {
                TextButton(onClick = {confirmDeleteFromDevice = false; onDismiss(); actions.onDeleteFromDevice()}) {
                    Text("Elimnar canciones", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {confirmDeleteFromDevice = false}) {
                    Text("Cancelar")
                }
            }
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
                tint = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else tint
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick)
    )
}