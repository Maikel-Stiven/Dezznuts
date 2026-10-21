package com.example.creproductor.data

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Filtros de escaneo que el usuario elige en "Escanear archivos" (se guardan en las preferencias). */
data class ScanSettings(val minSeconds: Int = 0, val minKb: Int = 0, val ignoreHidden: Boolean = false) {
    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(KEY_SECONDS, minSeconds).putInt(KEY_KB, minKb).putBoolean(KEY_HIDDEN, ignoreHidden).apply()
    }

    companion object {
        private const val PREFS = "dezznuts_prefs"
        private const val KEY_SECONDS = "scan_min_seconds"
        private const val KEY_KB = "scan_min_kb"
        private const val KEY_HIDDEN = "scan_ignore_hidden"

        fun read(context: Context): ScanSettings {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return ScanSettings(p.getInt(KEY_SECONDS, 0), p.getInt(KEY_KB, 0), p.getBoolean(KEY_HIDDEN, false))
        }
    }
}

/** Avance del escaneo: [done] de [total] archivos con audio, y el que se está procesando. */
data class ScanProgress(val done: Int, val total: Int, val current: String, val finished: Boolean = false) {
    val percent: Int get() = if (total == 0) (if (finished) 100 else 0) else done * 100 / total
}

/**
 * Busca archivos de audio en carpetas y pide al sistema que los indexe en MediaStore (así aparecen en la biblioteca).
 *
 * Dos fases: primero se recorren las carpetas (para conocer el total y poder mostrar un porcentaje real) y luego se
 * indexan por tandas pequeñas, sin abrir todos los archivos a la vez en un móvil lento.
 */
object FileScanner {
    private val AUDIO_EXTENSIONS = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus", "wma", "ape", "wv", "mka")
    private const val BATCH = 25

    /** ¿Este archivo debe indexarse con estos filtros? (el filtro de duración lo aplica la biblioteca al leer MediaStore). */
    fun isCandidate(file: File, settings: ScanSettings): Boolean {
        if (file.extension.lowercase() !in AUDIO_EXTENSIONS) return false
        if (settings.ignoreHidden && isHidden(file)) return false
        if (settings.minKb > 0 && file.length() < settings.minKb * 1024L) return false
        return true
    }

    /** Oculto = el archivo o alguna carpeta de su ruta empieza por punto. */
    fun isHidden(file: File): Boolean = file.path.split('/').any { it.startsWith(".") && it.length > 1 }

    fun primaryStorage(): File = Environment.getExternalStorageDirectory()

    fun downloads(): File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    /** Raíces de las tarjetas SD / almacenamiento extraíble montados (vacío si no hay). */
    fun sdCards(context: Context): List<File> =
        context.getExternalFilesDirs(null).orEmpty().filterNotNull()
            .filter { Environment.isExternalStorageRemovable(it) }
            .mapNotNull { dir -> dir.path.substringBefore("/Android/data").takeIf { it != dir.path }?.let(::File) }

    fun scan(context: Context, roots: List<File>, settings: ScanSettings): Flow<ScanProgress> = flow {
        emit(ScanProgress(0, 0, "Buscando archivos…"))
        val files = ArrayList<File>()
        for (root in roots) {
            root.walkTopDown()
                .onEnter { dir -> !(settings.ignoreHidden && dir.name.startsWith(".") && dir.name.length > 1) && !dir.resolve(".nomedia").exists() }
                .filter { it.isFile && isCandidate(it, settings) }
                .forEach { files += it }
        }
        var done = 0
        for (batch in files.chunked(BATCH)) {
            emit(ScanProgress(done, files.size, batch.last().path))
            suspendCancellableCoroutine { cont ->
                var pending = batch.size
                MediaScannerConnection.scanFile(context, batch.map { it.path }.toTypedArray(), null) { _, _ ->
                    if (--pending == 0 && cont.isActive) cont.resume(Unit)
                }
            }
            done += batch.size
        }
        emit(ScanProgress(done, files.size, "", finished = true))
    }.flowOn(Dispatchers.IO)
}
