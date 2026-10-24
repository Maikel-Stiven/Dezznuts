package com.example.creproductor.gui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Acciones del menú lateral (las ejecuta MainActivity, que es quien abre los diálogos). */
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
    object Options : AppAction("Opciones", "Portada con movimiento, color de la carátula, funciones en línea", Icons.Default.Tune)
    object SendFeedback : AppAction("Enviar comentarios", "Reportar un fallo o pedir una función en GitHub", Icons.AutoMirrored.Filled.HelpOutline)
}


/** Grupos del menú hamburguesa de la maqueta (Ecualizador, Escanear, Duplicados, Cortador, Acerca de, Opiniones, Temporizador…). */
private val DRAWER_GROUPS = listOf(
    null to listOf(AppAction.Equalizer, AppAction.HiFiAudio),
    "Biblioteca" to listOf(AppAction.ScanFiles, AppAction.SearchDuplicates, AppAction.FixUnknownArtists),
    "Herramientas" to listOf(AppAction.AudioCutter, AppAction.SleepTimer),
    "Copia de seguridad" to listOf(AppAction.ExportBackup, AppAction.ImportBackup, AppAction.ImportM3u),
    "Más" to listOf(AppAction.Options, AppAction.ClearImageCache, AppAction.About, AppAction.SendFeedback)
)

@Composable
fun AppDrawerContent(versionName: String, onAction: (AppAction) -> Unit, onCloseDrawer: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.width(300.dp)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    text = "DEZZNUTS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 12.dp)
                )
                HorizontalDivider(Modifier.padding(bottom = 8.dp))
                DRAWER_GROUPS.forEach { (group, actions) ->
                    if (group != null) Text(
                        group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                    actions.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(item.title) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            selected = false,
                            onClick = { onCloseDrawer(); onAction(item) },
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalDivider(Modifier.padding(bottom = 12.dp))
                Text("DEZZNUTS Player $versionName", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Opciones con interruptor (todas apagadas por defecto): lo visual y lo que usa internet. */
@Composable
fun OptionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Opciones") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PrefSwitch(
                    "Color de la carátula en el reproductor", "Los botones y el progreso toman el tono de la portada. Apagado: siempre el morado de DEZZNUTS",
                    Icons.Default.Palette, ::isCoverAccentOn, ::setCoverAccent
                )
                PrefSwitch(
                    "Portada con movimiento", "La portada se inclina con el teléfono (se desactiva sola en equipos modestos o con ahorro de batería)",
                    Icons.Default.ScreenRotation, ::isCoverMotionOn, ::setCoverMotion
                )
                PrefSwitch(
                    "Portada animada (en línea)", "Bucle de video del álbum cuando existe, solo con Wi-Fi. Consulta artwork.boidu.dev (servicio comunitario, no oficial) con título, artista y álbum",
                    Icons.Default.Movie, ::isAnimatedCoverOn, ::setAnimatedCover
                )
                PrefSwitch(
                    "Buscar y reproducir en YouTube Music", "Función no oficial (NewPipeExtractor): puede dejar de funcionar sin aviso y va contra los términos de YouTube. Usa internet y no necesita cuenta",
                    Icons.Default.CloudQueue, com.example.creproductor.data.YtMusic::isEnabled, com.example.creproductor.data.YtMusic::setEnabled
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo") } }
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
        headlineContent = { Text(title, style = MaterialTheme.typography.titleSmall) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { androidx.compose.material3.Switch(checked = on, onCheckedChange = { on = it; write(context, it) }) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.clickable { on = !on; write(context, on) }.padding(horizontal = 8.dp)
    )
}
