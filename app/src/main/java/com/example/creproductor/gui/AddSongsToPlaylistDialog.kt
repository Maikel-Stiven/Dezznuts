package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.compose.foundation.background
import com.example.creproductor.data.coverModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.Song

@OptIn(ExperimentalMaterial3Api :: class)
@Composable
fun AddSongsToPlaylistDialog(
    playlistName: String,
    allSongs: List<Song>,
    favoriteSongs: List<Song>,
    recentSongs: List<Song>,
    existingSongIds: List<Long>,
    onDismiss: () -> Unit,
    onAddSongs: (selectedSongIds: List<Long>) -> Unit
){
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val selectedSongIds = remember { mutableStateListOf<Long>() }

    val currentList = remember ( selectedTabIndex, searchQuery, favoriteSongs, recentSongs) {
        val baseList = when (selectedTabIndex){
            0 -> allSongs
            1 -> favoriteSongs
            2 -> recentSongs
            else -> allSongs
        }
        if (searchQuery.isBlank()){
            baseList
        }else{
            baseList.filter { song ->
                song.title.contains(searchQuery, ignoreCase = true) ||
                song.artist.contains(searchQuery, ignoreCase = true) ||
                song.album.contains(searchQuery, ignoreCase = true)
            }
        }
    }


    val sheetState = rememberModalBottomSheetState (skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr("Cerrar"),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Añadir a: $playlistName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (selectedSongIds.isNotEmpty()){
                    Button(
                        onClick = {
                            onAddSongs(selectedSongIds.toList())
                            onDismiss()
                        }
                    ) {
                        Text("Añadir (${selectedSongIds.size})")
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {searchQuery = it},
                placeholder = {Text(tr("Buscar"), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))},
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = tr("Buscar"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()){
                        IconButton(onClick = {searchQuery = ""}) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = tr("Limpiar busqueda"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = {selectedTabIndex = 0},
                    text = {Text(tr("Todas"))}
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = {selectedTabIndex = 1},
                    text = {Text(tr("Favoritos"))}
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = {selectedTabIndex = 2},
                    text = {Text(tr("Recientes"))}
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(currentList, key = {it.id}){song ->
                    val isAlreadyInPlaylist = existingSongIds.contains(song.id)
                    val isSelectedToAdd = selectedSongIds.contains(song.id)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAlreadyInPlaylist){
                                if (isSelectedToAdd){
                                    selectedSongIds.remove(song.id)
                                }else{
                                    selectedSongIds.add(song.id)
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ){
                            if (song.albumArtUri != null){
                                AsyncImage(
                                    model = song.coverModel,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }else{
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (isAlreadyInPlaylist) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text =  "${song.artist} • ${formatDuration(song.duration)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = {
                                if (!isAlreadyInPlaylist){
                                    if (isSelectedToAdd) selectedSongIds.remove(song.id) else selectedSongIds.add(song.id)
                                }
                            },
                            enabled = !isAlreadyInPlaylist
                        ) {
                            Icon(
                                imageVector = when{
                                    isAlreadyInPlaylist -> Icons.Default.Check
                                    isSelectedToAdd -> Icons.Default.Check
                                    else -> Icons.Default.Add
                                },
                                contentDescription = null,
                                tint = when {
                                    isAlreadyInPlaylist -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    isSelectedToAdd -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}