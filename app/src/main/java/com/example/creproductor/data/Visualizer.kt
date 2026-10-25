package com.example.creproductor.data

import android.content.Context
import androidx.core.content.edit

/** Interruptor del visualizador tipo CAVA (apagado de fábrica). Lo lee el servicio para meter el espía en la cadena de audio. */
object Visualizer {
    const val KEY = "visualizer"
    fun isOn(context: Context) = context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun set(context: Context, on: Boolean) = context.getSharedPreferences("dezznuts_prefs", Context.MODE_PRIVATE).edit { putBoolean(KEY, on) }
}
