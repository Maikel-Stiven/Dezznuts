package com.example.creproductor.gui

import com.example.creproductor.i18n.tr
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.Song
import com.example.creproductor.data.coverModel
import com.example.creproductor.gui.CoverQualityStrip
import com.example.creproductor.gui.PlayingEqualizerIcon

/**
 * Lista de canciones con selección múltiple: una pulsación larga entra en modo selección; la barra superior permite
 * añadir a una lista, a la cola o eliminar de una vez.
 */
@Composable
fun SongListScreen(
    songs: List<Song>,
    currentSongId: Long? = null,
    isPlaying: Boolean = false,
    playlists: List<com.example.creproductor.data.Playlist> = emptyList(),
    onSongClick: (Song) -> Unit,
    onEditSong: (Song) -> Unit,
    onAddToPlaylist: (playlistId: String, songs: List<Song>) -> Unit = { _, _ -> },
    onAddToQueue: (List<Song>) -> Unit = {},
    onNewPlaylist: () -> Unit = {},
    onDeleteSongs: (List<Song>) -> Unit = {}
) {
    // Por id: si la biblioteca se recarga o el filtro cambia, la selección se conserva sin referirse a objetos viejos.
    val selected = remember { mutableStateListOf<Long>() }
    val inSelection = selected.isNotEmpty()
    var confirmDelete by remember { mutableStateOf(false) }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    androidx.activity.compose.BackHandler(enabled = inSelection) { selected.clear() }
    fun chosen() = songs.filter { it.id in selected }

    Column(Modifier.fillMaxSize()) {
        androidx.compose.animation.AnimatedVisibility(visible = inSelection) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selected.clear() }) { Icon(Icons.Default.Close, contentDescription = tr("Cerrar")) }
                Text(
                    "${selected.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { selected.clear(); selected.addAll(songs.map { it.id }) }) {
                    Icon(Icons.Default.SelectAll, contentDescription = tr("Seleccionar todo"))
                }
                Box {
                    var menu by remember { mutableStateOf(false) }
                    IconButton(onClick = { menu = true }) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = tr("Añadir a una lista")) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(tr("Añadir a la cola")) },
                            onClick = { menu = false; onAddToQueue(chosen()); selected.clear() }
                        )
                        DropdownMenuItem(
                            text = { Text(tr("Nueva lista")) },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = { menu = false; onNewPlaylist() }
                        )
                        playlists.forEach { pl ->
                            DropdownMenuItem(
                                text = { Text(pl.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { menu = false; onAddToPlaylist(pl.id, chosen()); selected.clear() }
                            )
                        }
                    }
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Default.Delete, contentDescription = tr("Eliminar"), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(items = songs, key = { it.id }) { song ->
                val isCurrent = song.id == currentSongId
                SongItem(
                    song = song,
                    isSelected = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    checked = if (inSelection) song.id in selected else null,
                    onClick = {
                        if (inSelection) { if (!selected.remove(song.id)) selected.add(song.id) } else onSongClick(song)
                    },
                    onLongClick = {
                        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        if (!selected.remove(song.id)) selected.add(song.id)
                    },
                    onEditClick = { onEditSong(song) }
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("¿Eliminar del dispositivo?")) },
            text = { Text("${selected.size}") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDeleteSongs(chosen()); selected.clear() }) {
                    Text(tr("Eliminar"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Cancelar")) } }
        )
    }
}

fun formatDuration(durationMs: Long): String{
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (seconds < 10) "$minutes:0$seconds" else "$minutes:$seconds"
}

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun SongItem(
    song: Song,
    isSelected: Boolean = false,
    isPlaying: Boolean = false,
    /** `null` fuera del modo selección; si no, si esta fila está marcada. */
    checked: Boolean? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val backgroundColor = if (checked == true) {
        MaterialTheme.colorScheme.secondaryContainer
    } else if (isSelected){
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    }else{
        MaterialTheme.colorScheme.background
    }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (checked != null) {
                Checkbox(checked = checked, onCheckedChange = { onClick() })
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.coverModel,
                        contentDescription = tr("Portada"),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                // Calidad (Lossless / Hi-Res) en la base de la portada: el título conserva todo el ancho de la fila.
                CoverQualityStrip(song.quality, Modifier.align(Alignment.BottomCenter))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${displayArtist(song.artist)} • ${formatDuration(song.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected){
                PlayingEqualizerIcon(
                    color = MaterialTheme.colorScheme.primary,
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            if (checked == null) IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = tr("Opciones"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

}

/** "<unknown>" de MediaStore o vacío → "Artista desconocido". */
fun displayArtist(artist: String): String =
    if (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)) "Artista desconocido" else artist
