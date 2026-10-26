package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import kotlinx.coroutines.delay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.activity.compose.BackHandler
import com.example.creproductor.data.coverModel
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.Song
import com.example.creproductor.data.SongOrganizer

enum class SearchSubTab(val title: String){
    ALL("Todas"),
    SONGS("Canciones"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    GENRES("Géneros"),
    PLAYLISTS("Listas de reproducción"),
    FOLDERS("Carpetas")
}
private class SearchMatches(
    val songs: List<Song> = emptyList(),
    val albums: List<Song> = emptyList(),
    val artists: List<Song> = emptyList()
)

@Composable
fun SearchOverlayScreen(
    onEditSong: (Song) -> Unit = {},
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    searchHistory: List<String>,
    onHistoryItemClick: (String) -> Unit,
    onDeleteHistoryItem: (String) -> Unit,
    onAddSearchHistory: (String) -> Unit,
    allSongs: List<Song>,
    currentSongId: Long? = null,
    isPlaying: Boolean = false,
    onSongClick: (Song, List<Song>) -> Unit,
    onCloseSearch: () -> Unit,
    onAlbumClick: (albumName: String, albumSongs: List<Song>) -> Unit = { _, _ -> },
    onArtistClick: (artistName: String, artistSongs: List<Song>) -> Unit = { _, _ -> },
    /** Pestaña extra "YouTube Music" (opcional, ver [com.example.creproductor.data.YtMusic]). */
    ytEnabled: Boolean = false,
    onYtPlay: (com.example.creproductor.data.YtTrack) -> Unit = {}
){

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    /**
     * Cierra el teclado y suelta el foco. Ocultar el IME no basta: si el campo sigue enfocado,
     * Android lo vuelve a mostrar al regresar a la app, aunque ya estés en otra pantalla.
     */
    fun dismissKeyboard() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    var selectedTabItem by remember { mutableIntStateOf(0) }

    var songsLimit by remember(searchQuery) { mutableIntStateOf(10) }

    BackHandler {
        if (searchQuery.isNotEmpty()){
            onQueryChange("")
        }else{
            onCloseSearch()
        }
    }
    // Las tres búsquedas recorren toda la biblioteca en cada tecla: se hacen fuera del hilo principal.
    val matches by produceState(SearchMatches(), searchQuery, allSongs) {
        value = if (searchQuery.isBlank()) SearchMatches() else withContext(Dispatchers.Default) {
            SearchMatches(
                songs = allSongs.filter { song ->
                    song.title.contains(searchQuery, ignoreCase = true) ||
                    song.artist.contains(searchQuery, ignoreCase = true) ||
                    song.album.contains(searchQuery, ignoreCase = true)
                }.sortedWith(
                    compareBy <Song> { it.album }
                        .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                        .thenBy { it.title }
                ),
                albums = allSongs.filter { song ->
                    song.album.isNotBlank() &&
                            !song.album.equals("<unknown>", ignoreCase = true) &&
                            (song.album.contains(searchQuery, ignoreCase = true) ||
                                    song.artist.contains(searchQuery, ignoreCase = true))
                }.distinctBy { it.album.lowercase().trim() },
                artists = allSongs.filter { song ->
                    song.artist.isNotBlank() &&
                            !song.artist.equals("<unknown>", ignoreCase = true) &&
                            song.artist.contains(searchQuery, ignoreCase = true)
                }.distinctBy { it.artist.lowercase().trim() }
            )
        }
    }
    val matchingSongs = matches.songs
    val matchingAlbums = matches.albums
    val matchingArtists = matches.artists

    // La búsqueda se dibuja por encima del Scaffold, así que necesita su propia
    // zona segura para no quedar bajo la barra de estado (notch / reloj).
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Al entrar en Buscar, el cursor ya está en el campo y sale el teclado.
        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCloseSearch) {
                    Icon(Icons.Default.Close, contentDescription = tr("cerrar búsqueda"))
                }

                TextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text(tr("Canciones, álbumes o artistas")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()){
                                onAddSearchHistory(searchQuery)
                            }
                            dismissKeyboard()
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f).focusRequester(focusRequester)
                )

                if (searchQuery.isNotEmpty()){
                    TextButton(onClick = {onQueryChange("")}) {
                        Text(
                            text = tr("Limpiar"),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        if (searchQuery.isBlank() && searchHistory.isEmpty()) {
            Text(
                text = tr("Busca en toda tu biblioteca por título, artista o álbum."),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp)
            )
        } else if (searchQuery.isBlank()){
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                items(searchHistory, key = { it }){ item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable{
                                onHistoryItemClick(item)
                                dismissKeyboard()
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
                                contentDescription = tr("Eliminar del historial"),
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
                       selectedContentColor = MaterialTheme.colorScheme.onSurface,
                       unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                       text = {Text(tr(tab.title))}
                   )
               }
               if (ytEnabled) {
                   Tab(
                       selected = selectedTabItem == SearchSubTab.entries.size,
                       onClick = { selectedTabItem = SearchSubTab.entries.size },
                       selectedContentColor = MaterialTheme.colorScheme.onSurface,
                       unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                       text = { Text(tr("YouTube Music")) }
                   )
               }
           }

            val onYtTab = ytEnabled && selectedTabItem == SearchSubTab.entries.size
            val currentSubTab = SearchSubTab.entries[selectedTabItem.coerceAtMost(SearchSubTab.entries.lastIndex)]

            // Resultados de YouTube Music debajo de los locales: llegan solos tras una pausa al escribir y no mueven nada
            // de lo de arriba mientras cargan. Solo con YouTube Music activado.
            val ytInline by produceState<List<com.example.creproductor.data.YtTrack>?>(null, searchQuery, ytEnabled) {
                value = null
                if (!ytEnabled || searchQuery.trim().length < 2) return@produceState
                delay(700)
                value = withContext(Dispatchers.IO) {
                    runCatching { com.example.creproductor.data.YtMusic.search(searchQuery).take(5) }.getOrDefault(emptyList())
                }
            }
            if (onYtTab) {
                YtMusicResults(searchQuery) { dismissKeyboard(); onYtPlay(it) }
            } else LazyColumn(
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
                        val isSelected = song.id == currentSongId

                        SongItem(
                            song = song,
                            isSelected = isSelected,
                            isPlaying = isPlaying && isSelected,
                            onClick = {
                                if (searchQuery.isNotBlank()){
                                    onAddSearchHistory(searchQuery)
                                }
                                dismissKeyboard()
                                onSongClick(song, matchingSongs)
                            },
                            onEditClick = { onEditSong(song) }
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
                                        text = tr("Ver más"),
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
                                        if (searchQuery.isNotBlank()){
                                            onAddSearchHistory(searchQuery)
                                        }

                                        val sortedAlbumSongs = SongOrganizer.getAlbumSongsSorted(
                                            allSongs.filter { it.album.equals(albumSong.album, ignoreCase = true)}
                                        )
                                        onAlbumClick(albumSong.album, sortedAlbumSongs)
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
                        val (artistAlbumsCount, artistSongsCounts) = remember(allSongs, song.artist) {
                            val artistSongs = allSongs.filter { it.artist.contains(song.artist, ignoreCase = true) }
                            artistSongs.map { it.album.lowercase().trim() }
                                .filter { it.isNotBlank() && it != "<unknown>" }
                                .distinct()
                                .size to artistSongs.size
                        }

                        SearchCategoryItem(
                            title = song.artist,
                            subtitle = "$artistAlbumsCount álbumes •  $artistSongsCounts canciones",
                            // Foto real del artista (Deezer) cacheada desde aquí; si no se
                            // consigue, se cae a la portada de una de sus canciones.
                            imageModel = rememberArtistPhoto(song.artist, song.coverModel),
                            onClick = {

                                if (searchQuery.isNotBlank()){
                                    onAddSearchHistory(searchQuery)
                                }
                                val sortedArtistSongs = SongOrganizer.getArtisSorted(
                                    allSongs.filter { it.artist.equals(song.artist, ignoreCase = true) }
                                )
                                onArtistClick(song.artist, sortedArtistSongs)
                            }
                        )
                    }
                }
                // Sin red o sin resultados no se muestra nada (ni un indicador que gire para siempre).
                if (ytEnabled && searchQuery.trim().length >= 2 && (ytInline == null || ytInline?.isNotEmpty() == true) &&
                    (currentSubTab == SearchSubTab.ALL || currentSubTab == SearchSubTab.SONGS)) {
                    item(key = "yt_header") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                tr("También en YouTube Music"), style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
                            )
                            if (ytInline == null) CircularProgressIndicator(Modifier.size(18.dp).padding(end = 2.dp), strokeWidth = 2.dp)
                            else TextButton(onClick = { selectedTabItem = SearchSubTab.entries.size }) { Text(tr("Ver todo")) }
                        }
                    }
                    items(ytInline.orEmpty(), key = { "yt_${it.url}" }) { t ->
                        ListItem(
                            headlineContent = { Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text("${t.artist} · %d:%02d".format(t.durationSec / 60, t.durationSec % 60), maxLines = 1) },
                            leadingContent = {
                                AsyncImage(
                                    model = t.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                if (searchQuery.isNotBlank()) onAddSearchHistory(searchQuery)
                                dismissKeyboard(); onYtPlay(t)
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
                    model = song.coverModel,
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
        val shape = if (isCircle) CircleShape else MaterialTheme.shapes.small

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

/** Resultados de YouTube Music para [query]. Espera a que dejes de escribir antes de preguntar a la red. */
@Composable
internal fun YtMusicResults(query: String, onPlay: (com.example.creproductor.data.YtTrack) -> Unit) {
    // null = buscando, Result.failure = sin red o YouTube no respondió.
    val result by produceState<Result<List<com.example.creproductor.data.YtTrack>>?>(null, query) {
        value = null
        delay(600)
        value = withContext(Dispatchers.IO) { runCatching { com.example.creproductor.data.YtMusic.search(query) } }
        // El primer resultado es el que casi siempre se toca: se abre ya (queda en caché) y de paso se calienta
        // lo más lento de YouTube, que la primera vez tarda ~3 s más que las siguientes.
        value?.getOrNull()?.firstOrNull()?.let { withContext(Dispatchers.IO) { runCatching { com.example.creproductor.data.YtMusic.resolve(it.url) } } }
    }
    val list = result?.getOrNull()
    when {
        result == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        list == null -> Text(
            tr("No se pudo consultar YouTube Music. Revisa la conexión o inténtalo más tarde."),
            modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        list.isEmpty() -> Text(tr("Sin resultados."), modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(list, key = { it.url }) { t ->
                ListItem(
                    headlineContent = { Text(t.title, maxLines = 1) },
                    supportingContent = { Text("${t.artist} · %d:%02d".format(t.durationSec / 60, t.durationSec % 60), maxLines = 1) },
                    leadingContent = {
                        AsyncImage(
                            model = t.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small)
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onPlay(t) }
                )
            }
        }
    }
}
