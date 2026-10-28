package com.example.creproductor.gui

import android.webkit.CookieManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.YtItem
import com.example.creproductor.data.YtLibrary
import com.example.creproductor.data.YtMusic
import com.example.creproductor.data.YtTrack
import com.example.creproductor.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pestaña "YT Music": buscador, el inicio de YouTube Music (estantes y estados de ánimo) y, con sesión, tus "Me gusta". Todo es opcional: mientras
 * YouTube Music esté apagado solo explica qué es y ofrece activarlo. La reproducción pasa por el mismo reproductor.
 */
@Composable
fun YtMusicScreen(onPlay: (YtTrack) -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(YtMusic.isEnabled(context)) }
    var loggedIn by remember { mutableStateOf(YtLibrary.isLoggedIn(context)) }
    var reload by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") }

    var selectedItemForDetail by remember { mutableStateOf<YtItem?>(null) }
    var selectedTrackForMenu by remember { mutableStateOf<YtTrack?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    var searchHistory by remember {
        mutableStateOf(listOf("The Finals OST", "Carpenter Brut", "Sonic CD JP", "BLU-SWING"))
    }

    if (!enabled) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
            Text(tr("YouTube Music"), style = MaterialTheme.typography.headlineSmall)
            Text(
                tr("Función no oficial (NewPipeExtractor): puede dejar de funcionar sin aviso y va contra los términos de YouTube. Usa internet y no necesita cuenta"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp)
            )
            Button(onClick = { YtMusic.setEnabled(context, true); enabled = true }) { Text(tr("Activar YouTube Music")) }
        }
        return
    }
    // Refresca al volver de iniciar sesión; y mientras no haya sesión, va cargando la página de Google por adelantado.
    LaunchedEffect(YtLogin.authVersion) {
        if (YtLogin.authVersion > 0) { loggedIn = YtLibrary.isLoggedIn(context); reload++ }
    }
    // La página de Google se precarga (inicio de sesión al instante) solo en equipos con margen: un WebView cuesta
    // ~65 MB de RAM, y en uno de 2 GB eso es lo que más pesa de toda la app. Al salir de la pestaña se suelta.
    val preload = remember { canPreloadWebView(context) }
    LaunchedEffect(loggedIn) { YtLogin.warm = !loggedIn && preload }
    DisposableEffect(Unit) { onDispose { YtLogin.warm = false } }

    var searching by remember { mutableStateOf(false) }

    val activeItem = selectedItemForDetail
    if (activeItem != null) {
        BackHandler { selectedItemForDetail = null }
        val isArtist = activeItem.browseId?.startsWith("UC") == true ||
                activeItem.browseId?.startsWith("FEmusic_artist") == true ||
                activeItem.subtitle.contains("Artista", ignoreCase = true) ||
                activeItem.subtitle.contains("Artist", ignoreCase = true)

        if (isArtist) {
            YtArtistScreen(
                item = activeItem,
                onBackClick = { selectedItemForDetail = null },
                onPlayTrack = onPlay,
                onOpenItem = { newItem -> selectedItemForDetail = newItem },
                onTrackOptionsClick = { selectedTrackForMenu = it }
            )
        } else {
            YtAlbumScreen(
                item = activeItem,
                onBackClick = { selectedItemForDetail = null },
                onPlayTrack = onPlay,
                onTrackOptionsClick = { selectedTrackForMenu = it }
            )
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            // Cabecera de la interfaz: título, lupa y sesión.
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(tr("YT Music"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { searching = !searching; if (!searching) query = "" }) {
                    Icon(if (searching) Icons.Default.Close else Icons.Default.Search, contentDescription = tr("Buscar en YouTube Music"))
                }
                OutlinedButton(
                    onClick = {
                        if (loggedIn) { YtLibrary.logout(context); CookieManager.getInstance().removeAllCookies(null); loggedIn = false }
                        else YtLogin.open = true
                    },
                    shape = MaterialTheme.shapes.small,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) { Text(tr(if (loggedIn) "Cerrar sesión" else "Iniciar sesión"), style = MaterialTheme.typography.labelMedium) }
            }

            if (searching) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text(tr("Buscar en YouTube Music")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = tr("Limpiar")) } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (query.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Todos", "Canciones", "Artistas", "Álbumes").forEach { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                label = { Text(tr(filter)) }
                            )
                        }
                    }
                }

                if (query.isBlank()) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Text(
                                tr("Búsquedas recientes"),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(searchHistory, key = { "hist_$it" }) { item ->
                            ListItem(
                                headlineContent = { Text(item) },
                                leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { query = item }
                            )
                        }
                    }
                } else {
                    YtMusicResults(
                        query = query,
                        filter = selectedFilter,
                        onPlay = onPlay,
                        onTrackOptionsClick = { selectedTrackForMenu = it }
                    )
                }
            } else {
                YtHomeContent(
                    loggedIn = loggedIn,
                    reload = reload,
                    onPlay = onPlay,
                    onLogin = { YtLogin.open = true },
                    onOpenItem = { item -> selectedItemForDetail = item },
                    onTrackOptionsClick = { selectedTrackForMenu = it },
                    onCreatePlaylistClick = { showCreatePlaylistDialog = true }
                )
            }
        }
    }

    selectedTrackForMenu?.let { track ->
        YtTrackMenuSheet(
            track = track,
            onDismiss = { selectedTrackForMenu = null },
            onPlay = onPlay,
            onGoToArtist = { artistName ->
                selectedItemForDetail = YtItem(
                    title = artistName,
                    subtitle = "Artista",
                    thumbnail = track.thumbnail,
                    track = null,
                    browseId = "FEmusic_artist"
                )
            },
            onGoToAlbum = { currentTrack ->
                selectedItemForDetail = YtItem(
                    title = currentTrack.title,
                    subtitle = "Álbum • ${currentTrack.artist}",
                    thumbnail = currentTrack.thumbnail,
                    track = null,
                    browseId = "MPRE"
                )
            },
            onAddToPlaylist = {
                showCreatePlaylistDialog = true
            }
        )
    }

    if (showCreatePlaylistDialog) {
        YtCreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { _ ->
                showCreatePlaylistDialog = false
            }
        )
    }
}

@Composable
fun YtMusicResults(
    query: String,
    filter: String = "Todos",
    onPlay: (YtTrack) -> Unit,
    onTrackOptionsClick: (YtTrack) -> Unit = {}
) {
    val results by produceState<Result<List<YtTrack>>?>(null, query) {
        value = null
        if (query.isNotBlank()) {
            value = withContext(Dispatchers.IO) {
                runCatching { YtMusic.search(query) }
            }
        }
    }

    val rawList = results?.getOrNull().orEmpty()
    val list = remember(rawList, filter) {
        when (filter) {
            "Canciones" -> rawList.filter { it.title.isNotBlank() }
            "Artistas" -> rawList.filter { it.artist.contains(query, ignoreCase = true) }
            else -> rawList
        }
    }

    when {
        results == null -> {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        list.isEmpty() -> {
            Text(
                text = tr("No se encontraron resultados para \"$query\""),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp)
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(list, key = { "yt_res_" + it.url }) { track ->
                    ListItem(
                        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingContent = {
                            AsyncImage(
                                model = track.thumbnail,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onTrackOptionsClick(track) }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onPlay(track) }
                    )
                }
            }
        }
    }
}