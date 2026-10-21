# Ecualizador premium (rama `eq-premium`)

Pantalla completa para el ecualizador paramétrico: se maneja **sobre la curva**, no con deslizadores.

## Qué hay
- **Curva de respuesta real:** 160 puntos logarítmicos (20 Hz–20 kHz, ±12 dB) con relleno degradado y brillo. Se calcula con `PeakingFilter.responseDb`, que usa **la misma fórmula de coeficientes que el procesador de audio**: lo que se dibuja es lo que suena (un test lo comprueba midiendo el filtro real sobre un tono).
- **Nodos arrastrables:** horizontal = frecuencia, vertical = ganancia. Paso de 0,5 dB con imán en 0 dB, vibración al cruzar 0 dB y etiqueta flotante (`3.6 kHz · +4.5 dB`) junto al nodo.
- **En vivo:** al arrastrar se guarda cada ~80 ms (no en cada píxel) y el servicio aplica los cambios al instante, así se oye mientras se mueve.
- **Presets:** Plano, Graves, Voces, Brillo y Loudness; "Personalizado" aparece solo cuando los ajustes no coinciden con ninguno.
- **Interruptor Activo/Apagado** (A/B): apagado el procesador no toca la señal.
- **Lecturas** de banda, frecuencia, ganancia y Q, y un deslizador para el Q de la banda elegida.
- **Accesos:** icono de ecualizador en la cabecera del reproductor y Ajustes → Audio → Ecualizador.

## Técnica y optimización
- Un solo `Canvas`, sin composables por nodo; la curva se recalcula solo cuando cambian las bandas.
- Los gestos leen el estado con `rememberUpdatedState` (el bloque de gestos no se reinicia al recomponer; sin esto el arrastre movía el nodo equivocado, fallo detectado en el emulador).
- Accesibilidad: descripciones en la curva y en el deslizador de Q.

## Pendiente (ideas)
- Analizador de espectro en vivo detrás de la curva (FFT en el propio procesador, sin permiso de micrófono).
- Colores tomados de la carátula; guardar presets propios; pellizco para el Q.

## Verificación
`./gradlew :app:testDebugUnitTest` (tests de respuesta, presets y coincidencia gráfica/audio) y prueba en emulador: preset, arrastre de varios nodos, activación en vivo.
