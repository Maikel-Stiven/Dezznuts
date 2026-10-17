package com.example.creproductor.gui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.creproductor.data.RepeatMode
import com.example.creproductor.data.Song
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue
@OptIn(ExperimentalMaterial3Api :: class)
@Composable
fun FullScreenPlayer(
    song: Song,
    songs: List<Song>,
    isPlaying: Boolean,
    currentPositionMs: Long,
    repeatMode: RepeatMode = RepeatMode.OFF,
    isShuffleEnabled: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSongSelected: (Song) -> Unit,
    onToggleRepeatMode: () -> Unit,
    onDismiss: () -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onMoveQueueItem: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onRemoveQueue: (Song) -> Unit,
    onOpenQueueClick: () -> Unit = {}
) {

    BackHandler {
        onDismiss()
    }

    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderPositionMs by remember { mutableFloatStateOf(0f) }
    val currentSliderValue = if (isDraggingSlider) sliderPositionMs else currentPositionMs.toFloat()

    var showQueueBottomSheet by remember { mutableStateOf(false) }


    val (repeatIcon, repeatTint, modelLabel) = when (repeatMode) {
        RepeatMode.OFF -> Triple(
            Icons.Default.Repeat,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Repetición desactivada"
        )

        RepeatMode.ALL -> Triple(
            Icons.Default.Repeat,
            MaterialTheme.colorScheme.primary,
            "Repetir cola de reproducción"
        )

        RepeatMode.ONE -> Triple(
            Icons.Default.RepeatOne,
            MaterialTheme.colorScheme.primary,
            "Repetir canción"
        )

        RepeatMode.STOP_AFTER_CURRENT -> Triple(
            Icons.Default.Stop,
            MaterialTheme.colorScheme.primary,
            "Detener playlist al finalizar canción"
        )
    }

    val targetSongIndex = remember(song.id, songs) {
        songs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = targetSongIndex, pageCount = { songs.size })

    LaunchedEffect(targetSongIndex) {
        if (pagerState.currentPage != targetSongIndex) {
            pagerState.scrollToPage(targetSongIndex)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (songs.isNotEmpty() && pagerState.settledPage in songs.indices) {
            val selectedSongFromPager = songs[pagerState.settledPage]
            if (selectedSongFromPager.id != song.id) {
                onSongSelected(selectedSongFromPager)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            //Header del reproductor
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Salir del player",
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Reproducciendo desde: ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = song.album.ifEmpty { "Álbum desconocido" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }

                IconButton(onClick = { /* Menú de opciones*/ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opciones",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            //


            //Carrusel del reproductor/Vista

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->

                val currentPagerSong = songs.getOrNull(page)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .graphicsLayer {

                            val pageOffset = (pagerState.currentPage - page + pagerState.currentPageOffsetFraction).absoluteValue
                            val scale = (1f - (pageOffset * 0.10f)).coerceIn(0.90f, 1f)

                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            if (currentPagerSong?.albumArtUri != null) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        ),
                    contentAlignment = Alignment.Center

                ) {
                    if (currentPagerSong?.albumArtUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(currentPagerSong.albumArtUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Portada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(120.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            //


            // Slider y botones
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    //Botón de anterior actualizado
                    IconButton(
                        onClick = {
                            if (currentPositionMs > 5000L) {
                                onSeekTo(0L)
                            } else {
                                onPreviousClick()
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = onNextClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = onToggleRepeatMode) {
                        Icon(
                            imageVector = repeatIcon,
                            contentDescription = modelLabel,
                            tint = repeatTint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(vertical = 12.dp))

            // Panel inferior //
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    //Titulos. (nombre de la canción, artistas, etc)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { onToggleFavorite(song) }) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee()
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(onClick = {
                            showQueueBottomSheet = true
                            onOpenQueueClick()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Cola de Reproducción",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    //Slider del tiempo + timestamps

                    val sliderInteractionSource = remember { MutableInteractionSource() }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Slider(
                            value = currentSliderValue.coerceIn(0f, song.duration.toFloat()),
                            onValueChange = {
                                isDraggingSlider = true
                                sliderPositionMs = it
                            },
                            onValueChangeFinished = {
                                isDraggingSlider = false
                                onSeekTo(sliderPositionMs.toLong())
                            },
                            valueRange = 0f..(if (song.duration > 0) song.duration.toFloat() else 1f),
                            interactionSource = sliderInteractionSource,
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                )
                            },
                            track = { sliderState ->
                                SliderDefaults.Track(
                                    sliderState = sliderState,
                                    modifier = Modifier.height(4.dp),
                                    colors = SliderDefaults.colors(
                                        activeTickColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                    )
                                )
                            }
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentSliderValue.toLong()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatTime(song.duration),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    if (showQueueBottomSheet){
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val queueLazyListState = rememberLazyListState()

        var draggedIndex by remember { mutableStateOf<Int?>(null) }
        var draggingOffset by remember { mutableFloatStateOf(0f) }

        val stopNestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource
                ): Offset {
                    return available
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = {showQueueBottomSheet = false},
            sheetState = sheetState,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ){
                    IconButton(
                        onClick = {showQueueBottomSheet = false},
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cerrar Cola",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cola de reproducción",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = onToggleShuffle) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Aleatorio",
                                tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = onToggleRepeatMode) {
                            Icon(
                                imageVector = repeatIcon,
                                contentDescription = modelLabel,
                                tint = repeatTint
                            )
                        }

                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "opciones",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                LazyColumn(
                    state = queueLazyListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .nestedScroll(stopNestedScrollConnection),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(
                        items = songs,
                        key = {_, songItem -> songItem.id}
                    ){index, queueSong ->
                        val isCurrent = queueSong.id == song.id
                        val isBeingDragged = draggedIndex == index

                        val currentIndex by rememberUpdatedState(index)
                        val  currentLastIndex  by rememberUpdatedState(songs.lastIndex)
                        val currentOnMoveQueueItem by rememberUpdatedState(onMoveQueueItem)

                        val itemBackgroundColor = when {
                            isBeingDragged -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .zIndex(if (isBeingDragged) 2f else 0f)
                                .graphicsLayer{
                                    if (isBeingDragged){
                                        translationY = draggingOffset
                                        scaleX = 1.03f
                                        scaleY = 1.03f
                                    }
                                }
                                .shadow(
                                    elevation = if (isBeingDragged) 8.dp else 0.dp,
                                    shape = RoundedCornerShape(8.dp)
                                 )
                                .clip(RoundedCornerShape(10.dp))
                                .background(itemBackgroundColor)
                                .clickable{
                                    onSongSelected(queueSong)
                                    showQueueBottomSheet = false
                                }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 2.dp, end = 6.dp)
                                    .size(32.dp)
                                    .pointerInput(queueSong.id) {
                                        detectDragGestures(
                                            onDragStart = {
                                                draggedIndex = currentIndex
                                                draggingOffset = 0f
                                            },
                                            onDragEnd = {
                                                draggedIndex = null
                                                draggingOffset = 0f
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                draggingOffset = 0f
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                draggedIndex?.let { activeIdx ->
                                                    draggingOffset += dragAmount.y
                                                    val rowHeightPx = 58.dp.toPx()
                                                    val  steps = (draggingOffset / rowHeightPx).toInt()

                                                    if (steps != 0) {
                                                    val targetIndex = (activeIdx + steps).coerceIn(0, currentLastIndex)
                                                    if (targetIndex != activeIdx) {
                                                        val indexDiff = targetIndex - activeIdx
                                                        currentOnMoveQueueItem(activeIdx, targetIndex)
                                                        draggedIndex = targetIndex
                                                        draggingOffset -= indexDiff * rowHeightPx
                                                    }
                                                }
                                                }


                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ){
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Reordenar pista",
                                    tint = if (isBeingDragged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .padding( end = 8.dp)
                                        .size(20.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (queueSong.albumArtUri != null) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (queueSong.albumArtUri != null) {
                                    AsyncImage(
                                        model = queueSong.albumArtUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Text(
                                    text = queueSong.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val artistText = if (queueSong.artist.isBlank() || queueSong.artist.equals("<unknown>", ignoreCase = true)){
                                    "Artista desconocido"
                                }else{
                                    queueSong.artist
                                }

                                Text(
                                    text = artistText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isCurrent){
                                PlayingEqualizerIcon(
                                    color = MaterialTheme.colorScheme.primary,
                                    isPlaying = isPlaying,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                            }

                            IconButton(
                                onClick = {onRemoveQueue(queueSong)},
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.RemoveCircleOutline,
                                    contentDescription = "Quitar de la cola",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = {/**/},
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Opciones",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


private fun formatTime(ms: Long): String{
    val  minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return String.format(java.util.Locale.getDefault(),"%02d:%02d", minutes, seconds)
}