// Clasificación y etiqueta de calidad de audio (lossless / Hi-Res) de una canción.
//
// Es lógica pura (sin Android) para poder probarla con tests unitarios.

package com.example.creproductor.data

enum class QualityTier { LOSSY, LOSSLESS, HIRES }

object AudioQuality {

    private val LOSSLESS_EXT = setOf("flac", "wav", "aif", "aiff", "ape", "wv", "alac")
    private val LOSSLESS_MIME = setOf("audio/flac", "audio/x-flac", "audio/wav", "audio/x-wav", "audio/alac", "audio/aiff", "audio/x-aiff")

    /**
     * Hi-Res = sin pérdida **y** más que un CD (más de 16 bits o más de 48 kHz).
     * Si el dispositivo no informa de la profundidad (Android < 11) solo cuenta la frecuencia.
     * Limitación: un `.m4a` no se puede distinguir de AAC sin abrirlo, así que se trata como con pérdida.
     */
    fun tier(mime: String?, path: String, bitsPerSample: Int, sampleRate: Int): QualityTier {
        val lossless = mime?.lowercase() in LOSSLESS_MIME ||
            path.substringAfterLast('.', "").lowercase() in LOSSLESS_EXT
        return when {
            !lossless -> QualityTier.LOSSY
            bitsPerSample > 16 || sampleRate > 48_000 -> QualityTier.HIRES
            else -> QualityTier.LOSSLESS
        }
    }

    /** Nombre corto del códec a partir del MIME de Media3 / MediaStore. */
    fun codecName(mime: String?): String = when (val m = mime?.lowercase()) {
        null, "" -> ""
        "audio/flac", "audio/x-flac" -> "FLAC"
        "audio/mpeg", "audio/mp3" -> "MP3"
        "audio/mp4a-latm", "audio/aac", "audio/mp4" -> "AAC"
        "audio/alac" -> "ALAC"
        "audio/wav", "audio/x-wav", "audio/raw" -> "WAV"
        "audio/vorbis", "audio/ogg" -> "Vorbis"
        "audio/opus" -> "Opus"
        else -> m.substringAfter('/').removePrefix("x-").uppercase()
    }

    /** Ej.: `FLAC · 24 bit · 96 kHz · 2304 kbps`. Omite lo que no se conoce. */
    fun label(mime: String?, sampleRate: Int, bitsPerSample: Int, bitrate: Int): String =
        listOfNotNull(
            codecName(mime).ifEmpty { null },
            bitsPerSample.takeIf { it > 0 }?.let { "$it bit" },
            sampleRate.takeIf { it > 0 }?.let { khz(it) },
            bitrate.takeIf { it > 0 }?.let { "${it / 1000} kbps" }
        ).joinToString(" · ")

    private fun khz(hz: Int): String {
        val k = hz / 1000.0
        return (if (hz % 1000 == 0) "%d".format(hz / 1000) else "%.1f".format(java.util.Locale.US, k)) + " kHz"
    }
}
