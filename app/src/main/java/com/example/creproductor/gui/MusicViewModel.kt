package com.example.creproductor.gui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.creproductor.data.AudioCutter
import com.example.creproductor.data.AudioQuality
import com.example.creproductor.data.AudioTagWriter
import com.example.creproductor.data.TagSuggestion
import com.example.creproductor.data.TagSuggestions
import com.example.creproductor.data.HiFiAudio
import com.example.creproductor.data.QualityTier
import com.example.creproductor.data.FileScanner
import com.example.creproductor.data.MediaStoreScanner
import com.example.creproductor.data.ScanProgress
import com.example.creproductor.data.ScanSettings
import com.example.creproductor.data.Song
import com.example.creproductor.data.SongCoverCache
import com.example.creproductor.data.trackPlaybackPosition
import com.example.creproductor.service.MusicService
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.example.creproductor.data.Playlist
import com.example.creproductor.data.PlaylistSerializer
import com.example.creproductor.data.RepeatMode
import com.example.creproductor.data.SongOrganizer
import com.example.creproductor.data.next
import com.example.creproductor.service.PlaybackDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Petición de ir a la ficha de un álbum o artista desde el menú de una canción; la consume `MainScreen`. */
sealed interface NavRequest {
    data class Album(val name: String) : NavRequest
    data class Artist(val name: String) : NavRequest
}

sealed interface UiState {
    object PermissionRequired : UiState
    object Loading : UiState
    data class Success(val songs: List<Song>) : UiState
    object Empty : UiState
}

enum class SongFilter{
    ALL,
    FAVORITES,
    RECENT,
    LOSSLESS,
    HIRES
}

/** Tamaño de la primera publicación de la biblioteca y factor de crecimiento de las siguientes. */
private const val FIRST_PUBLISH_SIZE = 100
private const val PUBLISH_GROWTH = 4
private const val MAX_DELETED_PLAYLISTS = 10

class MusicViewModel (application: Application) : AndroidViewModel(application) {

