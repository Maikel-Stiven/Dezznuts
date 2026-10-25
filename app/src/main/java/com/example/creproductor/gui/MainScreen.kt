package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

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
import androidx.compose.ui.unit.sp
import com.example.creproductor.data.Playlist
import com.example.creproductor.data.Song
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.mutableStateListOf

/** Pantalla principal (pestañas + menú hamburguesa, como en la maqueta) o la búsqueda que se abre desde la lupa. */
enum class AppSection { HOME, SEARCH, SETTINGS }

/** Pestañas de la pantalla principal, en el orden de la maqueta. */
enum class AppTab(val title: String){
    QUICK("Selección rápida"),
    SONGS("Canciones"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    GENRES("Géneros"),
    PLAYLISTS("Listas de reproducción"),
    FOLDERS("Carpetas"),
    YT("YT Music")
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
    versionName: String,
    onAction: (AppAction) -> Unit,
    onExportPlaylist: (Playlist) -> Unit = {},
    onEditSong: (Song) -> Unit = {},
    onDeleteSongs: (List<Song>) -> Unit = {},
    /** Sube cada vez que se toca la barra inferior: cierra los detalles abiertos (álbum, artista…) para llegar a esa pantalla. */
    navReset: Int = 0,
    pagerState: androidx.compose.foundation.pager.PagerState = rememberPagerState(initialPage = 0, pageCount = { AppTab.entries.size })
){
    val coroutineScope = rememberCoroutineScope ()
    val navStyle = rememberNavStyle()

    val stack = remember { mutableStateListOf<Detail>() }
    fun open(detail: Detail) { stack.add(detail) }
    fun back() { stack.removeLastOrNull() }
    // Cambiar de sección desde la barra inferior cierra los detalles abiertos.
    LaunchedEffect(section, navReset) { stack.clear() }

    var showCreatedPlaylistDialog by remember { mutableStateOf(false) }
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

    // Atrás en la búsqueda vuelve a la pantalla principal, y en una pestaña distinta de la primera vuelve a "Selección rápida".
    BackHandler(enabled = stack.isEmpty() && section != AppSection.HOME) { onSectionChange(AppSection.HOME) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    BackHandler(enabled = stack.isEmpty() && section == AppSection.HOME && drawerState.isOpen) { coroutineScope.launch { drawerState.close() } }
    BackHandler(enabled = stack.isEmpty() && section == AppSection.HOME && !drawerState.isOpen && pagerState.currentPage != 0) {
        coroutineScope.launch { pagerState.animateScrollToPage(0) }
    }

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
                onAddSongsClick = { playlistForAddSongs = activePlaylist },
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
                ytEnabled = com.example.creproductor.data.YtMusic.isEnabled(androidx.compose.ui.platform.LocalContext.current),
                onYtPlay = { viewModel.playYt(it) },
                onCloseSearch = {
                    viewModel.setSearchQuery("")
                    onSectionChange(AppSection.HOME)
                }
            )
            AppSection.SETTINGS -> OptionsScreen(versionName = versionName, onAction = onAction, onClose = { onSectionChange(AppSection.HOME) })
            AppSection.HOME -> ModalNavigationDrawer(
                drawerState = drawerState,
                // El deslizamiento lateral es de las pestañas; el menú se abre con la hamburguesa y se cierra deslizando.
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    AppDrawerContent(versionName, onAction, onCloseDrawer = { coroutineScope.launch { drawerState.close() } })
                }
            ) {
            Scaffold(
                topBar = {
                    var showOverflow by remember { mutableStateOf(false) }
                    // Con la barra inferior todo vive en ella y en Ajustes: sin hamburguesa, lupa ni ⋮ arriba.
                    val topNav = navStyle == NavStyle.TOP
                    // YT Music y Biblioteca ya traen su propia cabecera/pestañas: el logotipo solo va en Inicio.
                    if (topNav || AppTab.entries[pagerState.currentPage] == AppTab.QUICK) TopAppBar(
                        navigationIcon = {
                            if (topNav) IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = tr("Abrir menú"))
                            }
                        },
                        title = {
                            // Logotipo en tipografía, no un título más: espaciado amplio y el color de la marca.
                            Text(
                                tr("DEZZNUTS"), style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = MaterialTheme.colorScheme.primary
                            )
                        },
                        actions = { if (topNav) {
                            IconButton(onClick = { onSectionChange(AppSection.SEARCH) }) {
                                Icon(Icons.Default.Search, contentDescription = tr("Buscar"))
                            }
                            Box {
                                IconButton(onClick = { showOverflow = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = tr("Más opciones"))
                                }
                                DropdownMenu(expanded = showOverflow, onDismissRequest = { showOverflow = false }) {
                                    listOf(AppAction.ScanFiles, AppAction.SleepTimer, AppAction.Options, AppAction.About).forEach { action ->
                                        DropdownMenuItem(text = { Text(tr(action.title)) }, onClick = { showOverflow = false; onAction(action) })
                                    }
                                }
                            }
                        } },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                },
                contentWindowInsets = WindowInsets(0)
            ) { innerPadding ->
                val topBarShown = navStyle == NavStyle.TOP || AppTab.entries[pagerState.currentPage] == AppTab.QUICK
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .then(if (topBarShown) Modifier else Modifier.statusBarsPadding())
                ) {
                    // Con la barra inferior, las pestañas de arriba son solo las de la biblioteca (y solo estando en ella).
                    val bottomNav = navStyle == NavStyle.BOTTOM
                    val tabs = if (bottomNav) LIBRARY_TABS else AppTab.entries
                    val currentTab = AppTab.entries[pagerState.currentPage]
                    if (!bottomNav || currentTab in LIBRARY_TABS) PrimaryScrollableTabRow(
                        selectedTabIndex = tabs.indexOf(currentTab).coerceAtLeast(0),
                        edgePadding = 16.dp,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        tabs.forEach { tab ->
                            Tab(
                                selected = currentTab == tab,
                                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(AppTab.entries.indexOf(tab)) } },
                                selectedContentColor = MaterialTheme.colorScheme.onSurface,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = { Text(tr(tab.title), style = MaterialTheme.typography.titleSmall) }
                            )
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { page ->
                         when (AppTab.entries[page]){
                             AppTab.QUICK -> QuickSelectionScreen(
                                 viewModel = viewModel,
                                 onSongClick = { song, queue -> viewModel.playSong(song, customQueue = queue)},
                                 onSeeAllCategoryClick = {type, categorySongs -> open(Detail.Category(type.title, categorySongs)) },
                                 onMoodClick = { mood, moodSongs -> open(Detail.Category(mood.label, moodSongs)) }
                             )
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
                                                 label = { Text(tr(label)) }
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
                                                     text = { Text(tr(sort.label)) },
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
                                     playlists = playlist,
                                     onSongClick = {song -> viewModel.playSong(song, customQueue = filteredSongs) },
                                     onEditSong = onEditSong,
                                     onAddToPlaylist = { id, list -> viewModel.addSongsToPlaylist(id, list.map { it.id }) },
                                     onAddToQueue = { list -> list.forEach(viewModel::addToQueue) },
                                     onNewPlaylist = { showCreatedPlaylistDialog = true },
                                     onDeleteSongs = onDeleteSongs
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
                             AppTab.YT -> YtMusicScreen(onPlay = { viewModel.playYt(it) })
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
