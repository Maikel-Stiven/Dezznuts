package com.example.creproductor.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.Playlist

/** Listas borradas recientemente: tocar una la devuelve (con sus canciones y su portada). */
@Composable
fun RestorePlaylistsDialog(deleted: List<Playlist>, onRestore: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Restaurar listas de reproducción") },
        text = {
            if (deleted.isEmpty()) {
                Text("No hay listas borradas para restaurar.")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    deleted.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { onRestore(p.id) }.padding(vertical = 10.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${p.songIds.size} canciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { onRestore(p.id) }) { Text("Restaurar") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

/** Opciones de una lista (⋮): hoy, eliminarla. Se puede deshacer desde "Restaurar listas de reproducción". */
@Composable
fun PlaylistOptionsDialog(playlist: Playlist, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(playlist.name) },
        text = { Text("¿Eliminar esta lista? Podrás recuperarla con \"Restaurar listas de reproducción\"; las canciones no se borran del dispositivo.") },
        confirmButton = { TextButton(onClick = { onDelete(); onDismiss() }) { Text("Eliminar lista") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
