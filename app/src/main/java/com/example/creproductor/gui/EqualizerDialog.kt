package com.example.creproductor.gui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.EqBand
import com.example.creproductor.data.EqBands
import com.example.creproductor.data.HiFiAudio
import kotlin.math.ln
import kotlin.math.exp

/**
 * Ecualizador paramétrico (**apagado por defecto**, para no tocar la señal bit-perfect).
 *
 * Cada banda tiene frecuencia, ganancia (±12 dB) y Q (ancho). El procesador trabaja en float de 32 bits y baja
 * el volumen automáticamente lo que suben las bandas, para evitar distorsión. Los cambios se guardan al soltar
 * el deslizador y el servicio los aplica en vivo. No actúa con audio offload.
 */
@Composable
fun EqualizerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(HiFiAudio.PREFS_NAME, android.content.Context.MODE_PRIVATE) }

    var enabled by remember { mutableStateOf(prefs.getBoolean(HiFiAudio.KEY_EQ_ENABLED, false)) }
    val bands = remember { mutableStateListOf<EqBand>().apply { addAll(EqBands.decode(prefs.getString(HiFiAudio.KEY_EQ_BANDS, null))) } }

    fun save() = prefs.edit().putString(HiFiAudio.KEY_EQ_BANDS, EqBands.encode(bands)).apply()

    // La frecuencia se desliza en escala logarítmica (así se oye de uniforme).
    val lnMin = ln(EqBands.MIN_FREQ)
    val lnMax = ln(EqBands.MAX_FREQ)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Tune, contentDescription = null) },
        title = { Text("Ecualizador paramétrico") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Activar", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(checked = enabled, onCheckedChange = {
                        enabled = it
                        prefs.edit().putBoolean(HiFiAudio.KEY_EQ_ENABLED, it).apply()
                    })
                }
                bands.forEachIndexed { i, band ->
                    Text(
                        text = "Banda ${i + 1}: ${band.freqHz.toInt()} Hz · ${"%+.1f".format(band.gainDb)} dB · Q ${"%.1f".format(band.q)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Slider(
                        modifier = Modifier.semantics { contentDescription = "Frecuencia banda ${i + 1}" },
                        value = ln(band.freqHz), enabled = enabled,
                        onValueChange = { bands[i] = band.copy(freqHz = exp(it)) },
                        onValueChangeFinished = { save() },
                        valueRange = lnMin..lnMax
                    )
                    Slider(
                        modifier = Modifier.semantics { contentDescription = "Ganancia banda ${i + 1}" },
                        value = band.gainDb, enabled = enabled,
                        onValueChange = { bands[i] = band.copy(gainDb = it) },
                        onValueChangeFinished = { save() },
                        valueRange = EqBands.MIN_GAIN_DB..EqBands.MAX_GAIN_DB
                    )
                    Slider(
                        modifier = Modifier.semantics { contentDescription = "Ancho (Q) banda ${i + 1}" },
                        value = band.q, enabled = enabled,
                        onValueChange = { bands[i] = band.copy(q = it) },
                        onValueChangeFinished = { save() },
                        valueRange = EqBands.MIN_Q..EqBands.MAX_Q
                    )
                }
                Text(
                    text = "Por banda, de arriba abajo: frecuencia, ganancia y Q. Sin efecto con audio offload; al activarlo se recrea el reproductor un instante.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } },
        dismissButton = {
            TextButton(onClick = { bands.clear(); bands.addAll(EqBands.DEFAULT); save() }) { Text("Restablecer") }
        }
    )
}
