// Hoja CUE: describe las pistas de un álbum entero guardado en un solo archivo (FLAC/WAV/APE…).
//
// Lógica pura (sin Android) para probarla con tests. Solo se soportan CUE con **un** archivo de audio:
// con varios archivos (FILE) se devuelve `null` y el álbum se muestra como un único archivo, como antes.

package com.example.creproductor.data

data class CueTrack(val number: Int, val title: String, val performer: String, val startMs: Long, val endMs: Long)

data class CueSheet(val album: String, val performer: String, val fileName: String, val tracks: List<CueTrack>) {

    companion object {
        private val QUOTED = Regex("\"([^\"]*)\"")
        private val INDEX = Regex("""INDEX\s+01\s+(\d+):(\d+):(\d+)""", RegexOption.IGNORE_CASE)

        /** `mm:ss:ff` con `ff` en fotogramas (75 por segundo) → milisegundos. */
        private fun toMs(m: String, s: String, f: String) = (m.toLong() * 60 + s.toLong()) * 1000 + f.toLong() * 1000 / 75

        /** Interpreta el texto de un `.cue`; `totalDurationMs` cierra la última pista. `null` si no sirve. */
        fun parse(text: String, totalDurationMs: Long): CueSheet? {
            var album = ""; var performer = ""; var file: String? = null
            var files = 0
            data class Raw(var number: Int, var title: String = "", var performer: String = "", var start: Long = -1)
            val raws = mutableListOf<Raw>()

            for (line in text.lineSequence().map { it.trim() }) {
                val upper = line.uppercase()
                val quoted = QUOTED.find(line)?.groupValues?.get(1) ?: line.substringAfter(' ', "").trim()
                when {
                    upper.startsWith("FILE ") -> { files++; file = quoted }
                    upper.startsWith("TRACK ") -> raws += Raw(line.split(Regex("\\s+")).getOrNull(1)?.toIntOrNull() ?: (raws.size + 1))
                    upper.startsWith("TITLE ") -> raws.lastOrNull()?.let { it.title = quoted } ?: run { album = quoted }
                    upper.startsWith("PERFORMER ") -> raws.lastOrNull()?.let { it.performer = quoted } ?: run { performer = quoted }
                    upper.startsWith("INDEX 01") -> INDEX.find(line)?.let { m ->
                        raws.lastOrNull()?.start = toMs(m.groupValues[1], m.groupValues[2], m.groupValues[3])
                    }
                }
            }
            if (files != 1 || file == null) return null
            val valid = raws.filter { it.start >= 0 }.sortedBy { it.start }
            if (valid.size < 2) return null // una sola pista no aporta nada

            val tracks = valid.mapIndexed { i, r ->
                val end = if (i + 1 < valid.size) valid[i + 1].start else totalDurationMs
                CueTrack(r.number, r.title.ifBlank { "Pista ${r.number}" }, r.performer.ifBlank { performer }, r.start, end)
            }.filter { it.endMs > it.startMs }
            return if (tracks.size >= 2) CueSheet(album, performer, file, tracks) else null
        }
    }
}
