# Cambios de la rama `fix/bugs-y-conexiones`

Rama alterna (no toca `main`) con **arreglos de bugs y conexiones seguras** detectados durante la revisión del proyecto. La idea es que sirva de propuesta para revisar, no como verdad absoluta: hay cambios que **no pude compilar ni probar en este entorno** (no había Android SDK), así que conviene probarlos en un dispositivo antes de fusionar.

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

---

## Checklist de prueba sugerido (en dispositivo)

- [ ] La app **compila** (`./gradlew assembleDebug`).
- [ ] Botón **Aleatorio** del reproductor: al activarlo, la siguiente canción es aleatoria y el icono queda resaltado; persiste al reabrir el reproductor.
- [ ] Botón de **Favorito en la notificación**: cambia el ícono y, al abrir la app, la canción aparece/desaparece de Favoritos.
- [ ] **Buscar** y tocar un **álbum** → abre el detalle del álbum.
- [ ] **Buscar** y tocar un **artista** → abre el detalle del artista.
- [ ] Pestaña **Carpetas** → lista carpetas; tocar una muestra sus canciones y permite reproducir/aleatorio.

---

## Lo que NO se tocó (a propósito)

- Funciones grandes pendientes: **Ecualizador**, **Escanear archivos**, **Buscar duplicados**, **Cortador de audio**, **Temporizador**, **Acerca de**, **Enviar comentarios**.
- Migración de persistencia (`SharedPreferences` → Room/DataStore).
- Otros pendientes menores: menús ⋮, "Restaurar lista", `MusicNote` con bajo contraste, historial de búsqueda sin orden, etc.

Detalle completo en [`RESUMEN_PROYECTO.md`](./RESUMEN_PROYECTO.md).
