package com.example.creproductor.innertube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface YtUiState {
    object Loading : YtUiState
    data class Success(val quickTracks: List<YtOnlineTrack>) : YtUiState
    data class Error(val message: String) : YtUiState
}

class YtMusicViewModel : ViewModel() {
    private val client = InnerTubeClient()

    private val _uiState = MutableStateFlow<YtUiState>(YtUiState.Loading)
    val uiState: StateFlow<YtUiState> = _uiState.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = YtUiState.Loading
            runCatching {
                val tracks = client.fetchHome()
                if (tracks.isNotEmpty()) {
                    _uiState.value = YtUiState.Success(tracks)
                } else {
                    _uiState.value = YtUiState.Error("No se pudieron obtener resultados")
                }
            }.onFailure { e ->
                _uiState.value = YtUiState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }
}
