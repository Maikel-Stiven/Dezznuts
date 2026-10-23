package com.example.creproductor.gui

import androidx.activity.compose.BackHandler
import com.example.creproductor.data.coverModel
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.Song
import com.example.creproductor.formatDuration

@Composable
fun AlbumDetailScreen(
    albumName: String,
    albumSongs: List<Song>,
    currentSongId: Long? = null,
    isPlaying: Boolean = false,
    onSongClick: (song: Song, albumQueue: List<Song>) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onBackClick: () -> Unit,
    onEditSongClick: (Song) -> Unit = {}
) {
    BackHandler {
        onBackClick()
    }

    val firstSong = albumSongs.firstOrNull()
    val artistName = remember(albumSongs) {
        if (firstSong ==  null || firstSong.artist.isBlank() || firstSong.artist.equals("<unknown>", ignoreCase = true)) {
            "Artista Desconocido"
        }else{
            firstSong.artist
        }
    }

    val  albumYear = remember (albumSongs){
        albumSongs.firstOrNull{it.year > 0}?.year
    }
    val albumCover = remember (albumSongs){
        albumSongs.firstOrNull{it.albumArtUri != null}?.coverModel
    }

    val formattedTotalDuration = remember (albumSongs) {
        val totalMs = albumSongs.sumOf { it.duration }
        val totalMinutes = totalMs / 1000 / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val songCountText = if (albumSongs.size == 1) "1 canción" else "${albumSongs.size} canciones"
        val timeText = if (hours > 0) "$hours h $minutes min" else "$minutes min"
         "$songCountText • $timeText"
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
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = albumName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 10.dp )
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
                            .size(250.dp)
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (albumCover != null){
                            AsyncImage(
                                model = albumCover,
                                contentDescription = albumName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }else{
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )


                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.width(275.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = albumName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            modifier = Modifier.weight(1f, fill = false) . basicMarquee()
                        )

                        if (albumYear != null){
                            Text(
                                text = " • $albumYear",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formattedTotalDuration,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false).basicMarquee()
                    )

                    Spacer(modifier =  Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPlayAllClick,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("REPRODUCIR")
                        }

                        OutlinedButton(
                            onClick = onShuffleClick,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                                disabledContentColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ALEATORIO")
                        }
                    }
                }
            }

            itemsIndexed(albumSongs, key = { _, song -> "track_${song.id}" }) {index, song ->

                val isSelected = song.id == currentSongId
                val displayTracknumber = if (song.trackNumber > 0) song.trackNumber else index + 1

                AlbumTrackItem(
                    trackNumber = displayTracknumber,
                    song = song,
                    isSelected = isSelected,
                    isPlaying = isSelected && isPlaying,
                    onClick = {onSongClick(song, albumSongs)},
                    onEditClick = {onEditSongClick(song)}
                )
            }
        }
    }
}

@Composable
private fun AlbumTrackItem(
    trackNumber: Int,
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit
){
    val backGroundColor = if (isSelected){
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.20f)
    }else{
        MaterialTheme.colorScheme.background
    }

    val secondaryTextColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backGroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$trackNumber",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.width(32.dp)
        )

        Box(
            modifier = Modifier
                .size(45.dp)
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
                    tint = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier
            .weight(1f)
            .padding(end = 12.dp)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
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
                color = secondaryTextColor,
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
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opciones de pista",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)

            )
        }
    }
}

@Composable
fun PlayingEqualizerIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isPlaying: Boolean = true
){
    // Sin música no se dibuja nada. Sonando: animada solo si se ve (LocalAnimationsEnabled) y el equipo lo aguanta
    // (rememberVisualEffectsEnabled); si no, barras fijas, que siguen indicando la fila que suena sin gastar fotogramas.
    val effects = rememberVisualEffectsEnabled()
    if (isPlaying && effects && LocalAnimationsEnabled.current) AnimatedEqualizerBars(modifier, color)
    else if (isPlaying) EqualizerBars(modifier, color, { 0.55f }, { 0.9f }, { 0.7f })
    else EqualizerBars(modifier, color, { 0f }, { 0f }, { 0f })
}

/**
 * Barras animadas con UN solo reloj que avanza ~15 veces por segundo (antes: tres animaciones infinitas a 60 fps, que
 * mantenían toda la ventana dibujando a 60 fps mientras sonaba una canción). Para un icono de 16×14 dp el
 * movimiento se ve igual de vivo.
 */
@Composable
private fun AnimatedEqualizerBars(modifier: Modifier, color: Color) {
    val phase = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val start = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            androidx.compose.runtime.withFrameNanos { now -> phase.floatValue = (now - start) / 1_000_000_000f }
            kotlinx.coroutines.delay(55)
        }
    }
    // Cada barra oscila a su ritmo (periodos ~0,84 s, 1,06 s y 0,72 s, como las animaciones originales).
    fun bar(period: Float, offset: Float, lo: Float, hi: Float): Float {
        val x = (kotlin.math.sin((phase.floatValue / period + offset) * 2f * Math.PI.toFloat()) + 1f) / 2f
        return lo + (hi - lo) * x
    }
    EqualizerBars(modifier, color, { bar(0.84f, 0.00f, 0.2f, 0.9f) }, { bar(1.06f, 0.30f, 0.25f, 0.85f) }, { bar(0.72f, 0.60f, 0.35f, 1.0f) })
}

@Composable
private fun EqualizerBars(modifier: Modifier, color: Color, bar1: () -> Float, bar2: () -> Float, bar3: () -> Float) {
    val  density = LocalDensity.current
    val barWidthPx = remember(density) {with(density) {3.dp.toPx()}}
    val gapPx = remember (density){with(density) {2.dp.toPx()} }
    val cornerRadius = remember (density) { CornerRadius(with(density) {1.5.dp.toPx()}) }

    Canvas(modifier = modifier.size(width = 16.dp, height = 14.dp)) {
        val maxHeight = size.height
        val b1H = maxHeight * bar1()
        val b2H = maxHeight * bar2()
        val b3H = maxHeight * bar3()

        drawRoundRect(
            color = color,
            topLeft = Offset(x = 0f, y = maxHeight - b1H),
            size = Size(width = barWidthPx, height = b1H),
            cornerRadius = cornerRadius
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(x = barWidthPx + gapPx, y = maxHeight - b2H),
            size = Size(width = barWidthPx, height = b2H),
            cornerRadius = cornerRadius
        )

        drawRoundRect(
            color = color,
            topLeft = Offset(x = (barWidthPx + gapPx) * 2, y = maxHeight - b3H),
            size= Size(width = barWidthPx, height = b3H),
            cornerRadius = cornerRadius
        )
    }
}