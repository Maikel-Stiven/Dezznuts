package com.example.creproductor.data

/** Una línea de letra; [timeMs] es `-1` si la letra no está sincronizada. */
data class LrcLine(val timeMs: Long, val text: String)

/** Letra de una canción: sincronizada (formato LRC) o texto plano. */
class Lyrics(val lines: List<LrcLine>, val synced: Boolean, val source: String)

/**
 * Formato LRC (`[mm:ss.xx] texto`). Lógica pura, probada con tramas de ejemplo.
 * Admite varias marcas por línea (`[00:10.00][00:50.00]estribillo`), la etiqueta `[offset:±ms]` y marcas de palabra
 * `<00:10.00>` (se ignoran). Las etiquetas de metadatos (`[ar:…]`, `[ti:…]`) se descartan.
 */
object Lrc {
    private val TIME = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val OFFSET = Regex("""\[offset:\s*([+-]?\d+)]""", RegexOption.IGNORE_CASE)
    private val WORD_TAG = Regex("""<\d{1,3}:\d{2}(?:[.:]\d{1,3})?>""")

    /** `null` si el texto no trae ninguna marca de tiempo (entonces es letra plana). */
    fun parse(raw: String): List<LrcLine>? {
        val offset = OFFSET.find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val out = ArrayList<LrcLine>()
        for (line in raw.lineSequence()) {
            val stamps = TIME.findAll(line).toList()
            if (stamps.isEmpty()) continue
            val text = WORD_TAG.replace(line.substring(stamps.last().range.last + 1), "").trim()
            for (m in stamps) {
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val frac = m.groupValues[3]
                // ".5" = 500 ms, ".50" = 500 ms, ".500" = 500 ms
                val ms = if (frac.isEmpty()) 0L else (frac + "00").take(3).toLong()
                out += LrcLine((min * 60_000 + sec * 1000 + ms - offset).coerceAtLeast(0), text)
            }
        }
        return out.sortedBy { it.timeMs }.ifEmpty { null }
    }

    /** Texto plano → líneas sin tiempo (se conservan las líneas en blanco entre estrofas, sin repetirlas). */
    fun plain(raw: String): List<LrcLine> =
        raw.lines().map { LrcLine(-1, it.trim()) }
            .fold(ArrayList<LrcLine>()) { acc, l -> if (l.text.isNotEmpty() || acc.lastOrNull()?.text?.isNotEmpty() == true) acc += l; acc }
            .dropLastWhile { it.text.isEmpty() }

    /** Interpreta lo que venga: LRC sincronizado si tiene marcas, texto plano si no. */
    fun fromText(raw: String, source: String): Lyrics? {
        if (raw.isBlank()) return null
        parse(raw)?.let { return Lyrics(it, synced = true, source = source) }
        return plain(raw).takeIf { it.isNotEmpty() }?.let { Lyrics(it, synced = false, source = source) }
    }

    /** Índice de la línea que suena en [positionMs] (la última con tiempo ≤ posición), o -1 antes de la primera. */
    fun activeIndex(lines: List<LrcLine>, positionMs: Long): Int {
        var lo = 0; var hi = lines.size - 1; var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= positionMs) { ans = mid; lo = mid + 1 } else hi = mid - 1
        }
        return ans
    }
}
