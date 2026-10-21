package com.example.creproductor.gui

import androidx.compose.foundation.background
import com.example.creproductor.data.coverModel
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.produceState
import com.example.creproductor.data.Mood
import com.example.creproductor.data.Moods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.creproductor.data.Song

enum class QuickSelectType(val title: String){
    RECENTLY_ADDED("Recién agregadas"),
    RECENTLY_PLAYED("Reproducción reciente"),
    MOST_PLAYED("Más reproducidas"),
    FAVORITES("Favoritos ❤")

}

@Composable
fun QuickSelectionScreen(
    viewModel: MusicViewModel,
    onSongClick: (song: Song, queue: List<Song>) -> Unit,
    onSeeAllCategoryClick: (type: QuickSelectType, songs: List<Song>) -> Unit = {_, _ -> },
    onMoodClick: (mood: Mood, songs: List<Song>) -> Unit = { _, _ -> }
){

    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val recentSongs by viewModel.recentPlayedSongs.collectAsStateWithLifecycle()
    val allSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()

    val recentlyAddedFull = remember(allSongs) {
        allSongs.sortedByDescending { it.id }.take(30)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp)
    ) {
        SectionRow(
            title = QuickSelectType.RECENTLY_ADDED.title,
            songs = recentlyAddedFull.take(8),
            emptyMessage = "No hay canciones agregadas recientemente",
            onSeeAllClick = {onSeeAllCategoryClick(QuickSelectType.RECENTLY_ADDED, recentlyAddedFull)},
            onSongClick = { song -> onSongClick(song, recentlyAddedFull)}
        )
        SectionRow(
            title = QuickSelectType.RECENTLY_PLAYED.title,
            songs = recentSongs.take(8),
            emptyMessage = "Aún no hay canciones escuchadas recientemente",
            onSeeAllClick = {onSeeAllCategoryClick(QuickSelectType.RECENTLY_PLAYED, recentSongs)},
            onSongClick = { song -> onSongClick(song, recentSongs) }
        )
        SectionRow(
            title = QuickSelectType.MOST_PLAYED.title,
            songs = mostPlayedSongs.take(8),
            emptyMessage = "Aún no hay canciones que escuches frecuentemente",
            onSeeAllClick = {onSeeAllCategoryClick(QuickSelectType.MOST_PLAYED, mostPlayedSongs)},
            onSongClick = { song -> onSongClick(song, mostPlayedSongs) }
        )
        SectionRow(
            title = QuickSelectType.FAVORITES.title,
            songs = favoriteSongs.take(8),
            emptyMessage = "Aún no tienes canciones favoritas",
            onSeeAllClick = {onSeeAllCategoryClick(QuickSelectType.FAVORITES, favoriteSongs)},
            onSongClick = { song -> onSongClick(song, favoriteSongs) })

        // Moods: agrupados por género fuera del hilo principal; los que no tienen canciones no se muestran.
        val moods by produceState(emptyMap<Mood, List<Song>>(), allSongs) {
            value = withContext(Dispatchers.Default) { Moods.group(allSongs) { it.genre } }
        }
        if (moods.isNotEmpty()) {
            Text("Moods:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moods.forEach { (mood, songs) ->
                    AssistChip(onClick = { onMoodClick(mood, songs) }, label = { Text("${mood.label} · ${songs.size}") })
                }
            }
        }
    }
}

@Composable
fun SectionRow(
    title: String,
    songs: List<Song>,
    emptyMessage: String = "",
    onSeeAllClick: () -> Unit = {},
    onSongClick: (Song) -> Unit
){
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onSeeAllClick) {
                Text("VER TODOS", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (songs.isEmpty()){
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else{
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(songs, key = {it.id}){ song ->
                    MediaCard(song = song, onClick = {onSongClick(song)})
                }
            }
        }
    }
}

@Composable
fun MediaCard(
    song: Song,
    onClick: () -> Unit
){
  Column(modifier = Modifier
      .width(120.dp)
      .clickable(onClick = onClick)
  ) {

      Box(
          modifier = Modifier
              .size(120.dp)
              .clip(RoundedCornerShape(8.dp))
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
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(36.dp)
              )
          }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Text(
          text = song.title,
          style = MaterialTheme.typography.bodySmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.basicMarquee()
      )
      Text(
          text = song.artist,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.basicMarquee()
      )
  }
}