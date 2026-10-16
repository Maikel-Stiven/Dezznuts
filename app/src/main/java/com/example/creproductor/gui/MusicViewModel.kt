package com.example.creproductor.gui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.creproductor.data.AudioTagWriter
import com.example.creproductor.data.MediaStoreScanner
import com.example.creproductor.data.Song
import com.example.creproductor.service.MusicService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.example.creproductor.data.Playlist
import com.example.creproductor.data.RepeatMode
import com.example.creproductor.data.next

sealed interface UiState {
    object PermissionRequired : UiState
    object Loading : UiState
    data class Success(val songs: List<Song>) : UiState
    object Empty : UiState
}

enum class SongFilter{
    ALL,
    FAVORITES,
    RECENT
}

class MusicViewModel (application: Application) : AndroidViewModel(application) {

    private val scanner = MediaStoreScanner(application.applicationContext)
    private val prefs = application.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<UiState>(UiState.PermissionRequired)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()


    private var mediaController: MediaController? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()


    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    //
    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()
    //

    //
    private val _currentFilter = MutableStateFlow(SongFilter.ALL)
    val currentFilter: StateFlow<SongFilter> = _currentFilter.asStateFlow()
    //

    //
    private  val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    //

    //
    private val playCountMap = mutableMapOf<Long, Int>()
    private val _mostPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val mostPlayedSongs: StateFlow<List<Song>> = _mostPlayedSongs.asStateFlow()
    //

    //
    private val _recentPlayedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recentPlayedSongs: StateFlow<List<Song>> = _recentPlayedSongs.asStateFlow()
    //

    //
    val favoriteSongs: StateFlow<List<Song>> = uiState.map { state ->
        if (state is UiState.Success) state.songs.filter { it.isFavorite } else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    //

    //
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()
    //

    //
     private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())

    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()
    //


    //
    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    //

    //
    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()
    //



