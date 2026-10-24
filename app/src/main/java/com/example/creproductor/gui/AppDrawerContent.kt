package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
    object Stats : AppAction("Estadísticas", "Lo más escuchado y las horas de música", Icons.Default.BarChart)
    object Options : AppAction("Opciones", "Portada con movimiento, color de la carátula, funciones en línea", Icons.Default.Tune)
    object SendFeedback : AppAction("Enviar comentarios", "Reportar un fallo o pedir una función en GitHub", Icons.AutoMirrored.Filled.HelpOutline)
}


/** Grupos del menú hamburguesa de la maqueta (Ecualizador, Escanear, Duplicados, Cortador, Acerca de, Opiniones, Temporizador…). */
private val DRAWER_GROUPS = listOf(
    null to listOf(AppAction.Equalizer, AppAction.HiFiAudio),
    "Biblioteca" to listOf(AppAction.Stats, AppAction.ScanFiles, AppAction.SearchDuplicates, AppAction.FixUnknownArtists),
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
                    text = tr("DEZZNUTS"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 24.dp, horizontal = 12.dp)
                )
                HorizontalDivider(Modifier.padding(bottom = 8.dp))
                DRAWER_GROUPS.forEach { (group, actions) ->
                    if (group != null) Text(
                        tr(group), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                    )
                    actions.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(tr(item.title)) },
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


/** "Tu música en números": reproducciones, horas y los más escuchados. Solo lee lo que ya guarda la app. */
@Composable
fun StatsDialog(songs: List<com.example.creproductor.data.Song>, counts: Map<Long, Int>, onDismiss: () -> Unit) {
    val stats = remember(songs, counts) { com.example.creproductor.data.ListeningStats.of(songs, counts) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Estadísticas")) },
        text = {
            if (stats.plays == 0) Text(tr("Aún no hay reproducciones. Escucha algo de tu biblioteca y vuelve."))
            else Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val hours = stats.listenedMs / 3_600_000.0
                Text("${stats.plays} reproducciones · ${"%.1f".format(hours)} h de música", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(tr("Canciones más escuchadas"), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                val byId = songs.associateBy { it.id }
                stats.topSongs.forEachIndexed { i, (id, n) ->
                    val song = byId[id] ?: return@forEachIndexed
                    Text("${i + 1}. ${song.title} — ${com.example.creproductor.displayArtist(song.artist)} ($n)")
                }
                if (stats.topArtists.isNotEmpty()) {
                    Text(tr("Artistas más escuchados"), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                    stats.topArtists.forEachIndexed { i, (artist, n) -> Text("${i + 1}. $artist ($n)") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } }
    )
}
