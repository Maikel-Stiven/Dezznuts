# Resumen técnico del proyecto — DEZZNUTS

Documento de apoyo para quien se incorpora a colaborar. Explica **cómo está construido**, **qué funciona**, **qué falta** y **por dónde empezar**.

- **Última revisión:** rama `fix/bugs-y-conexiones` (arreglos + pruebas sobre `main`).
- **Versión interna mostrada en la app:** `DEZZNUTS Player Alpha.0.0.1`.
- **Tamaño del código Kotlin:** ~7.000 líneas (app + 2 helpers nuevos + 6 archivos de pruebas).
- **Estado:** el proyecto **compila**, tiene **24 pruebas unitarias** que pasan y **CI** (GitHub Actions).

---

## 1. Visión general

DEZZNUTS es un reproductor de música **local** para Android. No reproduce streaming: indexa los archivos del propio dispositivo y los organiza. Dos APIs públicas se usan solo para enriquecer metadatos (carátulas y fotos):

- **iTunes Search API** → carátula y metadata oficial de una canción.
- **Deezer API** → foto de un artista.

Ambas son **gratuitas y sin API key**, lo cual encaja con la filosofía del proyecto ("funciones de pago, gratis").

---

## 2. Arquitectura

Arquitectura **MVVM** sencilla apoyada en Compose y un servicio de reproducción:

```
        Tabla/App (Compose)                Estado                Datos
 ┌───────────────────────────┐   ┌────────────────────┐   ┌──────────────────┐
 │  MainActivity             │   │  MusicViewModel    │   │ MediaStoreScanner│
 │  ├─ MainScreen (pestañas) │◄─►│  (UiState/Song…)   │◄─►│ Song / Playlist  │
 │  ├─ FullScreenPlayer      │   │  MediaController   │   │ SharedPreferences│
 │  └─ BottomPlayerBar       │   └─────────┬──────────┘   └──────────────────┘
 └───────────────────────────┘             │
                                           ▼
                              ┌────────────────────────┐
                              │  MusicService          │
                              │  (Media3 + ExoPlayer)  │
                              └────────────────────────┘
```

### Piezas clave

- **`MusicViewModel`** es el cerebro. Mantiene todo el estado como `StateFlow` (biblioteca, canción actual, cola, filtros, búsqueda, favoritos, listas, historial) y controla el `MediaController` de Media3. Persiste datos en `SharedPreferences` (`dezznuts_prefs`).
- **`MusicService`** extiende `MediaSessionService`. Crea el `ExoPlayer`, la `MediaSession` y la notificación (con botones personalizados de *Favorito* y *Cerrar*). Declara el permiso de reproducción y `handleAudioBecomingNoisy` (se pausa al desconectar audífonos).
- **`MainActivity`** pide el permiso de audio, arma el `ModalNavigationDrawer`, el `MainScreen` y superpone el `FullScreenPlayer` y el `EditTagDialog`.
- **`PlaylistSerializer`** (`data/`): convierte las listas de reproducción a/desde texto para `SharedPreferences`. **Codifica los campos** (URL-encoding) para que un nombre con `;`, `|` o `,` no corrompa los datos. Lo usa `MusicViewModel` en `savePlaylists`/`loadPlaylist`.
- **`SongOrganizer`** (`data/`): agrupaciones (álbum/artista/género/carpeta) y búsqueda como **funciones puras y genéricas**. Lo usan `GridScreens`, `ArtistInterfaceScreen` y el `filteredSongs` del ViewModel; al ser puras, están cubiertas por pruebas.

### Insets / edge-to-edge

Con `targetSdk 37` la app se dibuja **edge-to-edge**, es decir, por debajo de la barra de estado y del *cutout* de la cámara. Las pantallas de detalle usan `statusBarsPadding()`; el **reproductor a pantalla completa** usa `WindowInsets.safeDrawing` y el **mini-reproductor** `navigationBarsPadding()`, para que ni el header ni la barra inferior queden tapados.

### Pruebas y CI

- **6 archivos JUnit** (24 pruebas) sobre la lógica pura: `PlaylistSerializerTest`, `SongOrganizerTest`, `YoutubeTitleCleanerTest`, `SongEncodingTest`, `RepeatModeTest`, `DurationFormatTest`.
- **CI** en `.github/workflows/android.yml`: en cada push/PR corre `testDebugUnitTest` y `assembleDebug`.

### Flujo de reproducción

