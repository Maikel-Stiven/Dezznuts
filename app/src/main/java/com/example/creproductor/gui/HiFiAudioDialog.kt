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
 * Diálogo "Audio Hi/Res": explica qué hace la app con el audio y deja al
 * usuario decidir si activa los dos ajustes de salida que acercan la
 * reproducción al bit-perfect.
 *
 * **Ambos interruptores vienen apagados por defecto** ([HiFiAudio]): la app no
 * cambia la ruta de audio por su cuenta. Cada opción explica qué hace y su
 * riesgo, porque los dos dependen del dispositivo.
 *
 * Cada interruptor se persiste al instante y avisa con [onSettingsChanged] para
 * que el servicio recree el reproductor sin reiniciar la app ni perder la cola.
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
        title = { Text("Audio Hi/Res") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "DEZZNUTS ya decodifica FLAC, ALAC y WAV sin pérdida. " +
                        "Estos ajustes deciden cómo se entrega esa señal al DAC del dispositivo. " +
                        "Ninguno viene activado: pruébalos tú y quédate con el que mejor suene en tu equipo.",
                    style = MaterialTheme.typography.bodyMedium
                )

                AudioOption(
                    title = "Salida en 32 bits (float)",
                    description = "Evita que el audio de 24 y 32 bits se trunque a 16 bits antes de " +
                        "mezclar, conservando el detalle fino. En archivos de 16 bits no cambia nada: " +
                        "solo actúa en alta resolución.",
                    risk = "Si tu dispositivo no soporta la salida float, los archivos de 24/32 bits " +
                        "podrían no sonar. Desactívalo si notas silencio.",
                    checked = floatOutput,
                    onCheckedChange = { enabled ->
                        floatOutput = enabled
                        HiFiAudio.setFloatOutputEnabled(context, enabled)
                        onSettingsChanged()
                    }
                )

                AudioOption(
                    title = "Audio offload (bit-perfect)",
                    description = "Envía el archivo comprimido directamente al DSP, sin remuestreo ni " +
                        "mezcla: conserva la frecuencia y la profundidad originales. Requiere Android 10+.",
                    risk = "Depende del chip: algunos lo soportan mal y producen cortes o silencios. " +
                        "Mientras esté activo no se aplican efectos y la salida float no interviene.",
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

/**
 * Una opción de audio con su interruptor: título, qué hace y su riesgo.
 * El riesgo se muestra siempre (no solo al activar) para que la decisión sea
 * informada.
 */
@Composable
private fun AudioOption(
    title: String,
    description: String,
    risk: String,
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
            Text(
                text = "Riesgo: $risk",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
