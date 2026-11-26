package com.example.creproductor.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Textos de la interfaz en varios idiomas sin una constante por cada texto: el código conserva el texto en español
 * (que además es la clave) y `strings.xml` de cada idioma lo traduce con el nombre `s_<hash>`. Si un texto no tiene
 * traducción, o el idioma es el español, se muestra tal cual. Los textos con valores dentro (`"$n canciones"`) quedan
 * en español. Los recursos `s_*` están protegidos del recorte en `res/raw/keep.xml`.
 */
object I18n {
    private val cache = ConcurrentHashMap<String, String>()

    /** Nombre del recurso de un texto: `s_` + 10 hex del SHA-1. */
    fun key(es: String): String =
        "s_" + MessageDigest.getInstance("SHA-1").digest(es.toByteArray()).joinToString("") { "%02x".format(it) }.take(10)

    fun tr(context: Context, es: String): String {
        val lang = context.resources.configuration.locales[0].language
        return cache.getOrPut("$lang|$es") {
            val id = context.resources.getIdentifier(key(es), "string", context.packageName)
            if (id != 0) context.getString(id) else es
        }
    }
}

@Composable
fun tr(es: String): String = I18n.tr(LocalContext.current, es)

/**
 * Idioma propio de la app, independiente del del teléfono. En Android 13+ lo gestiona el sistema (también aparece en
 * Ajustes → Apps → Idioma); antes se guarda aquí y [wrap] lo aplica al crear la actividad.
 */
object AppLanguage {
    /** Código → nombre en su propio idioma (así lo reconoce quien no entiende el actual). "" = el del sistema. */
    val OPTIONS = listOf("" to "Del sistema", "es" to "Español", "en" to "English", "pt" to "Português",
        "fr" to "Français", "de" to "Deutsch", "it" to "Italiano")

    private fun prefs(c: Context) = c.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE)

    fun current(c: Context): String =
        if (android.os.Build.VERSION.SDK_INT >= 33)
            c.getSystemService(android.app.LocaleManager::class.java).applicationLocales.let { if (it.isEmpty) "" else it[0].language }
        else prefs(c).getString("app_lang", "") ?: ""

    fun set(activity: android.app.Activity, tag: String) {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(android.app.LocaleManager::class.java).applicationLocales =
                if (tag.isEmpty()) android.os.LocaleList.getEmptyLocaleList() else android.os.LocaleList.forLanguageTags(tag)
        } else {
            prefs(activity).edit().putString("app_lang", tag).apply()
            activity.recreate()
        }
    }

    fun wrap(base: Context): Context {
        if (android.os.Build.VERSION.SDK_INT >= 33) return base
        val tag = prefs(base).getString("app_lang", "").orEmpty().ifEmpty { return base }
        val cfg = android.content.res.Configuration(base.resources.configuration).apply { setLocale(java.util.Locale.forLanguageTag(tag)) }
        return base.createConfigurationContext(cfg)
    }
}
