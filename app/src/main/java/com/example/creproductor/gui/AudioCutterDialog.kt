package com.example.creproductor.gui

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.Song
import com.example.creproductor.formatDuration
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

private const val MIN_CUT_MS = 1000L

/** Cortador de audio: se eligen inicio y fin con el control de rango, se previsualiza el tramo y se guarda aparte. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioCutterDialog(song: Song, cutting: Boolean, onCut: (startMs: Long, endMs: Long) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val total = song.duration.coerceAtLeast(MIN_CUT_MS * 2)
    var range by remember { mutableStateOf(0f..total.toFloat()) }
    var previewing by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }

    DisposableEffect(Unit) { onDispose { runCatching { player.release() } } }
    // Previsualización: se oye solo el tramo elegido y se detiene sola al llegar al final.
    LaunchedEffect(previewing) {
        if (!previewing) { runCatching { if (player.isPlaying) player.pause() }; return@LaunchedEffect }
        runCatching {
            player.reset(); player.setDataSource(context, song.contentUri); player.prepare()
            player.seekTo(range.start.toInt()); player.start()
        }.onFailure { previewing = false; return@LaunchedEffect }
        while (previewing && player.isPlaying && player.currentPosition < range.endInclusive) delay(100)
        previewing = false
    }

    AlertDialog(
        onDismissRequest = { if (!cutting) onDismiss() },
        title = { Text("Cortador de audio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                RangeSlider(
                    value = range,
                    onValueChange = { v ->
                        // Siempre queda al menos 1 s de audio entre inicio y fin.
                        val s = v.start.coerceAtMost(v.endInclusive - MIN_CUT_MS)
                        val e = v.endInclusive.coerceAtLeast(v.start + MIN_CUT_MS)
                        range = s.coerceAtLeast(0f)..e.coerceAtMost(total.toFloat())
                        previewing = false
                    },
                    valueRange = 0f..total.toFloat(),
                    enabled = !cutting
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Inicio ${formatDuration(range.start.toLong())}")
                    Text("Fin ${formatDuration(range.endInclusive.toLong())}")
                }
                Text(
                    "Duración del corte: ${formatDuration((range.endInclusive - range.start).toLong())}",
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    OutlinedButton(onClick = { previewing = !previewing }, enabled = !cutting) {
                        Text(if (previewing) "Detener" else "Escuchar tramo")
                    }
                    if (cutting) {
                        CircularProgressIndicator(Modifier.padding(start = 16.dp).padding(2.dp), strokeWidth = 2.dp)
                        Text("Cortando…", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Text(
                    "Se guarda como archivo nuevo en Música/DEZZNUTS; el original no se modifica.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !cutting, onClick = { previewing = false; onCut(range.start.toLong(), range.endInclusive.toLong()) }) { Text("Guardar corte") }
        },
        dismissButton = { TextButton(enabled = !cutting, onClick = onDismiss) { Text("Cancelar") } }
    )
}
