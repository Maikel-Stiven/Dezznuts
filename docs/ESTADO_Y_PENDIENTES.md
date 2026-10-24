# Estado del trabajo y por dónde seguir

Foto del **2026-10-24**, versión **1.0.0**, todo en `main`.

## Reglas que fijó el usuario (no romperlas)
- **Identidad visual = la maqueta de Canva** ([enlace](https://canva.link/92ckehs775shns8), diseño `DAHW1CovPP0`):
  barra superior con ☰ · DEZZNUTS · lupa · ⋮, pestañas deslizables arriba, menú hamburguesa, morado `#BB86FC` y
  turquesa `#03DAC6` sobre `#121212`, tipografía y formas estándar de Material. El dorado/caramelo, la barra inferior
  y Manrope se retiraron porque se salían de la maqueta. Cualquier cambio visual nuevo: **comparar con la maqueta antes**.
- Todo lo nuevo (portadas, YouTube Music, temas) es **opcional y viene apagado**.
- Flujo de ramas: rama de trabajo → `optimizacion` → `main`. Commits sin mención a IA, a nombre del usuario.
- No publicar releases ni etiquetas sin que el usuario lo pida (ver «GitHub Actions» más abajo).
- **Probar siempre la versión *release*** (R8) en el emulador antes de entregar un APK: el widget ya hizo crashear una.

## Hecho
- **Maqueta restaurada**: pestañas Selección rápida · Canciones · Álbumes · Artistas · Géneros · Listas · Carpetas ·
  **YT Music**; menú lateral con las acciones; el menú ⋮ abre Escanear, Temporizador, Opciones y Acerca de.
- **Opciones** (pantalla completa, por secciones): Apariencia (tema DEZZNUTS/AMOLED/claro/Material You, color de la
  carátula, portada con movimiento), En línea (portada animada, YouTube Music y su cuenta), y las acciones de audio,
  biblioteca, herramientas, copias, almacenamiento e información.
- **Portada con movimiento** (acelerómetro) y **portada animada** (bucle de video por `artwork.boidu.dev`, solo Wi-Fi).
- **YouTube Music** (NewPipeExtractor + InnerTube): buscar y reproducir; inicio de sesión por WebView y lista de «Me
  gusta» (`VLLM`); sesión guardada en `yt_auth.xml`, excluida de las copias de seguridad. Pestaña propia con buscador.
- **Letras**: línea activa animada; las canciones de YouTube Music buscan la letra solas (LRCLIB devolvía 503 con el
  álbum falso «YouTube Music» y se tomaba por «sin conexión»; ahora el atajo exacto es opcional y se elige la versión
  sincronizada de duración más parecida).
- **Sistema**: reanudar desde Bluetooth (`onPlaybackResumption`), «reproducir a continuación» respeta el aleatorio,
  Android Auto (`MediaLibraryService`, árbol Recientes/Favoritas/Listas/Todas), widget (Glance), estadísticas.
- **Pantallas**: botón de escanear sobre la barra de gestos (reporte de Xiaomi), barra/etiquetas con texto grande,
  reproductor con portada que se adapta, riel lateral en teléfonos en horizontal, cuadrículas adaptables.
- **Textos en español e inglés**: `tr("texto")` + `scripts/gen_strings.py` (regenera `values/`, `values-en/` y
  `res/raw/keep.xml`). Los textos con valores dentro (`"$n canciones"`) y los párrafos del Audio Hi/Res siguen en español.

## Medidas (emulador de 2 núcleos y 2 GB, versión release)
- Arranque en frío ≈ 430–520 ms. En reposo ≈ 66 MB de RAM (PSS); con el reproductor sonando ≈ 89 MB.
- Reproductor abierto sonando ≈ 3–5 % de CPU. La portada animada y la inclinación se apagan solas en equipos de ≤ 2
  núcleos, poca RAM o ahorro de batería (`rememberVisualEffectsEnabled()`).
- Probado y **descartado por no mejorar**: iniciar WorkManager bajo demanda (sin diferencia medible en el arranque).

## Sin probar en un teléfono real (hacerlo primero)
Widget colocado en la pantalla de inicio · Android Auto / árbol de navegación · inicio de sesión y «Me gusta» de
YouTube Music con una cuenta real · sentido natural de la inclinación de la portada · Xiaomi (botón de escanear).

## Estado 1.0.0 (2026-10-24)
Todo lo anterior está fusionado en `main` y publicado como **v1.0.0** (CI: `android.yml`; release: `release.yml` al subir
una etiqueta `v*`, con firma propia si existen los secrets `KEYSTORE_B64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
Temas: DEZZNUTS (por defecto), Omarchy, Premium, AMOLED, claro y Material You (`ui/theme/Theme.kt` y `Type.kt`).

## Pendiente
1. Probar en teléfono real (widget, Android Auto, login de YT Music) y corregir lo que salga.
2. Firma propia (keystore de la organización con copia de seguridad) para poder actualizar sin desinstalar.
3. Lentitud al abrir una canción recién detectada (medir `loadSongs(silent)` → `publishSongs` antes de tocar).
4. Textos con valores (`"$n canciones"`) y párrafos de Audio Hi/Res a `strings.xml`.

## Cómo compilar y probar
```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk
./gradlew assembleDebug testDebugUnitTest --offline      # (la primera vez tras añadir dependencias, sin --offline)
./gradlew assembleRelease --offline                       # APK en app/build/outputs/apk/release/
python3 scripts/gen_strings.py                            # tras añadir textos nuevos con tr("...")
```
- Emulador `dezznuts_test` (API 35); gama baja: `-cores 2 -memory 2048`. `adb shell wm size 720x1560 && wm density 320`
  para capturas; `wm size reset` al terminar.
- Idioma de la app en el emulador: `adb shell cmd locale set-app-locales io.github.maikelstiven.dezznuts --locales en`.
- Trampas: en zsh no uses `path` como nombre de variable (es `PATH`); `run-as` no funciona con la release; `lint`
  necesita red por las dependencias de las pruebas instrumentadas.