1. `MediaStoreScanner` emite la biblioteca en bloques → `MusicViewModel` construye `UiState.Success`.
2. Al pulsar una canción, `playSong()` arma una lista de `MediaItem` (id + `contentUri`) y la pasa al controlador.
3. El controlador arma la sesión en `MusicService`; la UI observa `isPlaying`, `currentSong` y `currentPositionMs` (un *tracker* consulta la posición cada 500 ms).

### Cadena de audio y opciones Hi/Res

El reproductor es **Media3 ExoPlayer**. La decodificación (FLAC/ALAC/WAV) la hace el códec del
sistema y es exacta; la app **no aplica ningún DSP ni efecto** por defecto. El único punto donde la
app puede ganar fidelidad real es la entrega del PCM al DAC, y se controla desde
`service/HiFiRenderersFactory.kt` (menú lateral → **Audio Hi/Res**):

- `setEnableAudioFloatOutput(true)` → salida en punto flotante de 32 bits. ExoPlayer solo la usa
  cuando el archivo es de alta resolución (24/32 bits), así que en 16 bits no cambia nada.
- `setOffloadMode(OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED)` → **offload**: el bitstream comprimido va
  directamente al DSP, sin remuestreo ni mezcla. Se exige que no rompa la reproducción sin cortes
  (de ahí `GAPLESS_REQUIRED`) y solo se aplica en **API 29+**; si el chip no lo soporta, ExoPlayer
  cae automáticamente a PCM.

**Los dos ajustes van apagados por defecto** (`HiFiAudio.DEFAULT_* = false`): modifican la ruta de
salida y dependen del dispositivo, así que los decide el usuario desde el diálogo, que explica qué
hace cada uno y su riesgo. Al cambiarlos, `MusicService.rebuildPlayer()` recrea el `ExoPlayer` con la
nueva configuración y lo intercambia con `MediaSession.setPlayer()`: la sesión y los
`MediaController` siguen conectados, y se conservan cola, índice, posición, repetición y aleatorio.

> **Límite real de Android:** AudioFlinger remuestrea todo a la frecuencia nativa del dispositivo
> (habitualmente 48000 Hz). Un FLAC de 44100 Hz se remuestrea salvo que haya offload o un DAC USB
> que negocie la tasa. El diálogo muestra esa frecuencia nativa
> (`AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE`).

### Ajuste de latencia de *seek*

`MusicService.buildLoadControl()` sustituye los valores por defecto de ExoPlayer (pensados para
streaming) por unos de reproducción local:

| Ajuste | Por defecto | DEZZNUTS | Por qué |
|--------|------------|----------|---------|
| `backBufferDurationMs` | `0` | `30 s` | Con 0 no hay audio decodificado detrás del cursor: retroceder obliga a releer y recodificar. |
| `bufferForPlaybackMs` | `2500` | `1000` | Tras un *seek*, ExoPlayer espera a tener esa cantidad antes de reanudar. |
| `bufferForPlaybackAfterRebufferMs` | `5000` | `2000` | Igual, tras un corte o una pausa larga. |

Si el FLAC no incluye un bloque `SEEKTABLE`, el extractor de Media3 hace una búsqueda binaria sobre
el archivo (`FlacBinarySearchSeeker`) para localizar el fotograma; eso es propio del formato y no se
puede ajustar desde la app (con `SEEKTABLE` usa `FlacSeekTableSeekMap`, que es inmediato).

### Ruta de acceso al archivo (`file://` en vez de `content://`)

`MusicViewModel.playbackUri()` entrega el archivo por **ruta directa** cuando se puede leer (usa
`Song.path`, que el escáner ya guardaba para agrupar por carpeta) y solo cae al `content://` de
MediaStore si no. El motivo está medido: cada *seek* fuera del búfer obliga a ExoPlayer a **reabrir la
fuente**, y `content://` añade en cada apertura una llamada al proveedor de medios además del paso por
la capa de almacenamiento.

### Diagnóstico de latencia del *seek* (`PlaybackDiagnostics`)

Cuando el *seek* "se siente lento" no basta con suponer dónde está el tiempo: puede estar en el I/O,
en el decodificador, en el búfer del `AudioTrack` o en la interfaz. `PlaybackDiagnostics` engancha un
`AnalyticsListener` a cada reproductor (también a los que crea `rebuildPlayer()`) y el ViewModel marca
el instante exacto en que el usuario pide el salto, así que la traza mide el trayecto completo:
**interfaz → sesión → ExoPlayer → sonido**.

```bash
adb logcat -s DezzSeek      # y luego mover el tiempo de una canción
```

