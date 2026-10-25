package com.example.creproductor.gui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.webkit.CookieManager
import com.example.creproductor.data.YtLibrary
import com.example.creproductor.data.YtMusic
import com.example.creproductor.data.YtTrack
import com.example.creproductor.i18n.tr

/**
 * Pestaña "YT Music": buscador, el inicio de YouTube Music (estantes y estados de ánimo) y, con sesión, tus "Me gusta". Todo es opcional: mientras
 * YouTube Music esté apagado solo explica qué es y ofrece activarlo. La reproducción pasa por el mismo reproductor.
 */
@Composable
fun YtMusicScreen(onPlay: (YtTrack) -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(YtMusic.isEnabled(context)) }
    var loggedIn by remember { mutableStateOf(YtLibrary.isLoggedIn(context)) }
    var reload by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }

    if (!enabled) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
            Text(tr("YouTube Music"), style = MaterialTheme.typography.headlineSmall)
            Text(
                tr("Función no oficial (NewPipeExtractor): puede dejar de funcionar sin aviso y va contra los términos de YouTube. Usa internet y no necesita cuenta"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 12.dp)
            )
            Button(onClick = { YtMusic.setEnabled(context, true); enabled = true }) { Text(tr("Activar YouTube Music")) }
        }
        return
    }
    // Refresca al volver de iniciar sesión; y mientras no haya sesión, va cargando la página de Google por adelantado.
    androidx.compose.runtime.LaunchedEffect(YtLogin.authVersion) {
        if (YtLogin.authVersion > 0) { loggedIn = YtLibrary.isLoggedIn(context); reload++ }
    }
    androidx.compose.runtime.LaunchedEffect(loggedIn) { YtLogin.warm = !loggedIn }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text(tr("Buscar en YouTube Music")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, contentDescription = tr("Limpiar")) } },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )
        when {
            query.isNotBlank() -> YtMusicResults(query, onPlay)
            else -> YtHomeContent(
                loggedIn = loggedIn, reload = reload, onPlay = onPlay,
                onLogin = { YtLogin.open = true },
                onLogout = { YtLibrary.logout(context); CookieManager.getInstance().removeAllCookies(null); loggedIn = false }
            )
        }
    }
}
