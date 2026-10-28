package com.example.creproductor.gui

import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Lyrics
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.YtLibrary
import com.example.creproductor.data.YtMusic
import com.example.creproductor.i18n.tr
import com.example.creproductor.ui.theme.ThemeMode
import com.example.creproductor.ui.theme.ThemeSetting

/** Grupos de acciones de la pantalla de Opciones (las ejecuta `MainActivity`, que es quien abre los diálogos). */
private val OPTION_GROUPS = listOf(
    "Audio" to listOf(AppAction.Equalizer, AppAction.HiFiAudio),
    "Biblioteca" to listOf(AppAction.ScanFiles, AppAction.SearchDuplicates, AppAction.FixUnknownArtists, AppAction.Stats),
    "Herramientas" to listOf(AppAction.AudioCutter, AppAction.SleepTimer),
    "Copia de seguridad" to listOf(AppAction.ExportBackup, AppAction.ImportBackup, AppAction.ImportM3u),
    "Almacenamiento" to listOf(AppAction.ClearImageCache),
    "Información" to listOf(AppAction.About, AppAction.SendFeedback)
)

/**
 * Opciones a pantalla completa, por secciones: Apariencia, En línea y luego las acciones de la app. Lo que usa internet
 * o es experimental está apagado de fábrica y dice qué envía y a quién.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OptionsScreen(versionName: String, onAction: (AppAction) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    var ytEnabled by remember { mutableStateOf(YtMusic.isEnabled(context)) }
    var ytLoggedIn by remember { mutableStateOf(YtLibrary.isLoggedIn(context)) }

    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Volver")) } },
                title = { Text(tr("Opciones")) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                windowInsets = WindowInsets(0)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { inner ->
        LazyColumn(Modifier.fillMaxSize().padding(inner)) {
            item { SectionTitle("Apariencia") }
            item {
                // Sin ListItem: sus medidas intrínsecas no admiten las listas perezosas del selector.
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(tr("Tema"), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 16.dp))
                    }
                    ThemePicker()
                    Text(tr("Navegación"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
                    NavStylePicker()
                }
            }
            item {
                PrefSwitch(
                    "Color de la carátula en el reproductor", "Botones y progreso toman el tono de la portada",
                    Icons.Default.Palette, ::isCoverAccentOn, ::setCoverAccent
                )
            }
            item {
                PrefSwitch(
                    "Visualizador (barras tipo CAVA)", "Barras de frecuencia en el reproductor; no usa internet ni permisos (no con audio offload)",
                    Icons.Default.GraphicEq, com.example.creproductor.data.Visualizer::isOn, com.example.creproductor.data.Visualizer::set
                )
            }
            item {
                PrefSwitch(
                    "Portada con movimiento", "La portada se inclina con el teléfono",
                    Icons.Default.ScreenRotation, ::isCoverMotionOn, ::setCoverMotion
                )
            }

            item {
                PrefSwitch(
                    "Letra palabra por palabra", "Ilumina cada palabra de la línea que suena (tiempo estimado)",
                    Icons.Default.Lyrics, ::isWordLyricsOn, ::setWordLyrics
                )
            }

            item { SectionTitle("En línea (opcional)") }
            item {
                PrefSwitch(
                    "Portada animada (en línea)", "Bucle de video del álbum, solo con Wi-Fi (servicio comunitario artwork.boidu.dev)",
                    Icons.Default.Movie, ::isAnimatedCoverOn, ::setAnimatedCover
                )
            }
            item {
                PrefSwitch(
                    "Buscar y reproducir en YouTube Music", "No oficial: puede dejar de funcionar y va contra los términos de YouTube",
                    Icons.Default.CloudQueue, YtMusic::isEnabled, { c, on -> YtMusic.setEnabled(c, on); ytEnabled = on }
                )
            }
            if (ytEnabled) item {
                PrefSwitch(
                    "Seguir con las recomendadas de YouTube", "Al terminar una canción pasa a la siguiente recomendada",
                    Icons.Default.Radio, YtMusic::isAutoNext, YtMusic::setAutoNext
                )
            }
            if (ytEnabled) item {
                ListItem(
                    leadingContent = { Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    headlineContent = { Text(tr("Cuenta de YouTube Music"), style = MaterialTheme.typography.titleSmall) },
                    supportingContent = { Text(tr(if (ytLoggedIn) "Sesión iniciada en este teléfono" else "Sin sesión: inicia desde la pestaña YT Music")) },
                    trailingContent = {
                        if (ytLoggedIn) TextButton(onClick = {
                            YtLibrary.logout(context); android.webkit.CookieManager.getInstance().removeAllCookies(null); ytLoggedIn = false
                        }) { Text(tr("Cerrar sesión")) }
                    },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background)
                )
            }

            OPTION_GROUPS.forEach { (group, actions) ->
                item(key = group) { SectionTitle(group) }
                actions.forEach { action ->
                    item(key = action.title) {
                        ListItem(
                            leadingContent = { Icon(action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            headlineContent = { Text(tr(action.title), style = MaterialTheme.typography.titleSmall) },
                            supportingContent = { Text(tr(action.subtitle)) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.clickable { onAction(action) }
                        )
                    }
                }
            }
            item {
                Text(
                    "DEZZNUTS $versionName",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Text(
        tr(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
internal fun PrefSwitch(
    title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    read: (android.content.Context) -> Boolean, write: (android.content.Context, Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var on by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(read(context)) }
    ListItem(
        headlineContent = { Text(tr(title), style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(tr(subtitle)) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { androidx.compose.material3.Switch(checked = on, onCheckedChange = { on = it; write(context, it) }) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable { on = !on; write(context, on) }.padding(horizontal = 8.dp)
    )
}
