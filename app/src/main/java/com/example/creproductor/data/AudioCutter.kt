package com.example.creproductor.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Corta un tramo de una canción y lo guarda como archivo nuevo en `Música/DEZZNUTS` (el original no se toca).
 *
 * Se prefiere **no recodificar** (rápido, sin pérdida y sin gastar batería):
 * - MP3: se copian las tramas del tramo ([Mp3Frames]).
 * - AAC/M4A y Opus/Vorbis (Android 10+): se copian las muestras con `MediaMuxer`.
 * - Cualquier otro formato (FLAC, WAV, APE…): se decodifica a PCM y se guarda como WAV de 16 bits.
 */
object AudioCutter {
    class Result(val uri: Uri, val displayName: String)

    /** Devuelve el archivo creado, o `null` si no se pudo (formato no decodificable, sin espacio, sin permiso…). */
    fun cut(context: Context, song: Song, startMs: Long, endMs: Long): Result? = runCatching {
        require(endMs > startMs)
        val startUs = startMs * 1000
        val endUs = endMs * 1000
        val tmp = File(context.cacheDir, "corte_${System.currentTimeMillis()}.tmp")
        try {
            val mime = trackMime(context, song.contentUri)
            val ext = when {
                mime == "audio/mpeg" -> if (cutMp3(context, song.contentUri, tmp, startUs, endUs)) "mp3" else null
                mime == "audio/mp4a-latm" -> if (cutWithMuxer(context, song.contentUri, tmp, startUs, endUs, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)) "m4a" else null
                (mime == "audio/opus" || mime == "audio/vorbis") && Build.VERSION.SDK_INT >= 29 ->
                    if (cutWithMuxer(context, song.contentUri, tmp, startUs, endUs, MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG)) "ogg" else null
                else -> null
            } ?: if (decodeToWav(context, song.contentUri, tmp, startUs, endUs)) "wav" else return null
            val name = "${safeName(song.title)} (corte).$ext"
            publish(context, tmp, name, mimeFor(ext))?.let { Result(it, name) }
        } finally {
            tmp.delete()
        }
    }.getOrNull()

