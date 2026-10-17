// Clase del servicio de música en segundo plano
// y cancelarlo sin que genere consumo en el dispositivo


package com.example.creproductor.service

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.creproductor.R
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi :: class)
class MusicService : MediaSessionService(){
    private var mediaSession: MediaSession? = null
    private var isFavorite = false

    private val prefs by lazy { getSharedPreferences("dezznuts_prefs", MODE_PRIVATE) }

    companion object{
        const val ACTION_FAVORITE = "ACTION_FAVORITE"
        const val ACTION_CLOSE = "ACTION_CLOSE"
    }


    override fun onCreate(){
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(CustomMediaSessionCallback())
            .build()

        // Al cambiar de canción hay que recalcular si el nuevo tema es favorito,
        // para que el botón de la notificación muestre el estado correcto.
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateNotificationLayout()
            }
        })

        updateNotificationLayout()
    }

    /**
     * Marca o desmarca como favorita la canción actual y persiste el cambio en
     * `fav_ids` (mismas prefs que usa el ViewModel). El ViewModel lo detecta con
     * un `OnSharedPreferenceChangeListener` y refresca la UI.
     *
     * @return el nuevo estado de favorito (`true` si quedó marcada).
     */
    private fun toggleCurrentFavorite(): Boolean {
        val currentId = mediaSession?.player?.currentMediaItem?.mediaId ?: return isFavorite

        val favIds = prefs.getStringSet("fav_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        val nowFavorite = if (favIds.contains(currentId)) {
            favIds.remove(currentId)
            false
        } else {
            favIds.add(currentId)
            true
        }

        prefs.edit().putStringSet("fav_ids", favIds).apply()
        isFavorite = nowFavorite
        updateNotificationLayout()
        return nowFavorite
    }

    /**
     * Reconstruye la notificación. Recalcula [isFavorite] a partir de las prefs
     * (fuente de verdad) y aplica el icono correspondiente al botón "Favorito".
     */
    private fun updateNotificationLayout() {

        val currentId = mediaSession?.player?.currentMediaItem?.mediaId
        isFavorite = currentId != null && prefs.getStringSet("fav_ids", emptySet())?.contains(currentId) == true

        val favoriteButton = CommandButton.Builder()
            .setDisplayName("Favorito")
            .setIconResId(
                if (isFavorite) R.drawable.ic_corazon_filled
                else R.drawable.ic_corazon_outline
            )
            .setSessionCommand(SessionCommand(ACTION_FAVORITE, Bundle.EMPTY))
            .build()

        val closeButton = CommandButton.Builder()
            .setDisplayName("Cerrar")
            .setCustomIconResId(R.drawable.ic_cerrar)
            .setSessionCommand(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
            .build()

        mediaSession?.setCustomLayout(listOf(closeButton, favoriteButton))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
            return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?){
        val player = mediaSession?.player
        if (player != null && !player.playWhenReady){
            stopSelf()
        }
    }

    override fun  onDestroy(){
    mediaSession?.run {
        player.release()
        release()
    }
    mediaSession = null
    super.onDestroy()
    }

    private inner class CustomMediaSessionCallback : MediaSession.Callback{
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult{

            val connectionResult = super.onConnect(session, controller)

            val sessionCommands = connectionResult.availableSessionCommands.buildUpon()
                .add(SessionCommand(ACTION_FAVORITE, Bundle.EMPTY))
                .add(SessionCommand(ACTION_CLOSE, Bundle.EMPTY))
                .build()

            val playerCommands = connectionResult.availablePlayerCommands.buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()

            return MediaSession.ConnectionResult.accept(
                sessionCommands,
                playerCommands
            )
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction){
                ACTION_FAVORITE -> {
                    toggleCurrentFavorite()
                }
                ACTION_CLOSE -> {
                    session.player.stop()
                    stopSelf()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

}