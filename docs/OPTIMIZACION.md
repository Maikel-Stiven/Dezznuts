# Optimización del reproductor (rama `optimizacion`)

Resumen de lo que se hizo para que el reproductor gaste menos CPU, memoria y disco, y de lo que **no** se midió.

## Cómo está organizado el reproductor

```
MusicService (Media3 / ExoPlayer, proceso en segundo plano)
        ▲  MediaController
MusicViewModel ── StateFlow ──► MainActivity ──► FullScreenPlayer
 (estado, cola, favoritos,                         ├─ HorizontalPager (carátulas)
  posición cada 500 ms)                            ├─ botones (play, siguiente…)
                                                   └─ PlayerSeekBar (barra + tiempos)
```

La posición de reproducción es el único dato que cambia continuamente (cada 500 ms), así que es donde más cuesta un error de diseño.

## Cambios en esta rama

### 1. La posición solo recompone la barra de progreso
- **Antes:** `FullScreenPlayer` recibía `currentPositionMs: Long`. Al leerlo en su cuerpo, **todo** el reproductor (cabecera, carrusel de carátulas, botones) se recomponía 2 veces por segundo.
- **Ahora:** recibe `currentPositionMs: () -> Long` y solo la nueva función `PlayerSeekBar` lo invoca, de modo que Compose limita la recomposición a esa barra y a los dos textos de tiempo.
- El estado del arrastre (`isDragging`, `dragMs`) se movió dentro de `PlayerSeekBar`; el comportamiento del *slider* es el mismo.
- Archivos: `gui/FullScreenPlayer.kt`, `MainActivity.kt`.

### 2. Favoritos: se evita remapear la biblioteca dos veces
- **Antes:** `toggleFavorite` actualizaba la lista y guardaba `fav_ids`; ese guardado disparaba el *listener* de preferencias (pensado para la notificación), que **volvía a copiar toda la biblioteca**.
- **Ahora:** `syncFavoritesFromPrefs` sale sin hacer nada si el estado ya coincide con lo guardado. Con el botón de la notificación (que sí cambia las preferencias desde fuera) sigue sincronizando.
- Archivo: `gui/MusicViewModel.kt`.

### 3. Caché de URI de reproducción
- **Antes:** cada `playSong` / reanudar construía los `MediaItem` de **toda la cola** y llamaba a `File.canRead()` (acceso a disco, en el hilo principal) por cada canción.
- **Ahora:** el resultado se guarda en `playbackUriCache` (`id → Uri`) y se limpia cada vez que se publica la biblioteca (`publishSongs`), por si las rutas cambian.
- Archivo: `gui/MusicViewModel.kt`.

### 4. Ajustes tras fusionar con `main`
- **Caché de URI segura entre hilos:** `playbackUriCache` pasó a `ConcurrentHashMap`, porque `main` construye la cola en `Dispatchers.Default` y la caché se limpia desde el hilo principal.
- **`playSong` usa `buildMediaItems`:** antes armaba los `MediaItem` con `Uri.fromFile` sin comprobar `canRead()`; ahora comparte la caché y el respaldo a `content://`.
- **`MainScreen`:** el detalle de lista de reproducción se filtra con un `HashSet` dentro de `remember`, no en cada recomposición.
- **`SearchScreen`:** los conteos de álbumes/canciones por artista se calculan una vez por (biblioteca, artista) con `remember`, no en cada recomposición de la fila.
- **`GridScreens`:** la lista de claves (`keys.toList()`) se recuerda con `remember` y los álbumes ahora tienen `key`.

