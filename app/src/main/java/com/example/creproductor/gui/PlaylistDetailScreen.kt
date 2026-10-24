package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.activity.compose.BackHandler
import com.example.creproductor.data.coverModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.Playlist
import com.example.creproductor.data.Song
import com.example.creproductor.formatDuration
import com.example.creproductor.data.SongOrganizer
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex

private enum class PlaylistFilter(val label: String) {
    ALL("Todas"), FAVORITES("Favoritos"), RECENT("Reproducidas recientemente")
}

@Composable
fun PlaylistScreenDetail(
    playlist: Playlist,
    playlistSongs: List<Song>,
    currentSongId: Long?= null,
    isPlaying: Boolean = false,
    allPlaylists: List<Playlist> = emptyList(),
    onSongClick: (Song) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onAddSongsClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onBackClick: () -> Unit,
    onRemoveSongFromPlaylist: (Song) -> Unit = {},
    onMoveSong: (fromIndex: Int, toIndex: Int) -> Unit = {_, _ ->},
    recentSongs: List<Song> = emptyList(),
    onPlayNext: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddToPlaylist: (song: Song ,targetPlaylistId: String) -> Unit = {_, _ ->},
    onGoToAlbum: (Song) -> Unit = {},
    onGoToArtist: (Song) -> Unit = {},
    onEditTags: (Song) -> Unit = {},
    onAbout: (Song) -> Unit = {},
    onAudioCutter: (Song) -> Unit = {},
    onDeleteSong: (Song) -> Unit = {}
){
    BackHandler {
        onBackClick()
    }

    // Filtros de la maqueta: Todas / Favoritos / Reproducidas recientemente, y búsqueda dentro de la lista.
    var filter by remember { mutableStateOf(PlaylistFilter.ALL) }
    var query by remember { mutableStateOf("") }
    var selectedSongForMenu by remember { mutableStateOf<Song?>(null) }
    var isDraggingSong by remember { mutableStateOf(false) }
    var songToEditTags by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(songToEditTags) {
        val song = songToEditTags ?: return@LaunchedEffect
        onEditTags(song)
        songToEditTags = null
    }
    val isReorderable = filter == PlaylistFilter.ALL && query.isBlank()

    val visibleSongs = remember(playlistSongs, recentSongs, filter, query) {
        val base = when (filter) {
            PlaylistFilter.ALL -> playlistSongs
            PlaylistFilter.FAVORITES -> playlistSongs.filter { it.isFavorite }
            PlaylistFilter.RECENT -> {
                val inList = playlistSongs.associateBy { it.id }
                recentSongs.mapNotNull { inList[it.id] }   // en el orden de reproducción reciente
            }
        }
        if (query.isBlank()) base else base.filter { SongOrganizer.matchesQuery(it.title, it.artist, it.album, query.trim()) }
    }

    val displayCover = remember (playlist, playlistSongs) {
        playlist.coverUri ?: playlistSongs.firstOrNull {it.albumArtUri != null }?.coverModel
    }

    val formattedTotalDuration = remember (playlistSongs) {
        val totalMs = playlistSongs.sumOf { it.duration }
        val totalMins = totalMs / 1000 / 60
        val hours = totalMins / 60
        val mins = totalMins % 60

        val songCountText = if (playlistSongs.size == 1) "1 canción" else "${playlistSongs.size} canciones"
        val timeText = if (hours > 0) "$hours h $mins min" else "$mins min"
        "$songCountText • $timeText"
    }

    Box(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Volver"),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isDraggingSong,
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .aspectRatio(1f)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ){
                            if (displayCover != null){
                                AsyncImage(
                                    model = displayCover,
                                    contentDescription = playlist.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }else{
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(72.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (playlistSongs.isNotEmpty()){
                            Text(
                                text = formattedTotalDuration,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onPlayAllClick,
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(tr("REPRODUCIR"))
                                }

                                OutlinedButton(
                                    onClick = onShuffleClick,
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shuffle,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(tr("ALEATORIO"))
                                }
                            }
                        }else{
                            Text(
                                text = tr("Sin canciones"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = onAddSongsClick,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Text(tr("Añadir canciones +"))
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = onRestoreClick,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tr("Restaurar listas de reproducción"))
                            }
                        }
                    }
                }

                if (playlistSongs.isNotEmpty()){
                    item {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text(tr("Buscar")) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(Modifier.horizontalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PlaylistFilter.entries.forEach { f ->
                                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(tr(f.label)) })
                                }
                            }
                        }
                    }
                    if (visibleSongs.isEmpty()) {
                        item {
                            Text(
                                tr("No hay canciones que coincidan"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                   itemsIndexed(visibleSongs, key = { _, song -> "playlist_song_${song.id}" }) {index, song ->
                       val isSelected = song.id == currentSongId

                       PlaylistTrackItem(
                           song = song,
                           isSelected = isSelected,
                           isPlaying = isSelected && isPlaying,
                           isReorderable = isReorderable,
                           canMoveUp = index > 0,
                           canMoveDown = index < visibleSongs.size - 1,
                           onClick = {onSongClick(song)},
                           onOptionsClick = { selectedSongForMenu = song },
                           onMoveUp = {onMoveSong(index, index -1)},
                           onMoveDown = {onMoveSong(index, index + 1)},
                           onDragStart = {isDraggingSong = true},
                           onDragEnd = {isDraggingSong = false}
                       )
                   }
                }
            }
        }

        if (playlistSongs.isNotEmpty()){
            FloatingActionButton(
                onClick = onAddSongsClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = tr("Añadir canciones")
                )
            }
        }

        selectedSongForMenu?.let { song ->
            PlaylistSongMenuSheet(
                song = song,
                playlist = allPlaylists,
                actions = PlaylistSongMenuActions (
                    onPlayNext = {selectedSongForMenu = null; onPlayNext(song)},
                    onAddToQueue = {selectedSongForMenu = null; onAddToQueue(song)},
                    onAddToPlaylist = { targetPlaylistId ->
                        selectedSongForMenu = null
                        onAddToPlaylist(song, targetPlaylistId)},
                    onRemoveFromPlaylist = { selectedSongForMenu = null; onRemoveSongFromPlaylist(song)},
                    onGoToAlbum = {selectedSongForMenu = null; onGoToAlbum(song)},
                    onGoToArtist = {selectedSongForMenu = null; onGoToArtist(song)},
                    onEditTags = {selectedSongForMenu = null; onEditTags(song)},
                    onAbout = {selectedSongForMenu = null; onAbout(song)},
                    onAudioCutter = {selectedSongForMenu = null; onAudioCutter(song)},
                    onDeleteFromDevice = { selectedSongForMenu = null; onDeleteSong(song)}
                ),
                onDismiss = {selectedSongForMenu = null}
            )
        }
    }
}

