package com.example.creproductor.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Acciones de la pantalla de Ajustes (las ejecuta MainActivity, que es quien abre los diálogos). */
sealed class AppAction(
    val title: String,
    val subtitle: String,
    val icon : ImageVector
){
    object Equalizer : AppAction("Ecualizador", "Bandas, presets y perfiles por salida", Icons.Default.Equalizer)
    object HiFiAudio : AppAction("Audio Hi/Res", "Salida float, offload, bit-perfect, ReplayGain y fundidos", Icons.Default.GraphicEq)
    object ScanFiles : AppAction("Escanear archivos", "Buscar música nueva y filtrar audios cortos", Icons.Default.Radar)
    object SearchDuplicates : AppAction("Duplicados y audios de WhatsApp", "Encontrar copias para liberar espacio", Icons.Default.FindInPage)
    object FixUnknownArtists : AppAction("Corregir artistas desconocidos", "Deducir artista y título del nombre del archivo", Icons.Default.AutoFixHigh)
    object AudioCutter : AppAction("Cortador de audio", "Recortar la canción que suena (tonos, fragmentos)", Icons.Default.ContentCut)
    object SleepTimer: AppAction("Temporizador", "Pausar la música pasado un tiempo", Icons.Default.Timer)
    object ExportBackup : AppAction("Guardar copia de seguridad", "Favoritas, reproducciones y listas en un archivo", Icons.Default.Backup)
    object ImportBackup : AppAction("Restaurar copia", "Recuperarlas al reinstalar o en otro teléfono", Icons.Default.Restore)
    object ImportM3u : AppAction("Importar lista M3U", "Traer una lista de otro reproductor", Icons.Default.PlaylistAdd)
    object ClearImageCache : AppAction("Borrar caché de imágenes", "Carátulas y fotos de artistas descargadas", Icons.Default.DeleteSweep)
    object About: AppAction("Acerca de", "Versión, licencia y créditos", Icons.Default.Info)
    object SendFeedback : AppAction("Enviar comentarios", "Reportar un fallo o pedir una función en GitHub", Icons.AutoMirrored.Filled.HelpOutline)
}

private val SETTINGS_GROUPS = listOf(
    "Audio" to listOf(AppAction.Equalizer, AppAction.HiFiAudio),
    "Biblioteca" to listOf(AppAction.ScanFiles, AppAction.SearchDuplicates, AppAction.FixUnknownArtists),
    "Herramientas" to listOf(AppAction.AudioCutter, AppAction.SleepTimer),
    "Copia de seguridad" to listOf(AppAction.ExportBackup, AppAction.ImportBackup, AppAction.ImportM3u),
    "Almacenamiento" to listOf(AppAction.ClearImageCache),
    "Información" to listOf(AppAction.About, AppAction.SendFeedback)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    versionName: String,
    onAction: (AppAction) -> Unit
){
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0)
    ) { innerPadding ->
        LazyColumn(Modifier.fillMaxSize().padding(innerPadding)) {
            SETTINGS_GROUPS.forEach { (group, actions) ->
                item(key = group) {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)
                    )
                }
                actions.forEach { action ->
                    item(key = action.title) {
                        ListItem(
                            headlineContent = { Text(action.title, style = MaterialTheme.typography.titleSmall) },
                            supportingContent = { Text(action.subtitle) },
                            leadingContent = { Icon(action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                            modifier = Modifier.clickable { onAction(action) }.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
            item(key = "appearance") {
                Text(
                    text = "Apariencia",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp)
                )
                PrefSwitch(
                    "Portada con movimiento", "La portada se inclina con el teléfono (se desactiva sola en equipos modestos o con ahorro de batería)",
                    Icons.Default.ScreenRotation, ::isCoverMotionOn, ::setCoverMotion
                )
                PrefSwitch(
                    "Portada animada (en línea)", "Bucle de video del álbum cuando existe, solo con Wi-Fi. Consulta artwork.boidu.dev (servicio comunitario, no oficial) con título, artista y álbum",
                    Icons.Default.Movie, ::isAnimatedCoverOn, ::setAnimatedCover
                )
            }
            item {
                Text(
                    text = "DEZZNUTS $versionName · libre y gratuito",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp)
                )
            }
        }
    }
}

@Composable
private fun PrefSwitch(
    title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    read: (android.content.Context) -> Boolean, write: (android.content.Context, Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var on by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(read(context)) }
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { androidx.compose.material3.Switch(checked = on, onCheckedChange = { on = it; write(context, it) }) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable { on = !on; write(context, on) }.padding(horizontal = 8.dp)
    )
}
