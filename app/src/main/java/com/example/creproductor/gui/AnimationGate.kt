package com.example.creproductor.gui

import androidx.compose.runtime.compositionLocalOf

/**
 * `false` en lo que queda tapado por otra pantalla (p. ej. la lista bajo el reproductor a pantalla completa).
 * Las animaciones infinitas (barras del ecualizador de la fila que suena) lo consultan y no corren si nadie las ve:
 * una sola animación infinita fuerza 60 fotogramas por segundo en toda la ventana aunque esté cubierta.
 */
val LocalAnimationsEnabled = compositionLocalOf { true }

/**
 * ¿Se pueden usar efectos decorativos continuos (fondo en movimiento, barras animadas)? Una sola regla para toda la app:
 * no en equipos de poca RAM ni de 2 núcleos o menos, con ahorro de batería o con las animaciones del sistema apagadas.
 * En esos casos la app muestra la versión fija: mismo aspecto, cero fotogramas extra.
 */
@androidx.compose.runtime.Composable
fun rememberVisualEffectsEnabled(): Boolean {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember {
        val lowRam = (context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager).isLowRamDevice
        val animationsOn = android.provider.Settings.Global.getFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
        val powerSave = (context.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager).isPowerSaveMode
        !lowRam && Runtime.getRuntime().availableProcessors() > 2 && animationsOn && !powerSave
    }
}
