package com.example.creproductor.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Avisa cuando sale una versión nueva en GitHub y la descarga. Solo pregunta a la API pública de releases
 * (sin cuenta ni datos del usuario), como mucho una vez al día y si el usuario no lo apagó en Opciones.
 * La descarga la hace el gestor de descargas de Android: al terminar, tocar su notificación abre el instalador.
 */
object AppUpdater {
    private const val API = "https://api.github.com/repos/Maikel-Stiven/Dezznuts/releases/latest"
    private const val DAY_MS = 24L * 3600 * 1000

    class Release(val version: String, val notes: String, val apkUrl: String?, val pageUrl: String)

    private fun prefs(c: Context) = c.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE)

    fun isAutoCheck(c: Context) = prefs(c).getBoolean("update_check", true)
    fun setAutoCheck(c: Context, on: Boolean) = prefs(c).edit().putBoolean("update_check", on).apply()

    /** ¿Toca mirar hoy? Activado y sin mirar en las últimas 24 h. */
    fun shouldCheck(c: Context) =
        isAutoCheck(c) && System.currentTimeMillis() - prefs(c).getLong("update_checked_at", 0) > DAY_MS

    fun markChecked(c: Context) = prefs(c).edit().putLong("update_checked_at", System.currentTimeMillis()).apply()

    /** "Ahora no" en el aviso automático: esa versión ya no se vuelve a anunciar sola (sí al buscar a mano). */
    fun dismiss(c: Context, version: String) = prefs(c).edit().putString("update_dismissed", version).apply()
    fun isDismissed(c: Context, version: String) = prefs(c).getString("update_dismissed", null) == version

    /** "v1.10.0" > "1.9.2"; ignora la "v" y lo que siga a un guion ("1.3.0-beta" cuenta como 1.3.0). */
    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V").substringBefore('-')
            .split('.').map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val r = parts(remote); val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /**
     * Notas de la release (Markdown de GitHub) como texto llano: sin `#`, `**` ni `` ` ``, viñetas "•" y las líneas
     * partidas a mano (las que empiezan con espacios) unidas a la anterior.
     */
    fun plainNotes(md: String): String {
        val out = ArrayList<String>()
        for (raw in md.replace("\r", "").lines()) {
            if (raw.isBlank()) { if (out.lastOrNull()?.isNotEmpty() == true) out += ""; continue }
            val clean = raw.replace("**", "").replace("`", "")
            if (raw.startsWith(" ") && !raw.trimStart().startsWith("- ") && out.lastOrNull()?.isNotEmpty() == true)
                out[out.size - 1] = out.last() + " " + clean.trim()
            else out += clean.trim().replace(Regex("^#+\\s*"), "").replace(Regex("^[-*]\\s+"), "• ")
        }
        return out.joinToString("\n").trim()
    }

    /** Última release publicada. Bloquea: fuera del hilo principal. [IOException] sin red o si GitHub no responde. */
    fun latest(): Release {
        val c = URL(API).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 8000; c.readTimeout = 8000
            c.setRequestProperty("Accept", "application/vnd.github+json")
            c.setRequestProperty("User-Agent", "DEZZNUTS-updater")
            if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
            val o = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            val assets = o.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0)).map { assets!!.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            return Release(
                version = o.optString("tag_name"),
                notes = plainNotes(o.optString("body")),
                apkUrl = apk?.optString("browser_download_url")?.takeIf { it.isNotBlank() },
                pageUrl = o.optString("html_url")
            )
        } finally { c.disconnect() }
    }

    /** Pone el APK en Descargas con notificación; tocarla al terminar abre el instalador de Android. */
    fun download(c: Context, r: Release): Boolean {
        val url = r.apkUrl ?: return false
        val name = "dezznuts-${r.version.removePrefix("v")}.apk"
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle("DEZZNUTS ${r.version}")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .apply {
                // Android 9 o menos pediría permiso de escritura para Descargas: entonces va a la carpeta de la app.
                if (android.os.Build.VERSION.SDK_INT >= 29) setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
                else setDestinationInExternalFilesDir(c, Environment.DIRECTORY_DOWNLOADS, name)
            }
        return runCatching { (c.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req) }.isSuccess
    }
}
