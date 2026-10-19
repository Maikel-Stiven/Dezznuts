# Funciones para audiófilos (rama `audiofilos`)

Todo viene **apagado por defecto** y no altera la señal salvo que el usuario lo active (menú → *Audio Hi/Res*).

| Función | Dónde | Cómo funciona |
|---|---|---|
| Insignia y datos de la pista | Reproductor | Muestra `FLAC · 24 bit · 96 kHz · 2304 kbps → <ruta>`. Los datos salen del `Format` real de Media3. La **ruta es la real**: el servicio la lee del `AudioTrack` que se abrió (`onAudioTrackInitialized`) y la distingue en *Offload*, *PCM directo* o *PCM remuestreado a X kHz* por el mezclador. |
| Ficha técnica | Reproductor → ⋮ | Archivo, calidad, ruta real de salida y ruta del archivo. |
| ReplayGain | Diálogo Hi/Res | Lee `REPLAYGAIN_TRACK_GAIN` / `ALBUM_GAIN` (Vorbis/ID3) y ajusta el volumen; modo **pista** o **álbum** (este conserva la dinámica entre canciones de un disco). **Solo atenúa**, nunca amplifica (sin clipping). |
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
- Antes de Android 16 MediaStore no informa de profundidad/frecuencia: para FLAC y WAV se lee la cabecera del archivo (`AudioHeader`); otros formatos quedan sin dato.
- El ecualizador necesita haber reproducido algo (las bandas se publican al abrir la sesión de audio).
- El filtro de la pestaña Canciones comparte estado con "Selección rápida" (ya ocurría con la búsqueda).
- No probado en dispositivo: ReplayGain, ecualizador y DAC USB dependen del hardware y de archivos con etiquetas reales.

## Qué queda fuera (y por qué)
- **DSD/DoP, MQA:** Android no los decodifica; requeriría un decodificador propio.
- **Crossfade:** ExoPlayer no lo trae; el gapless depende de Media3 (actualizado a 1.9.1).
- **ReplayGain con subida (preamp + limitador por *peak*):** exige un efecto adicional y ajuste de oído en dispositivo.
- **Ecualizador paramétrico (Q, AutoEQ), perfiles por salida, CUE sheets, duplicados:** funciones grandes que conviene diseñar aparte.
- **Pruebas instrumentadas con tonos de referencia y reproducción de horas:** necesitan dispositivo.

## Pruebas en emulador (Android 15, API 35)
Con archivos generados con ffmpeg (FLAC 16/44,1, FLAC 24/96 con `REPLAYGAIN_TRACK_GAIN=-9 dB`, MP3 y WAV 24/48):
- **Clasificación:** FLAC 16/44,1 → Lossless; FLAC 24/96 y WAV 24/48 → Hi-Res; MP3 → sin insignia.
- **Ruta real:** WAV 48 kHz → `PCM directo`; MP3 44,1 kHz y FLAC 96 kHz → `remuestreado a 48 kHz`; con salida float → `PCM 32 bit`.
- **ReplayGain:** la etiqueta de −9 dB se lee del FLAC y el volumen queda en 0,355 (= −9 dB).
- **Ecualizador:** subir una banda +10 dB baja el volumen −10,3 dB (preamp); al apagarlo vuelve a su valor.
- **Offload:** el emulador no lo soporta y cae a PCM sin fallos (la ruta lo muestra).
- **Error encontrado y corregido gracias al emulador:** `SAMPLERATE`/`BITS_PER_SAMPLE` no existen antes de Android 16; pedirlas cerraba la app al arrancar en Android 11–15.

## Verificación
`./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:minifyReleaseWithR8` → compila, pasan los tests (`AudioQualityTest`: clasificación, etiqueta, ReplayGain, ruta real y cabeceras FLAC/WAV) y R8 de *release* funciona. Media3 subió de 1.4.1 a 1.9.1; no probado en dispositivo.
