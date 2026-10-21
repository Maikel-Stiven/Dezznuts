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

La posición de reproducción es el único dato que cambia continuamente (cada 500 ms, y solo mientras la barra está en pantalla), así que es donde más cuesta un error de diseño.

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

### 7. Revisión tras integrar ecualizador premium, bit-perfect, fundidos y editor
- **Curva del ecualizador:** `PeakingFilter.responseCurveDb` calcula los coeficientes una vez por banda y el seno/coseno de cada frecuencia una vez para todas las bandas (antes, por punto y por banda). 160 puntos × 5 bandas: **18 µs por fotograma** (un test lo vigila y comprueba que da lo mismo que el cálculo punto a punto).
- **Textos del lienzo:** `rememberTextMeasurer(cacheSize = 64)`; la caché por defecto (8) se quedaba corta con ~20 textos distintos por fotograma y los volvía a maquetar cada vez.
- **Limpiador de títulos:** las expresiones regulares se compilan una vez (antes, cinco por llamada; con miles de canciones sin etiquetas, "Corregir artistas desconocidos" las compilaba decenas de miles de veces).
- **Bit-perfect apagado (lo habitual):** `applyBitPerfect` sale antes de consultar dispositivos al sistema; antes hacía esa consulta en cada cambio de pista.
- **Prueba de resistencia del conjunto** (emulador API 35, 2 min, EQ y subida de ReplayGain activos): memoria estable en ~165 MB, 37 hilos, sin errores.

## Optimizaciones previas ya presentes en la rama

| Qué | Efecto |
|---|---|
| El sondeo de posición solo corre mientras **la barra la observa y suena** (`subscriptionCount` + `collectLatest` sobre `isPlaying`) | En pausa o con la pantalla del reproductor cerrada: 0 ticks; con ella abierta, 500 ms |
| La posición se persiste en **eventos del reproductor** (pausa, fin, cambio de pista) y en `onTaskRemoved` | Sin escrituras periódicas; se restaura bien al reabrir |
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

### 8. Menos trabajo en el hilo principal y menos RAM (equipos modestos)
- **Agrupados de pestañas** (álbumes, artistas, géneros, carpetas) y **búsqueda**: se calculan en `Dispatchers.Default`; antes bloqueaban la interfaz al abrir la pestaña o teclear.
- **`filteredSongs` y `favoriteSongs`**: `flowOn(Dispatchers.Default)`.
- **Portadas (Coil):** caché en memoria al 10 % de la RAM (5 % en equipos de poca RAM) en vez del 25 %, y RGB565.
- **Posición de reproducción:** el sondeo solo corre mientras suena (en pausa no despierta) y se guarda en disco al pausar y cada 10 s, no cada segundo ni en cada *seek*.
- **Interfaz:** `MainScreen` recoge los flujos con `collectAsStateWithLifecycle` (nada en segundo plano); `formatDuration` sin `String.format` (se llamaba por fila); conteo de álbumes por artista con `remember`.
- No medido en dispositivo: son recortes de trabajo evidentes, falta confirmar la mejora en un móvil real.

## 9. Pasada de rendimiento para equipos modestos (rama `perf/rendimiento`)

Objetivo: que la app corra bien en equipos de **2 núcleos y 2–4 GB de RAM**, sin tocar la ruta de audio (la
calidad de reproducción no cambia: todo esto es interfaz y red).

**Cómo se midió.** Emulador Android 15 con **2 núcleos y 2 GB**, APK *release* (minificado) firmado, biblioteca de
~2 060 canciones (40 artistas, 15 álbumes, 8 géneros) y una pista de 30 min con carátula. Mismo guion antes y
después (CPU leída de `/proc/<pid>/stat` durante 20 s; memoria con `dumpsys meminfo`; fotogramas con
`dumpsys gfxinfo`). El emulador dibuja por software, así que **los porcentajes absolutos exageran** lo que pasaría con
una GPU real; la *proporción* entre antes y después sí es comparable.

### Resultado (mismo emulador, 2 núcleos)
| Escenario (CPU del proceso, % de un núcleo) | Antes | Ahora |
|---|---|---|
| Reproductor completo abierto y sonando | 76,5 % | **6,0 %** |
| Lista visible con una canción sonando | 29,7 % | **3,5 %** |
| App en segundo plano sonando | 3,2 % | 3,1 % |
| Memoria (PSS) al abrir la pestaña Artistas | +24 MB | **+14 MB** |

### Qué se cambió (y por qué)
1. **La lista bajo el reproductor no anima.** La pantalla principal seguía compuesta debajo del reproductor a pantalla
   completa; su icono de barras de ecualizador (animación infinita) forzaba 60 fotogramas por segundo en toda la
   ventana aunque nadie lo veía. `LocalAnimationsEnabled` lo apaga cuando algo lo tapa.
2. **Icono de barras:** tres animaciones infinitas a 60 fps → un único reloj a ~15 fps.
3. **Fondo "cristal" más barato:** 2 capas en vez de 3, la saturación se hornea una vez en el bitmap de 24 px (antes un
   filtro de color por capa y fotograma) y se actualiza a ~15 fps.
4. **Regla única de efectos** (`rememberVisualEffectsEnabled`): en equipos de 2 núcleos o poca RAM, con ahorro de
   batería o con las animaciones del sistema apagadas, el fondo y las barras quedan **fijos** (mismo aspecto, cero
   fotogramas extra).
5. **Fotos de artistas:** las consultas a Deezer ya no se lanzan todas a la vez (`Semaphore(2)`): menos conexiones HTTPS
   simultáneas y las filas que se salen de pantalla cancelan su espera.
6. **Claves de listas** (auditoría de las 21 listas perezosas): el detalle de una playlist llevaba el índice en la clave y
   recreaba todas las filas siguientes al quitar una canción; el historial de búsqueda no tenía clave.
7. **Calidad (Lossless / Hi-Res) como cinta en la portada** de cada fila, en vez de un distintivo junto al título.

### Probado y descartado (no mejoraba, se revirtió)
| Idea | Medida | Veredicto |
|---|---|---|
| Baseline profile propio (`HSPL com/example/**`) | arranque en frío 305/298 → 295/284 ms (ruido ±15 ms) | revertido: dentro del ruido |
| Conectar con el servicio de reproducción tras el primer fotograma | arranque sin compilar 319 → 367 ms | revertido: empeora |

### Pendiente / límites conocidos
- Cambiar de pestaña por primera vez tiene un pico de 100–200 ms en el primer fotograma (Canciones y Artistas). Se
  comprobó que **no** es la red ni el JIT (persiste con el código compilado por completo y sin cargar fotos); es el
  primer dibujo de la lista en el emulador. No se midió en un teléfono real.
- Arranque en frío ~300 ms (compilado) / ~320–530 ms (recién instalado, sin compilar) en el emulador de 2 núcleos.
- Todo medido en emulador; conviene confirmar en un teléfono de gama baja real.