| Línea | Qué revela |
|-------|------------|
| `seek pedido -> N ms` | Inicio de la medición (ViewModel). |
| `posición movida a N ms (seek)` | El *seek* llegó al reproductor. Si la posición no coincide con la pedida, el archivo no trae `SEEKTABLE`. |
| `lectura que tardó N ms` | I/O lento (solo se registra a partir de 40 ms, para no inundar logcat). |
| `decodificador '...' inicializado en N ms` | Coste de arrancar el `MediaCodec`. |
| `AudioTrack creado: ... encoding=...` | **Formato real de salida** (PCM 16 bits / float 32 / offload) y tamaño de búfer. |
| `>>> AUDIO SONANDO` | El instante real en que empieza a oírse. **Es la línea que importa.** |
| `UNDERRUN` | El `AudioTrack` se quedó sin datos (se oye un salto). |

Solo escribe en logcat: no altera la reproducción. Para silenciarlo, basta con no filtrar por la
etiqueta `DezzSeek`.

### Persistencia actual (SharedPreferences)

| Clave | Contenido |
|-------|-----------|
| `fav_ids` | IDs de canciones favoritas |
| `play_counts` | `id:conteo,id:conteo,…` |
| `recent_ids` | IDs de reproducción reciente |
| `last_song_id`, `last_position_ms` | Última canción y posición |
| `saved_playlist` | Listas serializadas: `id|nombre|portada|ids…` |
| `search_history` | Historial de búsqueda (`StringSet`) |
| `artist_photos_cache` | URLs de fotos de artistas (prefs aparte) |
| `hifi_float_output` | Salida PCM en 32 bits (float) — por defecto `false` |
| `hifi_audio_offload` | Audio offload (bit-perfect) — por defecto `false` |

---

## 3. Inventario de archivos

### Paquete `data/` (modelos y acceso a datos)

| Archivo | Rol | Estado |
|---------|-----|--------|
| `Song.kt` | Modelo de canción + `fixEncoding()` (repara mojibake de títulos) | ✅ |
| `Playlist.kt` | Modelo de lista de reproducción | ✅ |
| `RepeatMode.kt` | Enum de repetición + `next()` | ✅ |
| `MediaStoreScanner.kt` | Escaneo de audio local en bloques | ✅ |
| `AudioTagWriter.kt` | Escritura de etiquetas ID3 y carátula | ✅ (con salvedades SAF) |
| `CoverFetcher.kt` | Metadata/carátula oficial (iTunes) | ✅ |
| `ArtistImageFetcher.kt` | Foto de artista (Deezer) + caché | ✅ |
| `HiFiAudio.kt` | Preferencias de calidad de audio y datos de la salida del dispositivo | ✅ |
| `YoutubeTitleCleaner.kt` | Limpia nombres tipo `Artista - Tema (Official Video)` | ✅ |
| `PlaylistSerializer.kt` | Serializa las listas para `SharedPreferences` (campos codificados) | ✅ |
| `SongOrganizer.kt` | Agrupaciones (álbum/artista/género/carpeta) y búsqueda, puras y testeables | ✅ |

### Paquete `service/`

| Archivo | Rol | Estado |
|---------|-----|--------|
| `MusicService.kt` | Reproducción en 2º plano y sesión | ✅ |
| `HiFiRenderersFactory.kt` | Salida float de 32 bits y audio offload | ✅ |
| `PlaybackDiagnostics.kt` | Traza de latencia del *seek* en logcat (`DezzSeek`) | ✅ |

### Paquete `gui/` (interfaz Compose)

| Archivo | Rol | Estado |
|---------|-----|--------|
| `MusicViewModel.kt` | Estado central de la app | ✅ |
| `MainScreen.kt` | Scaffold, pestañas y navegación interna | ✅ |
| `FullScreenPlayer.kt` | Reproductor completo + cola arrastrable + shuffle real | ✅ |
| `AppDrawerContent.kt` | Menú lateral | ✅ (ítems sin lógica) |
| `SearchScreen.kt` | Búsqueda con historial y sub-pestañas | ✅ |
| `GridScreens.kt` | Grillas de álbumes/géneros y lista de artistas | ✅ |
| `QuickSelectionScreen.kt` / `QuickSelectionDetailScreen.kt` | Filas rápidas y su detalle | ✅ |
| `AlbumDetailScreen.kt` | Detalle de álbum + animación de ecualizador | ✅ |
| `ArtistInterfaceScreen.kt` | Detalle de artista | ✅ |
| `GenreDetailScreen.kt` | Detalle de género | ✅ |
| `PlaylistScreen.kt` | Lista de listas de reproducción | ✅ |
| `PlaylistDetailScreen.kt` | Detalle de lista | ✅ |
| `EditTagDialog.kt` | Editar etiquetas + "Auto-web" + galería | ✅ |
| `CreatePlaylistDialog.kt` | Crear lista | ✅ |
| `AddSongsToPlaylistDialog.kt` | Añadir canciones a lista | ✅ |
| `HiFiAudioDialog.kt` | Ajustes de audio Hi/Res (float / offload) | ✅ |

