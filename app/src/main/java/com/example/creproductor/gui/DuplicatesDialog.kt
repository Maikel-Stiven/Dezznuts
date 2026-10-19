package com.example.creproductor.gui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.AudioQuality
import com.example.creproductor.data.DuplicateFinder
import com.example.creproductor.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Duplicados (mismo título/artista/duración en distintos archivos) y audios de WhatsApp.
 *
 * De cada grupo se propone **conservar la mejor calidad** y eliminar el resto. Borrar pide confirmación al
 * sistema (`MediaStore.createDeleteRequest`, Android 11+); en versiones anteriores solo se informa.
 * La búsqueda corre fuera del hilo principal para no congelar la interfaz con bibliotecas grandes.
 */
@Composable
fun DuplicatesDialog(songs: List<Song>, onDelete: (List<Song>) -> Unit, onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val groups by produceState<List<DuplicateFinder.Group<Song>>?>(null, songs) {
        value = withContext(Dispatchers.Default) { DuplicateFinder.findSongs(songs) }
    }
    val whatsApp by produceState<List<Song>?>(null, songs) {
        value = withContext(Dispatchers.Default) { DuplicateFinder.whatsAppAudios(songs) }
    }
    val canDelete = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FindInPage, contentDescription = null) },
        title = { Text("Duplicados y WhatsApp") },
        text = {
            Column {
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Duplicados") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("WhatsApp") })
                }
                val list = if (tab == 0) groups?.flatMap { g -> listOf(g.keep to true) + g.copies.map { it to false } } else whatsApp?.map { it to false }
                when {
                    list == null -> Text("Buscando…", Modifier.padding(16.dp))
                    list.isEmpty() -> Text(if (tab == 0) "No hay canciones repetidas." else "No hay audios de WhatsApp.", Modifier.padding(16.dp))
                    else -> LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(list, key = { it.first.id }) { (song, keep) ->
                            Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (tab == 0) Text(if (keep) "Conservar" else "Copia", style = MaterialTheme.typography.labelSmall,
                                        color = if (keep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                    Text(song.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text(
                                    listOf(song.artist, AudioQuality.label(song.mimeType, song.sampleRate, song.bitsPerSample, song.bitrate)).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                Text(song.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                if (!canDelete) Text("En Android 10 o anterior, borra los archivos desde el gestor de archivos.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            val toDelete = if (tab == 0) groups?.flatMap { it.copies }.orEmpty() else whatsApp.orEmpty()
            if (canDelete && toDelete.isNotEmpty()) {
                Button(onClick = { onDelete(toDelete) }) {
                    Text(if (tab == 0) "Eliminar ${toDelete.size} ${if (toDelete.size == 1) "copia" else "copias"}" else "Eliminar ${toDelete.size} ${if (toDelete.size == 1) "audio" else "audios"}")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
