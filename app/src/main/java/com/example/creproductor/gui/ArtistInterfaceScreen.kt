package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.activity.compose.BackHandler
import com.example.creproductor.data.coverModel
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.Song
import com.example.creproductor.data.SongOrganizer

@Composable
fun ArtistDetailScreen (
    artistName: String,
    artistSongs: List<Song>,
    currentSongId: Long? = null,
    isPlaying: Boolean = false,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (albumName: String, albumSongs: List<Song>) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onBackClick: () -> Unit,
    onEditSongClick: (Song) -> Unit = {}
){

    BackHandler{
        onBackClick()
    }

    val avatarUri = remember (artistSongs){
        artistSongs.firstOrNull { it.albumArtUri != null}?.coverModel
    }

    // Foto del artista: primero la de internet (Deezer) y, si no hay, la portada de
    // una de sus canciones. Si ya se cacheó desde la búsqueda, aparece al instante.
    val displayImage = rememberArtistPhoto(artistName, avatarUri, delayMs = 150L)

    val albumsMap = remember (artistSongs){
        SongOrganizer.groupByAlbum(
            items = artistSongs,
            albumOf = {it.album},
            trackNumberOf = {it.trackNumber},
            titleOf = {it.title}
        )
    }

    val statsText = remember (artistSongs) {
        val albumsCount = albumsMap.keys.size.coerceAtLeast(if (artistSongs.isNotEmpty()) 1 else 0 )
        val albumsLabel = if (albumsCount ==  1) "1 álbum" else "$albumsCount álbumes"
        val songLabel = if (artistSongs.size == 1) "1 canción" else "${artistSongs.size} canciones"
        "$albumsLabel • $songLabel"
    }

    val totalDurationText = remember (artistSongs) {
        val totalMs = artistSongs.sumOf { it.duration }
        val totalMins = totalMs / 1000 / 60
        val hours = totalMins / 60
        val mins = totalMins % 60

        if (hours > 0) "$hours h $mins min" else "$mins min"
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
                .padding(horizontal =  8.dp, vertical = 4.dp),
            // Centra el nombre verticalmente con la flecha de volver (por defecto
            // quedaba pegado arriba porque el IconButton es más alto).
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick =  onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("Volver"),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = artistName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
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
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ){

                        if (displayImage != null){
                            AsyncImage(
                                model = displayImage,
                                contentDescription = artistName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }else{
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(80.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = statsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = totalDurationText,
                        style = MaterialTheme.typography.bodySmall,
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

            if (albumsMap.isNotEmpty()){
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = tr("Álbumes"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(albumsMap.keys.toList(), key = {it}) {albumName ->
                                val albumSongs = albumsMap[albumName] ?: emptyList()
                                val  cover = albumSongs.firstOrNull {it.albumArtUri != null}?.coverModel

                                Column(
                                    modifier = Modifier
                                        .width(125.dp)
                                        .clickable {onAlbumClick(albumName, albumSongs)}
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(125.dp)
                                            .aspectRatio(1f)
                                            .clip(MaterialTheme.shapes.small)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ){
                                        if (cover != null){
                                            AsyncImage(
                                                model = cover,
                                                contentDescription = albumName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }else{
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(40.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = albumName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.basicMarquee()
                                    )

                                    val countLabel = if (albumSongs.size ==  1) "1 canción" else "${albumSongs.size} canciones"
                                    Text(
                                        text = countLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = tr("Canciones"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            itemsIndexed(artistSongs, key = {_, song -> "artist_song${song.id}"}) { _, song ->
                val isSelected = song.id == currentSongId

                ArtistTrackItem(
                    song = song,
                    isSelected = isSelected,
                    isPlaying = isSelected && isPlaying,
                    onClick = {onSongClick(song)},
                    onEditClick = {onEditSongClick(song)}
                )
            }
        }
    }
}


@Composable
private fun ArtistTrackItem(
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val albumName = if (song.album.isBlank() || song.album.equals("unknown", ignoreCase = true)){
                "Álbum desconocido"
            }else{
                song.album
            }

            Text(
                text = "$albumName • ${formatDuration(song.duration)}",
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

        IconButton(
            onClick = onEditClick,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = tr("opciones"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}