// Clase del servicio de música en segundo plano
// y cancelarlo sin que genere consumo en el dispositivo


package com.example.creproductor.service

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.SharedPreferences
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioMixerAttributes
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
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
import com.example.creproductor.data.BitPerfect
import com.example.creproductor.data.MixerFormat
import com.example.creproductor.data.EqBands
import com.example.creproductor.data.Fade
import com.example.creproductor.data.HiFiAudio
import com.example.creproductor.data.OutputClass
import com.example.creproductor.data.OutputProfiles
import com.example.creproductor.data.ProfileSettings
import com.example.creproductor.data.ReplayGain
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi :: class)
class MusicService : MediaSessionService(){
    private var mediaSession: MediaSession? = null
    private var isFavorite = false

    private val prefs by lazy { getSharedPreferences("dezznuts_prefs", MODE_PRIVATE) }

    /** ReplayGain de la pista actual (ganancia en dB y pico lineal); `null` si el archivo no lo trae. */
    private var trackGainDb: Float? = null
    private var trackPeak: Float? = null

    /** Ecualizador paramétrico + subida de ReplayGain del reproductor actual (ver [ParametricEqProcessor]). */
    private var eqProcessor = ParametricEqProcessor(enabledAtBuild = false)
    private var processorActive = false
    private var lastRoute: String? = null

    // Bit-perfect (ver BitPerfect): formato de mezcla activo y DAC al que se aplicó (null = modo normal), y el formato
    // de la pista que suena. Solo se recalculan al cambiar de pista, de dispositivo o de ajustes: sin sondeos.
    private var bitPerfectFormat: MixerFormat? = null
    private var bitPerfectDevice: AudioDeviceInfo? = null
    private var srcRate = 0
    private var srcChannels = 2
    private var srcBits = 0
    private val mediaAttrs by lazy {
        android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }

    /** Reproductor real; la sesión usa un [FadingPlayer] que lo envuelve para fundir al pausar. */
    private var exoPlayer: ExoPlayer? = null

