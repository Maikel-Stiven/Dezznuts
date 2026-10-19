# Funciones para audiófilos (rama `audiofilos`)

Todo viene **apagado por defecto** y no altera la señal salvo que el usuario lo active (menú → *Audio Hi/Res*).

| Función | Dónde | Cómo funciona |
|---|---|---|
| Insignia y datos de la pista | Reproductor | Muestra `FLAC · 24 bit · 96 kHz · 2304 kbps → Offload/Float 32 bit/PCM`. Los datos salen del `Format` real que reproduce Media3. La ruta de salida es la **solicitada**: si el chip no soporta offload, ExoPlayer cae a PCM sin avisar. |
| ReplayGain | Diálogo Hi/Res | Lee `REPLAYGAIN_TRACK_GAIN` (o álbum) de etiquetas Vorbis/ID3 y ajusta el volumen. **Solo atenúa**, nunca amplifica (sin clipping). |
| Filtros Lossless / Hi-Res | Pestaña Canciones | Clasifica con MIME/extensión y, en Android 11+, profundidad y frecuencia de MediaStore. Hi-Res = sin pérdida y >16 bit o >48 kHz. |
| Distintivos en listas | `SongItem` | Etiqueta "Lossless" / "Hi-Res" junto a cada canción. |
| DAC USB | Diálogo Hi/Res | Lista las salidas conectadas con sus frecuencias y permite preferir el DAC USB (`setPreferredAudioDevice`), reaplicado al conectar/desconectar. |
| Ecualizador | Diálogo Hi/Res → *Ecualizador…* | Ecualizador del sistema con las bandas del dispositivo; baja el volumen lo que suben las bandas (preamp automático). No actúa con offload. |

## Optimización
- **Ecualizador perezoso:** el efecto nativo solo existe mientras está activado; apagado, se crea una única vez para descubrir las bandas y se libera.
- **ReplayGain perezoso:** los metadatos solo se leen si la función está activa; al activarla se lee la pista en curso.
- **Clasificación al escanear:** la calidad (`QualityTier`) se calcula una vez por canción; filtros y distintivos solo comparan un campo.
- **Etiqueta de calidad:** se recalcula únicamente al cambiar de pista, no con la posición (no afecta a la barra de progreso).
- **Chips de filtro** como constante, sin reconstruir la lista en cada recomposición.

## Limitaciones conocidas
- Un `.m4a` (ALAC) no se distingue de AAC en las listas; en el reproductor sí aparece el códec real.
- Antes de Android 11 no hay profundidad/frecuencia en MediaStore: Hi-Res se infiere solo por frecuencia.
- El ecualizador necesita haber reproducido algo (las bandas se publican al abrir la sesión de audio).
- El filtro de la pestaña Canciones comparte estado con "Selección rápida" (ya ocurría con la búsqueda).
- No probado en dispositivo: ReplayGain, ecualizador y DAC USB dependen del hardware y de archivos con etiquetas reales.

## Verificación
`./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` → compila; tests nuevos en `AudioQualityTest` (clasificación, etiqueta y cálculo de ReplayGain).
