package com.example.creproductor.gui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.SongItem
import com.example.creproductor.data.Song
enum class SearchSubTab(val title: String){
    ALL("Todas"),
    SONGS("Canciones"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    GENRES("Géneros"),
    PLAYLISTS("Listas de reproducción"),
    FOLDERS("Carpetas")
}
@Composable
fun SearchOverlayScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchHistory: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    allSongs: List<Song>,
    onSongClick: (Song) -> Unit,
    onCloseSearch: () -> Unit,
    onAlbumClick: (albumName: String, albumSongs: List<Song>) -> Unit = { _, _ -> },
    onArtistClick: (artistName: String, artistSongs: List<Song>) -> Unit = { _, _ -> }
){

    val keyboardController = LocalSoftwareKeyboardController.current
    var selectedTabItem by remember { mutableIntStateOf(0) }

    var songsLimit by remember(searchQuery) { mutableIntStateOf(10) }

    BackHandler {
        if (searchQuery.isNotEmpty()){
            onQueryChange("")
        }else{
            onCloseSearch()
        }
    }
    val matchingSongs = remember (searchQuery, allSongs) {
        if (searchQuery.isBlank()) emptyList()
        else allSongs.filter { song ->
            song.title.contains(searchQuery, ignoreCase = true) ||
            song.artist.contains(searchQuery, ignoreCase = true) ||
            song.album.contains(searchQuery, ignoreCase = true)
        }
    }

    val matchingAlbums = remember (searchQuery, allSongs){
        if (searchQuery.isBlank()) emptyList()
        else allSongs
            .filter { song ->
                song.album.isNotBlank() &&
                        !song.album.equals("<unknown>", ignoreCase = true) &&
                        (song.album.contains(searchQuery, ignoreCase = true) ||
                                song.artist.contains(searchQuery, ignoreCase = true))

        }.distinctBy { it.album.lowercase().trim() }
    }

    val  matchingArtists = remember (searchQuery, allSongs){
        if (searchQuery.isBlank()) emptyList()
        else allSongs.filter { song ->
            song.artist.isNotBlank() &&
                    !song.artist.equals("<unknown>", ignoreCase = true) &&
                    song.artist.contains(searchQuery, ignoreCase = true)
        }.distinctBy { it.artist.lowercase().trim() }
    }

    // La búsqueda se dibuja por encima del Scaffold, así que necesita su propia
    // zona segura para no quedar bajo la barra de estado (notch / reloj).
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Default.Close, contentDescription = "cerrar búsqueda")
                }

                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Buscar...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {keyboardController?.hide()}
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )

                if (searchQuery.isNotEmpty()){
                    TextButton(onClick = {onQueryChange("")}) {
                        Text(
                            text = "Limpiar",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }else{
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                }
            }
        }

        if (searchQuery.isBlank()){
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                items(searchHistory){ item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable{
                                onHistoryItemClick(item)
                                keyboardController?.hide()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {onDeleteHistoryItem(item)},
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Eliminar del historial",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }else{
           PrimaryScrollableTabRow(
               selectedTabIndex = selectedTabItem,
               edgePadding = 16.dp,
               containerColor = MaterialTheme.colorScheme.background,
               contentColor = MaterialTheme.colorScheme.primary
           ) {
               SearchSubTab.entries.forEachIndexed { index, tab ->
                   Tab(
                       selected = selectedTabItem == index,
                       onClick = {selectedTabItem = index},
                       text = {Text(tab.title)}
                   )
               }
           }

            val currentSubTab = SearchSubTab.entries[selectedTabItem]

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if ((currentSubTab == SearchSubTab.ALL || currentSubTab == SearchSubTab.SONGS) && matchingSongs.isNotEmpty()){

                    val visibleSongs = matchingSongs.take(songsLimit)

                    item {
                        Text(
                            text = "Canciones (${matchingSongs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    items(visibleSongs, key = { "song_${it.id}"}){ song ->
                        SongItem(
                            song = song,

                            onClick = {
                                keyboardController?.hide()
                                onSongClick(song)
                            },
                            onEditClick = {}
                        )
                    }
                    if (matchingSongs.size > visibleSongs.size){
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                        ) {
                                TextButton(
                                    onClick =  {songsLimit += 10}
                                ) {
                                    Text(
                                        text = "Ver más",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                if ((currentSubTab == SearchSubTab.ALL || currentSubTab == SearchSubTab.ALBUMS) && matchingAlbums.isNotEmpty()){
                    item {
                        Text(
                            text = "Álbumes (${matchingAlbums.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    val chunkedAlbums = matchingAlbums.chunked(3)
                    items(chunkedAlbums, key = {chunk -> "album_row_${chunk.first().album.lowercase().trim()}"}){ rowAlbums ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (albumSong in rowAlbums){
                                AlbumGridCard(
                                    song = albumSong,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        onAlbumClick(
                                            albumSong.album,
                                            allSongs.filter { it.album == albumSong.album }
                                        )
                                    }
                                )
                            }
                            repeat(3 - rowAlbums.size){
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                }
                if ((currentSubTab == SearchSubTab.ALL || currentSubTab == SearchSubTab.ARTISTS) && matchingArtists.isNotEmpty()){
                    item {
                        Text(
                            text = "Artistas (${matchingArtists.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    items(matchingArtists, key = {"artist_${it.artist.lowercase().trim()}"}) { song ->
                        val artistAlbumsCount = allSongs
                            .filter { it.artist.contains(song.artist, ignoreCase = true) }
                            .map { it.album.lowercase().trim() }
                            .filter { it.isNotBlank() && it != "<unknown>" }
                            .distinct()
                            .size
                        val artistSongsCounts = allSongs.count { it.artist.contains(song.artist, ignoreCase = true)}

                        SearchCategoryItem(
                            title = song.artist,
                            subtitle = "$artistAlbumsCount álbumes •  $artistSongsCounts canciones",
                            // Foto real del artista (Deezer) cacheada desde aquí; si no se
                            // consigue, se cae a la portada de una de sus canciones.
                            imageModel = rememberArtistPhoto(song.artist, song.albumArtUri),
                            onClick = {
                                onArtistClick(
                                    song.artist,
                                    allSongs.filter { it.artist.equals(song.artist, ignoreCase = true) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun  AlbumGridCard(
    song: Song,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
){
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ){
            if (song.albumArtUri != null){
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = song.album,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchCategoryItem(
    title: String,
    subtitle: String,
    imageModel: Any?,
    isCircle: Boolean = true,
    onClick: () -> Unit = {}
){
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val shape = if (isCircle) CircleShape else RoundedCornerShape(8.dp)

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ){
            if ( imageModel != null){
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }else{
                Icon(
                    imageVector =  if (isCircle) Icons.Default.Person else Icons.Default.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}