### `ui/theme/`

`Color.kt`, `Theme.kt` (paleta oscura) y `Type.kt` (tipografía). ✅

### Pruebas (`app/src/test/java/...`)

| Archivo | Qué cubre |
|---------|-----------|
| `PlaylistSerializerTest.kt` | Serialización round-trip y nombres con `;` `|` `,` |
| `SongOrganizerTest.kt` | Agrupaciones (álbum/artista/género/carpeta) y búsqueda |
| `YoutubeTitleCleanerTest.kt` | Parseo de `Artista - Tema (Official Video)` |
| `SongEncodingTest.kt` | `String.fixEncoding()` (mojibake de acentos) |
| `RepeatModeTest.kt` | Ciclo de `RepeatMode.next()` |
| `DurationFormatTest.kt` | `formatDuration` |

---

## 4. Matriz de funcionalidades

| Función | Estado |
|---------|:------:|
| Escaneo de música local | ✅ |
| Reproducción en 2º plano + notificación | ✅ |
| Mini-reproductor | ✅ |
| Reproductor a pantalla completa | ✅ |
| Carrusel de carátulas | ✅ |
| Seek/progreso | ✅ |
| Anterior / Siguiente | ✅ |
| Repetición (OFF/ALL/ONE/STOP) | ✅ |
| Cola: reordenar y quitar | ✅ |
| Favoritos persistentes | ✅ |
| Más reproducidas / Recientes | ✅ |
| Álbumes / Artistas / Géneros | ✅ |
| Listas de reproducción (crear/añadir/quitar) | ✅ |
| Búsqueda (canciones) | ✅ |
| Edición de etiquetas ID3 | ✅ |
| Auto-web de carátula/metadata | ✅ |
| Fotos de artista (Deezer) | ✅ |
| **Shuffle real en el reproductor** | ✅ |
| **Pestaña Carpetas** | ✅ |
| **Navegar desde búsqueda (álbum/artista)** | ✅ |
| **Favorito desde la notificación** | ✅ |
| **Menús ⋮ y "Restaurar"** | 🟡 Sin lógica |
| **Ecualizador** | 🔴 Pendiente |
| **Escanear archivos (manual)** | 🔴 Pendiente |
| **Buscar duplicados / WhatsApp** | 🔴 Pendiente |
| **Cortador de audio** | 🔴 Pendiente |
| **Temporizador** | 🔴 Pendiente |
| **Acerca de / Enviar comentarios** | 🔴 Pendiente |

---

## 5. Problemas y deuda técnica

### ✅ Corregidos en la rama `fix/bugs-y-conexiones`

