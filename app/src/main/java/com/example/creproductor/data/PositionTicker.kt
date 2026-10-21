// Sondeo de la posición de reproducción, fuera de Android para poder probarlo.
//
// El bucle solo existe mientras alguien observa la posición (la barra de progreso) y hay
// reproducción; en pausa o con la pantalla del reproductor cerrada no da ni un tick.

package com.example.creproductor.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Alimenta [position] con la posición real solo mientras se cumplen las dos condiciones:
 * hay al menos un colector de [position] (la barra está en pantalla) y [isPlaying] es `true`.
 *
 * - Sin colectores o en pausa: no hay ticks, solo una última lectura al desactivarse.
 * - Observando y reproduciendo: una lectura cada [intervalMs].
 * - [readPosition] devuelve `null` cuando todavía no hay pista cargada; en ese caso **no** se
 *   sobrescribe el valor actual, para no pisar con 0 la posición restaurada del arranque.
 *
 * `subscriptionCount` cuenta cualquier colector de [position]: si otro componente empieza a
 * observarla, este bucle correrá también con la barra cerrada.
 */
fun CoroutineScope.trackPlaybackPosition(
    position: MutableStateFlow<Long>,
    isPlaying: Flow<Boolean>,
    intervalMs: Long = 500L,
    readPosition: () -> Long?
) {
    launch {
        combine(
            position.subscriptionCount.map { it > 0 },
            isPlaying
        ) { observado, sonando -> observado && sonando }
            .distinctUntilChanged()
            .collectLatest { activo ->
                if (activo) {
                    while (true) {
                        readPosition()?.let { position.value = it }
                        delay(intervalMs)
                    }
                } else {
                    // Última lectura al pausar o cerrar la pantalla: la barra no queda atrás.
                    readPosition()?.let { position.value = it }
                }
            }
    }
}
