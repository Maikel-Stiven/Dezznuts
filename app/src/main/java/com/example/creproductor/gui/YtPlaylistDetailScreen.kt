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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.creproductor.data.YtMusic
import com.example.creproductor.data.YtPlaylist
import com.example.creproductor.data.YtTrack
import com.example.creproductor.i18n.tr
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers

@Composable
fun YtPlaylistDetailScreen(
    playlist: YtPlaylist,
    currentSongUrl: String? = null,
    isPlaying: Boolean = false,
    onTrackClick: (index: Int) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onAddSongsClick: () -> Unit,
    onBackClick: () -> Unit,
    onRemoveTrackFromPlaylist: (YtTrack) -> Unit,
    onTrackOptionsClick: (YtTrack) -> Unit,
    onDeletePlaylist: () -> Unit,
    onDownloadAll: () -> Unit = {},
    downloading: Map<String, Float> = emptyMap()
){
    BackHandler {
        onBackClick()
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val displayCover = remember(playlist.tracks) {
        playlist.tracks.firstOrNull {!it.thumbnail.isNullOrBlank()}?.thumbnail
    }

    val formattedTime = remember(playlist.tracks) {
        val totalSec = playlist.tracks.sumOf { it.durationSec }
        val totalMins = totalSec / 60
        val hours = totalMins / 60
        val mins = totalMins % 60
        if (hours > 0) "$hours h $mins" else "$mins min"
    }
        val songCountText = if (playlist.tracks.size ==  1){
            tr("1 canción")
        }else{
            "${playlist.tracks.size} " + tr("canciones")
        }
        val formattedTotalDuration = "$songCountText • $formattedTime"

    Box(modifier = Modifier.fillMaxSize()){
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (playlist.tracks.isNotEmpty()) IconButton(onClick = onDownloadAll) {
                    Icon(Icons.Default.Download, contentDescription = tr("Descargar lista"), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = {showDeleteConfirm = true}) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = tr("Eliminar playlist"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item (key = "yt_pl_header") {
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
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(displayCover)
                                        .fetcherDispatcher(Dispatchers.IO)
                                        .decoderDispatcher(Dispatchers.IO)
                                        .crossfade(true)
                                        .build(),
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

                        if (playlist.tracks.isNotEmpty()){
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
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(tr("REPRODUCIR"), maxLines = 1, softWrap = false)
                                }

                                OutlinedButton(
                                    onClick = onShuffleClick,
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                                    Text(tr("ALEATORIO"), maxLines = 1, softWrap = false)
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
                        }
                    }
                }

                if (playlist.tracks.isNotEmpty()){
                    itemsIndexed(
                        items = playlist.tracks,
                        key = {index, track -> Pair (track.url, index)}
                    ) {index, track ->
                        val isSelected = currentSongUrl == track.url
                        YtPlaylistTrackItem(
                            track = track,
                            isSelected = isSelected,
                            isPlaying = isSelected && isPlaying,
                            onClick = {onTrackClick(index)},
                            onRemoveClick = {onRemoveTrackFromPlaylist(track)},
                            onOptionsClick = {onTrackOptionsClick(track)},
                            progress = downloading[track.url]
                        )
                    }
                }
            }
        }

        if (playlist.tracks.isNotEmpty()){
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
    }

    if (showDeleteConfirm){
        AlertDialog(
            onDismissRequest = {showDeleteConfirm = false},
            title = {Text(tr("Eliminar playlist"))},
            text = {Text(tr("¿Desea eliminar \"%s\"?").format(playlist.name))},
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeletePlaylist()
                    }
                ) {
                    Text(tr("Eliminar"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = {showDeleteConfirm = false}) {
                    Text(tr("Cancelar"))
                }
            }
        )
    }
}

@Composable
private fun YtPlaylistTrackItem(
    track: YtTrack,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onOptionsClick: () -> Unit,
    progress: Float? = null
) {
    val backgroundColor = if (isSelected){
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    }else{
        MaterialTheme.colorScheme.background
    }

    val durationText = remember (track.durationSec) {
        val min = TimeUnit.SECONDS.toMinutes(track.durationSec)
        val sec = track.durationSec % 60
        String.format(Locale.getDefault(), "%d:%02d", min, sec)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ){
            if (!track.thumbnail.isNullOrBlank()){
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.thumbnail)
                        .fetcherDispatcher(Dispatchers.IO)
                        .decoderDispatcher(Dispatchers.IO)
                        .crossfade(true)
                        .build(),
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
                text = YtMusic.cleanTitle(track.title, track.artist),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val artistText = YtMusic.cleanArtist(track.artist).ifBlank { "Youtube Music" }

            Text(
                text = "$artistText • $durationText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isSelected){
            PlayingEqualizerIcon(
                color = MaterialTheme.colorScheme.primary,
                isPlaying = isPlaying,
                modifier = Modifier.padding(end = 6.dp)
            )
        }

        if (progress != null) androidx.compose.material3.CircularProgressIndicator(
            progress = { progress }, strokeWidth = 2.dp,
            modifier = Modifier.padding(end = 6.dp).size(18.dp)
        )

        IconButton(
            onClick = onRemoveClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.RemoveCircleOutline,
                contentDescription = tr("Eliminar de la playlist"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onOptionsClick,
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