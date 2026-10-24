package com.example.creproductor.gui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.creproductor.SongListScreen
import com.example.creproductor.data.Playlist
import com.example.creproductor.data.Song
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.mutableStateListOf

/** Secciones de la barra inferior. */
enum class AppSection(val title: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Default.Home),
    LIBRARY("Biblioteca", Icons.Default.LibraryMusic),
    SEARCH("Buscar", Icons.Default.Search),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

/** Pestañas de la Biblioteca. */
enum class AppTab(val title: String){
    SONGS("Canciones"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    PLAYLISTS("Listas"),
    GENRES("Géneros"),
    FOLDERS("Carpetas")
}

/** Pantallas de detalle apiladas: "atrás" vuelve a la anterior (Artista → Álbum → atrás = Artista). */
private sealed interface Detail {
    data class Album(val name: String, val songs: List<Song>) : Detail
    data class Artist(val name: String, val songs: List<Song>) : Detail
    data class Genre(val name: String, val songs: List<Song>) : Detail
    data class Category(val title: String, val songs: List<Song>) : Detail
    data class PlaylistDetail(val playlist: Playlist) : Detail
}

@OptIn(ExperimentalMaterial3Api :: class)
@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    section: AppSection,
    onSectionChange: (AppSection) -> Unit,
    settingsContent: @Composable () -> Unit,
    onExportPlaylist: (Playlist) -> Unit = {},
    onEditSong: (Song) -> Unit = {}
){
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { AppTab.entries.size})
    val coroutineScope = rememberCoroutineScope ()

    val stack = remember { mutableStateListOf<Detail>() }
    fun open(detail: Detail) { stack.add(detail) }
    fun back() { stack.removeLastOrNull() }
    // Cambiar de sección desde la barra inferior cierra los detalles abiertos.
    LaunchedEffect(section) { stack.clear() }

    var showCreatedPlaylistDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var playlistForOptions by remember { mutableStateOf<Playlist?>(null) }

    val playlist by viewModel.playlists.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val recentSongs by viewModel.recentPlayedSongs.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val currentSort by viewModel.currentSort.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val songs = (uiState as? UiState.Success)?.songs ?: emptyList()
    val  searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    var playlistToEdit by remember { mutableStateOf<Playlist?>(null) }
    var playlistForAddSongs by remember { mutableStateOf<Playlist?>(null) }

    fun albumSongs(name: String) = songs.filter { it.album == name }.sortedWith(
        compareBy<Song> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }.thenBy { it.title }
    )

    // Menú de una canción → "Ir al álbum" / "Ir al artista": se abre la ficha desde cualquier pantalla.
    val navRequest by viewModel.navRequest.collectAsStateWithLifecycle()
    LaunchedEffect(navRequest) {
        val request = navRequest ?: return@LaunchedEffect
        when (request) {
            is NavRequest.Album -> open(Detail.Album(request.name, albumSongs(request.name)))
            is NavRequest.Artist -> open(Detail.Artist(request.name, songs.filter { it.artist == request.name }))
        }
        viewModel.clearNav()
    }

    val artisListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val genresGridstate = rememberLazyGridState()

    fun playAll(list: List<Song>) { if (list.isNotEmpty()) viewModel.playSong(list.first(), customQueue = list) }
    fun shuffleAll(list: List<Song>) {
        if (list.isNotEmpty()) { val shuffled = list.shuffled(); viewModel.playSong(shuffled.first(), customQueue = shuffled) }
    }

    // Atrás en la raíz de Biblioteca o Ajustes vuelve a Inicio en vez de salir de la app.
    BackHandler(enabled = stack.isEmpty() && section != AppSection.HOME) { onSectionChange(AppSection.HOME) }

    when (val detail = stack.lastOrNull()) {
        is Detail.Category -> QuickSelectDetailScreen(
            categoryTitle = detail.title,
            categorySongs = detail.songs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onEditSongClick = onEditSong,
            onSongClick = { song, queue -> viewModel.playSong(song, customQueue = queue) },
            onPlayAllClick = { playAll(detail.songs) },
            onShuffleClick = { shuffleAll(detail.songs) },
            onBackClick = ::back
        )
        is Detail.Album -> AlbumDetailScreen(
            albumName = detail.name,
            albumSongs = detail.songs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onEditSongClick = onEditSong,
            onSongClick = {song, queue -> viewModel.playSong(song, customQueue = queue)},
            onPlayAllClick = { playAll(detail.songs) },
            onShuffleClick = { shuffleAll(detail.songs) },
            onBackClick = ::back
        )
        is Detail.Artist -> ArtistDetailScreen(
            artistName = detail.name,
            artistSongs = detail.songs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onEditSongClick = onEditSong,
            onSongClick = {song -> viewModel.playSong(song, customQueue = detail.songs)},
            onAlbumClick = {albumName, albumSongs -> open(Detail.Album(albumName, albumSongs)) },
            onPlayAllClick = { playAll(detail.songs) },
            onShuffleClick = { shuffleAll(detail.songs) },
            onBackClick = ::back
        )
        is Detail.Genre -> GenreDetail(
            genreName = detail.name,
            genreSongs = detail.songs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onEditSongClick = onEditSong,
            onSongClick = {song -> viewModel.playSong(song, customQueue = detail.songs) },
            onPlayAllCLick = { playAll(detail.songs) },
            onShuffleClick = { shuffleAll(detail.songs) },
            onBackClick = ::back
        )
        is Detail.PlaylistDetail -> {
            val activePlaylist = playlist.find { it.id == detail.playlist.id } ?: detail.playlist
            val playlistSongs = remember(songs, activePlaylist.songIds) {
                val songMap = songs.associateBy { it.id }

                activePlaylist.songIds.mapNotNull { id -> songMap[id] }

            }
            PlaylistScreenDetail(
                playlist = activePlaylist,
                playlistSongs = playlistSongs,
                currentSongId = currentSong?.id,
                isPlaying = isPlaying,
                onSongClick = {song -> viewModel.playSong(song, customQueue =  playlistSongs)},
                onPlayAllClick = { playAll(playlistSongs) },
                onShuffleClick = { shuffleAll(playlistSongs) },
                onAddSongsClick = {showAddSongsDialog = true},
                onRestoreClick = { showRestoreDialog = true },
                recentSongs = recentSongs,
                onBackClick = ::back,
                onMoveSong = {fromIndex, toIndex ->
                    viewModel.movePlaylistSong(activePlaylist.id, fromIndex, toIndex)
                },
                onRemoveSongFromPlaylist = { song ->
                    viewModel.removeSongFromPlaylist(activePlaylist.id, song.id)
                },
                onPlayNext = {song -> viewModel.playNextInQueue(song)},
                onAddToQueue = {song -> viewModel.addToQueue(song)},
                onAddToPlaylist = {song, targetPlaylistId ->
                    viewModel.addSongToPlaylist(targetPlaylistId, song)
                },
                onGoToAlbum = {song ->
                    open(Detail.Album(song.album, albumSongs(song.album)))
                },
                onGoToArtist = {song ->
                    open(Detail.Artist(song.artist, songs.filter { it.artist == song.artist }))
                },
                onEditTags = {song -> onEditSong(song)},
                onDeleteSong = {song -> viewModel.deleteSongLegacy(song)}
            )
        }
        null -> when (section) {
            AppSection.HOME -> Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("DEZZNUTS", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                // La barra inferior ya reserva el espacio de la barra de gestos.
                contentWindowInsets = WindowInsets(0)
            ) { innerPadding ->
                Box(Modifier.padding(innerPadding)) {
                    QuickSelectionScreen(
                        viewModel = viewModel,
                        onSongClick = { song, queue -> viewModel.playSong(song, customQueue = queue)},
                        onSeeAllCategoryClick = {type, categorySongs -> open(Detail.Category(type.title, categorySongs)) },
                        onMoodClick = { mood, moodSongs -> open(Detail.Category(mood.label, moodSongs)) }
                    )
                }
            }
            AppSection.SEARCH -> SearchOverlayScreen(
                onEditSong = onEditSong,
                searchQuery = searchQuery,
                onQueryChange = {viewModel.setSearchQuery(it)},
                searchHistory = searchHistory,
                onHistoryItemClick = { selectedQuery -> viewModel.setSearchQuery(selectedQuery) },
                onDeleteHistoryItem = {item -> viewModel.removeFromHistory(item) },
                onAddSearchHistory = { query -> viewModel.addSearchHistory(query) },
                allSongs = songs,
                currentSongId = currentSong?.id,
                isPlaying = isPlaying,
                onSongClick = { song, searchQueue ->
                    viewModel.addSearchHistory(searchQuery)
                    viewModel.playSong(song, customQueue = searchQueue)
                },
                onAlbumClick = { albumName, albumSongs -> open(Detail.Album(albumName, albumSongs)) },
                onArtistClick = { artistName, artistSongs -> open(Detail.Artist(artistName, artistSongs)) },
                onCloseSearch = {
                    viewModel.setSearchQuery("")
                    onSectionChange(AppSection.HOME)
                }
            )
            AppSection.SETTINGS -> settingsContent()
            AppSection.LIBRARY -> Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Biblioteca", style = MaterialTheme.typography.headlineSmall) },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                // La barra inferior ya reserva el espacio de la barra de gestos.
                contentWindowInsets = WindowInsets(0)
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        AppTab.entries.forEachIndexed { index, tab ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                text = {Text(tab.title)}
                            )
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                         when (AppTab.entries[page]){
                             AppTab.SONGS -> Column(modifier = Modifier.fillMaxSize()) {
                                 Row(verticalAlignment = Alignment.CenterVertically) {
                                     Row(
                                         modifier = Modifier
                                             .weight(1f)
                                             .horizontalScroll(rememberScrollState())
                                             .padding(start = 12.dp),
                                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                                     ) {
                                         SONG_FILTER_CHIPS.forEach { (filter, label) ->
                                             FilterChip(
                                                 selected = currentFilter == filter,
                                                 onClick = { viewModel.setFilter(filter) },
                                                 label = { Text(label) }
                                             )
                                         }
                                     }
                                     Box {
                                         var showSortMenu by remember { mutableStateOf(false) }
                                         IconButton(onClick = { showSortMenu = true }) {
                                             Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Ordenar: ${currentSort.label}")
                                         }
                                         DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                             SongSort.entries.forEach { sort ->
                                                 DropdownMenuItem(
                                                     text = { Text(sort.label) },
                                                     trailingIcon = { if (sort == currentSort) Icon(Icons.Default.Check, contentDescription = null) },
                                                     onClick = { showSortMenu = false; viewModel.setSort(sort) }
                                                 )
                                             }
                                         }
                                     }
                                 }
                                 SongListScreen(
                                     songs = filteredSongs,
                                     currentSongId = currentSong?.id,
                                     isPlaying = isPlaying,
                                     onSongClick = {song -> viewModel.playSong(song, customQueue = filteredSongs) },
                                     onEditSong = onEditSong
                                 )
                             }
                             AppTab.ALBUMS -> AlbumsGridScreen(
                                 songs = songs,
                                 gridState = albumsGridState,
                                 onAlbumClick = {albumName, albumSongs -> open(Detail.Album(albumName, albumSongs)) }
                             )
                             AppTab.ARTISTS -> ArtistsListScreen(
                                 songs = songs,
                                 listState = artisListState,
                                 onArtistClick = {artistName, artistSongs -> open(Detail.Artist(artistName, artistSongs)) }
                             )
                             AppTab.GENRES -> GenresGridScreen(
                                 songs = songs,
                                 gridState = genresGridstate,
                                 onGenreClick = {genreName, genreSongs -> open(Detail.Genre(genreName, genreSongs)) }
                             )
                             AppTab.PLAYLISTS -> PlaylistScreen(
                                 playlist = playlist,
                                 allSongs = songs,
                                 onCreatePlaylistClick = { showCreatedPlaylistDialog = true},
                                 onRestorePlaylistClick = { showRestoreDialog = true },
                                 onPlaylistOptionsClick = { playlistForOptions = it },
                                 onPlaylistClick = { open(Detail.PlaylistDetail(it)) }
                             )
                             AppTab.FOLDERS -> FoldersListScreen(
                                 songs = songs,
                                 onFolderClick = {folderName, folderSongs ->
                                     open(Detail.Category(folderName.substringAfterLast('/').ifBlank { folderName }, folderSongs))
                                 }
                             )
                         }
                     }
                }
            }
        }
    }

    playlistForOptions?.let { activePlaylist ->
        PlaylistMenuSheet(
            playlist = activePlaylist,
            allSongs = songs,
            allPlaylist = playlist,
            actions = PlaylistMenuActions(
                onPlay = {
                    playlistForOptions = null
                    val songMap = songs.associateBy { it.id }
                    val playlistSongs = activePlaylist.songIds.mapNotNull { songMap[it] }
                    if (playlistSongs.isNotEmpty()) {
                        viewModel.playSong(playlistSongs.first(), customQueue = playlistSongs)
                    }
                },
                onAddSongs = {
                    playlistForOptions = null
                    playlistForAddSongs = activePlaylist
                },
                onAddToQueue = {
                    playlistForOptions = null
                    viewModel.addPlaylistToQueue(activePlaylist)
                },
                onAddToPlaylist = { targetId ->
                    playlistForOptions = null
                    viewModel.appendPlaylistToPlaylist(activePlaylist, targetId)
                },
                onSaveToDevice = {
                    playlistForOptions = null
                    onExportPlaylist(activePlaylist)
                },
                onEditTags = {
                    playlistForOptions = null
                    playlistToEdit = activePlaylist
                },
                onDeletePlaylist = {
                    playlistForOptions = null
                    viewModel.deletePlaylist(activePlaylist.id)
                },
                onRemoveDuplicates = {
                    playlistForOptions = null
                    viewModel.removeDuplicatesFromPlaylist(activePlaylist.id)
                },
                onDeleteFromDevice = {
                    playlistForOptions = null
                    viewModel.deletePlaylistSongsFromStorage(activePlaylist)
                }
            ),
            onDismiss = { playlistForOptions = null }
        )
    }

    playlistForAddSongs?.let { targetPlaylist ->
        AddSongsToPlaylistDialog(
            playlistName = targetPlaylist.name,
            allSongs = songs,
            favoriteSongs = favoriteSongs,
            recentSongs = recentSongs,
            existingSongIds = targetPlaylist.songIds,
            onDismiss = {playlistForAddSongs = null},
            onAddSongs = {selectedIds ->
                viewModel.addSongsToPlaylist(targetPlaylist.id, selectedIds)
                playlistForAddSongs = null
            }
        )
    }

    playlistToEdit?.let { targetPlaylist ->
        EditPlaylistDialog(
            playlist = targetPlaylist,
            onDismiss = {playlistToEdit = null},
            onSave = {newName, newCoverUri ->
                viewModel.updatedPlaylist(targetPlaylist.id, newName, newCoverUri)
                playlistToEdit = null
            }
        )
    }

    if (showRestoreDialog){
        val deleted by viewModel.deletedPlaylists.collectAsStateWithLifecycle()
        RestorePlaylistsDialog(deleted, onRestore = { viewModel.restorePlaylist(it) }, onDismiss = { showRestoreDialog = false })
    }

    if (showCreatedPlaylistDialog){
        CreationPlaylist(
            onDismiss =  {showCreatedPlaylistDialog = false},
            onCreate = {name, coverUri ->
                viewModel.createPlaylist(name, coverUri)
                showCreatedPlaylistDialog = false
            }
        )
    }
}

/** Chips de la pestaña Canciones (constante: no se reconstruye en cada recomposición). */
private val SONG_FILTER_CHIPS = listOf(
    SongFilter.ALL to "Todas",
    SongFilter.FAVORITES to "Favoritas",
    SongFilter.LOSSLESS to "Lossless",
    SongFilter.HIRES to "Hi-Res"
)
