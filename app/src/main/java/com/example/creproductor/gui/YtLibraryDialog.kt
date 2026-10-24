package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.zIndex
import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import coil.compose.AsyncImage
import com.example.creproductor.data.YtLibrary
import com.example.creproductor.data.YtTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Estado compartido del inicio de sesión de YouTube Music: lo pide la pestaña YT y lo dibuja [YtLoginHost]. */
object YtLogin {
    var open by mutableStateOf(false)
    /** Sube al iniciar sesión: las pantallas que dependen de la sesión lo observan para refrescarse. */
    var authVersion by mutableIntStateOf(0)
    /** La pestaña YT lo activa para ir cargando la página de Google antes de que el usuario pulse. */
    var warm by mutableStateOf(false)
}

/**
 * La página de Google vive en un WebView que se crea y empieza a cargar en cuanto se abre la pestaña YT (precarga), de
 * modo que al pulsar «Iniciar sesión» ya está lista. Al llegar a music.youtube.com con la sesión iniciada guarda la
 * cookie y avisa.
 */
@SuppressLint("SetJavaScriptEnabled")
internal class YtLoginSession(private val context: Context, private val onSuccess: () -> Unit) {
    var progress by mutableIntStateOf(0)
    private var web: WebView? = null

    fun ensure(): WebView = web ?: WebView(context).apply {
        web = this
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = YtLibrary.USER_AGENT
        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) { this@YtLoginSession.progress = newProgress }
        }
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                if (!url.startsWith("https://music.youtube.com")) return
                val cookie = CookieManager.getInstance().getCookie("https://music.youtube.com").orEmpty()
                if (cookie.contains("SAPISID=") || cookie.contains("__Secure-3PAPISID=")) {
                    YtLibrary.saveCookie(context, cookie)
                    destroy()
                    onSuccess()
                }
            }
        }
        loadUrl(
            "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true&continue=" +
                "https%3A%2F%2Fwww.youtube.com%2Fsignin%3Faction_handle_signin%3Dtrue%26next%3Dhttps%253A%252F%252Fmusic.youtube.com%252F"
        )
    }

    fun canGoBack() = web?.canGoBack() == true
    fun goBack() { web?.goBack() }

    fun destroy() {
        web?.let { (it.parent as? android.view.ViewGroup)?.removeView(it); it.destroy() }
        web = null
        progress = 0
    }
}

/**
 * Pantalla de inicio de sesión dentro de la propia ventana de la app (no un diálogo aparte): sube desde abajo sobre
 * todo lo demás, con la barra de la app y el progreso. Google no permite formularios propios (contraseña, verificación
 * en dos pasos y avisos de seguridad solo existen en su página), por eso se muestra su página.
 */
@Composable
fun YtLoginHost() {
    val context = LocalContext.current
    val session = remember { YtLoginSession(context) { YtLogin.open = false; YtLogin.authVersion++ } }
    DisposableEffect(Unit) { onDispose { session.destroy() } }
    LaunchedEffect(YtLogin.warm) { if (YtLogin.warm) session.ensure() }
    // Cerrar sin terminar no destruye la página: se conserva para reabrirla al instante.
    AnimatedVisibility(
        visible = YtLogin.open,
        enter = slideInVertically(tween(260)) { it / 4 } + fadeIn(tween(260)),
        exit = slideOutVertically(tween(200)) { it / 4 } + fadeOut(tween(200)),
        modifier = Modifier.zIndex(100f)
    ) {
        androidx.activity.compose.BackHandler { if (session.canGoBack()) session.goBack() else YtLogin.open = false }
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { YtLogin.open = false }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Cancelar")) }
                Column(Modifier.padding(start = 4.dp)) {
                    Text(tr("Iniciar sesión"), style = MaterialTheme.typography.titleLarge)
                    Text(tr("YouTube Music · cuenta de Google"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Box(Modifier.fillMaxWidth().height(3.dp)) {
                if (session.progress in 1..99) LinearProgressIndicator(progress = { session.progress / 100f }, modifier = Modifier.fillMaxSize())
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { session.ensure().also { v -> (v.parent as? android.view.ViewGroup)?.removeView(v) } }
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
