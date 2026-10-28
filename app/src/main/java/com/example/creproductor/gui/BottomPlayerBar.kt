package com.example.creproductor.gui

import com.example.creproductor.i18n.tr
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import coil.compose.AsyncImage
import com.example.creproductor.data.Song
import com.example.creproductor.data.coverModel

/**
 * Mini-reproductor flotante. Tocar o deslizar hacia arriba abre el reproductor; deslizar a los lados cambia de canción.
 */
@Composable
fun BottomPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onBarClick: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
    val swipePx = with(LocalDensity.current) { 56.dp.toPx() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                var dx = 0f; var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDragEnd = {
                        when {
                            -dy > swipePx && -dy > kotlin.math.abs(dx) -> onBarClick()
                            dx < -swipePx -> onNext()
                            dx > swipePx -> onPrevious()
                        }
                    }
                ) { change, drag -> change.consume(); dx += drag.x; dy += drag.y }
            },
        onClick = onBarClick,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.coverModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = displayArtist(song.artist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledIconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(44.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = tr("Siguiente"), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
