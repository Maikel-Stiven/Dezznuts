# Hoja de ruta: de reproductor alfa a reproductor premium

Este documento recoge el plan acordado el 2026-10-21 para que DEZZNUTS sea "el reproductor open source que vale la
pena tener en el celular", lo que ya está hecho y **por dónde seguir**. Cada etapa se trabaja en su rama, pasa por
`optimizacion` y después a `main`.

> **Actualización 2026-10-24.** La navegación y el estilo vuelven a la maqueta de Canva: pestañas arriba, menú
> hamburguesa, morado `#BB86FC` sobre `#121212`, tipografía y formas estándar (la barra inferior, la pantalla de
> Ajustes, Manrope y el tono caramelo de la etapa 2 se retiraron). Hecho desde entonces, todo opcional y apagado por
> defecto: portada con movimiento y animada, color de la carátula, temas AMOLED/claro/Material You, YouTube Music
> (búsqueda, reproducción y "Me gusta" con sesión), reanudar desde Bluetooth, aleatorio con "a continuación",
> Android Auto (árbol Recientes/Favoritas/Listas/Todas), widget, letras animadas y estadísticas. Pendiente: pasar los
> textos a `strings.xml` e inglés; probar en teléfonos reales (widget, Android Auto y la sesión de YouTube Music no se
> probaron en un equipo real).

## Estado de las etapas

| # | Etapa | Estado | Commit |
|---|-------|--------|--------|
| 1 | Arreglos rápidos | ✅ Hecha | `1f166f4` |
| 2 | Identidad visual | ✅ Hecha | `bfb6ee3` |
| 3 | Estructura y navegación | ✅ Hecha | `c23bbcb` |
| 4 | Datos y copias de seguridad | ✅ Hecha | `95e40df` |
| — | `applicationId` propio | ✅ Hecho | `e94c72b` |
| 5 | Integración con el sistema | ⏳ Siguiente | — |
| 6 | Lo que enamora | ⏳ Pendiente | — |

Todo lo anterior está en la rama `arreglos-rapidos`, fusionada en `optimizacion`.

---

## ✅ Lo hecho

### 1. Arreglos rápidos
- **Sonaba la canción equivocada**: `playSong` reutilizaba la cola cargada si tenía el mismo *tamaño*; ahora compara
  las pistas una a una. Tocar una canción con la música en pausa ahora la reproduce.
- El **aleatorio de un álbum** empezaba siempre por la pista 1.
- **Destello blanco al abrir**: el tema de la ventana era claro (`Theme.Material.Light`); ahora es oscuro.
- **Reproducir a continuación** en el menú ⋮ (la opción anterior pasó a llamarse "Agregar al final de la cola").
- **Ordenar** la pestaña Canciones por título, artista, álbum, duración o recién agregadas (se recuerda; compara sin
  distinguir mayúsculas ni tildes con `Collator`).

### 2. Identidad visual
- Tema propio en `ui/theme/`: negro cálido + caramelo (la bellota) con verde hoja de acento, escala tipográfica
  completa con **Manrope** (OFL, fuente variable en `res/font/`) y 4 radios (`MaterialTheme.shapes`) en lugar de 11.
- **Color de la carátula** (`data/CoverAccent.kt`, con pruebas): el tono que más pesa de la portada a 24×24, llevado a
  un brillo legible. `CoverAccentTheme` lo aplica al reproductor y al mini-reproductor; sin carátula o si es gris se
  queda el caramelo. La transición solo se anima donde `rememberVisualEffectsEnabled()` lo permite.
- Reproductor rediseñado: portada → título/favorito → progreso → controles → ecualizador/cola. Entra y sale
  deslizándose y se cierra arrastrando hacia abajo.
- Mini-reproductor flotante: deslizar arriba abre, a los lados cambia de canción.

### 3. Estructura y navegación
- **Barra inferior** Inicio · Biblioteca · Buscar · Ajustes; el menú lateral desapareció.
- **Pila de detalles** en `MainScreen` (`Detail`): Artista → Álbum → atrás vuelve al artista. En la raíz de una
  sección, atrás vuelve a Inicio. Sin `navigation-compose`: una lista basta y no añade dependencias.
- **Ajustes** (`gui/SettingsScreen.kt`) agrupa por Audio, Biblioteca, Herramientas, Copia de seguridad,
  Almacenamiento e Información. Las acciones (`AppAction`) las ejecuta `MainActivity`, que es quien abre los diálogos.