### 5. Revisión de las funciones de audiófilos (1.1.0)
- **Fallo corregido en el ecualizador:** el filtro reasignaba el estado de las bandas al cambiar cuántas estaban activas, mientras el hilo de audio lo leía → `ArrayIndexOutOfBoundsException` (reproducido con un test concurrente) que habría cortado la reproducción al mover un deslizador. Ahora el estado tiene tamaño fijo (16 bandas) y cada banda conserva su sitio; una banda que se activa arranca con el estado limpio.
- **Conversión de formatos del procesador:** un bucle por formato en lugar de un `when` por muestra (a 96 kHz estéreo son 192 000 muestras/s) y volcado/lectura en bloque para float. El filtro cuesta ~1 ms por segundo de audio (5 bandas, 96 kHz estéreo); un test lo vigila con presupuesto de tiempo real.
- **Cachés de escaneo:** la cabecera FLAC/WAV (Android < 16) y la hoja CUE se leen una vez por canción y no en cada recarga silenciosa de MediaStore, que ocurre por cada archivo nuevo.
- **Ruta real de salida:** se crea un `AudioTrack` por pista; ahora solo se escribe en las preferencias (y se notifica a la interfaz) si la ruta cambió.
- **Duplicados:** título y artista se normalizan una sola vez por canción (antes, tres).

### 6. Apertura lenta de una canción tras entrar una canción nueva
- **Síntoma:** al detectarse una canción nueva, la app tardaba un poco en dejar reproducir.
- **Causa (confirmada por la lógica, medida en emulador):** la caché de URI de reproducción se vaciaba entera en cada recarga de la biblioteca (`publishSongs`), así que la siguiente reproducción volvía a comprobar con `File.canRead()` **cada** canción de la cola. Con 2 512 canciones son ~800 ms de accesos a disco en el emulador; en un móvil, donde el almacenamiento pasa por una capa más lenta, es más.
- **Arreglo:** (1) la caché guarda la ruta con la que se resolvió, así que se invalida sola si la ruta cambia y ya **no se vacía** al recargar; solo se podan las canciones que desaparecen. (2) Un precalentamiento en segundo plano resuelve los URI que faltan por tandas de 250 y paralelismo limitado a 4, de modo que tocar una canción no paga esos accesos.
- **Medición (2 512 canciones, emulador API 35):** construir la cola al tocar una canción: 16–18 ms; tras entrar una canción nueva solo se resuelve esa (2 ms) y la cola vuelve a construirse en 16 ms. La recarga silenciosa tarda 40 ms.
- **Límite:** no se pudo reproducir la lentitud original en el emulador (el almacenamiento es rápido); la causa se deduce del coste medido y de que la caché se vaciaba. Conviene confirmarlo en el móvil donde se vio.

## Optimizaciones previas ya presentes en la rama

| Qué | Efecto |
|---|---|
| El sondeo de posición solo corre mientras **suena y la app es visible** (`collectLatest` sobre `isPlaying` + `uiVisible`) | En pausa o en segundo plano: 0 despertares de CPU (medido: de 2/s a 0 en emulador) |
| La posición se guarda al ocultar la UI | Se restaura bien al reabrir sin sondear en segundo plano |
| `collectAsStateWithLifecycle` en lugar de `collectAsState` | La UI deja de recolectar flujos cuando no es visible |
| Pertenencia a listas con `Set` y `remember` (`MainScreen`, `PlaylistScreen`) | De O(canciones × canciones de la lista) a O(canciones) |
| Se quitó la dependencia `media3-ui` (sin uso) | APK release −0,65 MB |
| R8 + recorte de recursos en *release* | APK sin firmar de ~17 MB a ~3,3 MB |
| `PlaybackDiagnostics` solo en builds depurables | Sin trazas ni coste en *release* |
| Recarga silenciosa de MediaStore con *debounce* de 1,5 s | Una sola recarga por ráfaga de cambios, sin parpadeo de la lista |

## Verificación

- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` → compila y las pruebas unitarias pasan.
- **No medido en dispositivo:** los puntos 1–3 son mejoras estructurales evidentes (menos trabajo por tick / por toque), pero no se cuantificaron con un perfilador. Para comprobarlo: *Layout Inspector → Recomposition counts* con el reproductor abierto (la cabecera y el carrusel no deben subir de contador mientras suena).

## Ideas pendientes (no hechas a propósito)

- `registerSongPlay` reordena toda la biblioteca para "Más reproducidas" en cada cambio de pista; solo importa con bibliotecas de varios miles de canciones.
- `savePlayCounts` reescribe todo el mapa en cada reproducción; podría hacerse con *debounce* si el coste llegara a notarse.
