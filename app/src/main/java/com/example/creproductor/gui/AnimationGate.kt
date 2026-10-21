package com.example.creproductor.gui

import androidx.compose.runtime.compositionLocalOf

/**
 * `false` en lo que queda tapado por otra pantalla (p. ej. la lista bajo el reproductor a pantalla completa).
 * Las animaciones infinitas (barras del ecualizador de la fila que suena) lo consultan y no corren si nadie las ve:
 * una sola animación infinita fuerza 60 fotogramas por segundo en toda la ventana aunque esté cubierta.
 */
val LocalAnimationsEnabled = compositionLocalOf { true }
