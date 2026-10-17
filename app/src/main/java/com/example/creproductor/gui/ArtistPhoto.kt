package com.example.creproductor.gui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.creproductor.data.ArtistImageFetcher
import kotlinx.coroutines.delay

/**
 * Recuerda la foto (ya descargada) de un artista para mostrarla en listas.
 *
 * Se usa para que las pantallas que listan artistas (búsqueda, pestaña "Artistas" y la
 * ficha del artista) muestren la misma foto y **compartan la caché** de [ArtistImageFetcher]:
 * la primera pantalla que pide a un artista descarga la imagen y las demás la leen del disco.
 *
 * Clave para que no "parezca que recarga": el estado inicial se rellena con lo que ya esté
 * descargado ([ArtistImageFetcher.cachedArtistImage]), así la foto aparece desde el primer
 * fotograma. Solo si el artista no está descargado se lanza la descarga en segundo plano.
 *
 * @param artistName nombre del artista que se busca.
 * @param fallback imagen a mostrar mientras no haya foto (o si no existe / no hay conexión).
 *   Normalmente la portada de una de sus canciones; puede ser `null` para mostrar el icono genérico.
 * @param delayMs pequeña espera antes de descargar. Sirve para no lanzar decenas de descargas
 *   a la vez mientras se compone una lista (por ejemplo, al hacer scroll).
 * @return el archivo de la foto si está disponible, o [fallback] en caso contrario.
 */
@Composable
fun rememberArtistPhoto(
    artistName: String,
    fallback: Any? = null,
    delayMs: Long = 0L
): Any? {
    val context = LocalContext.current
    val cleanArtist = artistName.trim()

    // Arranca con lo ya descargado (memoria o disco) para no mostrar la carátula y luego
    // cambiar a la foto. Si no hay nada, empieza en null.
    var photo by remember(cleanArtist) {
        mutableStateOf(ArtistImageFetcher.cachedArtistImage(context, cleanArtist))
    }

    LaunchedEffect(cleanArtist) {
        // Solo descargamos si no la teníamos ya.
        if (photo == null) {
            if (delayMs > 0L) delay(delayMs)
            photo = ArtistImageFetcher.fetchArtistImage(context, cleanArtist)
        }
    }

    return photo ?: fallback
}
