# Funciones para audiófilos (rama `audiofilos`)

Todo viene **apagado por defecto** y no altera la señal salvo que el usuario lo active (menú → *Audio Hi/Res*).

| Función | Dónde | Cómo funciona |
|---|---|---|
| Insignia y datos de la pista | Reproductor | Muestra `FLAC · 24 bit · 96 kHz · 2304 kbps → <ruta>`. Los datos salen del `Format` real de Media3. La **ruta es la real**: el servicio la lee del `AudioTrack` que se abrió (`onAudioTrackInitialized`) y la distingue en *Offload*, *PCM directo* o *PCM remuestreado a X kHz* por el mezclador. |
| Ficha técnica | Reproductor → ⋮ | Archivo, calidad, ruta real de salida y ruta del archivo. |
| ReplayGain | Diálogo Hi/Res | Lee `REPLAYGAIN_TRACK_GAIN` / `ALBUM_GAIN` (Vorbis/ID3) y ajusta el volumen; modo **pista** o **álbum** (este conserva la dinámica entre canciones de un disco). Por defecto **solo atenúa** (sin clipping); opcionalmente **sube** hasta lo que permite `REPLAYGAIN_*_PEAK` (`−20·log10(pico)`), sin recortar. |
| Filtros Lossless / Hi-Res | Pestaña Canciones | Clasifica con MIME/extensión y, en Android 11+, profundidad y frecuencia de MediaStore. Hi-Res = sin pérdida y >16 bit o >48 kHz. |
| Distintivos en listas | `SongItem` | Etiqueta "Lossless" / "Hi-Res" junto a cada canción. |
| DAC USB | Diálogo Hi/Res | Lista las salidas conectadas con sus frecuencias y permite preferir el DAC USB (`setPreferredAudioDevice`), reaplicado al conectar/desconectar. |
| Ecualizador paramétrico | Menú lateral → Ecualizador | 5 bandas con frecuencia, ganancia (±12 dB) y Q. Procesador propio en **float de 32 bits** dentro de la cadena de audio (`ParametricEqProcessor`), sin depender de efectos del sistema. Preamp automático = −(mayor subida) para no saturar. Con todo apagado el procesador no existe en la cadena (bit-perfect). No actúa con offload. |

| Hojas CUE | Biblioteca | Álbumes enteros en un solo archivo se ofrecen pista a pista (reproducción por tramo con `ClippingConfiguration`). Lee el `CUESHEET` **incrustado** en el FLAC; un `.cue` suelto solo se lee donde el sistema lo permita (en Android 11+ el almacenamiento con alcance lo bloquea). |
| Duplicados y WhatsApp | Menú lateral | Agrupa por título/artista normalizados y duración; propone conservar la mejor calidad y borrar el resto con confirmación del sistema (`createDeleteRequest`, Android 11+). Lista aparte los audios de WhatsApp. |
| Perfiles por salida | Diálogo Hi/Res | Guarda ReplayGain y ecualizador por tipo de salida (altavoz, cable, Bluetooth, DAC USB) y los cambia solos al conectar o desconectar. |

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
- **AutoEQ (perfiles por auricular) y presets del ecualizador:** requieren una base de datos de curvas.
- **Crossfade entre pistas CUE:** las pistas de un mismo archivo pueden tener un corte mínimo entre sí (cada una es un `MediaItem` recortado).
- **Prueba de perfiles con hardware real:** cambiar de salida (USB/Bluetooth) no se puede simular en el emulador; la lógica de clasificación sí está probada.

## Pruebas en emulador (Android 15, API 35)
Con archivos generados con ffmpeg (FLAC 16/44,1, FLAC 24/96 con `REPLAYGAIN_TRACK_GAIN=-9 dB`, MP3 y WAV 24/48):
- **Clasificación:** FLAC 16/44,1 → Lossless; FLAC 24/96 y WAV 24/48 → Hi-Res; MP3 → sin insignia.
- **Ruta real:** WAV 48 kHz → `PCM directo`; MP3 44,1 kHz y FLAC 96 kHz → `remuestreado a 48 kHz`; con salida float → `PCM 32 bit`.
- **ReplayGain:** la etiqueta de −9 dB se lee del FLAC y el volumen queda en 0,355 (= −9 dB).
- **Ecualizador:** subir una banda +10 dB baja el volumen −10,3 dB (preamp); al apagarlo vuelve a su valor.
- **Ecualizador paramétrico:** con el procesador activo el `AudioTrack` se abre en `PCM float 32 bits` y suenan FLAC 16/44,1, FLAC 24/96 y MP3 sin errores. La matemática se prueba en tests midiendo la ganancia real sobre tonos puros (+6 dB a 1 kHz → 0 dB neto en el centro y −6 dB fuera por el preamp).
- **ReplayGain con subida:** una pista con `+10 dB` y pico `0,5` recibe `+6,02 dB`, el máximo que el pico permite.
- **CUE:** un FLAC de 9 min con `CUESHEET` incrustado aparece como 3 pistas de 3:00; "Segunda" arranca en 00:00 / 03:00.
- **Duplicados:** FLAC y MP3 con el mismo título → se conserva el FLAC; el borrado pide confirmación al sistema y elimina el archivo.
- **Offload:** el emulador no lo soporta y cae a PCM sin fallos (la ruta lo muestra).
- **Error encontrado y corregido gracias al emulador:** `SAMPLERATE`/`BITS_PER_SAMPLE` no existen antes de Android 16; pedirlas cerraba la app al arrancar en Android 11–15.

## Prueba de resistencia
`scripts/soak-test.sh [segundos]` reproduce y salta pistas sin parar (con EQ y ReplayGain con subida activos) y registra memoria (PSS) e hilos cada 10 s, avisando si la memoria crece de forma sostenida.

**Resultado (emulador API 35, 6 min, ~120 saltos de pista, EQ y ReplayGain con subida activos):** la memoria se estabiliza en ~158 MB desde el segundo 15 (el +5,6 % frente a la primera muestra es el arranque, no una fuga: va de 162 MB a 158 MB), hilos constantes en 38, sin cierres ni errores de reproducción.

## Verificación
`./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:minifyReleaseWithR8` → compila, pasan los tests (`AudioQualityTest`: clasificación, etiqueta, ReplayGain, ruta real y cabeceras FLAC/WAV) y R8 de *release* funciona. Media3 subió de 1.4.1 a 1.9.1; no probado en dispositivo.
