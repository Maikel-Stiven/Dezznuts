package com.example.creproductor

import com.example.creproductor.gui.CoverAccentTheme
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.os.Build
import android.widget.Toast
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.creproductor.data.ArtistImageFetcher
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.creproductor.data.Song
import com.example.creproductor.data.SongCover
import com.example.creproductor.data.SongCoverFetcher
import com.example.creproductor.data.coverModel
import com.example.creproductor.gui.AboutDialog
import com.example.creproductor.gui.AppAction
import com.example.creproductor.gui.AppSection
import com.example.creproductor.gui.SettingsScreen
import com.example.creproductor.gui.StatusScreen
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.creproductor.gui.LocalAnimationsEnabled
import com.example.creproductor.gui.AudioCutterDialog
import com.example.creproductor.gui.ClearImageCacheDialog
import com.example.creproductor.gui.EditTagDialog
import com.example.creproductor.gui.FullScreenPlayer
import com.example.creproductor.gui.DuplicatesDialog
import com.example.creproductor.gui.EqualizerDialog
import com.example.creproductor.gui.FixUnknownArtistsDialog
import com.example.creproductor.gui.HiFiAudioDialog
import com.example.creproductor.gui.CoverQualityStrip
import com.example.creproductor.gui.MainScreen
import com.example.creproductor.gui.MusicViewModel
import com.example.creproductor.gui.NavRequest
import com.example.creproductor.gui.ScanScreen
import com.example.creproductor.gui.SleepTimerDialog
import com.example.creproductor.gui.SongMenuActions
import com.example.creproductor.gui.SongMenuSheet
import com.example.creproductor.gui.TrackInfoDialog
import com.example.creproductor.gui.UiState
import com.example.creproductor.ui.theme.DEZZNUTStheme
import kotlinx.coroutines.launch
import coil.Coil
import com.example.creproductor.gui.PlayingEqualizerIcon


