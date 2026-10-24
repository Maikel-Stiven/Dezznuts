package com.example.creproductor.gui

import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import com.example.creproductor.i18n.tr
import com.example.creproductor.ui.theme.swatch

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
    object SearchDuplicates : AppAction("Duplicados y WhatsApp", "Encontrar copias para liberar espacio", Icons.Default.FindInPage)
    object FixUnknownArtists : AppAction("Artistas desconocidos", "Deducir artista y título del nombre del archivo", Icons.Default.AutoFixHigh)
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

/** Selector de tema de un vistazo: un círculo por tema (fondo + acento); el elegido lleva anillo. */
@Composable
fun ThemeSwatches(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        com.example.creproductor.ui.theme.ThemeMode.entries
            .filter { it != com.example.creproductor.ui.theme.ThemeMode.DYNAMIC || com.example.creproductor.ui.theme.ThemeSetting.dynamicAvailable }
            .forEach { m ->
                val (bg, accent) = m.swatch()
                val selected = com.example.creproductor.ui.theme.ThemeSetting.mode == m
                val name = tr(m.label)
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .border(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape)
                        .background(bg)
                        .clickable(onClickLabel = name) { com.example.creproductor.ui.theme.ThemeSetting.set(context, m) }
                        .semantics { contentDescription = name; this.selected = selected }
                ) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.align(Alignment.Center).size(16.dp).clip(androidx.compose.foundation.shape.CircleShape).background(accent)
                    )
                }
            }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AppDrawerContent(versionName: String, onAction: (AppAction) -> Unit, onCloseDrawer: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface, modifier = Modifier.width(312.dp)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp)) {
                // Cabecera sobria: logotipo en tipografía y una línea de descripción, sin marco ni degradado.
                Column(Modifier.padding(start = 16.dp, top = 28.dp, bottom = 20.dp)) {
                    Text(
                        tr("DEZZNUTS"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp, color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${tr("Tu música, sin vueltas")} · v$versionName",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                DRAWER_GROUPS.forEach { (group, actions) ->
                    if (group != null) Text(
                        tr(group), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 4.dp)
                    )
                    actions.forEach { item ->
                        NavigationDrawerItem(
                            label = { Text(tr(item.title), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                            icon = { Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp)) },
                            selected = false,
                            onClick = { onCloseDrawer(); onAction(item) },
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }
            HorizontalDivider()
            Column(Modifier.padding(horizontal = 28.dp, vertical = 14.dp)) {
                Text(tr("Tema"), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 10.dp))
                ThemeSwatches()
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
