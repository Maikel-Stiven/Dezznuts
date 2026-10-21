package com.example.creproductor.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SquareCropTest {
    @Test fun `la escala minima cubre el cuadro en ambos ejes`() {
        assertEquals(0.5f, SquareCrop.minScale(1000, 2000, 500f), 1e-6f)   // lado corto 1000 -> 500
        assertEquals(1f, SquareCrop.minScale(500, 500, 500f), 1e-6f)
    }

    @Test fun `el desplazamiento no deja salir el cuadro de la imagen`() {
        // imagen 1000x1000 a escala 1 en cuadro de 500: offset entre -500 y 0
        assertEquals(0f, SquareCrop.clampOffset(40f, 1f, 1000, 500f), 0f)
        assertEquals(-500f, SquareCrop.clampOffset(-900f, 1f, 1000, 500f), 0f)
        assertEquals(-100f, SquareCrop.clampOffset(-100f, 1f, 1000, 500f), 0f)
    }

    @Test fun `el recorte de origen corresponde a lo que se ve`() {
        // escala 0.5: el cuadro de 500 px muestra 1000 px de la imagen, empezando en 200 si offset = -100
        assertEquals(1000f, SquareCrop.sourceSide(0.5f, 500f), 0f)
        assertEquals(200f, SquareCrop.sourceOrigin(-100f, 0.5f), 0f)
    }
}
