package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.creproductor.service.MusicService
import kotlinx.coroutines.delay

private val SLEEP_OPTIONS = listOf(15 to "15 minutos", 30 to "30 minutos", 45 to "45 minutos", 60 to "1 hora", 90 to "1 hora 30 min",
    MusicService.SLEEP_END_OF_TRACK to "Al terminar la canción")

/** Temporizador de apagado: elegir un tiempo (o el final de la canción), ver lo que queda y cancelarlo. */
@Composable
fun SleepTimerDialog(endState: Long, onSelect: (minutes: Int) -> Unit, onDismiss: () -> Unit) {
    // Se refresca cada segundo solo mientras el diálogo está abierto.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    val remainingMs = if (endState > 0) endState - now else 0L
    val active = endState == -1L || remainingMs > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Temporizador")) },
        text = {
            Column {
                if (active) {
                    val label = if (endState == -1L) "Se detendrá al terminar la canción" else {
                        val s = remainingMs / 1000
                        "Se detendrá en %d:%02d".format(s / 60, s % 60)
                    }
                    Text(tr(label), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
                }
                SLEEP_OPTIONS.forEach { (minutes, text) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(minutes); onDismiss() }
                    ) {
                        RadioButton(selected = false, onClick = { onSelect(minutes); onDismiss() })
                        Text(tr(text))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
        dismissButton = if (active) ({ TextButton(onClick = { onSelect(0); onDismiss() }) { Text(tr("Cancelar temporizador")) } }) else null
    )
}
