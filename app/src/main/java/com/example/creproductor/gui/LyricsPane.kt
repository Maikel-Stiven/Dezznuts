package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.Lrc
import com.example.creproductor.data.Lyrics
import com.example.creproductor.data.LyricsRepository
import com.example.creproductor.data.Song
import com.example.creproductor.data.isYoutube
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface LyricsState {
    data object Loading : LyricsState
    data class Found(val lyrics: Lyrics) : LyricsState
    data object Missing : LyricsState
    data object Searching : LyricsState
    data object NotOnline : LyricsState
    data object Offline : LyricsState
}

/**
 * Letras dentro del reproductor (en el lugar de la carátula, como en YT Music o Spotify), sobre el mismo fondo de
 * color: no es una ventana emergente. Primero se busca en local (guardada, incrustada, `.lrc`); si no hay, se ofrece
 * buscar en internet (LRCLIB) y solo se hace al pulsar el botón. Sincronizada: la línea actual se resalta y sube
 * sola, y tocar una línea salta a ese momento.
 */
@Composable
fun LyricsPane(song: Song, currentPositionMs: () -> Long, onSeekTo: (Long) -> Unit, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var state by remember(song.id) { mutableStateOf<LyricsState>(LyricsState.Loading) }
    var searchNonce by remember(song.id) { mutableStateOf(0) }

    LaunchedEffect(song.id) {
        state = withContext(Dispatchers.IO) { LyricsRepository.local(context, song) }
            ?.let { LyricsState.Found(it) } ?: LyricsState.Missing
        // Una canción de YouTube Music ya usa internet: la letra se busca sola, sin botón.
        if (state == LyricsState.Missing && song.isYoutube) searchNonce++
    }
    LaunchedEffect(searchNonce) {
        if (searchNonce == 0) return@LaunchedEffect
        state = LyricsState.Searching
        state = withContext(Dispatchers.IO) {
            try { LyricsRepository.online(context, song)?.let { LyricsState.Found(it) } ?: LyricsState.NotOnline }
            catch (_: IOException) { LyricsState.Offline }
        }
    }

    Box(modifier.fillMaxWidth()) {
        when (val s = state) {
            LyricsState.Loading, LyricsState.Searching -> Centered {
                CircularProgressIndicator()
                Text(tr(if (s == LyricsState.Searching) "Buscando en LRCLIB…" else "Cargando…"), modifier = Modifier.padding(top = 12.dp))
            }
            is LyricsState.Found -> LyricsList(s.lyrics, currentPositionMs, onSeekTo)
            else -> Centered {
                Text(
                    tr(when (s) {
                        LyricsState.Offline -> "Sin conexión. Inténtalo de nuevo con internet."
                        LyricsState.NotOnline -> "LRCLIB no tiene la letra de esta canción."
                        else -> "No hay letra guardada para esta canción."
                    }),
                    textAlign = TextAlign.Center
                )
                Button(onClick = { searchNonce++ }, modifier = Modifier.padding(top = 16.dp)) { Text(tr("Buscar en internet (LRCLIB)")) }
                Text(
                    tr("Se enviará a lrclib.net el título, artista, álbum y duración. Se guarda para usarla sin conexión."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
                )
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
    // Los efectos (suavizado de la línea activa, desplazamiento animado) siguen la regla única de la app: en equipos
    // modestos o con ahorro de batería la línea cambia al instante.
    val effects = rememberVisualEffectsEnabled()
    // La línea activa queda a un tercio de la altura (no pegada arriba) para ver también lo que viene.
    LaunchedEffect(active) {
        if (active < 0) return@LaunchedEffect
        val offset = -(listState.layoutInfo.viewportSize.height / 3)
        if (effects) listState.animateScrollToItem(active, offset) else listState.scrollToItem(active, offset)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            // Los bordes superior e inferior se desvanecen (como en Spotify) en vez de cortarse en seco.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, 0.12f to Color.Black, 0.85f to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            },
        contentPadding = PaddingValues(top = 32.dp, bottom = 120.dp, start = 12.dp, end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        itemsIndexed(lyrics.lines) { i, line ->
            val current = i == active
            // Cuanto más lejos de la línea activa, más tenue; la activa crece un poco y se aclara con suavidad.
            val targetAlpha = if (!lyrics.synced || current) 1f else if (kotlin.math.abs(i - active) == 1) 0.6f else 0.35f
            val targetScale = if (!lyrics.synced || current) 1f else 0.94f
            val alpha by if (effects) androidx.compose.animation.core.animateFloatAsState(targetAlpha, androidx.compose.animation.core.tween(300), label = "lyricAlpha")
                else androidx.compose.runtime.rememberUpdatedState(targetAlpha)
            val scale by if (effects) androidx.compose.animation.core.animateFloatAsState(targetScale, androidx.compose.animation.core.tween(300), label = "lyricScale")
                else androidx.compose.runtime.rememberUpdatedState(targetScale)
            val lineModifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale; scaleY = scale
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                }
                .let { if (lyrics.synced) it.clickable { onSeekTo(line.timeMs) } else it }
            // Sombra suave: se lee bien aunque la carátula (y por tanto el fondo) sea clara.
            val lineStyle = (if (lyrics.synced) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge)
                .copy(shadow = androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 10f))
            if (current && lyrics.synced && line.text.isNotEmpty()) {
                SingAlongLine(line.text, line.timeMs, lyrics.lines.getOrNull(i + 1)?.timeMs, currentPositionMs, effects, lineStyle, lineModifier)
            } else Text(
                text = line.text.ifEmpty { "♪" },
                style = lineStyle,
                fontWeight = if (current || !lyrics.synced) FontWeight.Bold else FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Start,
                modifier = lineModifier
            )
        }
    }
}

/**
 * Línea activa palabra por palabra, como el karaoke de Apple Music: lo ya cantado queda blanco y brillante (con un
 * halo suave), la palabra en curso se ilumina poco a poco y lo que falta sigue tenue. LRCLIB solo da el instante de
 * cada línea, no el de cada palabra: el reparto dentro de la línea se estima por la longitud de las palabras y se
 * deja un margen al final por si se canta antes de la línea siguiente. Sin efectos visuales ([smooth] falso) el
 * avance es por pasos pero igual de legible.
 */
@Composable
private fun SingAlongLine(
    text: String, startMs: Long, nextStartMs: Long?, currentPositionMs: () -> Long, smooth: Boolean,
    style: androidx.compose.ui.text.TextStyle, modifier: Modifier
) {
    val words = remember(text) { text.split(' ').filter { it.isNotEmpty() } }
    // Reparto del tiempo: cada palabra pesa su longitud (+1 por la pausa), acumulado en fracciones 0..1.
    val edges = remember(words) {
        val w = words.map { it.length + 1f }
        val total = w.sum()
        var acc = 0f
        listOf(0f) + w.map { acc += it; acc / total }
    }
    val sweepMs = remember(startMs, nextStartMs) { (((nextStartMs ?: (startMs + 4000)) - startMs).coerceIn(600, 9000) * 0.88f) }
    var pos by remember(startMs) { mutableStateOf(currentPositionMs()) }
    LaunchedEffect(startMs) {
        while (true) {
            // Fotograma a fotograma con efectos; en equipos modestos o con ahorro de batería, ocho veces por segundo.
            if (smooth) androidx.compose.runtime.withFrameNanos { } else kotlinx.coroutines.delay(125)
            pos = currentPositionMs()
        }
    }
    val f = ((pos - startMs) / sweepMs).coerceIn(0f, 1f)
    val glow = Color.White
    val dim = 0.42f
    val annotated = androidx.compose.ui.text.buildAnnotatedString {
        words.forEachIndexed { i, word ->
            val p = ((f - edges[i]) / (edges[i + 1] - edges[i])).coerceIn(0f, 1f)
            withStyle(
                androidx.compose.ui.text.SpanStyle(
                    color = Color.White.copy(alpha = dim + (1f - dim) * p),
                    shadow = androidx.compose.ui.graphics.Shadow(glow.copy(alpha = 0.6f * p), blurRadius = 26f * p + 2f)
                )
            ) { append(word) }
            if (i < words.lastIndex) append(" ")
        }
    }
    Text(text = annotated, style = style, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start, modifier = modifier)
}