    private fun trackMime(context: Context, uri: Uri): String? {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(context, uri, null)
            return (0 until ex.trackCount).map { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME) }
                .firstOrNull { it?.startsWith("audio/") == true }
        } finally { ex.release() }
    }

    private fun cutMp3(context: Context, uri: Uri, out: File, startUs: Long, endUs: Long): Boolean {
        val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
        val frames = Mp3Frames.parse(data)
        if (frames.isEmpty()) return false   // no es Layer III: se decodifica
        val cut = Mp3Frames.cut(data, frames, startUs, endUs) ?: return false
        out.writeBytes(cut)
        return true
    }

    private fun cutWithMuxer(context: Context, uri: Uri, out: File, startUs: Long, endUs: Long, format: Int): Boolean {
        val ex = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            ex.setDataSource(context, uri, null)
            val track = (0 until ex.trackCount).first { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
            ex.selectTrack(track)
            val fmt = ex.getTrackFormat(track)
            muxer = MediaMuxer(out.path, format)
            val dst = muxer.addTrack(fmt)
            muxer.start()
            ex.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val buf = ByteBuffer.allocate(if (fmt.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) fmt.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE) else 1 shl 20)
            val info = MediaCodec.BufferInfo()
            var written = 0
            while (true) {
                info.size = ex.readSampleData(buf, 0)
                val t = ex.sampleTime
                if (info.size < 0 || t > endUs) break
                if (t >= startUs) {
                    info.offset = 0
                    info.presentationTimeUs = t - startUs
                    info.flags = ex.sampleFlags
                    muxer.writeSampleData(dst, buf, info)
                    written++
                }
                ex.advance()
            }
            return written > 0
        } finally {
            runCatching { muxer?.stop() }
            runCatching { muxer?.release() }
            ex.release()
        }
    }

    /** Decodifica el tramo con `MediaCodec` y lo escribe como WAV PCM de 16 bits (cualquier formato que Android sepa leer). */
    private fun decodeToWav(context: Context, uri: Uri, out: File, startUs: Long, endUs: Long): Boolean {
        val ex = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            ex.setDataSource(context, uri, null)
            val track = (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true } ?: return false
            ex.selectTrack(track)
            val fmt = ex.getTrackFormat(track)
            codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(fmt, null, null, 0)
            codec.start()
            ex.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            var rate = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var float = false
            val info = MediaCodec.BufferInfo()
            var inputDone = false
            var dataBytes = 0L
            RandomAccessFile(out, "rw").use { raf ->
                raf.setLength(0)
                raf.write(ByteArray(44))   // cabecera: se rellena al final con los tamaños reales
                while (true) {
                    if (!inputDone) {
                        val i = codec.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val n = ex.readSampleData(codec.getInputBuffer(i)!!, 0)
                            if (n < 0 || ex.sampleTime > endUs) { codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                            else { codec.queueInputBuffer(i, 0, n, ex.sampleTime, 0); ex.advance() }
                        }
                    }
                    val o = codec.dequeueOutputBuffer(info, 10_000)
                    when {
                        o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> codec.outputFormat.let {
                            rate = it.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                            channels = it.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                            float = Build.VERSION.SDK_INT >= 24 && it.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                                it.getInteger(MediaFormat.KEY_PCM_ENCODING) == android.media.AudioFormat.ENCODING_PCM_FLOAT
                        }
                        o >= 0 -> {
                            val pcm = codec.getOutputBuffer(o)!!.order(ByteOrder.LITTLE_ENDIAN)
                            val bytesPerSample = if (float) 4 else 2
                            val frameBytes = bytesPerSample * channels
                            // Se recorta al tramo exacto (el decodificador arranca en un punto de sincronía anterior).
                            val totalFrames = info.size / frameBytes
                            val firstFrame = ((startUs - info.presentationTimeUs) * rate / 1_000_000).coerceIn(0, totalFrames.toLong()).toInt()
                            val lastFrame = ((endUs - info.presentationTimeUs) * rate / 1_000_000).coerceIn(0, totalFrames.toLong()).toInt()
                            if (lastFrame > firstFrame) {
                                val bytes = ByteArray((lastFrame - firstFrame) * channels * 2)
                                pcm.position(info.offset + firstFrame * frameBytes)
                                var w = 0
                                repeat((lastFrame - firstFrame) * channels) {
                                    val s = if (float) (pcm.float.coerceIn(-1f, 1f) * 32767f).toInt() else pcm.short.toInt()
                                    bytes[w++] = s.toByte(); bytes[w++] = (s shr 8).toByte()
                                }
                                raf.write(bytes)
                                dataBytes += bytes.size
                            }
                            codec.releaseOutputBuffer(o, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
                        }
                    }
                }
                raf.seek(0)
                raf.write(wavHeader(dataBytes, rate, channels))
            }
            return dataBytes > 0
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            ex.release()
        }
    }

    internal fun wavHeader(dataBytes: Long, rate: Int, channels: Int): ByteArray =
        ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt((36 + dataBytes).toInt()); put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(channels.toShort())
            putInt(rate); putInt(rate * channels * 2); putShort((channels * 2).toShort()); putShort(16)
            put("data".toByteArray()); putInt(dataBytes.toInt())
        }.array()

    /** Copia el archivo temporal a `Música/DEZZNUTS` (MediaStore en Android 10+, carpeta pública antes). */
    private fun publish(context: Context, tmp: File, name: String, mime: String): Uri? {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                put(MediaStore.Audio.Media.MIME_TYPE, mime)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/DEZZNUTS")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values) ?: return null
            resolver.openOutputStream(uri)?.use { o -> tmp.inputStream().use { it.copyTo(o) } } ?: return null
            resolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
            return uri
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "DEZZNUTS").apply { mkdirs() }
        val file = File(dir, name)
        tmp.copyTo(file, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(file.path), arrayOf(mime), null)
        return Uri.fromFile(file)
    }

    private fun mimeFor(ext: String) = when (ext) { "mp3" -> "audio/mpeg"; "m4a" -> "audio/mp4"; "ogg" -> "audio/ogg"; else -> "audio/x-wav" }

    private fun safeName(title: String) = title.filterNot { it in "\\/:*?\"<>|" }.trim().ifEmpty { "audio" }.take(80)
}
