package com.example.creproductor.gui

import android.widget.Toast
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.AppUpdater
import com.example.creproductor.i18n.I18n
import com.example.creproductor.i18n.tr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Comprueba si hay versión nueva en GitHub y la anuncia. Al abrir la app mira sola (una vez al día, si está activado
 * en Opciones); [manualRequest] sube cada vez que el usuario pulsa "Buscar actualizaciones" y entonces responde
 * siempre, también si ya está al día o no hay conexión.
 */
@Composable
fun UpdateChecker(versionName: String, manualRequest: Int) {
    val context = LocalContext.current
    var release by remember { mutableStateOf<AppUpdater.Release?>(null) }

    LaunchedEffect(manualRequest) {
        val manual = manualRequest > 0
        if (!manual && !AppUpdater.shouldCheck(context)) return@LaunchedEffect
        val r = withContext(Dispatchers.IO) { runCatching { AppUpdater.latest() }.getOrNull() }
        fun say(es: String) = Toast.makeText(context, I18n.tr(context, es), Toast.LENGTH_SHORT).show()
        if (r == null) { if (manual) say("No se pudo comprobar. Revisa tu conexión"); return@LaunchedEffect }
        AppUpdater.markChecked(context)
        when {
            !AppUpdater.isNewer(r.version, versionName) -> if (manual) say("Ya tienes la última versión")
            manual || !AppUpdater.isDismissed(context, r.version) -> release = r
        }
    }

    val r = release ?: return
    AlertDialog(
        onDismissRequest = { release = null },
        title = { Text(tr("Nueva versión %s").format(r.version.removePrefix("v")), fontWeight = FontWeight.Bold) },
        text = {
            Text(
                r.notes.ifBlank { tr("Hay una versión nueva de DEZZNUTS.") }.take(1500),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            Button(onClick = {
                release = null
                val ok = AppUpdater.download(context, r)
                if (ok) Toast.makeText(context, I18n.tr(context, "Descargando… al terminar, toca la notificación para instalar"), Toast.LENGTH_LONG).show()
                else runCatching {  // sin APK adjunto o sin gestor de descargas: la página de la release
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(r.pageUrl)))
                }
            }) { Text(tr("Descargar")) }
        },
        dismissButton = {
            TextButton(onClick = { AppUpdater.dismiss(context, r.version); release = null }) { Text(tr("Ahora no")) }
        }
    )
}
