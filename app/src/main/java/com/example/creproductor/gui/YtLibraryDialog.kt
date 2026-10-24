package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.creproductor.data.YtLibrary
import com.example.creproductor.data.YtTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * "Mi YouTube Music": inicio de sesión (ventana web de Google) y la lista de canciones que te gustan.
 * La sesión se guarda solo en este teléfono; "Cerrar sesión" la borra.
 */
@Composable
fun YtLibraryDialog(onPlay: (YtTrack) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var loggedIn by remember { mutableStateOf(YtLibrary.isLoggedIn(context)) }
    var reload by remember { mutableStateOf(0) }
    var showLogin by remember { mutableStateOf(false) }

    if (showLogin) {
        YtLoginWindow(onDone = { loggedIn = YtLibrary.isLoggedIn(context); showLogin = false; reload++ }, onCancel = { showLogin = false })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("YouTube Music")) },
        text = {
            if (!loggedIn) Column {
                Text(tr("Inicia sesión para ver las canciones que te gustan. La sesión se guarda solo en este teléfono y puedes cerrarla cuando quieras."))
                Text(
                    tr("Función no oficial: puede dejar de funcionar sin aviso y va contra los términos de YouTube."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                // null = cargando, Result.failure = sin red o la sesión caducó.
                val result by produceState<Result<List<YtTrack>>?>(null, reload) {
                    value = null
                    value = withContext(Dispatchers.IO) { runCatching { YtLibrary.liked(context) } }
                }
                val list = result?.getOrNull()
                when {
                    result == null -> Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    list == null -> Text(tr("No se pudo cargar. Revisa la conexión o inicia sesión de nuevo."))
                    list.isEmpty() -> Text(tr("Aún no tienes canciones con Me gusta."))
                    else -> LazyColumn(Modifier.size(width = 320.dp, height = 380.dp)) {
                        items(list, key = { it.url }) { t ->
                            ListItem(
                                headlineContent = { Text(t.title, maxLines = 1) },
                                supportingContent = { Text(t.artist, maxLines = 1) },
                                leadingContent = {
                                    AsyncImage(t.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)))
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onPlay(t); onDismiss() }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!loggedIn) TextButton(onClick = { showLogin = true }) { Text(tr("Iniciar sesión")) }
            else TextButton(onClick = onDismiss) { Text(tr("Cerrar")) }
        },
        dismissButton = {
            if (loggedIn) TextButton(onClick = { YtLibrary.logout(context); CookieManager.getInstance().removeAllCookies(null); loggedIn = false }) { Text(tr("Cerrar sesión")) }
            else TextButton(onClick = onDismiss) { Text(tr("Cancelar")) }
        }
    )
}

/** Ventana web a pantalla completa; al llegar a music.youtube.com con la sesión iniciada guarda la cookie y se cierra. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun YtLoginWindow(onDone: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TextButton(onClick = onCancel, modifier = Modifier.padding(4.dp)) { Text(tr("Cancelar")) }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = YtLibrary.USER_AGENT
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String) {
                                if (!url.startsWith("https://music.youtube.com")) return
                                val cookie = CookieManager.getInstance().getCookie("https://music.youtube.com").orEmpty()
                                if (cookie.contains("SAPISID=") || cookie.contains("__Secure-3PAPISID=")) {
                                    YtLibrary.saveCookie(context, cookie)
                                    onDone()
                                }
                            }
                        }
                        loadUrl(
                            "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=" +
                                "https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"
                        )
                    }
                },
                onRelease = { it.destroy() }
            )
        }
    }
}
