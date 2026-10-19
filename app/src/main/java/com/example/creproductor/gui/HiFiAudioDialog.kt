package com.example.creproductor.gui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.creproductor.data.Fade
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
 * @param onSettingsChanged se llama cuando cambia salida float u offload (obliga a recrear el reproductor).
 * @param onOpenEqualizer abre la pantalla del ecualizador. ReplayGain y DAC USB se aplican en vivo desde las prefs.
 */
@Composable
fun HiFiAudioDialog(
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit,
    onOpenEqualizer: () -> Unit = {}
) {
    val context = LocalContext.current

    var floatOutput by remember { mutableStateOf(HiFiAudio.isFloatOutputEnabled(context)) }
    var offload by remember { mutableStateOf(HiFiAudio.isOffloadEnabled(context)) }
    val sampleRate = remember { HiFiAudio.deviceSampleRate(context) }
    var replayGain by remember { mutableStateOf(HiFiAudio.isReplayGainEnabled(context)) }
    var rgAlbum by remember { mutableStateOf(HiFiAudio.isReplayGainAlbumMode(context)) }
    var rgBoost by remember { mutableStateOf(HiFiAudio.isReplayGainBoostEnabled(context)) }
    var fadeMs by remember { mutableStateOf(HiFiAudio.fadeMs(context)) }
    var bitPerfect by remember { mutableStateOf(HiFiAudio.isBitPerfectEnabled(context)) }
    var profiles by remember { mutableStateOf(HiFiAudio.isProfilesEnabled(context)) }
    var preferUsb by remember { mutableStateOf(HiFiAudio.isPreferUsbEnabled(context)) }
    val outputs = remember { HiFiAudio.outputDevices(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
        title = { Text("Audio Hi/Res") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                AudioOption(
                    title = "ReplayGain",
                    description = "Iguala el volumen entre pistas leyendo la etiqueta del archivo (FLAC, " +
                        "OGG, MP3). Solo atenúa: nunca amplifica, así que no recorta ni comprime la dinámica.",
                    risk = "Si el archivo no trae la etiqueta, suena igual que antes.",
                    checked = replayGain,
                    onCheckedChange = { enabled ->
                        replayGain = enabled
                        HiFiAudio.setReplayGainEnabled(context, enabled)
                    }
                )

                if (replayGain) {
                    AudioOption(
                        title = "Permitir subir el volumen",
                        description = "Si la pista es más baja que la referencia, la sube solo hasta lo que su " +
                            "pico permite sin recortar (necesita la etiqueta de pico). Sin efecto con offload.",
                        risk = "Activa el procesador de audio en 32 bits float; con offload no se aplica.",
                        checked = rgBoost,
                        onCheckedChange = { enabled ->
                            rgBoost = enabled
                            HiFiAudio.setReplayGainBoostEnabled(context, enabled)
                        }
                    )
                    TextButton(onClick = {
                        rgAlbum = !rgAlbum
                        HiFiAudio.setReplayGainAlbumMode(context, rgAlbum)
                    }) { Text(if (rgAlbum) "Modo: álbum (conserva la dinámica del disco)" else "Modo: pista (iguala cada canción)") }
                }

                AudioOption(
                    title = "Preferir DAC USB",
                    description = "Dirige la salida al DAC USB cuando está conectado, evitando que el " +
                        "mezclador del sistema lo ignore o remuestree.",
                    risk = "Si tu DAC no negocia bien, desactívalo para volver a la salida automática.",
                    checked = preferUsb,
                    onCheckedChange = { enabled ->
                        preferUsb = enabled
                        HiFiAudio.setPreferUsbEnabled(context, enabled)
                    }
                )

                Column {
                    Text("Fundido de entrada y salida", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Sube el volumen al empezar una pista o reanudar y lo baja al final de la pista y al pausar. " +
                            "En discos sin cortes (gapless) conviene dejarlo apagado.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = {
                        val options = Fade.OPTIONS_MS
                        fadeMs = options[(options.indexOf(fadeMs) + 1) % options.size]
                        HiFiAudio.setFadeMs(context, fadeMs)
                    }) { Text("Duración: ${Fade.label(fadeMs)}") }
                }

                AudioOption(
                    title = "Perfiles por salida",
                    description = "Recuerda ReplayGain y ecualizador por separado para altavoz, cable, Bluetooth " +
                        "y DAC USB, y los cambia solos al conectar o desconectar.",
                    risk = "Al cambiar de salida se sustituyen tus ajustes actuales por los guardados de esa salida.",
                    checked = profiles,
                    onCheckedChange = { enabled ->
                        profiles = enabled
                        HiFiAudio.setProfilesEnabled(context, enabled)
                    }
                )

                val canBitPerfect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                AudioOption(
                    title = "Bit-perfect con DAC USB",
                    description = "Pide al DAC USB el formato exacto de cada pista para que el sistema no remuestree ni " +
                        "mezcle (Android 14+). Solo entra si el DAC ofrece ese formato; si no, todo suena como siempre." +
                        if (canBitPerfect) "" else " Requiere Android 14.",
                    risk = "Se desactiva solo con ecualizador, subida de ReplayGain u offload (modifican la señal). " +
                        "Si tu DAC se porta mal, apágalo.",
                    checked = bitPerfect && canBitPerfect,
                    enabled = canBitPerfect,
                    onCheckedChange = { enabled ->
                        bitPerfect = enabled
                        HiFiAudio.setBitPerfectEnabled(context, enabled)
                    }
                )

                TextButton(onClick = onOpenEqualizer) { Text("Ecualizador…") }

                HorizontalDivider()

                Text(
                    text = "Salidas conectadas: " + if (outputs.isEmpty()) "ninguna" else "",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                outputs.forEach {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

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
    enabled: Boolean = true,
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
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