@Composable
private fun  PlaylistTrackItem(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isReorderable: Boolean,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit
){
    val backgroundColor = if (isSelected){
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    }else{
        MaterialTheme.colorScheme.background
    }

    val currentCanMoveUp by rememberUpdatedState(canMoveUp)
    val currentCanMoveDown by rememberUpdatedState(canMoveDown)
    val currentOnMoveUp by rememberUpdatedState(onMoveUp)
    val currentOnMoveDown by rememberUpdatedState(onMoveDown)
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val density = LocalDensity.current
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingThisItem by remember { mutableStateOf(false) }

    val elevation = if (isDraggingThisItem) 8.dp else 0.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDraggingThisItem) 1f else 0f)
            .graphicsLayer{
                translationY = offsetY
                shadowElevation = with(density) {elevation.toPx()}
            }
            .background(backgroundColor)
            .clickable(enabled = !isDraggingThisItem,onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (isReorderable){
            val itemHeightPx = with(density) {68.dp.toPx()}
            Icon(
                imageVector = Icons.Default.DragHandle,
                contentDescription = "Reordenar",
                tint = if (isDraggingThisItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(22.dp)
                    .pointerInput(Unit){
                        detectDragGestures(
                            onDragStart = {
                                offsetY = 0f
                                isDraggingThisItem = true
                                currentOnDragStart()
                            },
                            onDrag = {change, dragAmount ->
                                change.consume()
                                offsetY += dragAmount.y
                                if (offsetY > itemHeightPx / 2f && currentCanMoveDown){
                                    currentOnMoveDown()
                                    offsetY -= itemHeightPx
                                }else if (offsetY < -itemHeightPx / 2f && currentCanMoveUp){
                                    currentOnMoveUp()
                                    offsetY += itemHeightPx
                                }
                            },
                            onDragEnd = {
                                offsetY = 0f
                                isDraggingThisItem = false
                                currentOnDragEnd()
                            },
                            onDragCancel = {
                                offsetY = 0f
                                isDraggingThisItem = false
                                currentOnDragEnd()
                            }
                        )
                    }
            )
        }

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

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val artistText = if (song.artist.isBlank() || song.artist.equals("<unknown>", ignoreCase = true)){
                "Artista desconocido"
            }else{
                song.artist
            }

            Text(
                text = "$artistText • ${formatDuration(song.duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected) {
            PlayingEqualizerIcon(
                color = MaterialTheme.colorScheme.primary,
                isPlaying = isPlaying,
                modifier = Modifier.padding(end = 6.dp)
            )
        }

        IconButton(
            onClick  = onOptionsClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = tr("Opciones"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}