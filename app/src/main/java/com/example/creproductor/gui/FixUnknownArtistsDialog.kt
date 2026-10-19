package com.example.creproductor.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.Song
import com.example.creproductor.data.TagSuggestion
import com.example.creproductor.data.TagSuggestions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Canciones con artista desconocido para las que el nombre del archivo da una sugerencia ("Artista - Título").
 * Se revisan y se marcan antes de aplicar; las que no tienen sugerencia se corrigen una a una desde su menú (⋮).
 * La búsqueda corre fuera del hilo principal para no congelar la interfaz con bibliotecas grandes.
 */
@Composable
fun FixUnknownArtistsDialog(
    songs: List<Song>,
    onApply: (List<Pair<Song, TagSuggestion>>) -> Unit,
    onDismiss: () -> Unit
) {
    val result by produceState<Pair<List<Pair<Song, TagSuggestion>>, Int>?>(null, songs) {
        value = withContext(Dispatchers.Default) {
            val unknown = songs.filter { it.cueStartMs < 0 && TagSuggestions.isUnknown(it.artist) }
            val withSuggestion = unknown.mapNotNull { song ->
                TagSuggestions.suggest(song.title, song.artist, java.io.File(song.path).name)?.let { song to it }
            }
            withSuggestion to (unknown.size - withSuggestion.size)
        }
    }
    val checked = remember { mutableStateMapOf<Long, Boolean>() }
    val (items, withoutSuggestion) = result ?: (emptyList<Pair<Song, TagSuggestion>>() to 0)
    val selected = items.filter { checked[it.first.id] != false }   // todas marcadas por defecto

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
        title = { Text("Corregir artistas desconocidos") },
        text = {
            Column {
                when {
                    result == null -> Text("Buscando…")
                    items.isEmpty() -> Text("No hay canciones con artista desconocido que se puedan deducir del nombre del archivo.")
                    else -> LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(items, key = { it.first.id }) { (song, s) ->
                            Row(
                                Modifier.fillMaxWidth().clickable { checked[song.id] = checked[song.id] == false }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(checked = checked[song.id] != false, onCheckedChange = { checked[song.id] = it })
                                Column(Modifier.weight(1f)) {
                                    Text(s.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(s.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(song.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                if (result != null && withoutSuggestion > 0) {
                    Text(
                        "$withoutSuggestion más no se pueden deducir; edítalas una a una desde su menú (⋮).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (selected.isNotEmpty()) Button(onClick = { onApply(selected) }) { Text("Corregir ${selected.size}") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}
