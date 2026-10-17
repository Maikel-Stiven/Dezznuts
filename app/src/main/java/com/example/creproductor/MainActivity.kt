package com.example.creproductor

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import com.example.creproductor.data.Song
import com.example.creproductor.gui.AppDrawerContent
import com.example.creproductor.gui.DrawerMenuItem
import com.example.creproductor.gui.EditTagDialog
import com.example.creproductor.gui.FullScreenPlayer
import com.example.creproductor.gui.MainScreen
import com.example.creproductor.gui.MusicViewModel
import com.example.creproductor.gui.UiState
import com.example.creproductor.ui.theme.DEZZNUTStheme
import kotlinx.coroutines.launch
import coil.Coil


class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageLoader = ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("artist_images_cache"))
                    .maxSizeBytes(30 * 1024 * 1024)
                    .build()
            }
            .build()
        Coil.setImageLoader (imageLoader)

        setContent {
            DEZZNUTStheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    val currentSong by viewModel.currentSong.collectAsState()
                    val isPlaying by viewModel.isPlaying.collectAsState()
                    val currentQueue by viewModel.currentQueue.collectAsState()
                    val repeatMode by viewModel.repeatMode.collectAsState()
                    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsState()

                    var songToEdit by remember { mutableStateOf<Song?>(null) }
                    var isPlayerFullScreenVisible by remember { mutableStateOf(false) }

                    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            viewModel.loadSongs()
                        }
                    }

                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(requiredPermission)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val state = uiState) {
                            is UiState.PermissionRequired -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Button(onClick = { permissionLauncher.launch(requiredPermission) }) {
                                        Text("Conceder acceso")
                                    }
                                }
                            }
                            is UiState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Empty -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No se encontraron archivos de audio locales")
                                }
                            }
                            is UiState.Success -> {
                                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                                val scope = rememberCoroutineScope()

                                val currentPositionMs by viewModel.currentPositionMs.collectAsState()

                                BackHandler(enabled = isPlayerFullScreenVisible) {
                                    isPlayerFullScreenVisible = false
                                }

                                BackHandler(enabled = drawerState.isOpen) {
                                    scope.launch { drawerState.close() }
                                }

                                ModalNavigationDrawer(
                                    drawerState = drawerState,
                                    gesturesEnabled = !isPlayerFullScreenVisible,
                                    drawerContent = {
                                        AppDrawerContent(
                                            onMenuItem= { menuItem ->
                                                when(menuItem) {
                                                    is DrawerMenuItem.Equalizer -> { /*Falta lógica del ecualizador*/}
                                                    is DrawerMenuItem.ScanFiles -> { /*Falta lógica del escaner de archivos*/}
                                                    is DrawerMenuItem.AudioCutter -> {/*Falta lógica del cortador de audio*/}
                                                    is DrawerMenuItem.SleepTimer -> {/*Falta lógica del temporizador*/}
                                                    else -> {/*no sé, aún no pienso en otras funciones que le aporten al usuario*/}
                                                }
                                            },
                                            onCloseDrawer = {
                                                scope.launch { drawerState.close() }
                                            }
                                        )
                                    }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Column(modifier = Modifier.fillMaxSize()) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                MainScreen(
                                                    viewModel = viewModel,
                                                    onSongClick = { song -> viewModel.playSong(song) },
                                                    onOpenEqualizer = { /* TODO */ },
                                                    onOpenScanner = { /* TODO */ },
                                                    onOpenDrawer = {
                                                        scope.launch { drawerState.open() }
                                                    }
                                                )
                                            }

                                            AnimatedVisibility(
                                                visible = currentSong != null,
                                                enter = slideInVertically(initialOffsetY = { it }),
                                                exit = slideOutVertically(targetOffsetY = { it })
                                            ) {
                                                currentSong?.let { song ->
                                                    BottomPlayerBar(
                                                        song = song,
                                                        isPlaying = isPlaying,
                                                        onTogglePlayPause = { viewModel.togglePlayPause() },
                                                        onBarClick = { isPlayerFullScreenVisible = true }
                                                    )
                                                }
                                            }
                                        }

                                        if (isPlayerFullScreenVisible && currentSong != null) {
                                            val activeSongsQueue = currentQueue.ifEmpty { state.songs }

                                            FullScreenPlayer(
                                                song = currentSong!!,
                                                songs = activeSongsQueue,
                                                isPlaying = isPlaying,
                                                currentPositionMs = currentPositionMs,
                                                repeatMode = repeatMode,
                                                isShuffleEnabled = isShuffleEnabled,
                                                onToggleShuffle = { viewModel.toggleShuffle() },
                                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                                onNextClick = { viewModel.playNext() },
                                                onPreviousClick = { viewModel.playPrevious() },
                                                onSeekTo = { pos -> viewModel.seekTo(pos)},
                                                onSongSelected = {song -> viewModel.playSong(song, customQueue = activeSongsQueue)},
                                                onToggleRepeatMode = {viewModel.toggleRepeatMode()},
                                                onRemoveQueue = {song -> viewModel.removeFromQueue(song)},
                                                onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                                                onDismiss = { isPlayerFullScreenVisible = false },
                                                onToggleFavorite = { song -> viewModel.toggleFavorite(song) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        songToEdit?.let { song ->
                            EditTagDialog(
                                song = song,
                                onDismiss = { songToEdit = null },
                                onSave = { title, artist, album, coverUriOrUrl ->
                                    viewModel.updatedSongTags(song, title, artist, album, coverUriOrUrl)
                                    songToEdit = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SongListScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onEditSong: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(items = songs, key = { it.id }) { song ->
            SongItem(
                song = song,
                onClick = { onSongClick(song) },
                onEditClick = { onEditSong(song) }
            )
        }
    }
}

fun formatDuration(durationMs: Long): String{
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = "Portada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                val artistName = if (song.artist.isBlank() || song.artist == "<unknown>") {
                    "Artista desconocido"
                }else{
                    song.artist
                }

                Text(
                    text = "$artistName • ${formatDuration(song.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

}

@Composable
fun BottomPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onBarClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBarClick),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )

                val artistName = if (song.artist.isBlank() || song.artist == "<unknown>") {
                    "Artista desconocido"
                }else{
                    song.artist
                }

                val hasAlbum = song.album.isNotBlank() && !song.album.equals("<unknown>", ignoreCase = true)
                val subtitlteText = if (hasAlbum) "$artistName • ${song.album}" else artistName

                Text(
                    text = subtitlteText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

            }

            FilledIconButton(onClick = onTogglePlayPause) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                )
            }
        }
    }
}