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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.creproductor.SongListScreen
import com.example.creproductor.data.Playlist
import com.example.creproductor.data.Mood
import com.example.creproductor.data.Song
import kotlinx.coroutines.launch

enum class AppTab(val title: String){
    QUICK_SELECT("Selección rápida"),
    SONGS("Canciones"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    GENRES("Géneros"),
    PLAYLISTS("Listas de reproducción"),
    FOLDERS("Carpetas")
}

@OptIn(ExperimentalMaterial3Api :: class)
@Composable
fun MainScreen(
    viewModel: MusicViewModel,
    onOpenEqualizer: () -> Unit,
    onOpenScanner: () -> Unit,
    onEditSong: (Song) -> Unit = {},
    onOpenDrawer: () -> Unit = {},
    onPageChanged: (Int) -> Unit = {}
){
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { AppTab.entries.size})
    val coroutineScope = rememberCoroutineScope ()

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    var showMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var selectedAlbumsSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedQuickSelectType by remember { mutableStateOf<QuickSelectType?>(null) }
    var selectedQuickSelectSongs by remember { mutableStateOf <List<Song>> (emptyList()) }
    var selectedMood by remember { mutableStateOf<Mood?>(null) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var selectedArtistSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedGenreName by remember { mutableStateOf<String?>(null) }
    var selectedGenreSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedFolderName by remember { mutableStateOf<String?>(null) }
    var selectedFolderSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    var showCreatedPlaylistDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var playlistForOptions by remember { mutableStateOf<Playlist?>(null) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    val playlist by viewModel.playlists.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val recentSongs by viewModel.recentPlayedSongs.collectAsStateWithLifecycle()

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val songs = (uiState as? UiState.Success)?.songs ?: emptyList()
    val  searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()


    // Menú de una canción → "Ir al álbum" / "Ir al artista": se abre la ficha desde cualquier pantalla.
    val navRequest by viewModel.navRequest.collectAsStateWithLifecycle()
    LaunchedEffect(navRequest) {
        val request = navRequest ?: return@LaunchedEffect
        isSearchActive = false
        selectedQuickSelectType = null; selectedMood = null; selectedGenreName = null; selectedFolderName = null; selectedPlaylist = null
        when (request) {
            is NavRequest.Album -> {
                selectedAlbumsSongs = songs.filter { it.album == request.name }.sortedWith(
                    compareBy<Song> { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }.thenBy { it.title }
                )
                selectedArtistName = null
                selectedAlbumName = request.name
            }
            is NavRequest.Artist -> {
                selectedArtistSongs = songs.filter { it.artist == request.name }
                selectedAlbumName = null
                selectedArtistName = request.name
            }
        }
        viewModel.clearNav()
    }

    val artisListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val genresGridstate = rememberLazyGridState()


    if (selectedQuickSelectType != null || selectedMood != null){
        QuickSelectDetailScreen(
            categoryTitle = selectedMood?.label ?: selectedQuickSelectType!!.title,
            categorySongs = selectedQuickSelectSongs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onEditSongClick = onEditSong,
            onSongClick = { song, queue -> viewModel.playSong(song, customQueue = queue) },
            onPlayAllClick = {
                if (selectedQuickSelectSongs.isNotEmpty()){
                  viewModel.playSong(selectedQuickSelectSongs.first(), customQueue = selectedQuickSelectSongs)
                }
            },
            onShuffleClick = {
              if (selectedQuickSelectSongs.isNotEmpty()){
                  val shuffledList = selectedQuickSelectSongs.shuffled()
                  viewModel.playSong(shuffledList.first(), customQueue = shuffledList)
              }
            },
            onBackClick = { selectedQuickSelectType = null; selectedMood = null }
        )
    } else if (selectedAlbumName !=  null){
        AlbumDetailScreen(
            albumName = selectedAlbumName!!,
            albumSongs = selectedAlbumsSongs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onEditSongClick = onEditSong,
            onSongClick = {song, queue -> viewModel.playSong(song, customQueue = queue)},
            onPlayAllClick = {
                if (selectedAlbumsSongs.isNotEmpty()){
                    viewModel.playSong(selectedAlbumsSongs.first(), customQueue = selectedAlbumsSongs)
                }
            },
            onShuffleClick = {
                if (selectedAlbumsSongs.isNotEmpty()){
                    val shuffledList = selectedAlbumsSongs.shuffled()
                    viewModel.playSong(selectedAlbumsSongs.first(), customQueue = shuffledList)
                }
            },
            onBackClick = {selectedAlbumName = null}
        )
    }else if (selectedArtistName != null){
        ArtistDetailScreen(
            artistName = selectedArtistName!!,
            artistSongs = selectedArtistSongs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onEditSongClick = onEditSong,
            onSongClick = {song -> viewModel.playSong(song, customQueue = selectedArtistSongs)},
                onAlbumClick = {albumName, albumSongs ->
                selectedAlbumName = albumName
                selectedAlbumsSongs = albumSongs
            },
            onPlayAllClick = {
                if (selectedArtistSongs.isNotEmpty()) {
                    viewModel.playSong(
                        selectedArtistSongs.first(),
                        customQueue = selectedArtistSongs
                    )
                }
            },
            onShuffleClick = {
                if (selectedArtistSongs.isNotEmpty()){
                    val shuffledList = selectedArtistSongs.shuffled()
                    viewModel.playSong(shuffledList.first(), customQueue = shuffledList)
                }
            },
            onBackClick = {selectedArtistName = null}
        )
    }else if (selectedGenreName != null){
        GenreDetail(
            genreName =  selectedGenreName!!,
            genreSongs =  selectedGenreSongs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onEditSongClick = onEditSong,
            onSongClick = {song ->
                viewModel.playSong(song, customQueue = selectedGenreSongs)
            },
            onPlayAllCLick = {
                if (selectedGenreSongs.isNotEmpty()){
                    viewModel.playSong(selectedGenreSongs.first(), customQueue = selectedGenreSongs)
                }
            },
            onShuffleClick = {
                if (selectedGenreSongs.isNotEmpty()){
                    val shuffledList = selectedGenreSongs.shuffled()
                    viewModel.playSong(shuffledList.first(), customQueue = shuffledList)
                }
            },
            onBackClick = {
                selectedGenreName = null
                selectedGenreSongs = emptyList()
            }
        )
    } else if (selectedFolderName != null){
        QuickSelectDetailScreen(
            categoryTitle = selectedFolderName!!.substringAfterLast('/').ifBlank { selectedFolderName!! },
            categorySongs = selectedFolderSongs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onEditSongClick = onEditSong,
            onSongClick = {song, queue -> viewModel.playSong(song, customQueue = queue)},
            onPlayAllClick = {
                if (selectedFolderSongs.isNotEmpty()){
                    viewModel.playSong(selectedFolderSongs.first(), customQueue = selectedFolderSongs)
                }
            },
            onShuffleClick = {
                if (selectedFolderSongs.isNotEmpty()){
                    val shuffledList = selectedFolderSongs.shuffled()
                    viewModel.playSong(shuffledList.first(), customQueue = shuffledList)
                }
            },
            onBackClick = {
                selectedFolderName = null
                selectedFolderSongs = emptyList()
            }
        )
    } else if (selectedPlaylist != null){
        val activePlaylist = playlist.find { it.id == selectedPlaylist!!.id } ?: selectedPlaylist!!
        val playlistSongs = remember(songs, activePlaylist.songIds) {
            val ids = activePlaylist.songIds.toHashSet()
            songs.filter { it.id in ids }
        }

        PlaylistScreenDetail(
            playlist = activePlaylist,
            playlistSongs = playlistSongs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
            onSongClick = {song -> viewModel.playSong(song, customQueue =  playlistSongs)},
            onPlayAllClick = {
                if (playlistSongs.isNotEmpty()){
                    viewModel.playSong(playlistSongs.first(), customQueue =  playlistSongs)
                }
            },
            onShuffleClick = {
                if (playlistSongs.isNotEmpty()){
                    val shuffledList = playlistSongs.shuffled()
                    viewModel.playSong(shuffledList.first(), customQueue =  shuffledList)
                }
            },
            onAddSongsClick = {showAddSongsDialog = true},
            onRestoreClick = { showRestoreDialog = true },
            recentSongs = recentSongs,
            onBackClick = {selectedPlaylist = null},
            onRemoveSongFromPlaylist = { song ->
                viewModel.removeSongFromPlaylist(activePlaylist.id, song.id)
            }
        )
        if (showAddSongsDialog){
            AddSongsToPlaylistDialog(
                playlistName = activePlaylist.name,
                allSongs = songs,
                favoriteSongs = favoriteSongs,
                recentSongs = recentSongs,
                existingSongIds = activePlaylist.songIds,
                onDismiss = {showAddSongsDialog = false},
                onAddSongs = { selectedIds ->
                    viewModel.addSongsToPlaylist(activePlaylist.id, selectedIds)
                    showAddSongsDialog = false
                }

            )
        }
    }else{
        Box(modifier = Modifier.fillMaxSize()){
            if (isSearchActive){
                SearchOverlayScreen(
                    onEditSong = onEditSong,
                    searchQuery = searchQuery,
                    onQueryChange = {viewModel.setSearchQuery(it)},
                    searchHistory = searchHistory,
                    onHistoryItemClick = { selectedQuery ->
                        viewModel.setSearchQuery(selectedQuery)
                    },
                    onDeleteHistoryItem = {item ->
                        viewModel.removeFromHistory(item)
                    },
                    onAddSearchHistory = { query ->
                        viewModel.addSearchHistory(query)
                    },
                    allSongs = songs,
                    currentSongId = currentSong?.id,
                    isPlaying = isPlaying,
                    onSongClick = { song, searchQueue ->
                        viewModel.addSearchHistory(searchQuery)
                        viewModel.playSong(song, customQueue = searchQueue)
                    },
                    onAlbumClick = { albumName, albumSongs ->
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                        selectedAlbumName = albumName
                        selectedAlbumsSongs = albumSongs
                    },
                    onArtistClick = { artistName, artistSongs ->
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                        selectedArtistName = artistName
                        selectedArtistSongs = artistSongs
                    },
                    onCloseSearch = {
                        isSearchActive = false
                        viewModel.setSearchQuery("")
                    }
                )
            }else{
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("DEZZNUTS", style = MaterialTheme.typography.titleLarge)

                            },
                            navigationIcon = {
                                IconButton(onClick = onOpenDrawer) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menú")
                                }
                            },
                            actions = {
                                IconButton(onClick = { isSearchActive = true}) {
                                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                                }
                                IconButton(onClick = {showMenu = !showMenu}) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = {showMenu = false}
                                ) {
                                    DropdownMenuItem(
                                        text = {Text("Ecualizador")},
                                        onClick = {showMenu = false; onOpenEqualizer()}
                                    )
                                    DropdownMenuItem(
                                        text = {Text("Escanear archivos")},
                                        onClick = {showMenu = false; onOpenScanner()}
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background
                            )
                        )
                    }
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
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = {Text(tab.title)}
                                )
                            }
                        }
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f)
                        ) { page ->
                             when (AppTab.entries[page]){
                                 AppTab.QUICK_SELECT -> QuickSelectionScreen(
                                     viewModel = viewModel,
                                     onSongClick = { song, queue -> viewModel.playSong(song, customQueue = queue)},
                                     onSeeAllCategoryClick = {type, categorySongs ->
                                         selectedQuickSelectType = type
                                         selectedQuickSelectSongs = categorySongs
                                     },
                                     onMoodClick = { mood, moodSongs ->
                                         selectedMood = mood
                                         selectedQuickSelectSongs = moodSongs
                                     }
                                 )
                                 AppTab.SONGS -> Column(modifier = Modifier.fillMaxSize()) {
                                     Row(
                                         modifier = Modifier
                                             .horizontalScroll(rememberScrollState())
                                             .padding(horizontal = 12.dp),
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
                                     SongListScreen(
                                         songs = filteredSongs,
                                         currentSongId = currentSong?.id,
                                         isPlaying = isPlaying,
                                         onSongClick = {song ->
                                             viewModel.playSong(song, customQueue = filteredSongs)
                                         },
                                         onEditSong = onEditSong
                                     )
                                 }
                                 AppTab.ALBUMS -> AlbumsGridScreen(
                                     songs = songs,
                                     gridState = albumsGridState,
                                     onAlbumClick = {albumName, albumSongs ->
                                         selectedAlbumName = albumName
                                         selectedAlbumsSongs = albumSongs
                                     }
                                 )
                                 AppTab.ARTISTS -> ArtistsListScreen(
                                     songs = songs,
                                     listState = artisListState,
                                     onArtistClick = {artistName, artistSongs ->
                                         selectedArtistName = artistName
                                         selectedArtistSongs = artistSongs
                                     }
                                     )
                                 AppTab.GENRES -> GenresGridScreen(
                                     songs = songs,
                                     gridState = genresGridstate,
                                     onGenreClick = {genreName, genreSongs ->
                                         selectedGenreName = genreName
                                         selectedGenreSongs = genreSongs
                                     }
                                 )
                                 AppTab.PLAYLISTS -> PlaylistScreen(
                                     playlist = playlist,
                                     allSongs = songs,
                                     onCreatePlaylistClick = { showCreatedPlaylistDialog = true},
                                     onRestorePlaylistClick = { showRestoreDialog = true },
                                     onPlaylistOptionsClick = { playlistForOptions = it },
                                     onPlaylistClick = {playlist -> selectedPlaylist = playlist}
                                 )

                                 AppTab.FOLDERS -> FoldersListScreen(
                                     songs = songs,
                                     onFolderClick = {folderName, folderSongs ->
                                         selectedFolderName = folderName
                                         selectedFolderSongs = folderSongs
                                     }
                                 )
                             }
                         }
                    }
                }
            }
        }
    }

    playlistForOptions?.let { p ->
        PlaylistOptionsDialog(p, onDelete = { viewModel.deletePlaylist(p.id) }, onDismiss = { playlistForOptions = null })
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
