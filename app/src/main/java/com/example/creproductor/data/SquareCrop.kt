package com.example.creproductor.data

import kotlin.math.max

/**
 * Matemática del recorte 1:1 (sin Android, para poder probarla). La imagen se dibuja con esquina superior izquierda
 * en `offset` (px, ≤ 0) y se escala por `scale` dentro de un cuadro de lado `box`.
 */
object SquareCrop {
    /** Escala mínima: la imagen siempre cubre el cuadro entero (nunca quedan bordes vacíos). */
    fun minScale(imgW: Int, imgH: Int, box: Float): Float = max(box / imgW, box / imgH)

    /** Limita el desplazamiento para que el cuadro no salga de la imagen en ese eje. */
    fun clampOffset(offset: Float, scale: Float, imgSize: Int, box: Float): Float =
        offset.coerceIn(-(imgSize * scale - box).coerceAtLeast(0f), 0f)

    /** Lado, en píxeles de la imagen original, del cuadrado que se ve dentro del cuadro. */
    fun sourceSide(scale: Float, box: Float): Float = box / scale

    /** Esquina (en píxeles de la imagen original) de ese cuadrado. */
    fun sourceOrigin(offset: Float, scale: Float): Float = -offset / scale
}
