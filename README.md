# DEZZNUTS 🎧

Reproductor de música **local** para Android, **Open Source** y gratuito. La idea del proyecto es ofrecer de forma libre esas funciones que otros reproductores suelen encerrar tras un pago (etiquetado automático, cortador de audio, ecualizador, temporizador, etc.), "solo por amor al arte".

> ⚠️ **Estado: Alfa temprana (`Alpha 0.0.1`).** El reproductor base ya funciona y el proyecto **compila** (con pruebas unitarias y CI). Algunas funciones del menú todavía son solo interfaz sin lógica; revisa la sección [Problemas conocidos](#-problemas-conocidos).

---

## 📦 ¿Qué es?

DEZZNUTS es una app Android nativa escrita en **Kotlin + Jetpack Compose** que escanea la música almacenada en el dispositivo (MediaStore) y la reproduce con **Media3 / ExoPlayer** en segundo plano. Incluye organización por álbumes, artistas y géneros, listas de reproducción, favoritos, búsqueda, edición de etiquetas ID3 y carátulas automáticas (APIs públicas de Deezer e iTunes, sin claves de API).

---

## ✨ Funcionalidades

### ✅ Implementadas y funcionando

| Área | Detalle |
|------|---------|
| **Escaneo local** | Lee la música del dispositivo vía `MediaStore`, en bloques de 100 para no bloquear la UI. |
| **Reproducción en 2º plano** | Servicio `MusicService` (Media3 `MediaSessionService`) con notificación y controles. |
| **Reproductor a pantalla completa** | Carrusel de carátulas deslizable, barra de progreso con *seek*, anterior/siguiente, play/pausa. |
| **Modos de repetición** | `OFF` → `ALL` → `ONE` → `STOP_AFTER_CURRENT` (detener al terminar la canción). |
| **Cola de reproducción** | Hoja inferior con la cola; reordenar arrastrando y quitar canciones. |
| **Mini-reproductor** | Barra inferior fija cuando hay una canción activa. |
| **Modo aleatorio (Shuffle)** | Reordena la cola de verdad (`shuffleModeEnabled`); el icono refleja el estado. |
| **Pestaña Carpetas** | Agrupa la biblioteca por carpeta del archivo. |
| **Búsqueda navegable** | Los resultados de **Álbumes** y **Artistas** abren su pantalla de detalle. |
| **Favorito desde la notificación** | El botón ❤ de la notificación se sincroniza con los favoritos de la app. |
| **Favoritos ❤** | Marcar/desmarcar; se guardan entre sesiones. |
| **Contador de reproducciones** | Registra reproducciones para "Más reproducidas". |
| **Reproducción reciente** | Historial de las últimas canciones escuchadas. |
| **Listas rápidas** | Recién agregadas, reproducción reciente, más reproducidas y favoritos. |
| **Pestañas de biblioteca** | Canciones, Álbumes, Artistas, Géneros, Listas de reproducción. |
| **Búsqueda** | Buscador con historial y sub-pestañas (Canciones / Álbumes / Artistas). |
| **Edición de etiquetas ID3** | Título, artista, álbum y carátula; escritura con **jaudiotagger**. |
| **"Auto-web" de metadatos** | Busca carátula/metadata oficiales en la **API de iTunes** y limpia títulos tipo YouTube. |
| **Carátulas de artistas** | Fotos de artista desde la **API de Deezer**, cacheadas localmente. |
| **Listas de reproducción** | Crear, añadir/quitar canciones y elegir portada; persistidas localmente. |
| **Tema oscuro** | Material 3 con paleta oscura personalizada. |
| **Animación "ecualizador"** | Indicador animado en la pista/cola actualmente en reproducción. |

### 🟡 Parciales (UI presente, lógica incompleta)

- **Menú de opciones (⋮)** de canciones y listas: en su mayoría sin lógica.
- **Restaurar lista de reproducción**: botón sin lógica.

### 🔴 Pendientes (declaradas en el menú lateral, sin implementar)

- **Ecualizador**
- **Escanear archivos** (re-escaneo manual)
- **Buscar duplicados / audios de WhatsApp**
- **Cortador de audio**
- **Temporizador (Sleep Timer)**
- **Acerca de**
- **Enviar comentarios**

---

## 🧱 Stack tecnológico

| Componente | Versión / Herramienta |
|------------|-----------------------|
| Lenguaje | Kotlin `2.2.10` |
| UI | Jetpack Compose (BOM `2026.02.01`), Material 3 |
| Reproductor | AndroidX Media3 (`1.4.1`) — ExoPlayer + MediaSession |
| Imágenes | Coil `2.7.0` |
| Etiquetas ID3 | jaudiotagger `3.0.1` |
| Build | AGP `9.4.1`, Gradle `9.6.0`, Kotlin DSL |
| SDK | `compileSdk`/`targetSdk` 37 · `minSdk` 24 (Android 7.0) |
| Persistencia | `SharedPreferences` (sin base de datos) |
| Pruebas / CI | JUnit 4 (**24 pruebas unitarias**) + GitHub Actions |

---

## 🗂️ Estructura del proyecto

```
Dezznuts/
├── build.gradle.kts            # Build raíz
├── settings.gradle.kts         # Módulos y repositorios
├── gradle/libs.versions.toml   # Catálogo de dependencias
├── .github/workflows/android.yml # CI: pruebas unitarias + build del APK
└── app/
    ├── build.gradle.kts        # Módulo app (dependencias y SDK)
    ├── proguard-rules.pro      # Reglas de R8 (release)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/creproductor/
        │   │   ├── MainActivity.kt          # Entrada, permisos, scaffolding y mini-player
        │   │   ├── data/                    # Modelos y acceso a datos
        │   │   │   ├── Song.kt
        │   │   │   ├── Playlist.kt
        │   │   │   ├── RepeatMode.kt
        │   │   │   ├── PlaylistSerializer.kt  # Guarda/carga listas (campos codificados)
        │   │   │   ├── SongOrganizer.kt       # Agrupaciones y búsqueda (puras)
        │   │   │   ├── MediaStoreScanner.kt # Escaneo de audio local
        │   │   │   ├── AudioTagWriter.kt    # Escritura de etiquetas ID3
        │   │   │   ├── CoverFetcher.kt      # Carátulas/metadata (iTunes)
        │   │   │   ├── ArtistImageFetcher.kt# Fotos de artista (Deezer)
        │   │   │   └── YoutubeTitleCleaner.kt
        │   │   ├── service/
        │   │   │   └── MusicService.kt      # Servicio de reproducción + sesión
        │   │   ├── gui/                     # Pantallas y ViewModel (Compose)
        │   │   │   ├── MusicViewModel.kt    # Estado central de la app
        │   │   │   ├── MainScreen.kt        # Pestañas y navegación principal
        │   │   │   ├── FullScreenPlayer.kt  # Reproductor a pantalla completa
        │   │   │   ├── AppDrawerContent.kt  # Menú lateral
        │   │   │   ├── SearchScreen.kt
        │   │   │   ├── GridScreens.kt       # Álbumes, artistas, géneros, carpetas
        │   │   │   ├── QuickSelectionScreen.kt / QuickSelectionDetailScreen.kt
        │   │   │   ├── AlbumDetailScreen.kt / ArtistInterfaceScreen.kt / GenreDetailScreen.kt
        │   │   │   ├── PlaylistScreen.kt / PlaylistDetailScreen.kt
        │   │   │   ├── EditTagDialog.kt / CreatePlaylistDialog.kt / AddSongsToPlaylistDialog.kt
        │   │   │   └── ui/theme/            # Colores, tipografía y tema
        │   └── res/                    # Recursos (iconos, strings, xml de backup)
        └── test/java/com/example/creproductor/  # Pruebas unitarias (JUnit)
            ├── data/PlaylistSerializerTest.kt, SongOrganizerTest.kt, …
            └── DurationFormatTest.kt
```

📄 Para el detalle técnico completo (arquitectura, flujo de datos, bugs y hoja de ruta) consulta **[`RESUMEN_PROYECTO.md`](./RESUMEN_PROYECTO.md)**.

---

## 🔧 Requisitos y compilación

1. **Android Studio** (versión reciente, compatible con AGP 9.x) o el JDK 17+.
2. **Android SDK** con la API 37 instalada.
3. Clona y abre el proyecto, o compila por consola:

```bash
./gradlew assembleDebug     # genera el APK de depuración
./gradlew installDebug      # instala en un dispositivo/emulador conectado
```

El APK de depuración quedará en `app/build/outputs/apk/debug/`.

### Pruebas

```bash
./gradlew testDebugUnitTest   # pruebas unitarias (JUnit)
```

Cubren la lógica pura del proyecto: serialización de listas de reproducción, limpieza de títulos tipo YouTube, reparación de codificación de texto, agrupaciones/búsqueda y formato de duración. Además hay CI (`.github/workflows/android.yml`) que corre las pruebas y compila el APK en cada push/PR.

> No existe `local.properties` en el repo (está en `.gitignore`); Android Studio lo genera al abrir el proyecto.

---

## 🔐 Permisos

| Permiso | Para qué |
|---------|----------|
| `READ_MEDIA_AUDIO` (Android 13+) / `READ_EXTERNAL_STORAGE` (≤ Android 12) | Escanear la música local |
| `INTERNET` | Carátulas de artista (Deezer) y metadatos oficiales (iTunes) |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Reproducción en segundo plano |
| `POST_NOTIFICATIONS` | Notificación del reproductor |

---

## 🐞 Problemas conocidos

- Los **menús ⋮** de canciones y listas, y el botón **"Restaurar lista"**, siguen sin lógica.
- La persistencia sigue en `SharedPreferences` (migración a **Room/DataStore** pendiente); la serialización de listas ya es robusta frente a caracteres especiales.
- El campo **género** solo se lee en Android 11+ (`MediaStore.GENRE`); en versiones anteriores aparece como "Desconocido".
- La **edición de etiquetas** escribe directamente sobre el `contentUri` en modo `rwt`; en algunos dispositivos/rutas puede fallar por las restricciones de almacenamiento (SAF).
- El historial de búsqueda usa un `StringSet` (no garantiza el orden).
- Las **fotos de artista** se piden por cada fila; en listas grandes conviene cargarlas por lotes.

Detalles y sugerencias de arreglo en [`RESUMEN_PROYECTO.md`](./RESUMEN_PROYECTO.md).

---

## 🗺️ Hoja de ruta sugerida

> ✅ **Ya hecho:** compilación, shuffle real, favorito de la notificación, pestaña **Carpetas**, navegación de búsqueda y pruebas unitarias + CI.

1. Implementar **Escanear archivos** (re-escaneo manual).
2. Implementar **Ecualizador** y **Temporizador**.
3. Implementar **Cortador de audio** y **Buscar duplicados**.
4. Completar los **menús ⋮** y el botón **"Restaurar lista"**.
5. Migrar la persistencia a **Room / DataStore**.

---

## 🤝 Cómo contribuir

Al ser un proyecto abierto y "hecho por amor al arte", cualquier aporte suma:

1. Haz un *fork* y crea una rama (`feature/mi-mejora`).
2. Mantén el estilo del código existente (Compose idiomático, nombres en español donde ya se usan).
3. Abre un *Pull Request* describiendo el cambio.

¿No sabes por dónde empezar? Revisa la sección de **buenos primeros issues** en `RESUMEN_PROYECTO.md`.

---

## 📜 Licencia

**Por definir.** Se recomienda una licencia libre y compatible con el espíritu del proyecto (por ejemplo, **GPL-3.0**), para asegurar que DEZZNUTS y sus derivados permanezcan abiertos.

## 👤 Créditos

Desarrollado inicialmente por el autor del repositorio. Documentación generada como guía de colaboración.
