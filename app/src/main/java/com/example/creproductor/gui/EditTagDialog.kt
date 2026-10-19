package com.example.creproductor.gui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.creproductor.data.CoverFetcher
import com.example.creproductor.data.Song
import com.example.creproductor.data.TagSuggestions
import com.example.creproductor.data.YoutubeTitleCleaner
import kotlinx.coroutines.launch

@Composable
fun EditTagDialog(
    song: Song,
    onDismiss: () -> Unit,
    onSave: (title: String, artist: String, album: String, coverUriOrUrl: String?) -> Unit
){
    // Los marcadores "<unknown>" / "Artista desconocido" no son datos: el campo empieza vacío para escribir el real.
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(if (TagSuggestions.isUnknown(song.artist)) "" else song.artist) }
    var album by remember { mutableStateOf(if (TagSuggestions.isUnknown(song.album)) "" else song.album) }
    val suggestion = remember(song) { TagSuggestions.suggest(song.title, song.artist, java.io.File(song.path).name) }
    var selectedCoverUri by remember { mutableStateOf(song.albumArtUri?.toString()) }
    var isSearchingWeb by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedCoverUri = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar etiquetas de audio") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                    contentAlignment = Alignment.Center
                ){
                    if (selectedCoverUri != null) {
                        AsyncImage(
                            model = selectedCoverUri,
                            contentDescription = "Portada",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }else{
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isSearchingWeb = true
                            scope.launch {
                                val cleanData = YoutubeTitleCleaner.parseFileName(title)
                                val searchResult = CoverFetcher.fetchOfficialCover(
                                    cleanData.cleanTitle,
                                    cleanData.cleanArtist
                                )

                                searchResult?.let { result ->
                                    title = result.title
                                    artist = result.artist
                                    if (result.album.isNotBlank()) album = result.album
                                    result.highResCoverUrl?.let { selectedCoverUri = it }
                                }
                                isSearchingWeb = false
                            }
                        },
                        enabled = !isSearchingWeb
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSearchingWeb) "Buscando..." else "Auto-web")
                    }

                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    ) {
                        Text("Galeria")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Artista desconocido: se propone el que sale del nombre del archivo ("Artista - Título").
                if (suggestion != null && (artist != suggestion.artist || title != suggestion.title)) {
                    AssistChip(
                        onClick = { artist = suggestion.artist; title = suggestion.title },
                        label = { Text("Usar «${suggestion.artist}» — «${suggestion.title}»", maxLines = 1) },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text("Álbum") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank(),
                onClick = {
                    // La portada solo se reescribe si el usuario la cambió.
                    onSave(title, artist, album, selectedCoverUri.takeIf { it != song.albumArtUri?.toString() })
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss)  {
                Text("Cancelar")
            }
        }
    )
}