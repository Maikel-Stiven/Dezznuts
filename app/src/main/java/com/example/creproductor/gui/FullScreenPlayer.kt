package com.example.creproductor.gui

import androidx.activity.compose.BackHandler
import com.example.creproductor.data.coverModel
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.Tune
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
import kotlinx.coroutines.Dispatchers

/**
 * Reproductor a pantalla completa, superpuesto sobre el resto de la app.
 *
 * Reúne el carrusel de carátulas, la barra de progreso con *seek*, los controles
 * de reproducción, los modos aleatorio/repetición y la cola deslizable.
 *
 * @param song canción que está sonando en este momento.
 * @param songs lista usada por el carrusel (normalmente la cola o la biblioteca).
 * @param isShuffleEnabled estado del modo aleatorio, controlado por el ViewModel.
 * @param onToggleShuffle alterna el modo aleatorio en el ViewModel.
 * @param onMoveQueueItem reordena la cola (índice origen → índice destino).
 */
@OptIn(ExperimentalMaterial3Api :: class)
@Composable
fun FullScreenPlayer(
    song: Song,
    songs: List<Song>,
    isPlaying: Boolean,
    currentPositionMs: () -> Long,
    repeatMode: RepeatMode = RepeatMode.OFF,
    isShuffleEnabled: Boolean = false,
    audioInfo: String = "",
    onOpenEqualizer: () -> Unit = {},
    onEditSong: (Song) -> Unit = {},
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

    var showQueueBottomSheet by remember { mutableStateOf(false) }
    var showTrackInfo by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }

    var lastClickTime by remember { mutableStateOf(0L) }
    fun safeClick(action: () -> Unit){
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > 300){
            lastClickTime = currentTime
            action()
        }

    }

    val (repeatIcon, repeatTint, modelLabel) = when (repeatMode) {
        RepeatMode.OFF -> Triple(
            Icons.Default.Repeat,
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
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

    // Deslizar hacia abajo cierra el reproductor (si no se suelta lo bastante abajo, vuelve a su sitio).
    var dragY by remember { mutableFloatStateOf(0f) }
    val dismissPx = with(LocalDensity.current) { 100.dp.toPx() }
    val dragState = rememberDraggableState { delta -> dragY = (dragY + delta).coerceAtLeast(0f) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            // El arrastre va por fuera de la capa que se desplaza: si no, el dedo y la capa se mueven juntos y el delta es 0.
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (dragY > dismissPx || velocity > 2500f) onDismiss()
                    else animate(dragY, 0f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { v, _ -> dragY = v }
                }
            )
            .graphicsLayer { translationY = dragY },
        color = MaterialTheme.colorScheme.background
    ) {
        Box(Modifier.fillMaxSize()) {
        CoverBackdrop(song, isPlaying)
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Con targetSdk 37 la app es edge-to-edge: este padding evita que el
                // header quede bajo la barra de estado o el cutout, y el pie bajo la barra de gestos.
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Encabezado: cerrar · de dónde suena · información técnica
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cerrar reproductor",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "REPRODUCIENDO DESDE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                    Text(
                        text = song.album.takeUnless { it.isBlank() || it.equals("<unknown>", true) } ?: "Álbum desconocido",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
                IconButton(onClick = { showTrackInfo = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Información técnica"
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            PlayerTabs(showLyrics) { showLyrics = it }

            if (showLyrics) LyricsPane(
                song = song,
                currentPositionMs = currentPositionMs,
                onSeekTo = onSeekTo,
                modifier = Modifier.weight(1f)
            ) else HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                pageSpacing = 16.dp,
                verticalAlignment = Alignment.CenterVertically,
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
                            val scale = (1f - (pageOffset * 0.12f)).coerceIn(0.88f, 1f)
                            scaleX = scale
                            scaleY = scale
                        }
                        .shadow(24.dp, MaterialTheme.shapes.large)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentPagerSong?.coverModel != null) {
                        val context = LocalContext.current
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentPagerSong.coverModel)
                                .fetcherDispatcher(Dispatchers.IO)
                                .decoderDispatcher(Dispatchers.IO)
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
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                    }
                    // Calidad y formato sobre la portada.
                    if (page == pagerState.currentPage) {
                        CoverChips(song, audioInfo, Modifier.align(Alignment.BottomStart).padding(12.dp))
                    }
                }
            }

            // Título y artista a la izquierda, favorito a la derecha
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Text(
                        text = song.artist.takeUnless { it.isBlank() || it.equals("<unknown>", true) } ?: "Artista desconocido",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { onToggleFavorite(song) }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (song.isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                        tint = if (song.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            PlayerSeekBar(positionMs = currentPositionMs, durationMs = song.duration, onSeekTo = onSeekTo)

            // Controles
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = if (isShuffleEnabled) "Desactivar aleatorio" else "Activar aleatorio",
                        tint = if (isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = {
                        safeClick {
                            if (currentPositionMs() > 5000L) onSeekTo(0L) else onPreviousClick()
                        }
                    },
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", modifier = Modifier.size(40.dp))
                }
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { safeClick { onNextClick() } }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = onToggleRepeatMode) {
                    Icon(imageVector = repeatIcon, contentDescription = modelLabel, tint = repeatTint)
                }
            }

            // Acciones secundarias
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onOpenEqualizer) {
                    Icon(Icons.Default.Tune, contentDescription = "Ecualizador", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                IconButton(onClick = {
                    showQueueBottomSheet = true
                    onOpenQueueClick()
                }) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Cola de reproducción", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
        }
    }
    if (showTrackInfo) TrackInfoDialog(song, audioInfo, onEdit = { onEditSong(song) }) { showTrackInfo = false }
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
                                    shape = MaterialTheme.shapes.small
                                 )
                                .clip(MaterialTheme.shapes.small)
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
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(
                                        if (queueSong.albumArtUri != null) MaterialTheme.colorScheme.surfaceVariant
                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (queueSong.albumArtUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(queueSong.coverModel)
                                            .fetcherDispatcher(Dispatchers.IO)
                                            .decoderDispatcher(Dispatchers.IO)
                                            .crossfade(true)
                                            .build(),
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
                                onClick = { onEditSong(queueSong) },
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

/**
 * Barra de progreso + tiempos. Es el **único** lugar que lee la posición (que cambia cada
 * 500 ms): al recibirla como lambda, solo esta función se recompone y no todo el reproductor
 * (carrusel, cabecera, botones).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerSeekBar(positionMs: () -> Long, durationMs: Long, onSeekTo: (Long) -> Unit) {
    var isDragging by remember { mutableStateOf(false) }
    var dragMs by remember { mutableFloatStateOf(0f) }
    val value = (if (isDragging) dragMs else positionMs().toFloat()).coerceIn(0f, durationMs.toFloat())

    val sliderInteractionSource = remember { MutableInteractionSource() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = value,
            onValueChange = {
                isDragging = true
                dragMs = it
            },
            onValueChangeFinished = {
                isDragging = false
                onSeekTo(dragMs.toLong())
            },
            valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 1f),
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
                    modifier = Modifier.height(5.dp),
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
                text = formatTime(value.toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatTime(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/**
 * Fondo "cristal" del reproductor: capas de la carátula desenfocadas, más saturadas, que flotan y giran despacio.
 *
 * Coste (medido en emulador, ver docs/OPTIMIZACION.md): cada capa a pantalla completa y cada fotograma cuestan GPU y
 * CPU de dibujo, así que:
 * - Desenfoque: la imagen se pide a 24×24 px y se estira (el suavizado bilineal hace de blur); la saturación se
 *   hornea UNA vez en ese bitmap ([SaturateTransformation]) en vez de aplicar un filtro de color a cada capa y fotograma.
 * - Dos capas (no tres) con escala, desplazamiento y giro distintos, movidas por un solo valor leído en `graphicsLayer`
 *   (fase de dibujo: sin recomposición).
 * - El valor se actualiza ~15 veces por segundo, no 60: el movimiento es lentísimo (28 s por ciclo) y entre
 *   actualizaciones no se dibuja nada.
 * - Solo se anima mientras suena, y no en equipos de poca RAM o de 2 núcleos, con ahorro de batería ni con las
 *   animaciones del sistema desactivadas.
 */
@Composable
private fun CoverBackdrop(song: Song, playing: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val painter = coil.compose.rememberAsyncImagePainter(
        remember(song.id) { ImageRequest.Builder(context).data(song.coverModel).size(24).transformations(SaturateTransformation(1.35f)).build() }
    )
    val canAnimate = rememberVisualEffectsEnabled()
    val t = remember { androidx.compose.runtime.mutableFloatStateOf(0.5f) }
    LaunchedEffect(canAnimate && playing) {
        if (!(canAnimate && playing)) return@LaunchedEffect
        val start = androidx.compose.runtime.withFrameNanos { it }
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                val phase = ((now - start) / 1_000_000_000f) % 56f / 28f      // 0..2
                t.floatValue = if (phase <= 1f) phase else 2f - phase          // ida y vuelta
            }
            kotlinx.coroutines.delay(55)
        }
    }

    val bg = MaterialTheme.colorScheme.background
    fun lerp(a: Float, b: Float, x: Float) = a + (b - a) * x

    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.Image(
            painter, null, Modifier.fillMaxSize().graphicsLayer {
                val x = t.floatValue
                scaleX = 2.0f; scaleY = 2.0f
                translationX = size.width * lerp(-0.12f, 0.12f, x); translationY = size.height * lerp(0.05f, -0.05f, x)
                rotationZ = lerp(-8f, 8f, x)
            },
            contentScale = ContentScale.Crop
        )
        androidx.compose.foundation.Image(
            painter, null, Modifier.fillMaxSize().graphicsLayer {
                val x = t.floatValue
                scaleX = 2.6f; scaleY = 2.6f; alpha = 0.55f
                translationX = size.width * lerp(0.14f, -0.14f, x); translationY = size.height * lerp(-0.08f, 0.08f, x)
                rotationZ = lerp(14f, -14f, x)
            },
            contentScale = ContentScale.Crop, alignment = Alignment.TopEnd
        )
        // Oscurece para que texto y controles se lean con cualquier carátula (más fuerte abajo, donde van los controles).
        Box(
            Modifier.fillMaxSize().background(
                androidx.compose.ui.graphics.Brush.verticalGradient(listOf(bg.copy(alpha = 0.22f), bg.copy(alpha = 0.6f)))
            )
        )
    }
}

