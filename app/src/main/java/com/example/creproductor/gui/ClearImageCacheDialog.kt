package com.example.creproductor.gui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.ArtistImageFetcher
import com.example.creproductor.data.SongCoverCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Diálogo que avisa cuánto ocupan las imágenes de artistas descargadas y permite borrarlas.
 *
 * - Al abrirse, calcula el tamaño en segundo plano ([ArtistImageFetcher.artistImageCacheSize]).
 * - El botón "Borrar" está deshabilitado mientras calcula, mientras borra o si no hay nada.
 * - Al confirmar, limpia la caché ([ArtistImageFetcher.clearArtistImageCache]) y avisa con un toast.
 *
 * @param onDismiss se llama al cancelar o al terminar de borrar.
 */
@Composable
fun ClearImageCacheDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sizeBytes by remember { mutableStateOf<Long?>(null) }
    var isClearing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        sizeBytes = withContext(Dispatchers.IO) {
            ArtistImageFetcher.artistImageCacheSize(context) + SongCoverCache.size(context)
        }
    }

    val currentSize = sizeBytes

    AlertDialog(
        onDismissRequest = { if (!isClearing) onDismiss() },
        icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null) },
        title = { Text("Borrar caché de imágenes") },
        text = {
            Column {
                Text("Se borrarán las fotos de artistas descargadas y las carátulas extraídas de los archivos. Se volverán a generar cuando hagan falta.")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when {
                        currentSize == null -> "Calculando…"
                        currentSize <= 0L -> "No hay imágenes en caché."
                        else -> "Ocupa actualmente: ${formatCacheSize(currentSize)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isClearing && (currentSize ?: 0L) > 0L,
                onClick = {
                    isClearing = true
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            ArtistImageFetcher.clearArtistImageCache(context)
                            SongCoverCache.clear(context)
                        }
                        Toast.makeText(context, "Caché de imágenes borrada", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            ) {
                Text(if (isClearing) "Borrando…" else "Borrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isClearing) {
                Text("Cancelar")
            }
        }
    )
}

/** Formatea un tamaño en bytes a B / KB / MB para mostrarlo en el diálogo. */
private fun formatCacheSize(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format("%.0f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.2f MB", mb)
}