    // Fundidos: volumen base (ReplayGain) × factor de fundido. Solo hay temporizador mientras se está dentro de una
    // rampa o a punto de entrar en la de salida; fuera de eso no se despierta la CPU.
    private var baseVolume = 1f
    private var resumeAt = 0L
    private var pauseFadeStart = -1L
    private var lastFadeLog = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val fadeTick = Runnable { updateFade() }

    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    /** Aplica en vivo los ajustes de ReplayGain / ecualizador / DAC USB en cuanto cambian sus prefs. */
    private val hifiPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            HiFiAudio.KEY_REPLAYGAIN, HiFiAudio.KEY_RG_MODE -> {
                readReplayGain()
                applyVolume()
            }
            HiFiAudio.KEY_RG_BOOST, HiFiAudio.KEY_EQ_ENABLED -> {
                // El procesador solo existe en la cadena si algo lo necesita; cambiar eso exige recrear el player.
                if (needsProcessor() != processorActive) rebuildPlayer() else applyVolume()
            }
            HiFiAudio.KEY_EQ_BANDS -> applyVolume()
            HiFiAudio.KEY_FADE_MS -> updateFade()
            HiFiAudio.KEY_PREFER_USB -> applyPreferredDevice()
            HiFiAudio.KEY_BITPERFECT -> applyBitPerfect()
            HiFiAudio.KEY_PROFILES -> {
                // Al activarlos, los ajustes de ahora pasan a ser los de la salida actual.
                if (HiFiAudio.isProfilesEnabled(this)) prefs.edit().putString(HiFiAudio.KEY_PROFILE_ACTIVE, currentOutputClass().name).apply()
            }
        }
    }

    private fun currentOutputClass(): OutputClass =
        OutputProfiles.classify(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { it.type })

    private fun liveSettings() = ProfileSettings(
        replayGain = HiFiAudio.isReplayGainEnabled(this),
        replayGainAlbum = HiFiAudio.isReplayGainAlbumMode(this),
        replayGainBoost = HiFiAudio.isReplayGainBoostEnabled(this),
        eqEnabled = prefs.getBoolean(HiFiAudio.KEY_EQ_ENABLED, false),
        eqBands = prefs.getString(HiFiAudio.KEY_EQ_BANDS, null) ?: EqBands.encode(EqBands.DEFAULT)
    )

    /**
     * Si cambió el tipo de salida: guarda los ajustes actuales en el perfil de la salida anterior y carga el de
     * la nueva (si todavía no tiene perfil, se conservan los actuales). Los cambios se aplican solos porque
     * pasan por las mismas preferencias que escucha [hifiPrefsListener].
     */
    private fun switchProfileIfNeeded() {
        if (!HiFiAudio.isProfilesEnabled(this)) return
        val now = currentOutputClass()
        val before = prefs.getString(HiFiAudio.KEY_PROFILE_ACTIVE, null)?.let { runCatching { OutputClass.valueOf(it) }.getOrNull() }
        if (before == now) return
        val edit = prefs.edit()
        if (before != null) edit.putString(HiFiAudio.KEY_PROFILE_PREFIX + before.name, liveSettings().encode())
        ProfileSettings.decode(prefs.getString(HiFiAudio.KEY_PROFILE_PREFIX + now.name, null))?.let {
            edit.putBoolean(HiFiAudio.KEY_REPLAYGAIN, it.replayGain)
                .putString(HiFiAudio.KEY_RG_MODE, if (it.replayGainAlbum) "album" else "track")
                .putBoolean(HiFiAudio.KEY_RG_BOOST, it.replayGainBoost)
                .putString(HiFiAudio.KEY_EQ_BANDS, it.eqBands)
                .putBoolean(HiFiAudio.KEY_EQ_ENABLED, it.eqEnabled)
        }
        edit.putString(HiFiAudio.KEY_PROFILE_ACTIVE, now.name).apply()
    }

    private fun needsProcessor() =
        prefs.getBoolean(HiFiAudio.KEY_EQ_ENABLED, false) || HiFiAudio.isReplayGainBoostEnabled(this)

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) { applyPreferredDevice(); applyBitPerfect(); switchProfileIfNeeded() }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) { applyPreferredDevice(); applyBitPerfect(); switchProfileIfNeeded() }
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
        exoPlayer = player
        attachPlayerListener(player)
        mediaSession = MediaSession.Builder(this, FadingPlayer(player))
            .setCallback(CustomMediaSessionCallback())
            .build()

        updateNotificationLayout()

        prefs.registerOnSharedPreferenceChangeListener(hifiPrefsListener)
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        applyPreferredDevice()
        applyBitPerfect()
        switchProfileIfNeeded()
        applyVolume()
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

        processorActive = needsProcessor()
        eqProcessor = ParametricEqProcessor(enabledAtBuild = processorActive)

        val renderersFactory = HiFiRenderersFactory(
            context = this,
            useFloatOutput = HiFiAudio.isFloatOutputEnabled(this),
            useOffload = HiFiAudio.isOffloadEnabled(this),
            eqProcessor = eqProcessor
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
                    deviceRate = HiFiAudio.deviceSampleRate(this@MusicService),
                    // Solo si el AudioTrack abierto coincide con el formato de mezcla bit-perfect elegido.
                    bitPerfect = bitPerfectFormat?.let {
                        it.sampleRate == audioTrackConfig.sampleRate &&
                            it.bits == AudioQuality.bitsOfPcmEncoding(audioTrackConfig.encoding) &&
                            it.isFloat == (audioTrackConfig.encoding == C.ENCODING_PCM_FLOAT)
                    } == true
                )
                // Se crea un AudioTrack por pista: solo se escribe (y se avisa a la interfaz) si la ruta cambió.
                if (route != lastRoute) {
                    lastRoute = route
                    prefs.edit().putString(HiFiAudio.KEY_REAL_ROUTE, route).apply()
                }
            }
        })
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateNotificationLayout()
                updateFade()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) resumeAt = SystemClock.elapsedRealtime()
                updateFade()
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
            ) = updateFade()

            override fun onTracksChanged(tracks: Tracks) {
                rememberSourceFormat(tracks)
                applyBitPerfect()
                readReplayGain(tracks)
                applyVolume()
            }

        })
    }

    /** Lee ganancia y pico solo si ReplayGain está activo (apagado no se recorren los metadatos). */
    private fun readReplayGain(tracks: Tracks? = mediaSession?.player?.currentTracks) {
        trackGainDb = null
        trackPeak = null
        if (tracks == null || !HiFiAudio.isReplayGainEnabled(this)) return
        val tags = readReplayGainTags(tracks)
        val album = HiFiAudio.isReplayGainAlbumMode(this)
        // Cada modo cae al otro si falta su etiqueta.
        val gain = if (album) tags["REPLAYGAIN_ALBUM_GAIN"] ?: tags["REPLAYGAIN_TRACK_GAIN"] else tags["REPLAYGAIN_TRACK_GAIN"] ?: tags["REPLAYGAIN_ALBUM_GAIN"]
        val peak = if (album) tags["REPLAYGAIN_ALBUM_PEAK"] ?: tags["REPLAYGAIN_TRACK_PEAK"] else tags["REPLAYGAIN_TRACK_PEAK"] ?: tags["REPLAYGAIN_ALBUM_PEAK"]
        trackGainDb = ReplayGain.parseDb(gain)
        trackPeak = ReplayGain.parsePeak(peak)
    }

    /** Etiquetas `REPLAYGAIN_*` (en mayúsculas) de las etiquetas Vorbis (FLAC/OGG) o ID3 (MP3) de la pista. */
    private fun readReplayGainTags(tracks: Tracks): Map<String, String> {
        val tags = mutableMapOf<String, String>()
        tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }.forEach { group ->
            for (i in 0 until group.length) {
                val metadata: Metadata = group.getTrackFormat(i).metadata ?: continue
                for (e in 0 until metadata.length()) {
                    when (val entry = metadata[e]) {
                        is VorbisComment -> tags[entry.key.uppercase()] = entry.value
                        is TextInformationFrame -> entry.description?.let {
                            entry.values.firstOrNull()?.let { v -> tags[it.uppercase()] = v }
                        }
                    }
                }
            }
        }
        return tags
    }

    /**
     * ReplayGain → `Player.volume` (atenúa, funciona siempre) y, si está permitido subir, la subida en dB
     * limitada por el pico va al procesador. El EQ paramétrico recibe sus bandas en el mismo paso.
     */
    private fun applyVolume() {
        if (exoPlayer == null) return
        val rgOn = HiFiAudio.isReplayGainEnabled(this)
        val plan = if (rgOn) ReplayGain.plan(trackGainDb, trackPeak, HiFiAudio.isReplayGainBoostEnabled(this)) else ReplayGain.Plan(1f, 0f)
        baseVolume = plan.volume
        updateFade()
        eqProcessor.update(
            EqBands.decode(prefs.getString(HiFiAudio.KEY_EQ_BANDS, null)),
            prefs.getBoolean(HiFiAudio.KEY_EQ_ENABLED, false),
            plan.boostDb
        )
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Log.i("DezzAudio", "volumen=${plan.volume} subida=${plan.boostDb} dB (ReplayGain=${trackGainDb ?: "sin etiqueta"} dB, pico=${trackPeak ?: "?"}) procesador=$processorActive")
        }
    }

    /**
     * Volumen final = [baseVolume] × [Fade.factor]. Se reprograma solo si hace falta: dentro de una rampa cada
     * 40 ms; en mitad de la pista, una única vez justo antes de que empiece la rampa de salida.
     */
    private fun updateFade() {
        val player = exoPlayer ?: return
        handler.removeCallbacks(fadeTick)
        val fadeMs = HiFiAudio.fadeMs(this)
        if (fadeMs <= 0) {
            pauseFadeStart = -1L
            player.volume = baseVolume
            return
        }
        val now = SystemClock.elapsedRealtime()
        val pos = player.currentPosition
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: -1L
        val sinceResume = if (player.isPlaying) now - resumeAt else Long.MAX_VALUE
        val sincePause = if (pauseFadeStart >= 0) now - pauseFadeStart else -1L

        if (pauseFadeStart >= 0 && sincePause >= fadeMs) {
            // Fundido de salida terminado: ahora sí se pausa (el volumen se queda en 0 hasta reanudar).
            pauseFadeStart = -1L
            player.volume = 0f
            player.pause()
            return
        }
        val factor = Fade.factor(pos, dur, fadeMs, sinceResume, sincePause)
        player.volume = baseVolume * factor
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 && now - lastFadeLog >= 300) {
            lastFadeLog = now
            Log.i("DezzFade", "factor=%.2f pos=%d/%d reproduciendo=%b pausando=%b".format(factor, pos, dur, player.isPlaying, pauseFadeStart >= 0))
        }

        if (!player.isPlaying) return
        val inRamp = pauseFadeStart >= 0 || pos < fadeMs || sinceResume < fadeMs || (dur > 0 && dur - pos <= fadeMs)
        val delay = when {
            inRamp -> 40L
            dur > 0 -> minOf(1000L, dur - fadeMs - pos).coerceAtLeast(40L)   // despierta justo antes de la salida
            else -> 0L
        }
        if (delay > 0) handler.postDelayed(fadeTick, delay)
    }

    /** Pide pausar con fundido. `false` si no hay que fundir (se pausa al instante). */
    private fun beginPauseFade(): Boolean {
        val player = exoPlayer ?: return false
        if (HiFiAudio.fadeMs(this) <= 0 || !player.isPlaying || pauseFadeStart >= 0) return false
        pauseFadeStart = SystemClock.elapsedRealtime()
        updateFade()
        return true
    }

    /** Envuelve al reproductor para que pausar (desde la app, la notificación o los auriculares) haga fundido. */
    private inner class FadingPlayer(player: Player) : ForwardingPlayer(player) {
        override fun pause() { if (!beginPauseFade()) super.pause() }
        override fun play() { pauseFadeStart = -1L; super.play() }
        override fun setPlayWhenReady(playWhenReady: Boolean) {
            if (!playWhenReady && beginPauseFade()) return
            if (playWhenReady) pauseFadeStart = -1L
            super.setPlayWhenReady(playWhenReady)
        }
    }

    /** DAC USB conectado (o `null`). */
    private fun usbDevice(): AudioDeviceInfo? =
        audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).firstOrNull {
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE || it.type == AudioDeviceInfo.TYPE_USB_HEADSET
        }

    /** Con "preferir DAC USB" activo, dirige la salida al DAC USB conectado; si no, vuelve al automático. */
    private fun applyPreferredDevice() {
        val player = exoPlayer ?: return
        player.setPreferredAudioDevice(if (HiFiAudio.isPreferUsbEnabled(this)) usbDevice() else null)
    }

    /** Frecuencia, canales y profundidad de la pista que suena (la profundidad solo si el formato la informa). */
    private fun rememberSourceFormat(tracks: Tracks) {
        val format = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_AUDIO && it.isSelected }
            ?.let { g -> (0 until g.length).firstOrNull { g.isTrackSelected(it) }?.let { g.getTrackFormat(it) } } ?: return
        srcRate = format.sampleRate.takeIf { it > 0 } ?: 0
        srcChannels = format.channelCount.takeIf { it > 0 } ?: 2
        srcBits = AudioQuality.bitsOfPcmEncoding(format.pcmEncoding)
    }

    /**
     * Pide al mezclador del DAC USB el formato bit-perfect que coincide con la pista (ver [BitPerfect]) o, si no
     * procede (función apagada, sin DAC, EQ/offload activos, el DAC no ofrece ese formato), vuelve al modo normal.
     * Solo llama al sistema cuando el resultado cambia.
     */
    private fun applyBitPerfect() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        val usb = usbDevice()
        var target: MixerFormat? = null
        var chosen: AudioMixerAttributes? = null
        try {
            if (usb != null && srcRate > 0 && BitPerfect.allowed(Build.VERSION.SDK_INT, HiFiAudio.isBitPerfectEnabled(this),
                    true, processorActive, HiFiAudio.isOffloadEnabled(this))) {
                val supported = audioManager.getSupportedMixerAttributes(usb)
                    .filter { it.mixerBehavior == AudioMixerAttributes.MIXER_BEHAVIOR_BIT_PERFECT }
                val formats = supported.map {
                    MixerFormat(it.format.sampleRate, AudioQuality.bitsOfPcmEncoding(it.format.encoding),
                        it.format.encoding == AudioFormat.ENCODING_PCM_FLOAT, it.format.channelCount)
                }
                val want = BitPerfect.trackFormat(srcRate, srcChannels, srcBits, HiFiAudio.isFloatOutputEnabled(this))
                BitPerfect.choose(formats, want)?.let { target = want; chosen = supported[it] }
            }
            if (target == bitPerfectFormat && usb?.id == bitPerfectDevice?.id) return
            clearBitPerfect()
            if (usb != null && chosen != null && audioManager.setPreferredMixerAttributes(mediaAttrs, usb, chosen!!)) {
                bitPerfectFormat = target
                bitPerfectDevice = usb
            }
        } catch (_: Exception) {
            // El DAC rechazó el formato o desapareció a mitad: se queda el modo normal.
            bitPerfectFormat = null
            bitPerfectDevice = null
        }
    }

    /** Devuelve el DAC al modo normal si había un formato bit-perfect aplicado. */
    private fun clearBitPerfect() {
        val device = bitPerfectDevice ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try { audioManager.clearPreferredMixerAttributes(mediaAttrs, device) } catch (_: Exception) { }
        }
        bitPerfectFormat = null
        bitPerfectDevice = null
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
        val oldPlayer = exoPlayer ?: return

        val mediaItems = (0 until oldPlayer.mediaItemCount).map { oldPlayer.getMediaItemAt(it) }
        val index = oldPlayer.currentMediaItemIndex
        val positionMs = oldPlayer.currentPosition
        val wasPlaying = oldPlayer.playWhenReady
        val repeatMode = oldPlayer.repeatMode
        val shuffleMode = oldPlayer.shuffleModeEnabled

        val newPlayer = buildPlayer()
        exoPlayer = newPlayer
        pauseFadeStart = -1L
        attachPlayerListener(newPlayer)

        if (mediaItems.isNotEmpty()) {
            newPlayer.setMediaItems(mediaItems, index, positionMs)
        }
        newPlayer.repeatMode = repeatMode
        newPlayer.shuffleModeEnabled = shuffleMode
        newPlayer.prepare()

        session.setPlayer(FadingPlayer(newPlayer))
        applyPreferredDevice()
        applyBitPerfect()
        applyVolume()

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
    handler.removeCallbacksAndMessages(null)
    clearBitPerfect()
    prefs.unregisterOnSharedPreferenceChangeListener(hifiPrefsListener)
    audioManager.unregisterAudioDeviceCallback(deviceCallback)
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