private const val FEEDBACK_URL = "https://github.com/Maikel-Stiven/Dezznuts/issues/new"

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    /** Se activa al tocar la notificación del reproductor; la interfaz lo consume para abrir el reproductor completo. */
    private val openPlayerRequest = mutableStateOf(false)

    companion object {
        const val EXTRA_OPEN_PLAYER = "open_player"
    }

    // Con launchMode singleTop, tocar la notificación con la app abierta llega aquí y no crea otra pantalla.
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)) openPlayerRequest.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true) openPlayerRequest.value = true

        // Las portadas son miniaturas: Coil reserva por defecto el 25 % de la RAM de la app para ellas, demasiado en
        // móviles justos. 10 % (5 % en equipos de poca RAM) y RGB565 (la mitad de bytes por píxel) bastan.
        val lowRam = (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager).isLowRamDevice
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(if (lowRam) 0.05 else 0.10).build() }
            .allowRgb565(true)
            .crossfade(false)
            .components {
                // Carátulas: MediaStore o, si el proveedor no las indexa, la embebida del archivo.
                add(SongCoverFetcher.Factory(applicationContext))
                // Sin clave Coil no usa la caché en memoria para este modelo y re-lee/re-decodifica al volver a la fila.
                add(coil.key.Keyer<SongCover> { cover, _ -> cover.cacheKey })
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve(ArtistImageFetcher.IMAGE_CACHE_DIR))
                    .maxSizeBytes(30 * 1024 * 1024)
                    .build()
            }
            .build()
        Coil.setImageLoader (imageLoader)

        setContent {
            DEZZNUTStheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
                    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
                    val currentQueue by viewModel.currentQueue.collectAsStateWithLifecycle()
                    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
                    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
                    val audioInfo by viewModel.audioInfo.collectAsStateWithLifecycle()

                    var songToEdit by remember { mutableStateOf<Song?>(null) }
                    var isPlayerFullScreenVisible by remember { mutableStateOf(false) }
                    // Notificación tocada: se abre el reproductor completo (si aún no hay canción, se muestra en cuanto cargue).
                    LaunchedEffect(openPlayerRequest.value) {
                        if (openPlayerRequest.value) { isPlayerFullScreenVisible = true; openPlayerRequest.value = false }
                    }
                    var showClearImageCacheDialog by remember { mutableStateOf(false) }
                    var showHiFiAudioDialog by remember { mutableStateOf(false) }
                    var showEqualizerDialog by remember { mutableStateOf(false) }
                    var showDuplicatesDialog by remember { mutableStateOf(false) }
                    var showFixArtistsDialog by remember { mutableStateOf(false) }
                    var showAboutDialog by remember { mutableStateOf(false) }
                    var showScanScreen by remember { mutableStateOf(false) }
                    var songToCut by remember { mutableStateOf<Song?>(null) }
                    // Android 9 o menos: guardar en la carpeta pública de Música exige el permiso de escritura.
                    var pendingCut by remember { mutableStateOf<(() -> Unit)?>(null) }
                    val writePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
                        if (ok) pendingCut?.invoke() else Toast.makeText(this@MainActivity, "Sin permiso no se puede guardar el corte", Toast.LENGTH_LONG).show()
                        pendingCut = null
                    }
                    var showSleepTimerDialog by remember { mutableStateOf(false) }
                    val versionName = remember {
                        runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: "—"
                    }
                    // Escribir etiquetas en archivos de audio exige el permiso del sistema (Android 11+): se pide para
                    // las canciones afectadas y, si el usuario acepta, se ejecuta la acción pendiente.
                    var pendingWrite by remember { mutableStateOf<(() -> Unit)?>(null) }
                    val writeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                        if (result.resultCode == RESULT_OK) pendingWrite?.invoke()
                        pendingWrite = null
                    }
                    fun requestWrite(songs: List<Song>, action: () -> Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            pendingWrite = action
                            val request = MediaStore.createWriteRequest(contentResolver, songs.map { it.contentUri })
                            writeLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                        } else action()
                    }
                    // El ⋮ de cualquier canción abre el menú de acciones (SongMenuSheet).
                    var songMenuFor by remember { mutableStateOf<Song?>(null) }
                    var songInfoFor by remember { mutableStateOf<Song?>(null) }
                    val editSong: (Song) -> Unit = { song -> songMenuFor = song }
                    fun openCutter(song: Song) {
                        // Las pistas de una hoja CUE son un tramo de un archivo largo: se cortaría el archivo entero.
                        if (song.cueStartMs >= 0) { Toast.makeText(this@MainActivity, "Las pistas de una hoja CUE no se pueden cortar", Toast.LENGTH_LONG).show(); return }
                        viewModel.pauseIfPlaying()
                        songToCut = song
                    }
                    // Confirmación del sistema para borrar archivos (Android 11+); al terminar se recarga la biblioteca.
                    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                        viewModel.loadSongs(silent = true)
                    }
                    var section by rememberSaveable { mutableStateOf(AppSection.HOME) }
                    // Copia de seguridad y M3U: el usuario elige el archivo con el selector del sistema (sin permisos extra).
                    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
                        uri?.let(viewModel::exportBackup)
                    }
                    val openBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                        uri?.let(viewModel::importBackup)
                    }
                    val openM3u = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                        uri?.let { viewModel.importM3u(it, displayName(it)?.substringBeforeLast('.') ?: "Lista importada") }
                    }
                    var playlistToExport by remember { mutableStateOf<com.example.creproductor.data.Playlist?>(null) }
                    val saveM3u = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
                        val p = playlistToExport
                        if (uri != null && p != null) viewModel.exportPlaylistM3u(p.id, uri)
                        playlistToExport = null
                    }
                    fun runAction(action: AppAction) {
                        when (action) {
                            AppAction.Equalizer -> showEqualizerDialog = true
                            AppAction.SearchDuplicates -> showDuplicatesDialog = true
                            AppAction.FixUnknownArtists -> showFixArtistsDialog = true
                            AppAction.ScanFiles -> showScanScreen = true
                            AppAction.AudioCutter -> {
                                val playing = currentSong
                                if (playing != null) openCutter(playing)
                                else Toast.makeText(this@MainActivity, "Elige una canción desde su menú ⋮ → Cortador de audio", Toast.LENGTH_LONG).show()
                            }
                            AppAction.SleepTimer -> showSleepTimerDialog = true
                            AppAction.ExportBackup -> saveBackup.launch("dezznuts-copia-${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).format(java.util.Date())}.txt")
                            AppAction.ImportBackup -> openBackup.launch(arrayOf("text/*", "application/octet-stream"))
                            AppAction.ImportM3u -> openM3u.launch(arrayOf("audio/x-mpegurl", "audio/mpegurl", "application/vnd.apple.mpegurl", "text/*", "application/octet-stream"))
                            AppAction.ClearImageCache -> showClearImageCacheDialog = true
                            AppAction.HiFiAudio -> showHiFiAudioDialog = true
                            AppAction.About -> showAboutDialog = true
                            AppAction.SendFeedback -> {
                                // Los comentarios van a los "issues" del repositorio.
                                runCatching {
                                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(FEEDBACK_URL)))
                                }.onFailure { Toast.makeText(this@MainActivity, "No hay navegador para abrir el enlace", Toast.LENGTH_LONG).show() }
                            }
                        }
                    }

                    val requiredPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        if (isGranted) {
                            viewModel.loadSongs()
                        }
                    }

                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(requiredPermission)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val state = uiState) {
                            is UiState.PermissionRequired -> StatusScreen(
                                title = "Tu música, sin anuncios ni suscripciones",
                                message = "DEZZNUTS necesita permiso para leer los archivos de audio de este teléfono. No sube nada a internet.",
                                actionLabel = "Permitir acceso",
                                onAction = { permissionLauncher.launch(requiredPermission) }
                            )
                            is UiState.Loading -> StatusScreen(
                                title = "Buscando tu música…",
                                message = "La primera vez puede tardar unos segundos si tienes muchas canciones.",
                                loading = true
                            )
                            is UiState.Empty -> StatusScreen(
                                title = "No encontramos música",
                                message = "Copia archivos de audio a las carpetas Música o Descargas y vuelve a buscar.",
                                actionLabel = "Buscar de nuevo",
                                onAction = { viewModel.loadSongs() },
                                secondaryLabel = "Escanear carpetas",
                                onSecondary = { showScanScreen = true }
                            )
                            is UiState.Success -> {
                                BackHandler(enabled = isPlayerFullScreenVisible) {
                                    isPlayerFullScreenVisible = false
                                }

                                    Box(modifier = Modifier.fillMaxSize()) {
                                        // Lo que queda bajo el reproductor a pantalla completa no anima (no se ve).
                                        CompositionLocalProvider(LocalAnimationsEnabled provides !isPlayerFullScreenVisible) {
                                        // Horizontal en apaisado: el recorte (notch) o la barra lateral no deben tapar el contenido.
                                        // En pantallas bajas (teléfono en horizontal) la navegación pasa a un riel lateral: abajo se
                                        // comía la mitad de la altura entre mini-reproductor y barra.
                                        val compactHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 480
                                        Row(modifier = Modifier.fillMaxSize().windowInsetsPadding(
                                            androidx.compose.foundation.layout.WindowInsets.displayCutout.union(androidx.compose.foundation.layout.WindowInsets.systemBars)
                                                .only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal)
                                        )) {
                                        if (compactHeight) {
                                            androidx.compose.material3.NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                                                AppSection.entries.forEach { item ->
                                                    androidx.compose.material3.NavigationRailItem(
                                                        selected = section == item,
                                                        onClick = { section = item },
                                                        icon = { Icon(item.icon, contentDescription = item.title) },
                                                        alwaysShowLabel = false
                                                    )
                                                }
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                MainScreen(
                                                    viewModel = viewModel,
                                                    section = section,
                                                    onSectionChange = { section = it },
                                                    onEditSong = editSong,
                                                    onExportPlaylist = { playlistToExport = it; saveM3u.launch("${it.name}.m3u8") },
                                                    settingsContent = {
                                                        SettingsScreen(versionName = versionName, onAction = ::runAction)
                                                    }
                                                )
                                            }

                                            AnimatedVisibility(
                                                visible = currentSong != null,
                                                enter = slideInVertically(initialOffsetY = { it }),
                                                exit = slideOutVertically(targetOffsetY = { it })
                                            ) {
                                                currentSong?.let { song ->
                                                    CoverAccentTheme(song) {
                                                        BottomPlayerBar(
                                                            song = song,
                                                            isPlaying = isPlaying,
                                                            onTogglePlayPause = { viewModel.togglePlayPause() },
                                                            onBarClick = { isPlayerFullScreenVisible = true },
                                                            onNext = { viewModel.playNext() },
                                                            onPrevious = { viewModel.playPrevious() }
                                                        )
                                                    }
                                                }
                                            }

                                            // Con el texto del sistema muy grande las etiquetas se partían ("Bibliot-eca"): la barra
                                            // admite hasta 1,2× y cada etiqueta va en una sola línea.
                                            val navDensity = LocalDensity.current
                                            if (!compactHeight) CompositionLocalProvider(
                                                LocalDensity provides Density(navDensity.density, fontScale = navDensity.fontScale.coerceAtMost(1.2f))
                                            ) {
                                                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                                                    AppSection.entries.forEach { item ->
                                                        NavigationBarItem(
                                                            selected = section == item,
                                                            onClick = { section = item },
                                                            icon = { Icon(item.icon, contentDescription = null) },
                                                            label = { Text(item.title, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        }
                                        }

                                        AnimatedVisibility(
                                            visible = isPlayerFullScreenVisible && currentSong != null,
                                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                                        ) {
                                            val playingSong = currentSong ?: return@AnimatedVisibility
                                            val activeSongsQueue = currentQueue.ifEmpty { state.songs }
                                            // Solo aquí se suscribe a la posición, y se entrega como lambda: la lee
                                            // únicamente la barra de progreso, así el resto del reproductor no se recompone cada 500 ms.
                                            val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()

                                            // El reproductor reparte la altura entre portada y controles: con texto del sistema muy grande
                                            // la portada desaparecía, así que aquí el texto admite hasta 1,15×.
                                            val playerDensity = LocalDensity.current
                                            CompositionLocalProvider(
                                                LocalDensity provides Density(playerDensity.density, fontScale = playerDensity.fontScale.coerceAtMost(1.15f))
                                            ) {
                                            CoverAccentTheme(playingSong) {
                                            FullScreenPlayer(
                                                song = playingSong,
                                                songs = activeSongsQueue,
                                                isPlaying = isPlaying,
                                                currentPositionMs = { currentPositionMs },
                                                repeatMode = repeatMode,
                                                isShuffleEnabled = isShuffleEnabled,
                                                audioInfo = audioInfo,
                                                onOpenEqualizer = { showEqualizerDialog = true },
                                                onEditSong = editSong,
                                                onToggleShuffle = { viewModel.toggleShuffle() },
                                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                                onNextClick = { viewModel.playNext() },
                                                onPreviousClick = { viewModel.playPrevious() },
                                                onSeekTo = { pos -> viewModel.seekTo(pos)},
                                                onSongSelected = {song -> viewModel.playSong(song, customQueue = activeSongsQueue)},
                                                onToggleRepeatMode = {viewModel.toggleRepeatMode()},
                                                onRemoveQueue = {song -> viewModel.removeFromQueue(song)},
                                                onMoveQueueItem = { from, to -> viewModel.moveQueueItem(from, to) },
                                                onDismiss = { isPlayerFullScreenVisible = false },
                                                onToggleFavorite = { song -> viewModel.toggleFavorite(song) }
                                            )
                                            }
                                            }
                                        }
                                    }
                            }
                        }

                        songMenuFor?.let { song ->
                            val playlists by viewModel.playlists.collectAsStateWithLifecycle()
                            SongMenuSheet(
                                song = song,
                                playlists = playlists,
                                onDismiss = { songMenuFor = null },
                                actions = SongMenuActions(
                                    onPlayNext = { viewModel.playNextInQueue(song) },
                                    onAddToQueue = { viewModel.addToQueue(song) },
                                    onAddToPlaylist = { id -> viewModel.addSongToPlaylist(id, song) },
                                    onGoToAlbum = { isPlayerFullScreenVisible = false; viewModel.requestNav(NavRequest.Album(song.album)) },
                                    onGoToArtist = { isPlayerFullScreenVisible = false; viewModel.requestNav(NavRequest.Artist(song.artist)) },
                                    onEditTags = { songToEdit = song },
                                    onAbout = { songInfoFor = song },
                                    onAudioCutter = { openCutter(song) },
                                    onDelete = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                            val request = MediaStore.createDeleteRequest(contentResolver, listOf(song.contentUri))
                                            deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                        } else viewModel.deleteSongLegacy(song)
                                    }
                                )
                            )
                        }

                        songInfoFor?.let { song ->
                            TrackInfoDialog(song, audioInfo) { songInfoFor = null }
                        }

                        songToEdit?.let { song ->
                            EditTagDialog(
                                song = song,
                                onDismiss = { songToEdit = null },
                                onSave = { title, artist, album, coverUriOrUrl ->
                                    songToEdit = null
                                    requestWrite(listOf(song)) { viewModel.updatedSongTags(song, title, artist, album, coverUriOrUrl) }
                                }
                            )
                        }

                        songToCut?.let { song ->
                            val cutting by viewModel.cutting.collectAsStateWithLifecycle()
                            AudioCutterDialog(
                                song = song,
                                cutting = cutting,
                                onCut = { start, end ->
                                    val run = { viewModel.cutAudio(song, start, end) { songToCut = null } }
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        pendingCut = run
                                        writePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                    } else run()
                                },
                                onDismiss = { songToCut = null }
                            )
                        }

                        if (showScanScreen) {
                            val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
                            ScanScreen(
                                progress = scanProgress,
                                onStart = { roots, settings -> viewModel.startScan(roots, settings) },
                                onCancel = { viewModel.cancelScan() },
                                onClose = { settings -> viewModel.dismissScan(settings); showScanScreen = false }
                            )
                        }

                        if (showSleepTimerDialog) {
                            val sleepEnd by viewModel.sleepTimerEnd.collectAsStateWithLifecycle()
                            SleepTimerDialog(sleepEnd, onSelect = { viewModel.setSleepTimer(it) }) { showSleepTimerDialog = false }
                        }

                        if (showAboutDialog) AboutDialog(versionName) { showAboutDialog = false }

                        if (showClearImageCacheDialog) {
                            ClearImageCacheDialog(onDismiss = { showClearImageCacheDialog = false })
                        }

                        if (showHiFiAudioDialog) {
                            HiFiAudioDialog(
                                onDismiss = { showHiFiAudioDialog = false },
                                onSettingsChanged = { viewModel.refreshAudioOutput() },
                                onOpenEqualizer = { showEqualizerDialog = true }
                            )
                        }

                        if (showDuplicatesDialog) {
                            DuplicatesDialog(
                                songs = (uiState as? UiState.Success)?.songs.orEmpty(),
                                onDelete = { toDelete ->
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        val request = MediaStore.createDeleteRequest(contentResolver, toDelete.map { it.contentUri })
                                        deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                                    }
                                },
                                onDismiss = { showDuplicatesDialog = false }
                            )
                        }

                        if (showFixArtistsDialog) {
                            FixUnknownArtistsDialog(
                                songs = (uiState as? UiState.Success)?.songs.orEmpty(),
                                onApply = { items ->
                                    showFixArtistsDialog = false
                                    requestWrite(items.map { it.first }) { viewModel.applyTagSuggestions(items) }
                                },
                                onDismiss = { showFixArtistsDialog = false }
                            )
                        }

                        if (showEqualizerDialog) {
                            EqualizerDialog(onDismiss = { showEqualizerDialog = false })
                        }
                    }
                }
            }
        }
    }
}