    //
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
            }
            val matchesQuery = query.isBlank() ||
                    song.title.contains(query, ignoreCase = true) ||
                    song.artist.contains(query, ignoreCase = true) ||
                    song.album.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())
    //


    init {
        loadSavedPlaycounts()
        initMediaController()
        startPositionTracker()
        loadSearchHistory()
        loadPlaylist()
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

                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED){
                            _isPlaying.value = false
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
                    }
                })
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun startPositionTracker(){
        viewModelScope.launch {
            while (true){
                mediaController?.let{ controller ->
                    if (controller.isPlaying){
                        _currentPositionMs.value = controller.currentPosition
                    }
                }
                delay(500L)
            }

        }
    }

    private fun loadSearchHistory(){
        val historySet = prefs.getStringSet("search_history", emptySet()) ?: emptySet()
        _searchHistory.value = historySet.toList()
    }

    fun addSearchHistory(query: String){
        if (query.isBlank()) return
        val currentList = _searchHistory.value.toMutableList()
        currentList.remove(query)
        currentList.add(0, query)
        val updatedList = currentList.take(10)
        _searchHistory.value = updatedList
        prefs.edit{putStringSet("search_history", updatedList.toSet())}
    }

    fun removeFromHistory(query: String){
        val updatedList = _searchHistory.value.filter { it != query }
        _searchHistory.value = updatedList
        prefs.edit{putStringSet("search_history", updatedList.toSet())}
    }

    fun setFilter(filter: SongFilter){
        _currentFilter.value = filter
    }

    fun setSearchQuery(query: String){
        _searchQuery.value = query
    }
    fun seekTo(positionMs: Long){
        _currentPositionMs.value = positionMs
        mediaController?.seekTo(positionMs)
        prefs.edit { putLong("last_position_ms", positionMs) }
    }
    fun loadSongs(){
        viewModelScope.launch {
           _uiState.value = UiState.Loading
            val accumulatedSongs = mutableListOf<Song>()

            scanner.scanAudioFiles(chunkSize = 100).collect { chunk ->
                accumulatedSongs.addAll(chunk)
               val savedFavIds = getSavedFavoritesIds()
                val updatedChunk = accumulatedSongs.map { song ->
                    if (savedFavIds.contains(song.id)) song.copy(isFavorite = true) else song
                }

                _uiState.value = UiState.Success(updatedChunk)
                restoreDashboardLists(updatedChunk)
            }

            if (accumulatedSongs.isEmpty()){
                _uiState.value = UiState.Empty
            }
        }
    }

    private fun registerSongPlay(song: Song){
        val currentCount = playCountMap.getOrDefault(song.id, 0) + 1
        playCountMap[song.id] = currentCount
        savePlayCounts()

        val allSongs = (_uiState.value as? UiState.Success)?.songs ?: return

        _mostPlayedSongs.value = allSongs
            .filter { (playCountMap[it.id] ?: 0) > 0 }
            .sortedByDescending { playCountMap[it.id] ?: 0 }
            .take(30)

        val updatedRecent = (listOf(song) + _recentPlayedSongs.value)
            .distinctBy{it.id}
            .take(30)

        _recentPlayedSongs.value = updatedRecent
        saveRecentIds(updatedRecent.map {it.id})

        prefs.edit { putLong("last_song_id", song.id) }
    }
    fun playSong(song: Song, customQueue: List<Song>? = null){

        val queueToUse= if (!customQueue.isNullOrEmpty()) customQueue else (_uiState.value as? UiState.Success)?.songs ?: listOf(song)

        _currentQueue.value = queueToUse
        _currentSong.value = song
        registerSongPlay(song)

        mediaController?.let { controller ->

            val targetIndex = queueToUse.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

            val mediaItems = queueToUse.map { songItem ->
                MediaItem.Builder()
                    .setMediaId(songItem.id.toString())
                    .setUri(songItem.contentUri)
                    .build()
            }

            controller.setMediaItems(mediaItems, targetIndex, 0L)
            controller.prepare()
            controller.play()
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
                    val targetIndex = queueToUse.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

                    val mediaItems = queueToUse.map { songItem ->
                        MediaItem.Builder()
                            .setMediaId(songItem.id.toString())
                            .setUri(songItem.contentUri)
                            .build()
                    }

                    val savedPosition = _currentPositionMs.value
                    controller.setMediaItems(mediaItems, targetIndex, savedPosition)
                    controller.prepare()
                }else if (controller.playbackState == Player.STATE_ENDED){
                    controller.seekTo(0, 0L)
                    controller.prepare()
                }else if (controller.playbackState == Player.STATE_IDLE){
                    controller.prepare()
                }
                controller.play()
            }
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
                loadSongs()
            }
        }
    }

    override fun onCleared() {
        mediaController?.release()
    }

    fun playNext(){
        mediaController?.let { controller ->
            if (controller.hasNextMediaItem()){
                controller.seekToNextMediaItem()
            }else{
                val current = _currentSong.value ?: return
                val list = _currentQueue.value.ifEmpty { (_uiState.value as? UiState.Success)?.songs ?: return }
                val currentIndex= list.indexOfFirst { it.id == current.id }
                if (currentIndex != -1 && currentIndex < list.size - 1){
                    playSong(list[currentIndex + 1], customQueue = list)
                }
            }
        }
    }

    fun playPrevious(){
        mediaController?.let { controller ->
            if (controller.hasPreviousMediaItem()){
                controller.seekToPreviousMediaItem()
            }else{
                val current = _currentSong.value ?: return
                val list = _currentQueue.value.ifEmpty { (_uiState.value as? UiState.Success)?.songs ?: return }
                val currentIndex = list.indexOfFirst { it.id ==  current.id }
                if (currentIndex > 0){
                    playSong(list[currentIndex - 1], customQueue = list)
                }
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

            mediaController?.moveMediaItem(fromIndex, toIndex)
        }
    }


    fun createPlaylist(name: String, coverUri: String?){
        val newPlaylist = Playlist(name = name, coverUri = coverUri)
        val updated = _playlists.value + newPlaylist
        _playlists.value = updated
        savePlaylists(updated)
    }

    fun deletePlaylist(playlistId: String){
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
        val serialized = list.joinToString (";") { p ->
            "${p.id}|${p.name}|${p.coverUri ?: ""}|${p.songIds.joinToString(",")}"
        }
        prefs.edit {putString("saved_playlist", serialized)}
    }

    private fun loadPlaylist(){
        val str = prefs.getString("saved_playlist", "") ?: ""
        if (str.isNotBlank()){
            val parsed =str.split(";").mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size >= 4){
                    val id = parts[0]
                    val name = parts[1]
                    val cover = parts[2].ifBlank { null }
                    val songIds = parts[3].split(",").mapNotNull { it.toLongOrNull() }
                    Playlist(id = id, name = name, coverUri = cover, songIds = songIds)
                }else null
            }
            _playlists.value = parsed
        }
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

        val lastSongId = prefs.getLong("last_song_id", -1L)
        if (lastSongId != -1L && _currentSong.value == null){
            songMap[lastSongId]?.let { lastSong ->
                _currentSong.value = lastSong
                _currentPositionMs.value = prefs.getLong("last_position_ms", 0L)
            }
        }
    }
}