/** Sube la saturación de un bitmap pequeño una sola vez (Coil lo cachea ya transformado). */
private class SaturateTransformation(private val amount: Float) : coil.transform.Transformation {
    override val cacheKey = "saturate-$amount"
    override suspend fun transform(input: android.graphics.Bitmap, size: coil.size.Size): android.graphics.Bitmap {
        val out = android.graphics.Bitmap.createBitmap(input.width, input.height, android.graphics.Bitmap.Config.ARGB_8888)
        val paint = android.graphics.Paint().apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(amount) })
        }
        android.graphics.Canvas(out).drawBitmap(input, 0f, 0f, paint)
        return out
    }
}

/** Selector "Canción | Letra" bajo el encabezado: pastilla de cristal, como el de la referencia. */
@Composable
private fun PlayerTabs(showLyrics: Boolean, onSelect: (Boolean) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        listOf(false to "Canción", true to "Letra").forEach { (lyrics, label) ->
            val selected = showLyrics == lyrics
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable { onSelect(lyrics) }
                    .padding(horizontal = 22.dp, vertical = 7.dp)
            )
        }
    }
}

/**
 * Calidad y formato de la pista sobre la portada: "Hi-Res" / "Lossless", el códec con su profundidad y frecuencia
 * (o el bitrate si tiene pérdida) y, si aplica, Bit-perfect u Offload. El detalle completo sigue en "Información
 * técnica" (⋮).
 */