    private val scanner = MediaStoreScanner(application.applicationContext)
    private val prefs = application.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE)

    // Mantiene la UI sincronizada cuando los favoritos cambian fuera del ViewModel
    // (por ejemplo, al pulsar el botón de favorito en la notificación).
    private val favPrefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "fav_ids") syncFavoritesFromPrefs()
            else if (key == HiFiAudio.KEY_REAL_ROUTE) refreshAudioInfo()
            else if (key == MusicService.KEY_SLEEP_END) _sleepTimerEnd.value = prefs.getLong(MusicService.KEY_SLEEP_END, 0L)
        }

    private val _uiState = MutableStateFlow<UiState>(UiState.PermissionRequired)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()


    private var mediaController: MediaController? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** Modo aleatorio (shuffle); se sincroniza con el controlador de Media3. */
    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    /** Canción en reproducción (o `null` si no hay ninguna). */
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    /** Datos técnicos de la pista que suena (códec, bits, kHz, kbps) y ruta de salida; vacío si no hay. */
    private val _audioInfo = MutableStateFlow("")
    val audioInfo: StateFlow<String> = _audioInfo.asStateFlow()

    /** Posición de reproducción en milisegundos (la actualiza el *tracker* cada 500 ms). */
    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    /** Filtro activo de la pestaña "Canciones" (todas / favoritas / recientes). */
    private val _currentFilter = MutableStateFlow(SongFilter.ALL)
    val currentFilter: StateFlow<SongFilter> = _currentFilter.asStateFlow()

    /** Texto de búsqueda actual. */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Respaldo en memoria de `id → nº de reproducciones` (origen de "Más reproducidas"). */
    private val playCountMap = mutableMapOf<Long, Int>()

    /** Canciones ordenadas por número de reproducciones (descendente). */
    private val _mostPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val mostPlayedSongs: StateFlow<List<Song>> = _mostPlayedSongs.asStateFlow()

    /** Últimas canciones reproducidas, de la más reciente a la más antigua. */
    private val _recentPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentPlayedSongs: StateFlow<List<Song>> = _recentPlayedSongs.asStateFlow()

    /** Derivado de [uiState]: solo las canciones marcadas como favoritas. */
    val favoriteSongs: StateFlow<List<Song>> = uiState.map { state ->
        if (state is UiState.Success) state.songs.filter { it.isFavorite } else emptyList()
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /** Historial de búsquedas recientes (la más reciente primero). */
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    /** Cola de reproducción actual, en el orden que ve el usuario. */
    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    /** Modo de repetición actual (ver [RepeatMode]). */
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    /** Listas borradas que se pueden restaurar (las más recientes primero, hasta [MAX_DELETED_PLAYLISTS]). */
    private val _deletedPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val deletedPlaylists: StateFlow<List<Playlist>> = _deletedPlaylists.asStateFlow()

    /** Listas de reproducción creadas por el usuario. */
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    /**
     * Canciones visibles en la pestaña "Canciones". Combina tres fuentes:
     * la biblioteca ([uiState]), el [currentFilter] y el [searchQuery]
     * (que busca en título, artista o álbum).
     */
    val filteredSongs: StateFlow<List<Song>> = combine(
        uiState,
        _currentFilter,
        _searchQuery
    ){
        state, filter, query ->
        if (state !is UiState.Success) return@combine emptyList()

        state.songs.filter { song ->
            val matchesFilter = when (filter){
                SongFilter.ALL -> true
                SongFilter.FAVORITES -> song.isFavorite
                SongFilter.RECENT -> true
                SongFilter.LOSSLESS -> song.quality != QualityTier.LOSSY
                SongFilter.HIRES -> song.quality == QualityTier.HIRES
            }
            val matchesQuery = SongOrganizer.matchesQuery(song.title, song.artist, song.album, query)
            matchesFilter && matchesQuery
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())


    private val _navRequest = MutableStateFlow<NavRequest?>(null)
    val navRequest: StateFlow<NavRequest?> = _navRequest.asStateFlow()
    fun requestNav(request: NavRequest) { _navRequest.value = request }
    fun clearNav() { _navRequest.value = null }

    /** Añade la canción al final de la cola; si no hay nada sonando, la reproduce. */
    fun addToQueue(song: Song) {
        val controller = mediaController
        if (controller == null || controller.mediaItemCount == 0) { playSong(song, customQueue = listOf(song)); return }
        _currentQueue.value = _currentQueue.value.ifEmpty { (_uiState.value as? UiState.Success)?.songs ?: emptyList() } + song
        saveCurrentQueueIds(_currentQueue.value.map { it.id })
        controller.addMediaItems(buildMediaItems(listOf(song)))
        Toast.makeText(getApplication(), "Agregada a la cola", Toast.LENGTH_SHORT).show()
    }

    /** Borra el archivo (Android < 11; en 11+ lo hace el sistema con su propia confirmación) y recarga la biblioteca. */
    fun deleteSongLegacy(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching { getApplication<Application>().contentResolver.delete(song.contentUri, null, null) > 0 }.getOrDefault(false)
            withContext(Dispatchers.Main) {
                if (!ok) Toast.makeText(getApplication(), "No se pudo eliminar", Toast.LENGTH_LONG).show()
                loadSongs(silent = true)
            }
        }
    }

    fun addSongToPlaylist(playlistId: String, song: Song) {
        addSongsToPlaylist(playlistId, listOf(song.id))
        Toast.makeText(getApplication(), "Agregada a la lista", Toast.LENGTH_SHORT).show()
    }

    /** Estado del temporizador de apagado: 0 = apagado, -1 = al final de la canción, otro = hora de fin (ms). */
    private val _sleepTimerEnd = MutableStateFlow(0L)
    val sleepTimerEnd: StateFlow<Long> = _sleepTimerEnd.asStateFlow()

    /** [minutes]: > 0 minutos, [MusicService.SLEEP_END_OF_TRACK] o 0 para cancelar. */
    fun setSleepTimer(minutes: Int) {
        mediaController?.sendCustomCommand(
            SessionCommand(MusicService.ACTION_SLEEP_TIMER, Bundle.EMPTY),
            Bundle().apply { putInt(MusicService.EXTRA_MINUTES, minutes) }
        )
    }

    /** Escaneo manual de carpetas ("Escanear archivos"): `null` = sin escaneo en curso ni resultado que mostrar. */
    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    val scanProgress: StateFlow<ScanProgress?> = _scanProgress.asStateFlow()
    private var scanJob: Job? = null

    fun startScan(roots: List<java.io.File>, settings: ScanSettings) {
        if (scanJob?.isActive == true || roots.isEmpty()) return
        settings.save(getApplication())
        scanJob = viewModelScope.launch {
            FileScanner.scan(getApplication(), roots, settings).collect { _scanProgress.value = it }
            loadSongs(silent = true)   // aplica también los filtros nuevos a la biblioteca
        }
    }

    fun cancelScan() { scanJob?.cancel(); _scanProgress.value = null }

    /** Cierra el resultado del escaneo (y guarda los filtros aunque no se haya escaneado). */
    fun dismissScan(settings: ScanSettings) {
        if (scanJob?.isActive == true) return
        if (settings != ScanSettings.read(getApplication())) { settings.save(getApplication()); loadSongs(silent = true) }
        _scanProgress.value = null
    }

    /** Recorte de audio en curso (el diálogo del cortador se bloquea mientras tanto). */
    private val _cutting = MutableStateFlow(false)
    val cutting: StateFlow<Boolean> = _cutting.asStateFlow()

    fun cutAudio(song: Song, startMs: Long, endMs: Long, onDone: () -> Unit) {
        if (_cutting.value) return
        viewModelScope.launch {
            _cutting.value = true
            val result = withContext(Dispatchers.IO) { AudioCutter.cut(getApplication(), song, startMs, endMs) }
            _cutting.value = false
            Toast.makeText(
                getApplication(),
                if (result != null) "Guardado: ${result.displayName} (Música/DEZZNUTS)" else "No se pudo cortar este archivo",
                Toast.LENGTH_LONG
            ).show()
            if (result != null) { onDone(); loadSongs(silent = true) }
        }
    }

    /** Pausa si suena (el cortador se abre con la música en silencio para poder previsualizar). */
    fun pauseIfPlaying() { mediaController?.takeIf { it.isPlaying }?.pause() }

    private var loadJob: Job? = null
    private var observerJob: Job? = null

    /**
     * MediaStore avisa varias veces seguidas por cada archivo nuevo, así que se
     * espera a que se calme y se recarga una sola vez, en silencio (sin pasar
     * por `Loading`, para no tapar la lista ni interrumpir lo que suena).
     */
    private val mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            if (_uiState.value is UiState.PermissionRequired) return
            observerJob?.cancel()
            observerJob = viewModelScope.launch {
                delay(1500)
                loadSongs(silent = true)
            }
        }
    }

    init {
        loadSavedPlaycounts()
        initMediaController()
        startPositionTracker()
        loadSearchHistory()
        loadPlaylist()
        prefs.registerOnSharedPreferenceChangeListener(favPrefsListener)
        application.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, mediaStoreObserver
        )
    }

    private fun initMediaController(){
        val context = getApplication<Application>()
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )

        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {

                val controller = controllerFuture.get()
                mediaController = controller

                _isPlaying.value = controller.isPlaying
                _isShuffleEnabled.value = controller.shuffleModeEnabled

                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        if (!isPlaying) persistPosition()
                    }

                    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                        _isShuffleEnabled.value = shuffleModeEnabled
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        lastTracks = tracks
                        _audioInfo.value = describeAudio(tracks)
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED){
                            _isPlaying.value = false
                            persistPosition()
                        }
                    }

                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                      if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && _repeatMode.value == RepeatMode.STOP_AFTER_CURRENT) {
                          controller.pause()
                          controller.seekTo(0, 0L)
                          return
                      }
                        mediaItem?.mediaId?.let { idStr ->

                            val allSongs = (_uiState.value as?  UiState.Success)?.songs ?: emptyList()
                            val activeQueue = _currentQueue.value.ifEmpty { allSongs }

                            val foundSong = activeQueue.find { it.id.toString() == idStr }
                                ?: (_uiState.value as? UiState.Success)?.songs?.find { it.id.toString() == idStr }

                            if (foundSong !== null){
                                _currentSong.value = foundSong
                                registerSongPlay(foundSong)
                            }
                        }
                        // Un cambio de cola (incluida la restauración inicial de setMediaItems) no es una
                        // reproducción: guardar aquí escribiría 0 y pisaría last_position_ms.
                        // AUTO, SEEK y REPEAT sí son movimientos reales.
                        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) persistPosition()
                    }
                })
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    /**
     * Describe la pista de audio activa con los datos reales que ve el reproductor y,
     * debajo, la ruta de salida **solicitada** (offload / float / PCM): si el chip no
     * soporta offload, ExoPlayer cae a PCM por su cuenta sin avisar a la app.
     */
    private var lastTracks: Tracks? = null

    private fun refreshAudioInfo() { lastTracks?.let { _audioInfo.value = describeAudio(it) } }

    private fun describeAudio(tracks: Tracks): String {
        val format = tracks.groups
            .firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?.let { g -> (0 until g.length).firstOrNull { g.isTrackSelected(it) }?.let { g.getTrackFormat(it) } }
            ?: return ""
        val song = _currentSong.value
        val bits = AudioQuality.bitsOfPcmEncoding(format.pcmEncoding).takeIf { it > 0 } ?: song?.bitsPerSample ?: 0
        val label = AudioQuality.label(
            format.sampleMimeType,
            format.sampleRate.takeIf { it > 0 } ?: song?.sampleRate ?: 0,
            bits,
            format.bitrate.takeIf { it > 0 } ?: song?.bitrate ?: 0
        )
        val ctx = getApplication<Application>()
        // Ruta real publicada por el servicio; hasta que llegue, la solicitada.
        val route = prefs.getString(HiFiAudio.KEY_REAL_ROUTE, null) ?: when {
            HiFiAudio.isOffloadEnabled(ctx) -> "Offload"
            HiFiAudio.isFloatOutputEnabled(ctx) && bits > 16 -> "Float 32 bit"
            else -> "PCM"
        }
        return if (label.isEmpty()) "" else "$label → $route"
    }

    /**
     * Pide al servicio que recree el reproductor para aplicar los ajustes de
     * audio de alta fidelidad (salida float / offload) sin reiniciar la app ni
     * perder la cola de reproducción. Va por el mismo canal de comandos de
     * sesión que ya usan favorito y cerrar.
     */
    fun refreshAudioOutput() {
        mediaController?.sendCustomCommand(
            SessionCommand(MusicService.ACTION_REFRESH_AUDIO, Bundle.EMPTY),
            Bundle.EMPTY
        )
    }

    /**
     * Sondea la posición solo mientras la barra la observa y hay reproducción.
     *
     * El bucle vive en [trackPlaybackPosition] (lógica pura, con pruebas). `subscriptionCount`
     * cuenta los colectores de [_currentPositionMs] (el `asStateFlow()` delega en él); hoy el
     * único es MainActivity, dentro de `if (isPlayerFullScreenVisible…)`. Si alguien más empieza
     * a recolectar `currentPositionMs`, el bucle correrá también con la pantalla cerrada.
     */
    private fun startPositionTracker(){
        viewModelScope.trackPlaybackPosition(
            position = _currentPositionMs,
            isPlaying = _isPlaying,
            readPosition = {
                mediaController?.takeIf { it.currentMediaItem != null }?.currentPosition
            }
        )
    }

    /**
     * Guarda la posición y la canción actuales.
     *
     * Se llama en eventos reales del reproductor (pausa, fin, transición y cierre), nunca con un
     * temporizador: así no hay una escritura por segundo mientras suena.
     */
    private fun persistPosition() {
        val controller = mediaController ?: return
        val songId = controller.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        // Al terminar la pista la posición es la duración; guardar 0 evita reabrir en el final.
        val position = if (controller.playbackState == Player.STATE_ENDED) 0L else controller.currentPosition
        prefs.edit {
            putLong("last_position_ms", position)
            putLong("last_song_id", songId)
        }
    }


    private fun loadSearchHistory(){
        val raw = prefs.getString("search_history_str", "") ?: ""
        _searchHistory.value = if (raw.isBlank()) emptyList() else raw.split("|")
    }

    fun addSearchHistory(query: String){
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return
        val currentList = _searchHistory.value.toMutableList()
        currentList.remove(cleanQuery)
        currentList.add(0, cleanQuery)
        val updatedList = currentList.take(10)
        _searchHistory.value = updatedList
        prefs.edit{putString("search_history_str", updatedList.joinToString ("|"))}
    }

    fun removeFromHistory(query: String){
        val updatedList = _searchHistory.value.filter { it != query }
        _searchHistory.value = updatedList
        prefs.edit{putString("search_history_str", updatedList.joinToString("|"))}
    }

    fun setFilter(filter: SongFilter){
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String){
        _searchQuery.value = query
    }

    /**
     * Construye los items de reproducción de una cola.
     *
     * El `mediaId` es el id de la canción porque el servicio lo usa para saber
     * si el tema actual es favorito (ver `MusicService.toggleCurrentFavorite`).
     */
    private fun buildMediaItems(queue: List<Song>): List<MediaItem> {
        var porRuta = 0
        val t0 = SystemClock.elapsedRealtime()
        val items = queue.map { song ->
            val uri = playbackUri(song)
            if (uri.scheme == "file") porRuta++
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(uri)
                .apply {
                    // Pista de una hoja CUE: se reproduce solo su tramo del archivo largo.
                    if (song.cueStartMs >= 0) {
                        setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(song.cueStartMs)
                                .setEndPositionMs(song.cueEndMs)
                                .build()
                        )
                    }
                }
                .build()
        }
        // Si la ruta no es legible, la app cae a content:// sin avisar y el
        // ahorro de `playbackUri` no se aplica; esta línea lo deja claro.
        Log.i(
            "DezzSeek",
            "cola de ${queue.size} pistas: $porRuta por file://, " +
                "${queue.size - porRuta} por content:// (${SystemClock.elapsedRealtime() - t0} ms)"
        )
        return items
    }

    /**
     * URI con el que se le entrega el archivo a ExoPlayer.
     *
     * Se prefiere la **ruta directa** (`file://`) al `content://` de MediaStore
     * por una razón concreta y medida: cada *seek* que cae fuera del búfer
     * obliga a ExoPlayer a **reabrir la fuente**, y `content://` suma en cada
     * apertura una llamada al proveedor de medios (además del paso por la capa
     * de almacenamiento). Con la ruta directa se ahorra esa llamada, lo que se
     * nota justo cuando el extractor necesita varios accesos seguidos
     * (p. ej. un FLAC sin bloque `SEEKTABLE`).
     *
     * Si la ruta no existe o no se puede leer (permisos, almacenamiento
     * extraíble, archivos que solo expone el proveedor), se vuelve al
     * `content://`, que siempre funciona.
     */
    private fun playbackUri(song: Song): Uri {
        val cached = playbackUriCache[song.id]
        if (cached != null && cached.path == song.path) return cached.uri   // la ruta no cambió: sigue valiendo
        val path = song.path
        val file = if (path.isNotEmpty()) File(path) else null
        val uri = if (file != null && file.canRead()) Uri.fromFile(file) else song.contentUri
        playbackUriCache[song.id] = ResolvedUri(path, uri)
        return uri
    }

    private class ResolvedUri(val path: String, val uri: Uri)

    /**
     * `id → URI` ya resuelto: evita un `canRead()` (un acceso al almacenamiento) por canción en cada cambio de cola.
     * Se guarda con la ruta con la que se resolvió, así que una canción cuya ruta cambia se vuelve a resolver sola y
     * **no hace falta vaciarla cuando la biblioteca se recarga** (antes, cada canción nueva obligaba a repetir los miles
     * de accesos en la siguiente reproducción: justo el "se demora al entrar a la canción").
     */
    private val playbackUriCache = java.util.concurrent.ConcurrentHashMap<Long, ResolvedUri>()

    /**
     * Resuelve en segundo plano los URI que faltan, para que tocar una canción no pague esos accesos. Va por tandas y
     * con paralelismo limitado: en el almacenamiento de un móvil cada acceso es lento y la espera es de E/S, no de CPU.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun warmUpPlaybackUris(songs: List<Song>) {
        val missing = songs.filter { playbackUriCache[it.id]?.path != it.path }
        if (missing.isEmpty()) return
        val t0 = SystemClock.elapsedRealtime()
        withContext(Dispatchers.IO.limitedParallelism(4)) {
            coroutineScope { missing.chunked(250).map { chunk -> async { chunk.forEach { playbackUri(it) } } }.awaitAll() }
        }
        Log.i("DezzSeek", "URI de reproducción precalculados: ${missing.size} en ${SystemClock.elapsedRealtime() - t0} ms")
    }

    fun seekTo(positionMs: Long){
        // Marca el inicio de la medición para la traza de PlaybackDiagnostics.
        PlaybackDiagnostics.markSeekRequest(positionMs)
        _currentPositionMs.value = positionMs
        mediaController?.seekTo(positionMs)
    }
    fun loadSongs(silent: Boolean = false){
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Traza de carga: `adb logcat -s DezzLoad` (ver también DezzSeek).
            val t0 = SystemClock.elapsedRealtime()
            var publishes = 0
            if (!silent) _uiState.value = UiState.Loading
            val accumulatedSongs = mutableListOf<Song>()

            // Cada publicación repinta toda la interfaz y rehace los agrupados, así
            // que se publica en tramos que crecen x4 (100, 400, 1600…): se ve algo
            // enseguida y el total de repintados es logarítmico, no n/100.
            var nextPublishAt = FIRST_PUBLISH_SIZE
            scanner.scanAudioFiles(chunkSize = 100).collect { chunk ->
                accumulatedSongs.addAll(chunk)
                // En una recarga silenciosa se publica al final: publicar por
                // trozos haría que la lista encoja y vuelva a crecer.
                if (!silent && accumulatedSongs.size >= nextPublishAt) {
                    nextPublishAt *= PUBLISH_GROWTH
                    publishSongs(accumulatedSongs.toList())
                    if (++publishes == 1) {
                        Log.i("DezzLoad", "[+${SystemClock.elapsedRealtime() - t0} ms] primera lista publicada (${accumulatedSongs.size} canciones)")
                    }
                }
            }

            if (accumulatedSongs.isEmpty()){
                _uiState.value = UiState.Empty
            } else if (silent || (_uiState.value as? UiState.Success)?.songs?.size != accumulatedSongs.size) {
                publishSongs(accumulatedSongs)
                publishes++
            }
            Log.i("DezzLoad", "[+${SystemClock.elapsedRealtime() - t0} ms] carga terminada: ${accumulatedSongs.size} canciones, $publishes publicaciones, silent=$silent")
        }
    }

    private fun publishSongs(songs: List<Song>){
        viewModelScope.launch(Dispatchers.Default) {
            val savedFavIds = getSavedFavoritesIds()
            val updated = songs.map { song ->
                if (savedFavIds.contains(song.id)) song.copy(isFavorite = true) else song
            }
            // Canciones que ya no están: fuera de la caché (las nuevas se resuelven en el precalentamiento).
            val ids = updated.mapTo(HashSet()) { it.id }
            playbackUriCache.keys.retainAll(ids)
            withContext(Dispatchers.Main){
                _uiState.value = UiState.Success(updated)
                restoreDashboardLists(updated)
            }
            warmUpPlaybackUris(updated)
        }
    }

    private fun registerSongPlay(song: Song){

        viewModelScope.launch (Dispatchers.Default){
            val currentCount = playCountMap.getOrDefault(song.id, 0) + 1
            playCountMap[song.id] = currentCount
            savePlayCounts()

            val allSongs = (_uiState.value as? UiState.Success)?.songs ?: return@launch

            val mostPlayed = allSongs
                .filter { (playCountMap[it.id] ?: 0) > 0 }
                .sortedByDescending { playCountMap[it.id] ?: 0 }
                .take(30)

            val currentRecentSnapshot = _recentPlayedSongs.value.toList()
            val updatedRecent = (listOf(song) + _recentPlayedSongs.value)
                .distinctBy{it.id}
                .take(30)

            withContext(Dispatchers.Main){
                _mostPlayedSongs.value = mostPlayed
                _recentPlayedSongs.value = updatedRecent
            }

            saveRecentIds(updatedRecent.map {it.id})
            prefs.edit { putLong("last_song_id", song.id) }
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0){
        if (songs.isEmpty() || startIndex !in songs.indices) return
        val targetSong = songs[startIndex]
        playSong(targetSong, customQueue = songs)
    }

    fun playSong(song: Song, customQueue: List<Song>? = null){

        val queueToUse= if (!customQueue.isNullOrEmpty()) customQueue else (_uiState.value as? UiState.Success)?.songs ?: listOf(song)

        _currentQueue.value = queueToUse
        saveCurrentQueueIds(queueToUse.map { it.id })
        registerSongPlay(song)

        // Pista elegida por el usuario: empieza en 0, así que la posición guardada debe ser 0.
        // En las transiciones PLAYLIST_CHANGED no se persiste; esto cubre ese hueco.
        _currentPositionMs.value = 0L
        prefs.edit {
            putLong("last_song_id", song.id)
            putLong("last_position_ms", 0L)
        }

        val controller = mediaController ?: return
        val targetIndex = queueToUse.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        if (controller.mediaItemCount == queueToUse.size){
            controller.seekTo(targetIndex, 0L)
        }else{
            viewModelScope.launch(Dispatchers.Default) {
                // Misma ruta que togglePlayPause: caché de URI y respaldo a content:// si la ruta no es legible.
                val mediaItems = buildMediaItems(queueToUse)

                withContext(Dispatchers.Main){
                    controller.setMediaItems(mediaItems, targetIndex, 0L)
                    controller.prepare()
                    controller.play()
                }
            }
        }
    }

    fun togglePlayPause(){
        mediaController?.let { controller ->
            if (controller.isPlaying){
                controller.pause()
            }else{
                if (controller.mediaItemCount == 0 || controller.currentMediaItem == null){
                    val song = _currentSong.value ?: return
                    val queueToUse = _currentQueue.value.ifEmpty { (_uiState.value as? UiState.Success)?.songs ?: listOf() }

                    viewModelScope.launch(Dispatchers.Default) {
                        val targetIndex = queueToUse.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                        val savedPosition = _currentPositionMs.value
                        val mediaItems = buildMediaItems(queueToUse)

                        withContext(Dispatchers.Main){
                            controller.setMediaItems(mediaItems, targetIndex, savedPosition)
                            controller.prepare()
                            controller.play()
                        }
                    }
                }else if (controller.playbackState == Player.STATE_ENDED){
                    controller.seekTo(0, 0L)
                    controller.prepare()
                    controller.play()
                }else if (controller.playbackState == Player.STATE_IDLE){
                    controller.prepare()
                    controller.play()
                }else{
                    controller.play()
                }
            }
        }
    }

    /**
     * Alterna el modo aleatorio. Se apoya en `shuffleModeEnabled` del reproductor
     * (que reordena la cola de verdad) y refleja el resultado en [isShuffleEnabled].
     */
    fun toggleShuffle(){
        mediaController?.let { controller ->
            val newValue = !controller.shuffleModeEnabled
            controller.shuffleModeEnabled = newValue
            _isShuffleEnabled.value = newValue
        }
    }

    fun toggleRepeatMode(){
        val nextMode = _repeatMode.value.next()
        _repeatMode.value =nextMode

        mediaController?.let { controller ->
            when (nextMode){
                RepeatMode.OFF -> controller.repeatMode = Player.REPEAT_MODE_OFF
                RepeatMode.ALL -> controller.repeatMode = Player.REPEAT_MODE_ALL
                RepeatMode.ONE -> controller.repeatMode = Player.REPEAT_MODE_ONE
                RepeatMode.STOP_AFTER_CURRENT -> controller.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    /**
     * Corrige varias canciones a la vez con las sugerencias (artista y título del nombre del archivo). Una a una y en
     * segundo plano: cada escritura copia el archivo, así que no se paraleliza. Informa del resultado al final.
     */
    fun applyTagSuggestions(items: List<Pair<Song, TagSuggestion>>) {
        if (items.isEmpty()) return
        Toast.makeText(getApplication(), "Corrigiendo ${items.size} canciones…", Toast.LENGTH_SHORT).show()
        viewModelScope.launch {
            var ok = 0
            for ((song, suggestion) in items) {
                // El álbum desconocido se deja como está (vacío = no tocar).
                val album = if (TagSuggestions.isUnknown(song.album)) "" else song.album
                if (AudioTagWriter.writeMetaData(getApplication(), song, suggestion.title, suggestion.artist, album, null)) ok++
            }
            loadSongs(silent = true)
            Toast.makeText(getApplication(), "Se corrigieron $ok de ${items.size}", Toast.LENGTH_LONG).show()
        }
    }

    fun updatedSongTags(
        song: Song,
        newTitle: String,
        newArtist: String,
        newAlbum: String,
        coverUriOrUrl: String?
    ){
        viewModelScope.launch {
            val success = AudioTagWriter.writeMetaData(
                context = getApplication(),
                song = song,
                newTitle = newTitle,
                newArtist = newArtist,
                newAlbum = newAlbum,
                coverUrlOrUri = coverUriOrUrl
            )
            if (success){
                SongCoverCache.invalidate(getApplication(), song)
                loadSongs(silent = true)
            } else {
                Toast.makeText(getApplication(), "No se pudieron guardar los tags", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCleared() {
        persistPosition()
        prefs.unregisterOnSharedPreferenceChangeListener(favPrefsListener)
        getApplication<Application>().contentResolver.unregisterContentObserver(mediaStoreObserver)
        mediaController?.release()
    }

    /**
     * Relee los favoritos guardados y actualiza la biblioteca y la canción actual.
     *
     * Se invoca desde [favPrefsListener] cuando el botón de favorito de la
     * **notificación** cambia `fav_ids` fuera del ViewModel, para que la UI no
     * quede desincronizada.
     */
    private fun syncFavoritesFromPrefs(){
        val currentState = _uiState.value as? UiState.Success ?: return
        val favIds = getSavedFavoritesIds()
        // toggleFavorite ya actualizó el estado antes de guardar: evita remapear toda la biblioteca otra vez.
        if (currentState.songs.none { it.isFavorite != favIds.contains(it.id) }) return
        val updatedSongs = currentState.songs.map { it.copy(isFavorite = favIds.contains(it.id)) }
        _uiState.value = UiState.Success(updatedSongs)

        _currentSong.value = _currentSong.value?.let { it.copy(isFavorite = favIds.contains(it.id)) }
    }

    fun playNext(){
        mediaController?.let { controller ->
            if (controller.hasNextMediaItem()){
                controller.seekToNextMediaItem()
            }
        }
    }

    fun playPrevious(){
        mediaController?.let { controller ->
            if (controller.hasPreviousMediaItem()){
                controller.seekToPreviousMediaItem()
            }
        }
    }

    fun toggleFavorite(song: Song){
        val currentState = _uiState.value
        if (currentState is UiState.Success){
           val updatedSongs = currentState.songs.map { item ->
               if (item.id ==  song.id){
                   item.copy(isFavorite = !item.isFavorite)
               }else{
                   item
               }
           }

            _uiState.value = UiState.Success(updatedSongs)

            if (_currentSong.value?.id ==  song.id){
                _currentSong.value = _currentSong.value?.copy(isFavorite = !song.isFavorite)
            }

            val  favIds = updatedSongs.filter { it.isFavorite }.map { it.id }.toSet()
            saveFavoriteIds(favIds)
        }
    }

    fun removeFromQueue(song: Song){
        val updateQueue = _currentQueue.value.filter { it.id  != song.id }
        _currentQueue.value = updateQueue
        saveCurrentQueueIds(updateQueue.map { it.id })

        mediaController?.let { controller ->
            val indexToRemove = (0 until controller.mediaItemCount).firstOrNull {index ->
                controller.getMediaItemAt(index).mediaId == song.id.toString()
            }

            if (indexToRemove != null) {
                controller.removeMediaItem(indexToRemove)
            }
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int){
        val currentList = _currentQueue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices){
            val movedItem = currentList.removeAt(fromIndex)
            currentList.add(toIndex, movedItem)
            _currentQueue.value = currentList
            saveCurrentQueueIds(currentList.map { it.id })

            mediaController?.moveMediaItem(fromIndex, toIndex)
        }
    }

    private fun getSavedQueueIds(): List<Long> {
        val str = prefs.getString("last_queue_ids", "") ?: ""
        if (str.isBlank()) return emptyList()
        return str.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    private fun saveCurrentQueueIds(queueIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO){
            prefs.edit { putString("last_queue_ids", queueIds.joinToString(",")) }
        }
    }


    fun createPlaylist(name: String, coverUri: String?){
        val newPlaylist = Playlist(name = name, coverUri = coverUri)
        val updated = _playlists.value + newPlaylist
        _playlists.value = updated
        savePlaylists(updated)
    }

    private fun saveDeleted(list: List<Playlist>) {
        _deletedPlaylists.value = list
        prefs.edit { putString("deleted_playlists", PlaylistSerializer.encode(list)) }
    }

    fun restorePlaylist(playlistId: String) {
        val playlist = _deletedPlaylists.value.firstOrNull { it.id == playlistId } ?: return
        saveDeleted(_deletedPlaylists.value.filter { it.id != playlistId })
        val updated = _playlists.value + playlist
        _playlists.value = updated
        savePlaylists(updated)
    }

    fun deletePlaylist(playlistId: String){
        _playlists.value.firstOrNull { it.id == playlistId }?.let { gone ->
            saveDeleted((listOf(gone) + _deletedPlaylists.value).take(MAX_DELETED_PLAYLISTS))
        }
        val updated = _playlists.value.filter { it.id != playlistId }
        _playlists.value = updated
        savePlaylists(updated)
    }

    fun addSongsToPlaylist(playlistId: String, newSongsIds: List<Long>){
        val updated = _playlists.value.map { playlist ->
            if (playlist.id == playlistId){
                val combinedIds = (playlist.songIds + newSongsIds).distinct()
                playlist.copy(songIds = combinedIds)
            }else playlist
        }
        _playlists.value = updated
        savePlaylists(updated)
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long){
        val updated = _playlists.value.map { playlist ->
            if (playlist.id == playlistId){
                playlist.copy(songIds = playlist.songIds.filter { it != songId })
            }else playlist
        }
        _playlists.value = updated
        savePlaylists(updated)
    }

    private fun savePlaylists(list: List<Playlist>){
        prefs.edit { putString("saved_playlist", PlaylistSerializer.encode(list)) }
    }

    private fun loadPlaylist(){
        _deletedPlaylists.value = PlaylistSerializer.decode(prefs.getString("deleted_playlists", "") ?: "")
        val str = prefs.getString("saved_playlist", "") ?: ""
        _playlists.value = PlaylistSerializer.decode(str)
    }


    private fun saveFavoriteIds(favIds: Set<Long>){
       prefs.edit { putStringSet("fav_ids", favIds.map { it.toString() }.toSet()) }
    }

    private fun getSavedFavoritesIds(): Set<Long>{
        return prefs.getStringSet("fav_ids", emptySet())
            ?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
    }

    private fun saveRecentIds(recentIds: List<Long>){
        prefs.edit { putString("recent_ids", recentIds.joinToString (",")) }
    }

    private fun getSavedRecentIds(): List<Long>{
        val str = prefs.getString("recent_ids", "") ?: ""
        if (str.isBlank()) return emptyList()
        return str.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    private fun savePlayCounts(){
        val str = playCountMap.entries.joinToString(","){ "${it.key}:${it.value}"}
        prefs.edit{ putString("play_counts", str)}
    }

    private fun loadSavedPlaycounts(){
        val str = prefs.getString("play_counts", "") ?: ""
        if (str.isNotBlank()){
            str.split(",").forEach { pair ->
                val parts = pair.split(":")
                if (parts.size == 2) {
                    val id = parts[0].toLongOrNull()
                    val count = parts[1].toIntOrNull()
                    if (id != null && count != null){
                        playCountMap[id] = count
                    }
                }
            }
        }
    }

    private fun restoreDashboardLists(allSongs: List<Song>){
        _mostPlayedSongs.value = allSongs
            .filter { (playCountMap[it.id] ?: 0) > 0}
            .sortedByDescending { playCountMap[it.id] ?: 0 }
            .take(30)

        val recentIds = getSavedRecentIds()
        val songMap = allSongs.associateBy { it.id }
        _recentPlayedSongs.value = recentIds.mapNotNull { songMap[it] }.take(30)

        if (_currentSong.value == null) {
            val savedQueueIds = getSavedQueueIds()
            if (savedQueueIds.isNotEmpty()) {
                val restoredQueue = savedQueueIds.mapNotNull { songMap[it] }
                if (restoredQueue.isNotEmpty()){
                    _currentQueue.value = restoredQueue
                }
            }
        }
        val lastSongId = prefs.getLong("last_song_id", -1L)
        if (lastSongId != -1L && _currentSong.value == null){
            songMap[lastSongId]?.let { lastSong ->
                _currentSong.value = lastSong
                _currentPositionMs.value = prefs.getLong("last_position_ms", 0L)
            }
        }
    }

}

