# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/). Versionado semántico.

## [1.2.1] - 2026-10-26

### Correcciones
- **El teclado ya no se queda encima del reproductor**: al abrir la pantalla completa se suelta el foco de la búsqueda y se oculta el IME. Si minimizas el teclado con su botón y vuelves a abrir la canción, ya no aparece el cursor/flecha de la búsqueda sobre el menú del reproductor. También se suelta al elegir una canción de YT Music o al salir de la app al fondo.
- **YouTube Music al cerrar desde recientes**: la canción sigue sonando y se recupera al volver a la app, en vez de detener la reproducción; se reconstruye desde la sesión de medios.

## [1.2.0] - 2026-10-25

### Novedades
- **Unificación de interfaces** (rama `temas/interfaces` de Maikel).
  - **Color de acento a medida** sobre cualquier tema: colores rápidos y un selector con tono, intensidad y brillo.
  - **Selector de tema con vistas previas** en miniatura (barra, portada, lista y reproductor) en vez de círculos de color.
  - **Inicio de YouTube Music** en la pestaña YT: estados de ánimo y estantes (con tu cuenta es el personalizado);
    los álbumes y listas se abren en una hoja con sus canciones. Mismo cliente InnerTube que los «Me gusta».
- **Barra de navegación inferior por defecto** (interfaz de Maikel: YT Music · Biblioteca · Inicio · Buscar · Ajustes),
  bajo el mini-reproductor. Sin hamburguesa: todo el menú vive en **Ajustes**. Se puede volver a pestañas arriba con menú
  lateral en Ajustes → Apariencia → Navegación. Con la barra abajo, las pestañas de arriba son solo las de la biblioteca.
- **Pestaña YT Music rehecha** con el orden de Maikel: cabecera (lupa y sesión), estados de ánimo, Selección rápida con
  «Reproducir todo», Accesos directos y estantes.
- **Tipografía Silkscreen** de Maikel en el tema Omarchy (sustituye a Pixelify Sans).
- Código: lista de canciones y mini-reproductor salen de `MainActivity` a `gui/`.
- **Visualizador tipo CAVA** (Opciones, apagado de fábrica): barras de frecuencia que suben de golpe y caen con
  gravedad en la mitad baja del reproductor. Usa un procesador «espía» en la cadena de audio (la señal pasa idéntica):
  sin permisos y también con YouTube Music; no hay datos con audio offload.
- **Selección múltiple** en Canciones (pulsación larga): añadir a una lista, a la cola o eliminar de una vez.

### Correcciones
- **Moods**: las canciones sin género (descargas de Snaptube y similares) entran por palabras del título, álbum y
  carpeta y por el género de otras canciones del mismo artista; lo que no encaja va a «Otras».
- La barra inferior cierra los detalles abiertos; en horizontal se vuelve a pestañas arriba; el menú de una canción de
  YT ya no ofrece la cola local (no sonaba).
- **Fotos de artista de otro artista**: se tomaba el primer resultado de Deezer sin comprobar el nombre («Aurora Vega»
  mostraba a «aurorawave»); ahora solo vale un resultado con el mismo nombre (la caché antigua se descarta).
- Botones «Reproducir/Aleatorio» de álbum, artista, género y lista sin partirse en dos líneas; pestañas de la búsqueda
  con el color de la activa distinto al de las demás; etiquetas de Omarchy más pequeñas para que quepan.
- **Canciones duplicadas a 0:00** con archivos de descargadores (Snaptube y similares): se descartan las entradas rotas
  que tienen una gemela buena (misma ruta, o mismo título y artista).
- **«Añadir canciones» de una lista** no hacía nada desde el detalle de la lista.
- **«Reproducción reciente»** ya no incluye canciones de YouTube Music (es opcional y no es de la biblioteca).
- Las canciones de YouTube Music ya no ofrecen editar etiquetas, cortar, borrar, ir al álbum ni añadir a listas, ni
  muestran ruta/archivo ni el corazón de favoritos; la duración se toma del audio si la tarjeta no la trae.
- **Portadas HD y en movimiento**: una sola puerta a iTunes con caché y separación entre peticiones (antes la portada
  HD, la animada y el editor pedían lo mismo por separado); el servicio de portadas animadas respeta su límite, se
  espera a que la canción se quede un momento antes de preguntar y saltar de canción cancela lo pendiente.

## [1.1.0] - 2026-10-24

### Novedades
- **YouTube Music**: la portada es la oficial (cuadrada, de iTunes) en vez del fotograma del video, con lo que también
  funcionan la portada animada y el fondo «cristal»; abrir una canción tarda ≈ 1,2 s (antes ≈ 4,4 s la primera vez):
  se muestra al instante, la dirección de audio se guarda en caché y el primer resultado se prepara solo.
- **Sigue con las recomendadas de YouTube** al terminar una canción (opción, activada por defecto); el botón siguiente
  de la notificación también funciona.
- **Búsqueda**: debajo de lo local aparece «También en YouTube Music» (sin mover lo de arriba mientras carga).
- **Letra palabra por palabra** con halo luminoso (opción). El reparto es estimado: LRCLIB solo da el tiempo de cada línea.
- **4 temas nuevos** (Sakura, Océano, Synthwave y Brasa) y selector por muestras de color en el menú lateral y en Opciones.
- **Portada animada** más acertada: prueba el álbum sin sufijos de edición («Remastered», «Deluxe», «- Single») y los
  álbumes en los que iTunes tiene la canción; los fallos del servicio ya no se guardan como «no hay».
- **Inicio de sesión** dentro de la ventana de la app (ya no es una ventana aparte) y con la página precargada.
- Pulido visual: logotipo tipográfico, pestañas con el acento solo en la activa, menú lateral más sobrio y sin textos
  cortados, cuadrículas de 2 columnas en teléfonos pequeños, «Ver todo» en vez de mayúsculas, textos de Opciones más cortos.

### Correcciones
- Las canciones de YouTube Music en «Reproducción reciente» (y en Más reproducidas o la cola) ya se reproducen.
- Una canción de YT ya no pisa la última canción local ni su posición al reabrir la app.
- Búsqueda: sin indicador eterno si no hay red; la precarga de YT no se pide dos veces.
- Letra: sin fotogramas innecesarios en pausa y barrido más fluido.

## [1.0.0] - 2026-10-24

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
