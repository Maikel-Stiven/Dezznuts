package com.example.creproductor.gui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.creproductor.SongListScreen
import com.example.creproductor.data.Playlist
import com.example.creproductor.data.Song

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
    onSongClick: (Song) -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenScanner: () -> Unit,
    onOpenDrawer: () -> Unit = {}
){
    var selectedTabIndex by remember{ mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var selectedAlbumsSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedQuickSelectType by remember { mutableStateOf<QuickSelectType?>(null) }
    var selectedQuickSelectSongs by remember { mutableStateOf <List<Song>> (emptyList()) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    var selectedArtistSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedGenreName by remember { mutableStateOf<String?>(null) }
    var selectedGenreSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var selectedFolderName by remember { mutableStateOf<String?>(null) }
    var selectedFolderSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    var showCreatedPlaylistDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    val playlist by viewModel.playlists.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()
    val recentSongs by viewModel.recentPlayedSongs.collectAsState()

    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val songs = (uiState as? UiState.Success)?.songs ?: emptyList()
    val  searchHistory by viewModel.searchHistory.collectAsState()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()


    val artisListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val genresGridstate = rememberLazyGridState()


    if (selectedQuickSelectType != null){
        QuickSelectDetailScreen(
            categoryTitle = selectedQuickSelectType!!.title,
            categorySongs = selectedQuickSelectSongs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
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
            onBackClick = {selectedQuickSelectType = null}
        )
    } else if (selectedAlbumName !=  null){
        AlbumDetailScreen(
            albumName = selectedAlbumName!!,
            albumSongs = selectedAlbumsSongs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
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
            categoryTitle = selectedFolderName!!,
            categorySongs = selectedFolderSongs,
            currentSongId = currentSong?.id,
            isPlaying = isPlaying,
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
        // Con un Set la pertenencia es O(1); `remember` evita refiltrar toda la biblioteca en cada
        // recomposición mientras la lista no cambie.
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
            onRestoreClick = {/*falta lógica*/},
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
                            selectedTabIndex = selectedTabIndex,
                            edgePadding = 16.dp,
                            containerColor = MaterialTheme.colorScheme.background,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            AppTab.entries.forEachIndexed { index, tab ->
                                Tab(
                                    selected = selectedTabIndex == index,
                                    onClick = {selectedTabIndex = index},
                                    text = {Text(tab.title)}
                                )
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            when (AppTab.entries[selectedTabIndex]){
                                AppTab.QUICK_SELECT -> QuickSelectionScreen(
                                    viewModel = viewModel,
                                    onSongClick = { song, queue -> viewModel.playSong(song, customQueue = queue)},
                                    onSeeAllCategoryClick = {type, categorySongs ->
                                        selectedQuickSelectType = type
                                        selectedQuickSelectSongs = categorySongs
                                    }
                                )
                                AppTab.SONGS -> SongListScreen(
                                    songs = filteredSongs,
                                    currentSongId = currentSong?.id,
                                    isPlaying = isPlaying,
                                    onSongClick = {song ->
                                        viewModel.playSong(song, customQueue = filteredSongs)
                                    },
                                    onEditSong = {}
                                )
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
                                    onRestorePlaylistClick = {/*agregar lógica*/},
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