/** Nombre visible de un archivo elegido con el selector del sistema. */
private fun android.content.Context.displayName(uri: android.net.Uri): String? =
    contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }

@Composable
fun SongListScreen(
    songs: List<Song>,
    currentSongId: Long? = null,
    isPlaying: Boolean = false,
    onSongClick: (Song) -> Unit,
    onEditSong: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(items = songs, key = { it.id }) { song ->
            val isSelected = song.id == currentSongId
            SongItem(
                song = song,
                isSelected = isSelected,
                isPlaying = isSelected && isPlaying,
                onClick = { onSongClick(song) },
                onEditClick = { onEditSong(song) }
            )
        }
    }
}

fun formatDuration(durationMs: Long): String{
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (seconds < 10) "$minutes:0$seconds" else "$minutes:$seconds"
}

@Composable
fun SongItem(
    song: Song,
    isSelected: Boolean = false,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val backgroundColor = if (isSelected){
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    }else{
        MaterialTheme.colorScheme.background
    }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.coverModel,
                        contentDescription = "Portada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                // Calidad (Lossless / Hi-Res) en la base de la portada: el título conserva todo el ancho de la fila.
                CoverQualityStrip(song.quality, Modifier.align(Alignment.BottomCenter))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${displayArtist(song.artist)} • ${formatDuration(song.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelected){
                PlayingEqualizerIcon(
                    color = MaterialTheme.colorScheme.primary,
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

}

/**
 * Mini-reproductor flotante. Tocar o deslizar hacia arriba abre el reproductor; deslizar a los lados cambia de canción.
 */
@Composable
fun BottomPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onBarClick: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {}
) {
    val swipePx = with(LocalDensity.current) { 56.dp.toPx() }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                var dx = 0f; var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDragEnd = {
                        when {
                            -dy > swipePx && -dy > kotlin.math.abs(dx) -> onBarClick()
                            dx < -swipePx -> onNext()
                            dx > swipePx -> onPrevious()
                        }
                    }
                ) { change, drag -> change.consume(); dx += drag.x; dy += drag.y }
            },
        onClick = onBarClick,
        shape = MaterialTheme.shapes.large,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.coverModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee()
                )
                Text(
                    text = displayArtist(song.artist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledIconButton(
                onClick = onTogglePlayPause,
                modifier = Modifier.size(44.dp),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** "<unknown>" de MediaStore o vacío → "Artista desconocido". */
fun displayArtist(artist: String): String =
    if (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)) "Artista desconocido" else artist
