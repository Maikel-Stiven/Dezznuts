package com.example.creproductor.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/** Una canción de YouTube Music tal como la devuelve la búsqueda. */
data class YtTrack(
    val url: String,
    val title: String,
    val artist: String,
    val durationSec: Long,
    val thumbnail: String?
)

/**
 * YouTube Music sin cuenta, con [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) (GPLv3): él se
 * encarga de InnerTube, de descifrar las direcciones y de seguir los cambios de YouTube. Es una vía no oficial: puede
 * dejar de funcionar sin aviso y va contra los términos de YouTube, por eso es opcional y apagada por defecto.
 * Todo bloquea: llamar fuera del hilo principal.
 */
object YtMusic {
    private const val KEY = "yt_music"

    fun isEnabled(context: android.content.Context) =
        context.getSharedPreferences("dezznuts_prefs", android.content.Context.MODE_PRIVATE).getBoolean(KEY, false)

    fun setEnabled(context: android.content.Context, on: Boolean) =
        context.getSharedPreferences("dezznuts_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()

    private val init by lazy { NewPipe.init(Http) }

    /** Canciones que coinciden con [query] (hasta ~20 de la primera página). Lanza [IOException] si no hay red. */
    fun search(query: String): List<YtTrack> {
        init
        val extractor = ServiceList.YouTube.getSearchExtractor(query, listOf(YoutubeSearchQueryHandlerFactory.MUSIC_SONGS), "")
        extractor.fetchPage()
        return extractor.initialPage.items.filterIsInstance<StreamInfoItem>().map {
            YtTrack(it.url, it.name, it.uploaderName.orEmpty(), it.duration, it.thumbnails.maxByOrNull { t -> t.width }?.url)
        }
    }

    /** Dirección directa del mejor audio de la canción. Caduca en unas horas: resolver justo antes de reproducir. */
    fun audioUrl(trackUrl: String): String {
        init
        val audio = StreamInfo.getInfo(ServiceList.YouTube, trackUrl).audioStreams
        // Preferimos el de más bitrate; ExoPlayer reproduce tanto Opus/WebM como AAC/M4A.
        return audio.maxByOrNull { it.averageBitrate }?.content ?: throw IOException("Sin audio disponible")
    }

    /** Descargador mínimo que pide el extractor. */
    private object Http : Downloader() {
        override fun execute(request: Request): Response {
            val c = URL(request.url()).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 15000; c.readTimeout = 15000
                c.requestMethod = request.httpMethod()
                c.setRequestProperty("User-Agent", USER_AGENT)
                request.headers().forEach { (k, values) ->
                    c.setRequestProperty(k, values.joinToString(", "))
                }
                request.dataToSend()?.let { c.doOutput = true; c.outputStream.use { o -> o.write(it) } }
                val code = c.responseCode
                if (code == 429) throw ReCaptchaException("YouTube pide verificación", request.url())
                val body = (if (code >= 400) c.errorStream else c.inputStream)?.use { it.readBytes() } ?: ByteArray(0)
                val headers = c.headerFields.filterKeys { it != null }.mapKeys { it.key!! }
                return Response(code, c.responseMessage.orEmpty(), headers, String(body, Charsets.UTF_8), c.url.toString())
            } finally { c.disconnect() }
        }

        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:128.0) Gecko/20100101 Firefox/128.0"
    }
}
