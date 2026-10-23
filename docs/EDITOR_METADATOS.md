# Editor de metadatos (rama `editor-metadatos`)

Permite corregir título, artista, álbum y carátula de una canción, y arreglar de golpe los "artista desconocido".

## Qué había y qué faltaba
El diálogo (`EditTagDialog`), la escritura (`AudioTagWriter`, con copia de seguridad y restauración) y la acción del `ViewModel` ya existían, pero **nada los abría**: el estado `songToEdit` nunca se asignaba y los botones ⋮ de las listas pasaban `{}`. Además, desde Android 11 escribir en un audio requiere el consentimiento del sistema.

## Qué se hizo
- **Botón ⋮ conectado** en Canciones, Álbumes, Artistas, Géneros, Carpetas/Selección y Búsqueda, y botón **Editar etiquetas** en la ficha técnica del reproductor.
- **Permiso de escritura** con `MediaStore.createWriteRequest` (Android 11+): el sistema pregunta "¿Permitir que DEZZNUTS modifique este archivo?" y solo entonces se escribe. Antes de Android 11 se escribe directamente.
- **Sugerencias para artista desconocido** (`TagSuggestions`, puro y probado): deduce "Artista - Título" del título o del nombre del archivo, quita números de pista (`01. `, `02 - `) sin confundir `7 Years` ni `21 Guns`, y limpia ruido de YouTube. Sin internet. El diálogo muestra un chip «Usar «Beck» — «Loser»».
- **Los marcadores no son datos:** `<unknown>` y "Artista desconocido" empiezan como campo vacío, y un campo vacío **no se escribe** en el archivo (antes se habría guardado el marcador).
- **Corregir artistas desconocidos** (Ajustes → Biblioteca): lista las canciones con sugerencia, marcadas por defecto; se aplican todas con **un solo** permiso del sistema. Las que no se pueden deducir se editan una a una.
- **Portada:** solo se reescribe si el usuario la cambió (búsqueda web "Auto-web" o galería, ya existentes).
- Recarga de la biblioteca silenciosa tras editar.
- Las pistas de una hoja CUE no se editan (comparten archivo con todo el álbum): se avisa.

## Comprobado en emulador (API 35)
- Archivos sin etiquetas (`Beck - Loser.mp3`, `01. Radiohead - Creep.mp3`, `Daft Punk - One More Time.mp3`).
- Edición individual: ⋮ → chip de sugerencia → Guardar → diálogo del sistema → el archivo queda con `title=Loser`, `artist=Beck` (verificado con `ffprobe`).
- Lote: "Corregir 2" → un único permiso para 2 archivos → MediaStore muestra `Creep / Radiohead` y `One More Time / Daft Punk`.

## Limitaciones
- Cambiar la **carátula** con búsqueda web usa iTunes (ya existente); MusicBrainz queda como opción futura.
- Cada escritura copia el archivo (lo exige jaudiotagger): corregir cientos de FLAC grandes tarda; se hace de uno en uno y se informa al terminar.
- La carátula nueva y la búsqueda web no se volvieron a probar en esta rama.
