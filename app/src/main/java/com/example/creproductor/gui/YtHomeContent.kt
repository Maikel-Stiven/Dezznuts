package com.example.creproductor.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.YtHome
import com.example.creproductor.data.YtHomeFeed
import com.example.creproductor.data.YtItem
import com.example.creproductor.data.YtMood
import com.example.creproductor.data.YtTrack
import com.example.creproductor.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Inicio de la pestaña YT Music: estados de ánimo, "Me gusta" (con sesión) y los estantes de YouTube Music. Un álbum o
 * lista se abre en una hoja con sus canciones; una canción suena al tocarla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YtHomeContent(
    loggedIn: Boolean, reload: Int, onPlay: (YtTrack) -> Unit, onLogin: () -> Unit, onLogout: () -> Unit
) {
    val context = LocalContext.current
    var mood by remember { mutableStateOf<YtMood?>(null) }
    var showLiked by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var opened by remember { mutableStateOf<YtItem?>(null) }

    // null = cargando; el estado de ánimo elegido se vuelve a pedir, el resto sale de la caché de 10 min.
    val result by produceState<Result<YtHomeFeed>?>(null, mood, loggedIn, reload, retry) {
        value = null
        value = withContext(Dispatchers.IO) { runCatching { YtHome.load(context, mood?.params, force = retry > 0 && mood == null) } }
    }
    // Los estados de ánimo cambian al elegir uno; se conservan los del inicio general para poder volver.
    var allMoods by remember { mutableStateOf<List<YtMood>>(emptyList()) }
    result?.getOrNull()?.moods?.takeIf { mood == null && it.isNotEmpty() }?.let { allMoods = it }

    if (showLiked) {
        Column(Modifier.fillMaxSize()) {
            TextButton(onClick = { showLiked = false }, modifier = Modifier.padding(start = 8.dp)) { Text("← " + tr("Inicio")) }
            YtLikedList(reload, onPlay)
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loggedIn) FilterChip(selected = false, onClick = { showLiked = true }, label = { Text(tr("Me gusta")) })
                allMoods.forEach { m ->
                    FilterChip(selected = mood == m, onClick = { mood = if (mood == m) null else m }, label = { Text(m.label) })
                }
            }
        }
        if (!loggedIn) item {
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(tr("Tus Me gusta, a mano"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            tr("Inicia sesión para ver tus canciones con Me gusta. La sesión se guarda solo en este teléfono y puedes cerrarla cuando quieras."),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Button(onClick = onLogin, modifier = Modifier.padding(start = 12.dp)) { Text(tr("Iniciar sesión")) }
                }
            }
        }
        val feed = result?.getOrNull()
        when {
            result == null -> item { Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            feed == null -> item {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(tr("No se pudo consultar YouTube Music. Revisa la conexión o inténtalo más tarde."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { retry++ }) { Text(tr("Reintentar")) }
                }
            }
            else -> {
                // Accesos directos (orden de la pantalla de Maikel): "Me gusta" y las primeras canciones del inicio.
                val quick = feed.shelves.firstOrNull()?.items?.mapNotNull { it.track }.orEmpty()
                if (quick.isNotEmpty()) item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Accesos directos"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Button(onClick = { onPlay(quick.first()) }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) { Text(tr("Reproducir todo")) }
                    }
                    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val likedLabel = tr("Música que te gustó")
                        val cells = listOf(Triple(likedLabel, null as String?, { if (loggedIn) showLiked = true else onLogin() })) +
                            quick.take(5).map { t -> Triple(t.title, t.thumbnail, { onPlay(t) }) }
                        cells.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (title, thumb, act) -> ShortcutTile(Modifier.weight(1f), title, thumb, act) }
                                repeat(3 - row.size) { Box(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
                items(feed.shelves, key = { it.title }) { shelf ->
                    Text(shelf.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(shelf.items, key = { it.track?.url ?: it.browseId ?: it.title }) { item ->
                            YtItemCard(item) { item.track?.let(onPlay) ?: run { opened = item } }
                        }
                    }
                }
                if (loggedIn) item { TextButton(onClick = onLogout, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) { Text(tr("Cerrar sesión")) } }
            }
        }
    }

    opened?.let { item ->
        ModalBottomSheet(onDismissRequest = { opened = null }) {
            val tracks by produceState<Result<List<YtTrack>>?>(null, item) {
                value = withContext(Dispatchers.IO) { runCatching { YtHome.tracksOf(context, item) } }
            }
            Text(item.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            val list = tracks?.getOrNull()
            when {
                tracks == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                list.isNullOrEmpty() -> Text(tr("No se pudo cargar."), modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(list, key = { it.url }) { t ->
                        ListItem(
                            headlineContent = { Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = { Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingContent = { AsyncImage(t.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(MaterialTheme.shapes.small)) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { opened = null; onPlay(t) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun YtItemCard(item: YtItem, onClick: () -> Unit) {
    Column(Modifier.width(140.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)) {
        AsyncImage(
            model = item.thumbnail, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(140.dp).clip(MaterialTheme.shapes.medium)
        )
        Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        if (item.subtitle.isNotBlank()) Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Atajo cuadrado del inicio: portada (o el pulgar de "Me gusta") y título. */
@Composable
private fun ShortcutTile(modifier: Modifier, title: String, thumbnail: String?, onClick: () -> Unit) {
    Surface(modifier.height(124.dp).clip(MaterialTheme.shapes.medium).clickable(onClick = onClick), color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(8.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (thumbnail != null) AsyncImage(thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small))
            else androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.ThumbUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp).padding(12.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
