package com.example.creproductor.gui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.SquareCrop
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_SIDE = 2048     // se decodifica a como mucho esto: una foto de 12 MP ocuparía ~48 MB
private const val OUTPUT_SIDE = 512

/**
 * Recorte con el cuadro bloqueado en 1:1: se puede elegir cualquier imagen, y se mueve y amplía con los dedos
 * hasta encuadrar la parte que se quiere. Devuelve (vía [onCropped]) un archivo propio de la app de 512×512.
 */
@Composable
fun SquareCropDialog(source: Uri, onCropped: (Uri) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(source) { bitmap = withContext(Dispatchers.IO) { loadBitmap(context, source) } }

    val boxPx = with(LocalDensity.current) { 280.dp.toPx() }
    var scale by remember { mutableFloatStateOf(1f) }
    var ox by remember { mutableFloatStateOf(0f) }
    var oy by remember { mutableFloatStateOf(0f) }
    var saving by remember { mutableStateOf(false) }

    // Al cargar: la imagen cubre el cuadro y queda centrada.
    LaunchedEffect(bitmap) {
        val b = bitmap ?: return@LaunchedEffect
        scale = SquareCrop.minScale(b.width, b.height, boxPx)
        ox = (boxPx - b.width * scale) / 2
        oy = (boxPx - b.height * scale) / 2
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Editar foto") },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val b = bitmap
                Box(Modifier.size(280.dp).clipToBounds(), contentAlignment = Alignment.Center) {
                    if (b == null) CircularProgressIndicator() else {
                        val minScale = SquareCrop.minScale(b.width, b.height, boxPx)
                        Canvas(
                            Modifier.size(280.dp).pointerInput(b) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(minScale, minScale * 6)
                                    // El punto bajo los dedos se queda donde está al ampliar.
                                    val k = newScale / scale
                                    ox = SquareCrop.clampOffset(centroid.x - (centroid.x - ox) * k + pan.x, newScale, b.width, boxPx)
                                    oy = SquareCrop.clampOffset(centroid.y - (centroid.y - oy) * k + pan.y, newScale, b.height, boxPx)
                                    scale = newScale
                                }
                            }
                        ) {
                            drawImage(
                                b.asImageBitmap(),
                                dstOffset = IntOffset(ox.toInt(), oy.toInt()),
                                dstSize = IntSize((b.width * scale).toInt(), (b.height * scale).toInt())
                            )
                        }
                    }
                }
                Text("Arrastra y pellizca para encuadrar (1:1)", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                enabled = bitmap != null && !saving,
                onClick = {
                    val b = bitmap ?: return@TextButton
                    saving = true
                    // ponytail: recorta en el hilo principal (512 px, ~20 ms); mover a IO si se sube la resolución.
                    onCropped(cropToFile(context, b, ox, oy, scale, boxPx))
                }
            ) { Text("Listo") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancelar") } }
    )
}

/** Decodifica reduciendo (≤ [MAX_SIDE]) y aplicando la orientación EXIF, para que una foto de cámara no salga girada. */
private fun loadBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        ?: return null
    val rotation = resolver.openInputStream(uri)?.use {
        when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f
    if (rotation == 0f) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
}.getOrNull()

private fun cropToFile(context: Context, b: Bitmap, ox: Float, oy: Float, scale: Float, boxPx: Float): Uri {
    val side = SquareCrop.sourceSide(scale, boxPx)
    val x = SquareCrop.sourceOrigin(ox, scale).coerceIn(0f, (b.width - side).coerceAtLeast(0f))
    val y = SquareCrop.sourceOrigin(oy, scale).coerceIn(0f, (b.height - side).coerceAtLeast(0f))
    val cropped = Bitmap.createBitmap(b, x.toInt(), y.toInt(), side.toInt().coerceAtMost(b.width), side.toInt().coerceAtMost(b.height))
    val out = Bitmap.createScaledBitmap(cropped, OUTPUT_SIDE, OUTPUT_SIDE, true)
    val dir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
    val file = File(dir, "${System.currentTimeMillis()}.jpg")
    file.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    return Uri.fromFile(file)
}
