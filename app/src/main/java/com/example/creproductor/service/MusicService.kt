// Clase del servicio de música en segundo plano
// y cancelarlo sin que genere consumo en el dispositivo


package com.example.creproductor.service

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.creproductor.R
import com.example.creproductor.data.AudioQuality
import com.example.creproductor.data.HiFiAudio
import com.example.creproductor.data.ReplayGain
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi :: class)
class MusicService : MediaSessionService(){
    private var mediaSession: MediaSession? = null
    private var isFavorite = false

    private val prefs by lazy { getSharedPreferences("dezznuts_prefs", MODE_PRIVATE) }

    /** ReplayGain (en dB) de la pista actual; `null` si el archivo no lo trae. */
    private var trackGainDb: Float? = null
    private var equalizer: Equalizer? = null

    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    /** Aplica en vivo los ajustes de ReplayGain / ecualizador / DAC USB en cuanto cambian sus prefs. */
    private val hifiPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            HiFiAudio.KEY_REPLAYGAIN, HiFiAudio.KEY_RG_MODE -> {
                trackGainDb = currentReplayGainDb()
                applyVolume()
            }
            HiFiAudio.KEY_EQ_ENABLED -> {
                // El ecualizador solo existe mientras está activo: apagado no gasta DSP ni CPU.
                attachEqualizer((mediaSession?.player as? ExoPlayer)?.audioSessionId ?: C.AUDIO_SESSION_ID_UNSET)
            }
            HiFiAudio.KEY_EQ_LEVELS -> { applyEqualizer(); applyVolume() }
            HiFiAudio.KEY_PREFER_USB -> applyPreferredDevice()
        }
    }

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = applyPreferredDevice()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = applyPreferredDevice()
    }

    companion object{
        const val ACTION_FAVORITE = "ACTION_FAVORITE"
        const val ACTION_CLOSE = "ACTION_CLOSE"

        /** Comando de sesión para reconstruir el reproductor con los ajustes Hi-Fi actuales. */
        const val ACTION_REFRESH_AUDIO = "ACTION_REFRESH_AUDIO"
    }


    override fun onCreate(){
        super.onCreate()

        val player = buildPlayer()
        attachPlayerListener(player)
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(CustomMediaSessionCallback())
            .build()

        updateNotificationLayout()

        prefs.registerOnSharedPreferenceChangeListener(hifiPrefsListener)
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        applyPreferredDevice()
    }

    /**
     * Crea el reproductor con la configuración de alta fidelidad vigente.
     *
     * Se usa una [HiFiRenderersFactory] para poder activar la salida float y el
     * audio offload (ver los detalles en esa clase). El resto son los ajustes de
     * siempre: uso "media", foco de audio gestionado por ExoPlayer y pausa
     * automática al desconectar los auriculares.
     */
    private fun buildPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val renderersFactory = HiFiRenderersFactory(
            context = this,
            useFloatOutput = HiFiAudio.isFloatOutputEnabled(this),
            useOffload = HiFiAudio.isOffloadEnabled(this)
        )

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setLoadControl(buildLoadControl())
            .build()

        // Traza de latencia del *seek* (ver PlaybackDiagnostics). Se engancha aquí
        // para que la tengan tanto el reproductor de arranque como el que crea
        // rebuildPlayer().
        // Solo en builds depurables: en release la traza es trabajo y logs sin lector.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            PlaybackDiagnostics.attach(player)
        }

        return player
    }

    /**
     * Control de carga afinado para reproducción **local** (no streaming).
     *
     * Los valores por defecto de ExoPlayer están pensados para internet y hacen
     * que el *seek* se sienta lento. Aquí se ajustan tres cosas:
     *
     * - **backBufferDurationMs = 30 s**: por defecto es 0, así que al retroceder
     *   no hay nada decodificado detrás y hay que releer y recodificar el tramo.
     *   Con búfer trasero, retroceder es instantáneo.
     * - **bufferForPlaybackMs = 1 s** (era 2.5 s): tras un *seek* ExoPlayer espera
     *   a tener esa cantidad y solo entonces reanuda. Bajarlo reanuda antes.
     * - **bufferForPlaybackAfterRebufferMs = 2 s** (era 5 s): lo mismo tras un
     *   corte o una pausa larga.
     */
    private fun buildLoadControl(): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs= */ 15_000,
                /* maxBufferMs= */ 30_000,
                /* bufferForPlaybackMs= */ 250,
                /* bufferForPlaybackAfterRebufferMs= */ 1_000
            )
            .setBackBuffer(
                /* backBufferDurationMs= */ 15_000,
                /* retainBackBufferFromKeyframe= */ true
            )
            .build()
    }

    /**
     * Al cambiar de canción hay que recalcular si el nuevo tema es favorito,
     * para que el botón de la notificación muestre el estado correcto.
     */
    private fun attachPlayerListener(player: ExoPlayer) {
        // Lo que el AudioTrack abrió de verdad: offload, frecuencia y profundidad reales.
        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onAudioTrackInitialized(
                eventTime: AnalyticsListener.EventTime,
                audioTrackConfig: AudioSink.AudioTrackConfig
            ) {
                val route = AudioQuality.route(
                    offload = audioTrackConfig.offload,
                    pcmBits = AudioQuality.bitsOfPcmEncoding(audioTrackConfig.encoding),
                    trackRate = audioTrackConfig.sampleRate,
                    deviceRate = HiFiAudio.deviceSampleRate(this@MusicService)
                )
                prefs.edit().putString(HiFiAudio.KEY_REAL_ROUTE, route).apply()
            }
        })
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateNotificationLayout()
            }

            override fun onTracksChanged(tracks: Tracks) {
                trackGainDb = currentReplayGainDb(tracks)
                applyVolume()
            }

            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attachEqualizer(audioSessionId)
            }
        })
    }

    /** Lee la etiqueta solo si ReplayGain está activo (apagado no se recorren los metadatos). */
    private fun currentReplayGainDb(tracks: Tracks? = mediaSession?.player?.currentTracks): Float? =
        if (tracks != null && HiFiAudio.isReplayGainEnabled(this)) readReplayGainDb(tracks) else null

    /** Ganancia ReplayGain (pista, o álbum si no hay) de las etiquetas Vorbis (FLAC/OGG) o ID3 (MP3). */
    private fun readReplayGainDb(tracks: Tracks): Float? {
        val gains = mutableMapOf<String, String>()
        tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }.forEach { group ->
            for (i in 0 until group.length) {
                val metadata: Metadata = group.getTrackFormat(i).metadata ?: continue
                for (e in 0 until metadata.length()) {
                    when (val entry = metadata[e]) {
                        is VorbisComment -> gains[entry.key.uppercase()] = entry.value
                        is TextInformationFrame -> entry.description?.let {
                            entry.values.firstOrNull()?.let { v -> gains[it.uppercase()] = v }
                        }
                    }
                }
            }
        }
        val track = gains["REPLAYGAIN_TRACK_GAIN"]
        val album = gains["REPLAYGAIN_ALBUM_GAIN"]
        // Cada modo cae al otro si falta su etiqueta.
        return ReplayGain.parseDb(if (HiFiAudio.isReplayGainAlbumMode(this)) album ?: track else track ?: album)
    }

    /**
     * Volumen final = ReplayGain (si está activo) × pre-amplificación automática del ecualizador.
     * El preamp baja el volumen lo que suben las bandas más altas, para que el EQ no sature.
     */
    private fun applyVolume() {
        val player = mediaSession?.player ?: return
        val rg = if (HiFiAudio.isReplayGainEnabled(this)) ReplayGain.volumeFor(trackGainDb) else 1f
        val preampDb = if (prefs.getBoolean(HiFiAudio.KEY_EQ_ENABLED, false)) -maxBoostDb() else 0f
        player.volume = (rg * Math.pow(10.0, preampDb / 20.0).toFloat()).coerceIn(0f, 1f)
    }

    private fun eqLevelsMb(): List<Int> =
        (prefs.getString(HiFiAudio.KEY_EQ_LEVELS, "") ?: "").split(",").mapNotNull { it.trim().toIntOrNull() }

    private fun maxBoostDb(): Float = (eqLevelsMb().maxOrNull() ?: 0).coerceAtLeast(0) / 100f

    /**
     * Crea el ecualizador del sistema sobre la sesión de audio y publica sus bandas
     * (`nº|mín|máx|frecuencias`) en las prefs para que la pantalla del EQ las dibuje.
     * Con audio offload el DSP salta el ecualizador, por eso no tendrá efecto en ese modo.
     */
    private fun attachEqualizer(audioSessionId: Int) {
        equalizer?.release()
        equalizer = null
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET) return
        val enabled = prefs.getBoolean(HiFiAudio.KEY_EQ_ENABLED, false)
        // Apagado y con las bandas ya publicadas: no se crea el efecto nativo.
        if (!enabled && prefs.contains(HiFiAudio.KEY_EQ_INFO)) { applyVolume(); return }
        try {
            val eq = Equalizer(0, audioSessionId)
            val freqs = (0 until eq.numberOfBands).joinToString(",") { (eq.getCenterFreq(it.toShort()) / 1000).toString() }
            prefs.edit().putString(
                HiFiAudio.KEY_EQ_INFO,
                "${eq.numberOfBands}|${eq.bandLevelRange[0]}|${eq.bandLevelRange[1]}|$freqs"
            ).apply()
            if (enabled) {
                equalizer = eq
                applyEqualizer()
            } else {
                eq.release() // solo servía para descubrir las bandas
            }
        } catch (_: Exception) {
            // Dispositivo sin ecualizador: la función simplemente no está disponible.
        }
        applyVolume()
    }

    private fun applyEqualizer() {
        val eq = equalizer ?: return
        try {
            eq.enabled = true
            val levels = eqLevelsMb()
            val range = eq.bandLevelRange
            for (band in 0 until eq.numberOfBands) {
                val level = (levels.getOrNull(band) ?: 0).coerceIn(range[0].toInt(), range[1].toInt())
                eq.setBandLevel(band.toShort(), level.toShort())
            }
        } catch (_: Exception) { }
    }

    /** Con "preferir DAC USB" activo, dirige la salida al DAC USB conectado; si no, vuelve al automático. */
    private fun applyPreferredDevice() {
        val player = mediaSession?.player as? ExoPlayer ?: return
        val usb = if (HiFiAudio.isPreferUsbEnabled(this)) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
            }
        } else null
        player.setPreferredAudioDevice(usb)
    }

    /**
     * Rehace el reproductor para aplicar los ajustes de audio nuevos **sin**
     * perder la cola ni desconectar los controles:
     *
     * 1. Copia items, índice, posición, repetición y aleatorio del player actual.
     * 2. Construye uno nuevo con la configuración Hi-Fi vigente.
     * 3. Lo intercambia con `MediaSession.setPlayer`; la sesión no cambia, así
     *    que los `MediaController` siguen conectados.
     * 4. Libera el antiguo y restaura el estado de reproducción.
     *
     * El player nuevo se deja en pausa hasta que la sesión ya lo controla, para
     * no solapar dos reproducciones a la vez.
     */
    private fun rebuildPlayer() {
        val session = mediaSession ?: return
        val oldPlayer = session.player as? ExoPlayer ?: return

        val mediaItems = (0 until oldPlayer.mediaItemCount).map { oldPlayer.getMediaItemAt(it) }
        val index = oldPlayer.currentMediaItemIndex
        val positionMs = oldPlayer.currentPosition
        val wasPlaying = oldPlayer.playWhenReady
        val repeatMode = oldPlayer.repeatMode
        val shuffleMode = oldPlayer.shuffleModeEnabled

        val newPlayer = buildPlayer()
        attachPlayerListener(newPlayer)

        if (mediaItems.isNotEmpty()) {
            newPlayer.setMediaItems(mediaItems, index, positionMs)
        }
        newPlayer.repeatMode = repeatMode
        newPlayer.shuffleModeEnabled = shuffleMode
        newPlayer.prepare()

        equalizer?.release()
        equalizer = null
        session.setPlayer(newPlayer)
        applyPreferredDevice()

        newPlayer.playWhenReady = wasPlaying
        oldPlayer.release()

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
    prefs.unregisterOnSharedPreferenceChangeListener(hifiPrefsListener)
    audioManager.unregisterAudioDeviceCallback(deviceCallback)
    equalizer?.release()
    equalizer = null
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
                .add(SessionCommand(ACTION_REFRESH_AUDIO, Bundle.EMPTY))
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
                ACTION_REFRESH_AUDIO -> {
                    rebuildPlayer()
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

}