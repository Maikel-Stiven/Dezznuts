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

/** Ventana web a pantalla completa; al llegar a music.youtube.com con la sesión iniciada guarda la cookie y se cierra. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun YtLoginWindow(onDone: () -> Unit, onCancel: () -> Unit) {
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

/** Tus canciones con "Me gusta" (necesita sesión). [reload] fuerza volver a pedirlas. */
@Composable
internal fun YtLikedList(reload: Int, onPlay: (YtTrack) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // null = cargando, Result.failure = sin red o la sesión caducó.
    val result by produceState<Result<List<YtTrack>>?>(null, reload) {
        value = null
        value = withContext(Dispatchers.IO) { runCatching { YtLibrary.liked(context) } }
    }
    val list = result?.getOrNull()
    when {
        result == null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        list == null -> Text(tr("No se pudo cargar. Revisa la conexión o inicia sesión de nuevo."), modifier = modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        list.isEmpty() -> Text(tr("Aún no tienes canciones con Me gusta."), modifier = modifier.padding(24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else -> LazyColumn(modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
            items(list, key = { it.url }) { t ->
                ListItem(
                    headlineContent = { Text(t.title, maxLines = 1) },
                    supportingContent = { Text(t.artist, maxLines = 1) },
                    leadingContent = {
                        AsyncImage(t.thumbnail, null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)))
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onPlay(t) }
                )
            }
        }
    }
}