- Estados de permiso, carga y biblioteca vacía con la bellota (`gui/StatusScreen.kt`).
- Buscar: foco automático, barra en forma de píldora y una pista cuando no hay historial.

### 4. Datos y copias de seguridad
- **Copia de seguridad** (`data/LibraryBackup.kt`): favoritas, reproducciones y listas en texto plano. Guarda
  **rutas**, no ids de MediaStore, y si la ruta cambió busca por nombre de archivo (`SongMatcher`). Al restaurar
  suma: favoritas unidas, reproducciones al máximo, listas fusionadas por nombre.
- **M3U**: exportar una lista (opciones de la lista) e importar desde Ajustes.

### `applicationId`
- Pasó de `com.example.creproductor` a `io.github.maikelstiven.dezznuts` (F-Droid y Play rechazan `com.example`). El
  paquete del código no cambió. Android la trata como otra app: la versión anterior queda instalada aparte con sus
  datos.

### Decisiones tomadas (y por qué)
- **Sin Room por ahora.** Con las bibliotecas habituales, SharedPreferences rinde bien; el riesgo real era perder
  datos al reinstalar, y eso lo resuelve la copia por rutas. Room vuelve a la mesa si aparecen bibliotecas de decenas
  de miles de canciones o consultas que hoy no existen.
- **Sin saltar silencios ni velocidad.** `HiFiRenderersFactory` usa una cadena con un único procesador (el
  ecualizador en float) porque Sonic y el silenciador solo admiten 16 bits. Añadirlos rompería la ruta de alta
  fidelidad, que es la seña de la app.
- **Se quitó el panel de cristal** del reproductor (el fondo animado con la carátula se mantiene) para seguir el
  orden habitual portada → título → progreso → controles.

---

## ⏳ Lo que sigue

### Etapa 5: integración con el sistema
1. **Widget de pantalla de inicio** (Glance): portada, título y play/pausa/siguiente.
2. **Reanudar desde Bluetooth o el panel de medios** del sistema: `onPlaybackResumption` en `MusicService`
   (hoy solo se restaura la cola al abrir la app).
3. **Android Auto / Wear / asistente**: pasar de `MediaSessionService` a `MediaLibraryService` con un árbol de
   navegación (Recientes, Listas, Álbumes, Artistas).
4. **Tablets, plegables y horizontal**: `WindowSizeClass`; en pantallas anchas, lista y reproductor lado a lado y
   rejillas con más columnas.
5. **"Reproducir a continuación" con aleatorio**: un `ShuffleOrder` propio en el servicio que coloque la pista
   justo después de la actual.

### Etapa 6: lo que enamora
1. **Letras animadas**: resaltar la línea activa con desplazamiento suave.
2. **Estadísticas / "tu año"**: ya se guardan las reproducciones; falta la pantalla (top canciones, artistas,
   horas escuchadas).
3. **Temas**: claro, negro AMOLED y Material You opcional (`dynamicDarkColorScheme`) en Ajustes → Apariencia.
4. **Traducciones**: pasar los textos de la interfaz a `strings.xml` (hoy están en el código) y añadir inglés.
5. **Accesibilidad**: revisar los ~60 iconos con `contentDescription = null` y los tamaños táctiles.

### Pendientes anteriores que siguen abiertos
- **Lentitud al abrir una canción recién detectada**: medir antes de tocar; sospecha en la recarga silenciosa de
  MediaStore (`loadSongs(silent)` → `publishSongs`) seguida de `playSong`.
- **Editor de etiquetas**: ya está conectado (menú ⋮ → Editor de etiquetas, con `createWriteRequest` en Android 11+);
  falta confirmarlo en teléfonos reales con archivos de otras apps.
- **Ecualizador**: Surround/Virtualizer y Amplificador (el motor actual no los tiene).
- **YouTube Music (InnerTube)**: en rama aparte, con la cuenta del usuario.
- **Probar en teléfonos reales**: todo lo de esta hoja se verificó en el emulador `dezznuts_test` (API 35).

---

## Cómo retomar

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew assembleDebug testDebugUnitTest --offline   # compila y corre las 105 pruebas
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm grant io.github.maikelstiven.dezznuts android.permission.READ_MEDIA_AUDIO
```

- Nueva rama desde `optimizacion` para cada etapa; commit por hito; fusionar en `optimizacion` y después a `main`.
- Rendimiento: medir antes/después en el emulador con 2 núcleos y 2 GB (`-cores 2 -memory 2048`) y revertir lo que
  no mejore (ver [`OPTIMIZACION.md`](OPTIMIZACION.md)).
