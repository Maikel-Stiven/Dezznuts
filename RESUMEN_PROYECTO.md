# Resumen técnico del proyecto — DEZZNUTS

Documento de apoyo para quien se incorpora a colaborar. Explica **cómo está construido**, **qué funciona**, **qué falta** y **por dónde empezar**. No modifica código: es solo análisis del estado actual.

- **Última revisión:** commit `dbe5b86` (rama `main`).
- **Versión interna mostrada en la app:** `DEZZNUTS Player Alpha.0.0.1`.
- **Tamaño del código Kotlin:** ~6.700 líneas repartidas en 28 archivos.

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

### Flujo de reproducción

1. `MediaStoreScanner` emite la biblioteca en bloques → `MusicViewModel` construye `UiState.Success`.
2. Al pulsar una canción, `playSong()` arma una lista de `MediaItem` (id + `contentUri`) y la pasa al controlador.
3. El controlador arma la sesión en `MusicService`; la UI observa `isPlaying`, `currentSong` y `currentPositionMs` (un *tracker* consulta la posición cada 500 ms).

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
| `YoutubeTitleCleaner.kt` | Limpia nombres tipo `Artista - Tema (Official Video)` | ✅ |

### Paquete `service/`

| Archivo | Rol | Estado |
|---------|-----|--------|
| `MusicService.kt` | Reproducción en 2º plano y sesión | ✅ (favorito de notificación no funcional) |

### Paquete `gui/` (interfaz Compose)

| Archivo | Rol | Estado |
|---------|-----|--------|
| `MusicViewModel.kt` | Estado central de la app | ✅ |
| `MainScreen.kt` | Scaffold, pestañas y navegación interna | ✅ (carpetas pendiente) |
| `FullScreenPlayer.kt` | Reproductor completo + cola arrastrable | ✅ (shuffle visual) |
| `AppDrawerContent.kt` | Menú lateral | ✅ (ítems sin lógica) |
| `SearchScreen.kt` | Búsqueda con historial y sub-pestañas | 🟡 (álbumes/artistas no navegan) |
| `GridScreens.kt` | Grillas de álbumes/géneros y lista de artistas | ✅ |
| `QuickSelectionScreen.kt` / `QuickSelectionDetailScreen.kt` | Filas rápidas y su detalle | ✅ |
| `AlbumDetailScreen.kt` | Detalle de álbum + animación de ecualizador | ✅ |
| `ArtistInterfaceScreen.kt` | Detalle de artista | ✅ |
| `GenreDetailScreen.kt` | Detalle de género | ✅ |
| `PlaylistScreen.kt` | Lista de listas de reproducción | ⚠️ **error de compilación** |
| `PlaylistDetailScreen.kt` | Detalle de lista | ✅ |
| `EditTagDialog.kt` | Editar etiquetas + "Auto-web" + galería | ✅ |
| `CreatePlaylistDialog.kt` | Crear lista | ✅ |
| `AddSongsToPlaylistDialog.kt` | Añadir canciones a lista | ✅ |

### `ui/theme/`

`Color.kt`, `Theme.kt` (paleta oscura) y `Type.kt` (tipografía). ✅

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
| **Shuffle real en el reproductor** | 🟡 UI solamente |
| **Pestaña Carpetas** | 🟡 Placeholder |
| **Navegar desde búsqueda (álbum/artista)** | 🟡 Sin acción |
| **Favorito desde la notificación** | 🟡 Sin efecto |
| **Menús ⋮ y "Restaurar"** | 🟡 Sin lógica |
| **Ecualizador** | 🔴 Pendiente |
| **Escanear archivos (manual)** | 🔴 Pendiente |
| **Buscar duplicados / WhatsApp** | 🔴 Pendiente |
| **Cortador de audio** | 🔴 Pendiente |
| **Temporizador** | 🔴 Pendiente |
| **Acerca de / Enviar comentarios** | 🔴 Pendiente |

---

## 5. Problemas y deuda técnica

### ❗ Bloqueante

1. **`PlaylistScreen.kt` línea 198** contiene `imageVector = Icons.Default.,` (código inválido). Para compilar debe cambiarse por un icono válido, muy probablemente `Icons.Default.MoreVert` (ya está importado en el archivo). **Sin esto, el proyecto no compila.**

