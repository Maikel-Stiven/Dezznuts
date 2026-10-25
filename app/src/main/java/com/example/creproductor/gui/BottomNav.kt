package com.example.creproductor.gui

import android.content.Context
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.creproductor.i18n.tr
import kotlinx.coroutines.launch

/** Dónde va la navegación principal: barra inferior (interfaz de Maikel, rama temas/interfaces; por defecto) o pestañas arriba con menú lateral (maqueta). */
enum class NavStyle { BOTTOM, TOP }

object NavSetting {
    private const val KEY = "nav_style"
    var style by mutableStateOf(NavStyle.BOTTOM)
        private set

    fun load(context: Context) {
        val name = context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).getString(KEY, null)
        style = NavStyle.entries.firstOrNull { it.name == name } ?: NavStyle.BOTTOM
    }

    fun set(context: Context, value: NavStyle) {
        style = value
        context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).edit().putString(KEY, value.name).apply()
    }
}

/** Destinos de la barra inferior, en el orden de la interfaz de Maikel. */
private enum class BottomDest(val title: String, val icon: ImageVector) {
    YT("YT Music", Icons.Default.PlayCircleOutline),
    LIBRARY("Biblioteca", Icons.Default.LibraryMusic),
    HOME("Inicio", Icons.Default.Home),
    SEARCH("Buscar", Icons.Default.Search),
    SETTINGS("Ajustes", Icons.Default.Settings)
}

/** Páginas de la biblioteca (Canciones…Carpetas) dentro del pager de [AppTab]. */
internal val LIBRARY_TABS = AppTab.entries.filter { it != AppTab.QUICK && it != AppTab.YT }

/**
 * Barra inferior: YT Music · Biblioteca · Inicio · Buscar · Ajustes. Mueve el mismo pager de las pestañas (Inicio =
 * Selección rápida, Biblioteca = el resto de pestañas locales), así no hay pantallas duplicadas.
 */
@Composable
fun AppBottomBar(section: AppSection, pagerState: PagerState, onSectionChange: (AppSection) -> Unit) {
    val scope = rememberCoroutineScope()
    val page = AppTab.entries[pagerState.currentPage]
    val selected = when {
        section == AppSection.SEARCH -> BottomDest.SEARCH
        section == AppSection.SETTINGS -> BottomDest.SETTINGS
        page == AppTab.YT -> BottomDest.YT
        page == AppTab.QUICK -> BottomDest.HOME
        else -> BottomDest.LIBRARY
    }
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer, windowInsets = WindowInsets(0)) {
        BottomDest.entries.forEach { d ->
            NavigationBarItem(
                selected = selected == d,
                onClick = {
                    fun goTo(tab: AppTab) { onSectionChange(AppSection.HOME); scope.launch { pagerState.scrollToPage(AppTab.entries.indexOf(tab)) } }
                    when (d) {
                        BottomDest.YT -> goTo(AppTab.YT)
                        BottomDest.LIBRARY -> goTo(if (selected == BottomDest.LIBRARY) page else AppTab.SONGS)
                        BottomDest.HOME -> goTo(AppTab.QUICK)
                        BottomDest.SEARCH -> onSectionChange(AppSection.SEARCH)
                        BottomDest.SETTINGS -> onSectionChange(AppSection.SETTINGS)
                    }
                },
                icon = { Icon(d.icon, contentDescription = null) },
                label = { Text(tr(d.title), maxLines = 1, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), softWrap = false) },
                alwaysShowLabel = true
            )
        }
    }
}

/** Selector Arriba / Abajo para Opciones → Apariencia. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavStylePicker() {
    val context = LocalContext.current
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        NavStyle.entries.forEachIndexed { i, s ->
            SegmentedButton(
                selected = NavSetting.style == s,
                onClick = { NavSetting.set(context, s) },
                shape = SegmentedButtonDefaults.itemShape(i, NavStyle.entries.size)
            ) { Text(tr(if (s == NavStyle.TOP) "Pestañas arriba" else "Barra abajo")) }
        }
    }
}