1. **`PlaylistScreen.kt` no compilaba** (`Icons.Default.,` → `Icons.Default.MoreVert`). Era el bug bloqueante.
2. **Shuffle sin efecto:** `FullScreenPlayer` ahora recibe `isShuffleEnabled`/`onToggleShuffle` y el ViewModel lo aplica con `controller.shuffleModeEnabled` (reordena la cola de verdad).
3. **Favorito en notificación:** `MusicService.toggleCurrentFavorite()` actualiza `fav_ids`; el ViewModel lo detecta con un `OnSharedPreferenceChangeListener` y refresca la UI.
4. **Búsqueda por álbum/artista:** `SearchScreen` recibe `onAlbumClick`/`onArtistClick` y `MainScreen` los conecta a la navegación.
5. **Navegación de artistas** desde grillas/búsqueda: conectada.
6. **Pestaña Carpetas:** implementada (`FoldersListScreen` en `GridScreens.kt`, agrupa con `SongOrganizer.groupByFolder`).
7. **Persistencia frágil** (era deuda #9): mitigada con `PlaylistSerializer` (campos codificados).
8. **`INTERNET` duplicado** (era deuda #15): eliminado.
9. **`proguard-rules.pro` inexistente** (era deuda #19): creado.
10. **Sin tests** (era deuda #17): 24 pruebas + CI.
11. **Condiciones `equals("<unknown>")` duplicadas** en `ArtistInterfaceScreen` (era deuda #14): unificadas en `SongOrganizer`.
12. **Insets del reproductor** (reportado en dispositivo): `FullScreenPlayer` y `BottomPlayerBar` respetan ahora la barra de estado/cámara y la de gestos.

### Pendientes y deuda técnica

1. **Menús ⋮ y "Restaurar lista":** sin lógica.
2. **Icono `MusicNote` en varios sitios** con `tint = surfaceVariant` sobre fondo `surfaceVariant` (poca visibilidad) — detalle estético.
3. **Historial de búsqueda con `StringSet`:** no conserva el orden de inserción.
4. **`MediaStore.GENRE` requiere API 30+:** en Android < 11 todo aparece como "Desconocido".
5. **Escritura de etiquetas vía `contentUri` en modo `rwt`:** puede fallar por SAF en almacenamiento scoped.
6. **Fetch de fotos de artista por cada fila** (`LaunchedEffect` con `delay`): muchas peticiones simultáneas en listas grandes; considerar carga perezosa/por lotes.
7. **`sortOrder = TITLE ASC`** ignora mayúsculas y acentos; podría usar `COLLATE NOCASE`/locale.
8. **Toolchain muy nuevo** (Gradle 9.8.1, AGP 9.4.1, SDK 37): asegúrate de tener Android Studio actualizado o fallará el build por versión.
9. **Migración de persistencia** (`SharedPreferences` → Room/DataStore) sigue pendiente.

---

## 6. Por dónde empezar a colaborar

**Buenos primeros issues (bajo riesgo):**

1. **Menús ⋮** de canciones y listas: darles lógica.
2. **"Restaurar lista de reproducción":** implementar o retirar el botón.
3. **Icono `MusicNote`:** mejorar el contraste (hoy casi invisible).
4. **Historial de búsqueda:** guardarlo ordenado por recencia (usar lista, no `StringSet`).

**Features medianas (alto impacto):**

5. **Escanear archivos** (re-ejecutar `MediaStoreScanner` + `MediaScannerConnection`).
6. **Ecualizador** (`android.media.audiofx.Equalizer` enganchado al `ExoPlayer`).
7. **Temporizador** (corrutina que pausa el reproductor).
8. **Cortador de audio** (media3 `Transformer`).
9. **Buscar duplicados** (comparar por duración/tamaño/título).

> Ya resueltos en `fix/bugs-y-conexiones`: compilación, shuffle real, favorito de notificación, navegación de búsqueda, pestaña Carpetas, `INTERNET` duplicado y `proguard-rules.pro`.

**Refactor recomendado (a mediano plazo):**

10. Migrar persistencia a **Room/DataStore**.
11. Extraer un `repository` y dividir `MusicViewModel` (hoy maneja demasiadas responsabilidades).
12. Ampliar la cobertura de pruebas y añadir un README de arquitectura por módulo.

---

## 7. Convenciones del código

- Comentarios y **textos de UI en español**; varios nombres de variables también.
- Compose idiomático: `@Composable` + `remember`/`collectAsState`.
- Estado expuesto con `StateFlow` y `UiState` sellado (`PermissionRequired`, `Loading`, `Success`, `Empty`).
- Colores siempre desde `MaterialTheme.colorScheme`.

---

## 8. Glosario rápido

- **MediaStore:** base de datos de medios de Android; de ahí salen las canciones.
- **Media3 / ExoPlayer:** motor de reproducción de AndroidX.
- **MediaSessionService:** servicio que permite reproducir en 2º plano con controles.
- **MediaController:** objeto del ViewModel que envía órdenes al reproductor (play, cola, shuffle, repeat).
- **contentUri / albumArtUri:** punteros de contenido (`content://…`) al audio y a la carátula.
- **ID3:** etiquetas de metadatos incrustadas en los archivos de audio.
- **SAF:** Storage Access Framework; restringe el acceso a archivos en Android moderno.
- **Edge-to-edge / insets:** dibujar bajo las barras del sistema; los *insets* son las zonas ocupadas por la barra de estado, la de navegación y el *cutout* de la cámara.
- **URL-encoding:** codificación de texto para que caracteres especiales (`;`, `|`, `%`…) no rompan un formato de datos (lo usa `PlaylistSerializer`).
- **JUnit:** framework de pruebas unitarias; aquí prueba solo lógica pura (sin Android).