### Bugs y comportamientos incompletos

2. **Shuffle sin efecto:** en `FullScreenPlayer.kt`, `isShuffleActive` es un estado local que solo cambia el color; no se pasa al `ViewModel`.
3. **Favorito en notificación:** `MusicService.ACTION_FAVORITE` no hace nada.
4. **Búsqueda por álbum/artista:** los `onClick` de `AlbumGridCard` y `SearchCategoryItem` están vacíos.
5. **Navegación de artistas desde `GridScreens`/búsqueda:** no abre el detalle del artista.
6. **`Folders`:** muestra un `Text` de placeholder.
7. **`onRestoreClick` / "Restaurar lista":** sin lógica.
8. **Icono `MusicNote` en varios sitios** con `tint = surfaceVariant` sobre fondo `surfaceVariant` (poca visibilidad) — detalle estético.

### Deuda técnica / riesgos

9. **Persistencia frágil:** `saved_playlist` usa `;`, `|` y `,` como separadores. Un nombre con esos caracteres rompe el parseo. Migrar a **Room** o **DataStore + JSON**.
10. **Historial de búsqueda con `StringSet`:** no conserva el orden de inserción.
11. **`MediaStore.GENRE` requiere API 30+:** en Android < 11 todo aparece como "Desconocido".
12. **Escritura de etiquetas vía `contentUri` en modo `rwt`:** puede fallar por SAF en almacenamiento scoped.
13. **Fetch de fotos de artista por cada fila** (`LaunchedEffect` con `delay`): muchas peticiones simultáneas en listas grandes; considerar carga perezosa/por lotes.
14. **`ArtistInterfaceScreen`** filtra álbumes con dos condiciones `equals("<unknown>")` duplicadas (inofensivo, pero es ruido).
15. **`INTERNET` duplicado** en el `AndroidManifest.xml`.
16. **`sortOrder = TITLE ASC`** ignora mayúsculas y acentos; podría usar `COLLATE NOCASE`/locale.
17. **Sin tests reales:** los archivos `ExampleUnitTest`/`ExampleInstrumentedTest` son plantillas.
18. **Toolchain muy nuevo** (Gradle 9.6, AGP 9.4.1, SDK 37): asegúrate de tener Android Studio actualizado o fallará el build por versión.
19. **`proguard-rules.pro` referenciado en `app/build.gradle.kts` no existe.** Como `isMinifyEnabled = false`, no rompe hoy; conviene crear el archivo o migrar a `keepRules/` antes de activar R8.

---

## 6. Por dónde empezar a colaborar

**Buenos primeros issues (bajo riesgo):**

1. **Arreglar `PlaylistScreen.kt`** (desbloquea todo).
2. **Conectar el shuffle** al `ViewModel` (`player.shuffleModeEnabled`).
3. **Quitar el `INTERNET` duplicado** del manifest.
4. **Hacer navegables** los resultados de búsqueda de álbumes/artistas.
5. **Implementar favorito desde la notificación.**
6. **Crear `proguard-rules.pro`** (o mover reglas a `keepRules/`).

**Features medianas (alto impacto):**

7. **Pestaña Carpetas** (agrupar por `song.path`).
8. **Escanear archivos** (re-ejecutar `MediaStoreScanner` + `MediaScannerConnection`).
9. **Ecualizador** (`android.media.audiofx.Equalizer` enganchado al `ExoPlayer`).
10. **Temporizador** (corrutina que pausa el reproductor).
11. **Cortador de audio** (media3 `Transformer`).
12. **Buscar duplicados** (comparar por duración/tamaño/título).

**Refactor recomendado (a mediano plazo):**

13. Migrar persistencia a **Room/DataStore**.
14. Extraer un `repository` y dividir `MusicViewModel` (hoy maneja demasiadas responsabilidades).
15. Añadir tests y un `README` de arquitectura por módulo.

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
- **contentUri / albumArtUri:** punteros de contenido (`content://…`) al audio y a la carátula.
- **ID3:** etiquetas de metadatos incrustadas en los archivos de audio.
- **SAF:** Storage Access Framework; restringe el acceso a archivos en Android moderno.
