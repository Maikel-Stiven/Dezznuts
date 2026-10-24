
Primera versión estable: DEZZNUTS sale de alfa y beta.

### Novedades
- **Temas a elegir** (Opciones → Apariencia): **DEZZNUTS** (morado `#BB86FC`, el original y por defecto),
  **Omarchy** (verde terminal con tipografía pixel y esquinas rectas), **Premium** (caramelo con Manrope),
  **Negro AMOLED**, **Claro** y **Material You**.
- **Interfaz de la maqueta**: ☰ · lupa · ⋮, pestañas deslizables (Selección rápida, Canciones, Álbumes, Artistas,
  Géneros, Listas, Carpetas y YT Music) y menú lateral con todas las acciones.
- **Reproductor** a pantalla completa con pestañas Canción/Letra, fondo «cristal» con la carátula, calidad
  (Lossless/Hi-Res) sobre la portada, color de la carátula (opcional) y portada con movimiento (opcional).
- **Letras sincronizadas** (archivo `.lrc` y LRCLIB) con línea activa animada.
- **YouTube Music** (opcional, apagado por defecto): buscar y reproducir, inicio de sesión y lista de «Me gusta».
- **Portada animada** (opcional, solo Wi-Fi): bucle de video del álbum.
- **Android Auto y asistentes** (árbol Recientes/Favoritas/Listas/Todas), **widget** de inicio, reanudar la última
  cola desde Bluetooth y «reproducir a continuación» que respeta el aleatorio.
- **Estadísticas** de escucha (reproducciones, horas, lo más escuchado).
- **Editor de etiquetas** (título, artista, álbum y carátula) con permiso de escritura del sistema y corrección en
  lote de «artista desconocido».
- **Listas**: portada con recorte 1:1, editar, reordenar en cuadrícula, submenús por canción, búsqueda y filtros,
  restaurar borradas, importar/exportar M3U.
- **Herramientas**: cortador de audio, buscador de duplicados y audios de WhatsApp, escaneo de carpetas, temporizador.
- **Copia de seguridad** por rutas (sobrevive a reinstalar o cambiar de teléfono).
- **Español e inglés** (textos en `strings.xml`).

### Audio
- Calidad Hi-Res/Lossless, ficha técnica, hojas CUE, ReplayGain (pista/álbum) y ecualizador paramétrico propio
  con presets, curva y perfiles por salida.
- Salida de 32 bits (float), audio offload, DAC USB y **bit-perfect** en Android 14+.
- Fundidos de entrada y salida; seek más rápido en FLAC y reproducción por ruta directa.

### Rendimiento
- Pensada para equipos de 2 núcleos y 2–4 GB: las animaciones se apagan solas en equipos modestos o con ahorro de
  batería. En emulador de 2 núcleos: arranque ≈ 0,5 s, reproductor abierto 76 % → ≈ 5 % de CPU, lista 30 % → ≈ 4 %,
  ≈ 66 MB de RAM en reposo. Carga de biblioteca por tramos, posición de reproducción sondeada solo si se ve.
  Detalle en [`docs/OPTIMIZACION.md`](docs/OPTIMIZACION.md).

### Correcciones
- Abrir la app desde la notificación, carátulas donde MediaStore no las indexa, `skipNBytes` en FLAC antes de
  Android 12, cola y aleatorio de álbum, botón de escanear sobre la barra de gestos (Xiaomi), reglas de R8 para el
  widget y contador real de reproducciones.

### Notas
- La firma por defecto del APK es la de depuración; si instalas una versión firmada con otra clave, desinstala antes.
- `applicationId`: `io.github.maikelstiven.dezznuts`.
- Sin probar aún en equipo real: widget colocado, Android Auto real y el inicio de sesión de YouTube Music.

Gracias a quienes lo hicieron posible:
- **@Pedroj-64**: diseño, interfaz y temas, reproductor, letras, YouTube Music, Android Auto, widget, estadísticas, textos en inglés, optimización y esta release.
- **@Maikel-Stiven**: menús y listas de reproducción (editar, reordenar en cuadrícula, submenús), persistencia de la cola y arreglos de conexiones.
- **@AiiwA** (Brandon Gil): audio de alta fidelidad, rendimiento del *seek*, carátulas, pruebas y CI.
