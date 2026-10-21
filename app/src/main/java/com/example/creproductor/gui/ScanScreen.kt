package com.example.creproductor.gui

import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.FileScanner
import com.example.creproductor.data.ScanProgress
import com.example.creproductor.data.ScanSettings
import java.io.File

/**
 * "Escanear archivos": fuentes (tarjeta SD, carpeta elegida, descargas, almacenamiento interno), filtros avanzados
 * opcionales y, mientras escanea, el radar con el porcentaje y la ruta que se procesa.
 */
@Composable
fun ScanScreen(
    progress: ScanProgress?,
    onStart: (roots: List<File>, settings: ScanSettings) -> Unit,
    onCancel: () -> Unit,
    onClose: (ScanSettings) -> Unit
) {
    val context = LocalContext.current
    val saved = remember { ScanSettings.read(context) }
    var advanced by remember { mutableStateOf(saved != ScanSettings()) }
    var seconds by remember { mutableStateOf(if (saved.minSeconds > 0) saved.minSeconds.toString() else "") }
    var kb by remember { mutableStateOf(if (saved.minKb > 0) saved.minKb.toString() else "") }
    var hidden by remember { mutableStateOf(saved.ignoreHidden) }
    val extraFolders = remember { mutableStateListOf<File>() }
    var useInternal by remember { mutableStateOf(true) }
    var useDownloads by remember { mutableStateOf(false) }
    var useSd by remember { mutableStateOf(false) }
    val sdCards = remember { FileScanner.sdCards(context) }

    fun settings() = if (advanced) ScanSettings(seconds.toIntOrNull() ?: 0, kb.toIntOrNull() ?: 0, hidden) else ScanSettings()
    val running = progress != null && !progress.finished

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { treeToFile(it) }?.let { if (it !in extraFolders) extraFolders += it }
    }

    BackHandler { if (running) onCancel() else onClose(settings()) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                IconButton(onClick = { if (running) onCancel() else onClose(settings()) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
                Text("Escanear archivos", style = MaterialTheme.typography.titleLarge)
            }

            if (progress != null) {
                ScanProgressContent(progress, onCancel = onCancel, onDone = { onClose(settings()) })
            } else {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Dónde buscar", style = MaterialTheme.typography.titleMedium)
                    SourceRow("Almacenamiento interno", useInternal) { useInternal = it }
                    SourceRow("Descargas", useDownloads) { useDownloads = it }
                    if (sdCards.isNotEmpty()) SourceRow("Tarjeta SD", useSd) { useSd = it }
                    extraFolders.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(f.path, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { extraFolders.remove(f) }) { Text("Quitar") }
                        }
                    }
                    OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Elegir carpeta…") }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Avanzado", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Switch(checked = advanced, onCheckedChange = { advanced = it })
                    }
                    AnimatedVisibility(advanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField("Ignorar archivos de menos de (segundos)", seconds) { seconds = it }
                            NumberField("Ignorar archivos de menos de (KB)", kb) { kb = it }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ignorar archivos y carpetas ocultos", modifier = Modifier.weight(1f))
                                Switch(checked = hidden, onCheckedChange = { hidden = it })
                            }
                        }
                    }
                }
                val roots = buildList {
                    if (useInternal) add(FileScanner.primaryStorage())
                    if (useDownloads) add(FileScanner.downloads())
                    if (useSd) addAll(sdCards)
                    addAll(extraFolders)
                }
                Button(
                    onClick = { onStart(roots, settings()) },
                    enabled = roots.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) { Text("INICIAR ESCANEO") }
            }
        }
    }
}

@Composable
private fun SourceRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label)
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter(Char::isDigit).take(6)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ScanProgressContent(progress: ScanProgress, onCancel: () -> Unit, onDone: () -> Unit) {
    Column(
        modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Radar(spinning = !progress.finished, modifier = Modifier.size(220.dp))
        Spacer(Modifier.height(24.dp))
        if (progress.finished) {
            Text("Escaneo terminado", style = MaterialTheme.typography.titleLarge)
            Text("${progress.total} archivos de audio revisados", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onDone) { Text("LISTO") }
        } else {
            Text("${progress.percent} %", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
            Text(
                progress.current,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onCancel) { Text("CANCELAR") }
        }
    }
}

/** Radar con barrido giratorio; la animación solo existe mientras se escanea (no gasta nada cuando termina). */
@Composable
private fun Radar(spinning: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val angle = if (spinning) {
        rememberInfiniteTransition(label = "radar").animateFloat(
            0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart), label = "angle"
        ).value
    } else 0f
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        val r = size.minDimension / 2
        for (i in 1..3) drawCircle(primary.copy(alpha = 0.35f), r * i / 3, c, style = Stroke(2.dp.toPx()))
        if (spinning) {
            rotate(angle, c) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Color.Transparent, primary.copy(alpha = 0.6f)), c),
                    startAngle = -60f, sweepAngle = 60f, useCenter = true
                )
            }
        }
        drawCircle(primary, 4.dp.toPx(), c)
    }
}

/** `content://…/tree/primary:Music/Rock` → `/storage/emulated/0/Music/Rock` (o `/storage/<id>/…` en una SD). */
private fun treeToFile(uri: Uri): File? {
    val doc = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull() ?: return null
    val (volume, path) = doc.split(":", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
    val root = if (volume.equals("primary", ignoreCase = true)) Environment.getExternalStorageDirectory() else File("/storage/$volume")
    return File(root, path).takeIf { it.isDirectory }
}
