package com.example.creproductor.gui

import com.example.creproductor.i18n.tr

import androidx.compose.foundation.background
import com.example.creproductor.data.coverModel
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.Song
import com.example.creproductor.data.SongOrganizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Calcula fuera del hilo principal; `null` mientras tanto. Agrupar miles de canciones al abrir una pestaña
 * bloqueaba el hilo de la interfaz (se notaba más en móviles lentos).
 */
@Composable
private fun <T> rememberOffMain(key: Any?, compute: () -> T): T? =
    produceState<T?>(null, key) { value = withContext(Dispatchers.Default) { compute() } }.value

@Composable
fun AlbumsGridScreen(
    songs: List<Song>,
    gridState: LazyGridState = rememberLazyGridState(),
    onAlbumClick: (albumName: String, albumSongs: List<Song>) -> Unit = { _, _ ->}
){

    val albumsMap = rememberOffMain(songs) {
        SongOrganizer.groupByAlbum(
            items = songs,
            albumOf = {it.album},
            trackNumberOf = {it.trackNumber},
            titleOf = {it.title}
        )
    } ?: return
    val albumsKeys = remember(albumsMap) { albumsMap.keys.toList() }

    if (albumsMap.isEmpty()){
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = tr("no se encontraron álbumes"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }else{
        LazyVerticalGrid(
            // Adaptable: 2 columnas en un teléfono, más en tablets o en horizontal (mismas tarjetas, ~160 dp).
            columns = GridCells.Adaptive(minSize = 160.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albumsKeys, key = { it }){albumName ->
                val albumSongs = albumsMap[albumName] ?: emptyList()
                val firstSong = albumSongs.firstOrNull()

                val artistName = when {
                    firstSong == null || firstSong.artist.isBlank() || firstSong.artist.equals("<unknown>", ignoreCase = true) -> "Artista desconcido"
                    else -> firstSong.artist
                }

                val  covertUri = albumSongs.firstOrNull{ it.albumArtUri != null}?.coverModel
                val countText = if (albumSongs.size == 1) "1 canción" else "${albumSongs.size} canciones"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable{onAlbumClick(albumName, albumSongs)}
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ){
                        if (covertUri != null){
                            AsyncImage(
                                model = covertUri,
                                contentDescription = albumName,
                                modifier =  Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }else{
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text =  "$artistName • $countText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun getGenreIcon(genreName: String): ImageVector{
    return when (genreName.lowercase().trim()) {
        "rock", "hard rock", "rock alternativo" -> Icons.Default.GraphicEq
        "metal", "heavy metal" -> Icons.Default.FlashOn
        "pop", "k-pop", "j-pop" -> Icons.Default.Star
        "hip-hop", "rap", "trap" -> Icons.Default.Speaker
        "electronica", "electronic", "edm", "techno", "dubstep", "sytnhwave" -> Icons.Default.Headphones
        "lo-fi", "lofi", "chill" -> Icons.Default.Radio
        "soundtrack", "ost", "anime" -> Icons.Default.Movie
        "jazz", "blues" -> Icons.Default.MusicNote
        else -> Icons.Default.LibraryMusic
    }
}

@Composable
fun GenresGridScreen(
    songs: List<Song>,
    gridState: LazyGridState = rememberLazyGridState(),
    onGenreClick: (genreName: String, genreSongs: List<Song>) -> Unit = {_, _ ->}
){

    val genresMap = rememberOffMain(songs) {
        SongOrganizer.groupByGenre(songs) { it.genre }
    } ?: return
    val genresKeys = remember(genresMap) { genresMap.keys.toList() }

    if (genresMap.isEmpty()){
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = tr("No se encontraron géneros"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }else{
        LazyVerticalGrid(
            state = gridState,
            // Adaptable: 2 columnas en un teléfono, más en tablets o en horizontal (mismas tarjetas, ~160 dp).
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(genresKeys, key = {it}) { genreName ->
                val genreSongs = genresMap[genreName] ?: emptyList()
                val countText = if (genreSongs.size == 1) "1 canción" else "${genreSongs.size} canciones"
                val genreIcon = getGenreIcon(genreName)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable{onGenreClick(genreName, genreSongs )},
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = genreIcon,
                            contentDescription = genreName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = genreName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = countText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsListScreen(
    songs: List<Song>,
    listState: LazyListState = rememberLazyListState(),
    onArtistClick: (artistName: String, artistSongs: List<Song>) -> Unit = {_, _ ->}
){

    val artistsMap = rememberOffMain(songs) {
        SongOrganizer.groupByArtist(songs) { it.artist }
    } ?: return
    val artistsKeys = remember(artistsMap) { artistsMap.keys.toList() }

    if (artistsMap.isEmpty()){
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = tr("No se encontraron artistas"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(artistsKeys, key = { it }) {artistName ->
                val artistSongs = artistsMap[artistName] ?: emptyList()
                val avatarUri = artistSongs.firstOrNull { it.albumArtUri != null }?.coverModel

                // Foto real del artista (Deezer). Comparte caché con la búsqueda y con la
                // ficha del artista, así que aparece al instante si ya se cargó antes.
                val artistPhoto = rememberArtistPhoto(artistName, avatarUri, delayMs = 100L)

                val albumsCount = remember(artistSongs) {
                    artistSongs
                        .map { it.album }
                        .filter { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }
                        .distinct()
                        .size
                        .coerceAtLeast(if (artistSongs.isNotEmpty()) 1 else 0)
                }

                val albumText = if (albumsCount == 1) "1 álbum" else "$albumsCount álbumes"
                val songText = if (artistSongs.size == 1) "1 canción" else "${artistSongs.size} canciones"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {onArtistClick(artistName, artistSongs)}
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ){

                        if (artistPhoto != null) {
                            AsyncImage(
                                model = artistPhoto,
                                contentDescription = artistName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }else{
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                     Column(modifier = Modifier.weight(1f)) {

                         Text(
                             text = artistName,
                             style = MaterialTheme.typography.titleMedium,
                             fontWeight = FontWeight.Bold,
                             color = MaterialTheme.colorScheme.onSurface,
                             maxLines = 1,
                             overflow = TextOverflow.Ellipsis
                         )

                         Spacer(modifier = Modifier.height(2.dp))

                         Text(
                             text = "$albumText • $songText",
                             style = MaterialTheme.typography.bodyMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             maxLines = 1,
                             overflow = TextOverflow.Ellipsis
                         )
                     }
                }
            }
        }
    }
}
@Composable
fun FoldersListScreen(
    songs: List<Song>,
    listState: LazyListState = rememberLazyListState(),
    onFolderClick: (folderName: String, folderSongs: List<Song>) -> Unit = { _, _ -> }
){
    val foldersMap = rememberOffMain(songs) {
        SongOrganizer.groupByFolder(songs) { it.path }
    } ?: return
    val foldersKeys = remember(foldersMap) { foldersMap.keys.toList() }

    if (foldersMap.isEmpty()){
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ){
            Text(
                text = tr("No se encontraron carpetas"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(foldersKeys, key = { it }) { folderPath ->
                val folderSongs = foldersMap[folderPath] ?: emptyList()
                val displayName = folderPath.substringAfterLast('/').ifBlank { folderPath }
                val countText = if (folderSongs.size == 1) "1 canción" else "${folderSongs.size} canciones"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFolderClick(folderPath, folderSongs) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ){
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$countText • $folderPath",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
