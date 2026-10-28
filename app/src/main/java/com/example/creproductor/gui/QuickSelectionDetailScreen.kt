package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.Song

@Composable
fun QuickSelectDetailScreen(
    categoryTitle: String,
    categorySongs: List<Song>,
    currentSongId: Long? = null,
    isPlaying: Boolean =false,
    onSongClick: (song: Song, categoryQueue: List<Song>) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onBackClick: () -> Unit,
    onEditSongClick: (Song) -> Unit = {}
){
    BackHandler {
        onBackClick()
    }

    val coverUris = remember(categorySongs){
        categorySongs.mapNotNull { it.coverModel }.distinct().take(3)
    }

    val formattedInfo = remember (categorySongs) {
        val totalMs = categorySongs.sumOf { it.duration }
        val totalMins = totalMs / 1000 / 60
        val hours = totalMins / 60
        val minutes = totalMins % 60

        val countText = if (categorySongs.size == 1) "1 canción" else "${categorySongs.size} canciones"
        val timeText = if (hours > 0) "$hours h $minutes min" else "$minutes min"
        "$countText • $timeText"
    }

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
                    contentDescription = tr("volver"),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = categoryTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    StackedCovers(
                        covers = coverUris,
                        modifier = Modifier.size(200.dp),
                        iconSize = 64.dp,
                        cornerRadius = 8.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formattedInfo,
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
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
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
                }
            }

            itemsIndexed(categorySongs, key = {_, song -> "qsd_${song.id}"}) {_, song ->
                val isSelected = song.id == currentSongId

                QuickSelectTrackItem(
                    song = song,
                    isSelected = isSelected,
                    isPlaying = isSelected && isPlaying,
                    onClick = {onSongClick(song, categorySongs)},
                    onEditClick = {onEditSongClick(song)}
                )
            }
        }
    }
}

@Composable
fun StackedCovers(
    covers: List <Any?>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
    cornerRadius: Dp = 5.dp
){

    val validCovers = covers.filterNotNull()
    val topCover = validCovers.getOrNull(0)
    val midCover = validCovers.getOrNull(1) ?: topCover
    val backCover = validCovers.getOrNull(2) ?: midCover

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter
    ){
        Box(
            modifier = Modifier
                .fillMaxWidth(0.80f)
                .fillMaxHeight(0.86f)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.onSurfaceVariant),
            contentAlignment = Alignment.Center
        ){
            if (backCover != null){
                AsyncImage(
                    model = backCover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer{ alpha = 0.5f },
                    contentScale = ContentScale.Crop
                )
            }else{
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f))
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .fillMaxHeight(0.93f)
                .padding(top = 6.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.onSurfaceVariant),
            contentAlignment = Alignment.Center
        ){
            if (midCover != null){
                AsyncImage(
                    model = midCover,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer{ alpha = 0.75f},
                    contentScale = ContentScale.Crop
                )
            }else{
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f))
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ){
            if (topCover != null){
                AsyncImage(
                    model = topCover,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }else{
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}


@Composable
private fun QuickSelectTrackItem(
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit
){
    val backgroundColor = if (isSelected){
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    }else{
        MaterialTheme.colorScheme.background
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

        val artistText = if (song.artist.isBlank() || song.artist.equals("<unknown>", ignoreCase = true) || song.artist.equals("unknown", ignoreCase = true)){
            "Artista desconocido"
            }else{
                song.artist
            }

            Text(
                text = "$artistText • ${formatDuration(song.duration)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
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

        IconButton(
            onClick = onEditClick,
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
