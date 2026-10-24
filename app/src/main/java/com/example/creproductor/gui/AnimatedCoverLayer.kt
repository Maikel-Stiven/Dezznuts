package com.example.creproductor.gui

import android.content.Context
import android.net.ConnectivityManager
import android.view.TextureView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.creproductor.data.AnimatedCover
import com.example.creproductor.data.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val KEY_ANIMATED_COVER = "animated_cover"

fun isAnimatedCoverOn(context: Context): Boolean =
    context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).getBoolean(KEY_ANIMATED_COVER, false)

fun setAnimatedCover(context: Context, on: Boolean) =
    context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).edit { putBoolean(KEY_ANIMATED_COVER, on) }

/** Con datos móviles no se descarga video sin que lo pidas: solo Wi-Fi o redes sin límite. */
private fun networkAllowsVideo(context: Context): Boolean =
    !(context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered

/**
 * Bucle de video encima de la portada fija (que sigue debajo como respaldo). Sin sonido ni foco de audio, solo
 * 720p como máximo, y pausado si la música lo está. Si no hay bucle, red o efectos permitidos, no dibuja nada.
 */
@Composable
fun AnimatedCoverLayer(song: Song, isPlaying: Boolean, active: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val allowed = rememberVisualEffectsEnabled()
    if (!active || !allowed) return
    var url by remember(song.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(song.id) {
        url = if (!networkAllowsVideo(context)) null
        else runCatching { AnimatedCover.find(context, song) }.getOrNull()
    }
    val videoUrl = url ?: return

    var shown by remember(videoUrl) { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, label = "animatedCoverAlpha")
    val player = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            trackSelectionParameters = trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setMaxVideoSize(720, 720)
                .build()
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ALL
            addListener(object : Player.Listener {
                override fun onRenderedFirstFrame() { shown = true }
            })
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    LaunchedEffect(player, isPlaying) { player.playWhenReady = isPlaying }

    AndroidView(
        factory = { TextureView(it).also(player::setVideoTextureView) },
        modifier = modifier.fillMaxSize().alpha(alpha)
    )
}