@Composable
private fun CoverChips(song: Song, audioInfo: String, modifier: Modifier = Modifier) {
    val parts = audioInfo.substringBefore(" → ").split(" · ").filter { it.isNotBlank() }
    val format = if (song.quality == com.example.creproductor.data.QualityTier.LOSSY)
        parts.filter { it == parts.firstOrNull() || it.endsWith("kbps") }
    else parts.filterNot { it.endsWith("kbps") }
    val route = audioInfo.substringAfter(" → ", "").let { r ->
        when {
            r.startsWith("Bit-perfect") -> "Bit-perfect"
            r.startsWith("Offload") -> "Offload"
            else -> ""
        }
    }
    val labels = listOfNotNull(
        when (song.quality) {
            com.example.creproductor.data.QualityTier.HIRES -> "Hi-Res"
            com.example.creproductor.data.QualityTier.LOSSLESS -> "Lossless"
            else -> null
        },
        format.joinToString(" · ").ifEmpty { null },
        route.ifEmpty { null }
    )
    if (labels.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { i, text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (i == 0 && song.quality != com.example.creproductor.data.QualityTier.LOSSY) FontWeight.Bold else FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            )
        }
    }
}

/**
 * Tema con el acento de la carátula: sustituye `primary` (botón de reproducir, progreso, favorito…) por el tono
 * dominante de la portada. Sin carátula o si es casi gris se queda el caramelo de la marca. La transición de color
 * solo se anima donde los efectos visuales están permitidos (ver [rememberVisualEffectsEnabled]).
 */
@Composable
fun CoverAccentTheme(song: Song, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val base = MaterialTheme.colorScheme
    var accent by remember { mutableStateOf<Color?>(null) }
    LaunchedEffect(song.coverModel) {
        accent = song.coverModel?.let { model ->
            kotlinx.coroutines.withContext(Dispatchers.IO) {
                val request = ImageRequest.Builder(context).data(model).size(24).allowHardware(false).build()
                val bitmap = (coil.Coil.imageLoader(context).execute(request).drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                bitmap?.let {
                    val px = IntArray(it.width * it.height)
                    it.getPixels(px, 0, it.width, 0, 0, it.width, it.height)
                    com.example.creproductor.data.CoverAccent.fromPixels(px)?.let(::Color)
                }
            }
        }
    }
    val target = accent ?: base.primary
    val color = if (rememberVisualEffectsEnabled())
        androidx.compose.animation.animateColorAsState(target, androidx.compose.animation.core.tween(500), label = "acento").value
    else target
    MaterialTheme(
        colorScheme = base.copy(
            primary = color,
            onPrimary = Color(0xFF14110F),
            primaryContainer = androidx.compose.ui.graphics.lerp(color, Color.Black, 0.6f)
        ),
        content = content
    )
}
