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
