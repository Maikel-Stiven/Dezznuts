package com.example.creproductor.gui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.HiFiAudio

/**
 * Ecualizador del sistema (opcional, **apagado por defecto** para no tocar la señal bit-perfect).
 *
 * Las bandas las publica el servicio en las prefs al abrir la sesión de audio (`eq_info`:
 * `nº|mín|máx|frecuencias`, niveles en milibelios), así que hace falta haber reproducido algo.
 * Los cambios se guardan al soltar el deslizador y el servicio los aplica en vivo; además
 * baja el volumen lo que suben las bandas para no saturar. No actúa con audio offload.
 */
@Composable
fun EqualizerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(HiFiAudio.PREFS_NAME, android.content.Context.MODE_PRIVATE) }

    val info = remember { prefs.getString(HiFiAudio.KEY_EQ_INFO, null)?.split("|") }
    val bands = info?.getOrNull(0)?.toIntOrNull() ?: 0
    val minMb = info?.getOrNull(1)?.toIntOrNull() ?: -1500
    val maxMb = info?.getOrNull(2)?.toIntOrNull() ?: 1500
    val freqs = info?.getOrNull(3)?.split(",") ?: emptyList()

    var enabled by remember { mutableStateOf(prefs.getBoolean(HiFiAudio.KEY_EQ_ENABLED, false)) }
    val levels = remember {
        val saved = (prefs.getString(HiFiAudio.KEY_EQ_LEVELS, "") ?: "").split(",").mapNotNull { it.trim().toIntOrNull() }
        mutableStateListOf<Int>().apply { repeat(bands) { add(saved.getOrNull(it) ?: 0) } }
    }

    fun save() = prefs.edit().putString(HiFiAudio.KEY_EQ_LEVELS, levels.joinToString(",")).apply()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Tune, contentDescription = null) },
        title = { Text("Ecualizador") },
        text = {
            if (bands == 0) {
                Text("Reproduce una canción para habilitar el ecualizador (si tu dispositivo lo soporta).")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Activar", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = enabled, onCheckedChange = {
                            enabled = it
                            prefs.edit().putBoolean(HiFiAudio.KEY_EQ_ENABLED, it).apply()
                        })
                    }
                    repeat(bands) { i ->
                        val db = levels[i] / 100f
                        Text(
                            text = "${freqs.getOrNull(i) ?: "?"} Hz   ${"%+.1f".format(db)} dB",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = levels[i].toFloat(),
                            onValueChange = { levels[i] = it.toInt() },
                            onValueChangeFinished = { save() },
                            valueRange = minMb.toFloat()..maxMb.toFloat(),
                            enabled = enabled
                        )
                    }
                    Text(
                        text = "No tiene efecto con audio offload. Si activas el ecualizador, el volumen se " +
                            "reduce automáticamente lo que suben las bandas para evitar distorsión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
        dismissButton = {
            if (bands > 0) TextButton(onClick = { repeat(bands) { levels[it] = 0 }; save() }) { Text("Restablecer") }
        }
    )
}
