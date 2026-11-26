package com.example.creproductor.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.YtPlaylist
import com.example.creproductor.data.YtTrack
import com.example.creproductor.i18n.tr

@Composable
fun YtAddToPlaylistDialog(
    playlists: List <YtPlaylist>,
    track: YtTrack,
    onDismiss: () -> Unit,
    onSelectedPlaylist: (YtPlaylist) -> Unit,
    onCreateNewClick: () -> Unit
){
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Agregar a playlist de YT")) },
        text = {
            Column {
                TextButton(
                    onClick = onCreateNewClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+${tr("Crear nueva playlist")}")
                }

                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(playlists, key = {it.id}) {playlist ->
                        ListItem(
                            headlineContent = {Text(playlist.name)},
                            supportingContent = {Text("${playlist.tracks.size} canciones")},
                            modifier = Modifier.clickable{onSelectedPlaylist(playlist)}
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancelar"))}
        }
    )
}