package com.example.creproductor.gui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.HiFiAudio

/**
 * Diálogo "Audio de alta fidelidad": explica qué hace la app con el audio y
 * permite activar los dos ajustes de salida que acercan la reproducción al
 * bit-perfect.
 *
 * Cada interruptor se persiste al instante con [HiFiAudio] y avisa con
 * [onSettingsChanged] para que el servicio recree el reproductor sin reiniciar
 * la app ni perder la cola.
 *
 * @param onDismiss se llama al cerrar el diálogo.
 * @param onSettingsChanged se llama cada vez que cambia un interruptor.
 */
@Composable
fun HiFiAudioDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit
) {
    val context = LocalContext.current

    var floatOutput by remember { mutableStateOf(HiFiAudio.isFloatOutputEnabled(context)) }
    var offload by remember { mutableStateOf(HiFiAudio.isOffloadEnabled(context)) }
    val sampleRate = remember { HiFiAudio.deviceSampleRate(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
        title = { Text("Audio de alta fidelidad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "DEZZNUTS ya decodifica FLAC, ALAC y WAV sin pérdida. " +
                        "Estas opciones controlan cómo se entrega esa señal al DAC del dispositivo.",
                    style = MaterialTheme.typography.bodyMedium
                )

                SettingSwitch(
                    title = "Salida en 32 bits (float)",
                    description = "Evita truncar el PCM de 24/32 bits a 16 bits antes del mezclador. " +
                        "En archivos de 16 bits no cambia nada.",
                    checked = floatOutput,
                    onCheckedChange = { enabled ->
                        floatOutput = enabled
                        HiFiAudio.setFloatOutputEnabled(context, enabled)
                        onSettingsChanged()
                    }
                )

                SettingSwitch(
                    title = "Audio offload (bit-perfect)",
                    description = "Envía el archivo comprimido directamente al DSP, sin remuestreo ni mezcla: " +
                        "conserva la frecuencia y la profundidad originales. Si notas cortes o silencios, desactívalo.",
                    checked = offload,
                    onCheckedChange = { enabled ->
                        offload = enabled
                        HiFiAudio.setOffloadEnabled(context, enabled)
                        onSettingsChanged()
                    }
                )

                HorizontalDivider()

                Text(
                    text = "Salida nativa del dispositivo: " +
                        (sampleRate?.let { "$it Hz" } ?: "desconocida"),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "El sistema remuestrea a esa frecuencia todo lo que no coincida con ella " +
                        "(por ejemplo, un FLAC de CD a 44100 Hz). El offload evita ese remuestreo " +
                        "en los dispositivos que lo soportan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo") }
        }
    )
}

/** Interruptor con título y explicación, usado por [HiFiAudioDialog]. */
@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
