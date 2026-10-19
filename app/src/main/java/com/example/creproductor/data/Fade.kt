// Fundidos de entrada y de salida.
//
// Lógica pura (sin Android): dado el punto de la pista y el tiempo desde que se reanudó o se pidió pausar,
// devuelve el factor de volumen (0..1) a aplicar. Se combinan con el mínimo, así que los fundidos se solapan bien
// (por ejemplo una pista corta que empieza y termina dentro de la ventana).

package com.example.creproductor.data

object Fade {
    /** Duraciones ofrecidas en ajustes (ms); 0 = sin fundido. */
    val OPTIONS_MS = listOf(0, 1000, 2000, 3000, 5000)

    fun label(ms: Int) = if (ms <= 0) "Apagado" else "${ms / 1000} s"

    /**
     * @param posMs posición en la pista; @param durMs duración (≤0 si se desconoce → sin fundido de salida)
     * @param sinceResumeMs ms desde que empezó a sonar (`Long.MAX_VALUE` si no hay reanudación reciente)
     * @param sincePauseMs ms desde que se pidió pausar (−1 si no hay pausa en curso)
     */
    fun factor(posMs: Long, durMs: Long, fadeMs: Int, sinceResumeMs: Long, sincePauseMs: Long): Float {
        if (fadeMs <= 0) return 1f
        val fade = fadeMs.toFloat()
        var f = 1f
        f = minOf(f, posMs / fade)                                  // entrada al inicio de la pista
        if (durMs > 0) f = minOf(f, (durMs - posMs) / fade)         // salida al final de la pista
        f = minOf(f, sinceResumeMs / fade)                          // entrada al reanudar
        if (sincePauseMs >= 0) f = minOf(f, 1f - sincePauseMs / fade) // salida al pausar
        val x = f.coerceIn(0f, 1f)
        return x * x   // curva cuadrática: el oído percibe la rampa lineal como demasiado brusca al principio
    }
}
