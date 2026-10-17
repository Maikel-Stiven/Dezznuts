# Cambios de la rama `fix/bugs-y-conexiones`

Rama alterna (no toca `main`) con **arreglos de bugs y conexiones seguras** detectados durante la revisión del proyecto. La idea es que sirva de propuesta para revisar, no como verdad absoluta: aunque **compila y las pruebas pasan**, conviene probar en dispositivo antes de fusionar.

Base: `main` @ `81f2aa4`.

---

## Qué se cambió

### 1. ❗ Error que rompía la compilación
- `gui/PlaylistScreen.kt`: `Icons.Default.,` → `Icons.Default.MoreVert`.
  **Sin esto el proyecto no compila.** Es el arreglo más importante.

### 2. Shuffle real en el reproductor
- `gui/MusicViewModel.kt`: nuevo estado `isShuffleEnabled` + `toggleShuffle()`, que usa `Player.shuffleModeEnabled` del controlador y escucha `onShuffleModeEnabledChanged`.
- `gui/FullScreenPlayer.kt`: el botón de aleatorio ya no es estado local; recibe `isShuffleEnabled` y `onToggleShuffle`. Ahora **sí reordena la reproducción**, tanto en los controles como en la hoja de cola.
- `MainActivity.kt`: conecta el estado y la acción.

### 3. Favorito desde la notificación
- `service/MusicService.kt`: el botón de favorito de la notificación ahora **alterna el favorito** del `mediaId` actual en `dezznuts_prefs` (`fav_ids`) y **refresca el ícono**. También refresca el layout al cambiar de pista.
- `gui/MusicViewModel.kt`: escucha cambios de `fav_ids` (`OnSharedPreferenceChangeListener`) y **sincroniza la UI** (biblioteca y canción actual) cuando el favorito se cambia desde la notificación.

### 4. Navegación desde la búsqueda
- `gui/SearchScreen.kt`: los resultados de **álbumes** y **artistas** ahora tienen `onClick`; se agregaron los parámetros `onAlbumClick` y `onArtistClick`.
- `gui/MainScreen.kt`: al tocar un álbum/artista en la búsqueda, cierra la búsqueda y abre su pantalla de detalle.

### 5. Pestaña Carpetas
- `gui/GridScreens.kt`: nuevo `FoldersListScreen` que agrupa por carpeta (según `Song.path`), muestra nombre + ruta + nº de canciones.
- `gui/MainScreen.kt`: la pestaña **Carpetas** ya no es un texto placeholder; al tocar una carpeta se abre el detalle (reutilizando `QuickSelectDetailScreen`).

### 6. Limpieza
- `AndroidManifest.xml`: se quitó el permiso `INTERNET` duplicado.
- `app/proguard-rules.pro`: se creó el archivo que `app/build.gradle.kts` referenciaba y no existía (con reglas `keep` para jaudiotagger).

### 7. Refactor para poder probar
Se extrajo lógica pura (antes encerrada en Composables y en el ViewModel) a helpers reutilizables:
- `data/PlaylistSerializer.kt`: serialización de listas, **codificando los campos** para que nombres con `;`, `|` o `,` ya no corrompan los datos (antes sí lo hacían).
- `data/SongOrganizer.kt`: agrupaciones (álbum/artista/género/carpeta) y búsqueda, como funciones puras y genéricas.
- `MusicViewModel`, `GridScreens` y `ArtistInterfaceScreen` ahora usan esos helpers (mismo comportamiento, menos duplicación).

### 8. Pruebas unitarias + CI
- 6 archivos de test en `app/src/test/...` (**24 pruebas**): serialización de playlists, `YoutubeTitleCleaner`, `fixEncoding`, `RepeatMode.next`, agrupaciones/búsqueda y `formatDuration`.
- `.github/workflows/android.yml`: CI que corre las pruebas y compila el APK en cada push/PR.

> Verificado localmente: `./gradlew testDebugUnitTest` → **BUILD SUCCESSFUL, 24 pruebas, 0 fallos**; `./gradlew assembleDebug` → **BUILD SUCCESSFUL**.

### 9. Insets del reproductor (reportado en dispositivo)
Con `targetSdk 37` la app es *edge-to-edge*, así que el contenido se dibuja bajo la barra de estado/cámara.
- `gui/FullScreenPlayer.kt`: se agregó `windowInsetsPadding(WindowInsets.safeDrawing)`. El header (flecha, "Reproduciendo desde", ⋮) ya no queda bajo la barra de estado ni el *cutout* de la cámara, y el panel inferior no queda bajo la barra de navegación.
- `MainActivity.kt` (`BottomPlayerBar`): `navigationBarsPadding()` para que el mini-reproductor no quede tapado por la barra de gestos.

### 10. Arreglo del CI (fallaba en "Configurar Android SDK")
- `.github/workflows/android.yml`: se actualizó `android-actions/setup-android` de **v3 → v4**. La v3 fallaba porque Google **eliminó el paquete `tools`** del SDK (la v3 intentaba instalarlo); la v4 ya no lo pide.
- Además se instalan explícitamente la plataforma y las build-tools del proyecto (`platforms;android-37.0`, `build-tools;36.0.0`) para no depender de la descarga automática de Gradle.
- **Acción pendiente:** al abrir un PR, verificar que el workflow ahora pase en verde.

---

## Checklist de prueba sugerido (en dispositivo)

- [x] La app **compila** (`./gradlew assembleDebug`) — verificado.
- [x] Las **pruebas unitarias** pasan (`./gradlew testDebugUnitTest`) — 24/24 verificado.
- [ ] Botón **Aleatorio** del reproductor: al activarlo, la siguiente canción es aleatoria y el icono queda resaltado.
- [ ] Botón de **Favorito en la notificación**: cambia el ícono y, al abrir la app, la canción aparece/desaparece de Favoritos.
- [ ] **Buscar** y tocar un **álbum** → abre el detalle del álbum.
- [ ] **Buscar** y tocar un **artista** → abre el detalle del artista.
- [ ] Pestaña **Carpetas** → lista carpetas; tocar una muestra sus canciones y permite reproducir/aleatorio.
- [ ] Las **listas de reproducción** existentes siguen cargando tras el cambio de formato.
- [ ] En el reproductor a pantalla completa, el header (flecha, "Reproduciendo desde", ⋮) queda **debajo** de la cámara/barra de estado, y el mini-reproductor no queda tapado por la barra de gestos.

---

## Lo que NO se tocó (a propósito)

- Funciones grandes pendientes: **Ecualizador**, **Escanear archivos**, **Buscar duplicados**, **Cortador de audio**, **Temporizador**, **Acerca de**, **Enviar comentarios**.
- Migración de persistencia (`SharedPreferences` → Room/DataStore).
- Otros pendientes menores: menús ⋮, "Restaurar lista", `MusicNote` con bajo contraste, historial de búsqueda sin orden, etc.

Detalle completo en [`RESUMEN_PROYECTO.md`](./RESUMEN_PROYECTO.md).
