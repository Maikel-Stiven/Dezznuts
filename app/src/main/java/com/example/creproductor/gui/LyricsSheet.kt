package com.example.creproductor.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.Lrc
import com.example.creproductor.data.Lyrics
import com.example.creproductor.data.LyricsRepository
import com.example.creproductor.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

private sealed interface LyricsState {
    data object Loading : LyricsState
    data class Found(val lyrics: Lyrics) : LyricsState
    data object Missing : LyricsState
    data object Searching : LyricsState
    data object NotOnline : LyricsState
    data object Offline : LyricsState
}

/**
 * Letras de la canción. Primero se busca en local (guardada, incrustada, `.lrc`); si no hay, se ofrece buscar en
 * internet (LRCLIB), y solo se hace al pulsar el botón. Sincronizada: la línea actual se resalta y se centra sola,
 * y tocar una línea salta a ese momento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(song: Song, currentPositionMs: () -> Long, onSeekTo: (Long) -> Unit, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var state by remember(song.id) { mutableStateOf<LyricsState>(LyricsState.Loading) }
    var searchNonce by remember(song.id) { mutableStateOf(0) }

    LaunchedEffect(song.id) {
        state = withContext(Dispatchers.IO) { LyricsRepository.local(context, song) }
            ?.let { LyricsState.Found(it) } ?: LyricsState.Missing
    }
    LaunchedEffect(searchNonce) {
        if (searchNonce == 0) return@LaunchedEffect
        state = LyricsState.Searching
        state = withContext(Dispatchers.IO) {
            try { LyricsRepository.online(context, song)?.let { LyricsState.Found(it) } ?: LyricsState.NotOnline }
            catch (_: IOException) { LyricsState.Offline }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().height(520.dp).padding(horizontal = 20.dp)) {
            Text(song.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            when (val s = state) {
                LyricsState.Loading, LyricsState.Searching -> Centered {
                    CircularProgressIndicator()
                    Text(if (s == LyricsState.Searching) "Buscando en LRCLIB…" else "Cargando…", modifier = Modifier.padding(top = 12.dp))
                }
                is LyricsState.Found -> LyricsList(s.lyrics, currentPositionMs, onSeekTo)
                else -> Centered {
                    Text(
                        when (s) {
                            LyricsState.Offline -> "Sin conexión. Inténtalo de nuevo con internet."
                            LyricsState.NotOnline -> "LRCLIB no tiene la letra de esta canción."
                            else -> "No hay letra guardada para esta canción."
                        },
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { searchNonce++ }, modifier = Modifier.padding(top = 16.dp)) { Text("Buscar en internet (LRCLIB)") }
                    Text(
                        "Se enviará a lrclib.net el título, artista, álbum y duración. Se guarda para usarla sin conexión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { content() }
}

@Composable
private fun LyricsList(lyrics: Lyrics, currentPositionMs: () -> Long, onSeekTo: (Long) -> Unit) {
    val listState = rememberLazyListState()
    // La lectura de la posición ocurre dentro de derivedStateOf: solo se recompone cuando cambia la línea, no cada tick.
    val active by remember(lyrics) { derivedStateOf { if (lyrics.synced) Lrc.activeIndex(lyrics.lines, currentPositionMs()) else -1 } }
    LaunchedEffect(active) { if (active >= 0) listState.animateScrollToItem(active) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 120.dp, bottom = 240.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        itemsIndexed(lyrics.lines) { i, line ->
            val current = i == active
            Text(
                text = line.text.ifEmpty { "♪" },
                style = if (lyrics.synced) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (!lyrics.synced || current) 1f else 0.4f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().let { if (lyrics.synced) it.clickable { onSeekTo(line.timeMs) } else it }
            )
        }
    }
}
