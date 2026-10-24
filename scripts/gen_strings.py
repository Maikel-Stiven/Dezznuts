#!/usr/bin/env python3
"""Genera res/values/strings.xml (español) y res/values-en/strings.xml (inglés) y res/raw/keep.xml.

El código usa tr("texto en español"); el nombre del recurso es s_<sha1[:10]> (ver i18n/I18n.kt). Para añadir un idioma:
copia EN a otro diccionario y pasa su carpeta (p. ej. values-pt). Los textos sin entrada en el diccionario se
quedan en español. Uso: python3 scripts/gen_strings.py
"""
import hashlib, os, re

ROOT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")

EN = {
"Abrir menú": "Open menu", "Acerca de": "About", "Activar aleatorio": "Turn shuffle on", "Activo": "On",
"Agregada a la cola": "Added to the queue", "Agregada a la lista": "Added to the list",
"Agregar a la cola de reproducción actual": "Add to the current queue", "Agregar a una playlist": "Add to a playlist",
"Agregar al final de la cola": "Add to the end of the queue", "Agregar pistas": "Add tracks",
"Al terminar la canción": "At the end of the song", "Aleatorio": "Shuffle", "ALEATORIO": "SHUFFLE", "Alegre": "Happy",
"Almacenamiento interno": "Internal storage", "Ancho de banda (Q)": "Bandwidth (Q)", "Anterior": "Previous",
"Apagado · señal sin tocar": "Off · signal untouched", "Archivo": "File",
"Arrastra los puntos sobre la curva. Sin efecto con audio offload.": "Drag the points on the curve. No effect with audio offload.",
"Arrastra y pellizca para encuadrar (1:1)": "Drag and pinch to frame (1:1)", "Artista": "Artist",
"Artista desconocido": "Unknown artist", "Artistas": "Artists", "Artistas más escuchados": "Most played artists",
"Audio Hi/Res": "Hi-Res audio", "Avanzado": "Advanced", "Añadir a favoritos": "Add to favorites",
"Añadir canciones": "Add songs", "Añadir canciones +": "Add songs +",
"Aún no has creado una lista de reproducción": "You haven't created a playlist yet",
"Aún no hay canciones escuchadas recientemente": "No songs played recently yet",
"Aún no hay canciones que escuches frecuentemente": "No frequently played songs yet",
"Aún no hay reproducciones. Escucha algo de tu biblioteca y vuelve.": "No plays yet. Listen to something from your library and come back.",
"Aún no tienes canciones con Me gusta.": "You don't have any liked songs yet.",
"Aún no tienes canciones favoritas": "You don't have favorite songs yet", "Banda": "Band",
"Bandas, presets y perfiles por salida": "Bands, presets and per-output profiles", "Biblioteca": "Library",
"Borrando…": "Deleting…", "Borrar": "Delete", "Borrar caché de imágenes": "Clear image cache",
"Bucle de video del álbum cuando existe, solo con Wi-Fi. Consulta artwork.boidu.dev (servicio comunitario, no oficial) con título, artista y álbum":
  "Album video loop when one exists, on Wi-Fi only. Queries artwork.boidu.dev (community service, unofficial) with title, artist and album",
"Busca en toda tu biblioteca por título, artista o álbum.": "Search your whole library by title, artist or album.",
"Buscando en LRCLIB…": "Searching LRCLIB…", "Buscando tu música…": "Looking for your music…", "Buscando…": "Searching…",
"Buscar": "Search", "Buscar de nuevo": "Search again", "Buscar en internet (LRCLIB)": "Search online (LRCLIB)",
"Buscar música nueva y filtrar audios cortos": "Find new music and filter out short audio",
"Buscar y reproducir en YouTube Music": "Search and play on YouTube Music", "CANCELAR": "CANCEL",
"Caché de imágenes borrada": "Image cache cleared", "Calculando…": "Calculating…", "Calidad": "Quality",
"Calidad Hi-Res": "Hi-Res quality", "Calidad sin pérdida": "Lossless quality", "Cancelar": "Cancel",
"Cancelar temporizador": "Cancel timer", "Canciones": "Songs", "Canciones añadidas a la playlist": "Songs added to the playlist",
"Canciones más escuchadas": "Most played songs", "Canciones, álbumes o artistas": "Songs, albums or artists",
"Canción": "Song", "Cargando…": "Loading…", "Carpetas": "Folders",
"Carátulas y fotos de artistas descargadas": "Downloaded covers and artist photos", "Cerrar": "Close",
"Cerrar Cola": "Close queue", "Cerrar reproductor": "Close player", "Cerrar sesión": "Sign out", "Claro": "Light",
"Cola de reproducción": "Play queue", "Color de la carátula en el reproductor": "Cover color in the player",
"Con pérdida": "Lossy", "Conservar": "Keep", "Copia": "Copy",
"Copia archivos de audio a las carpetas Música o Descargas y vuelve a buscar.": "Copy audio files to the Music or Downloads folders and search again.",
"Copia de seguridad": "Backup", "Corregir artistas desconocidos": "Fix unknown artists", "Cortador de audio": "Audio cutter",
"Cortando…": "Cutting…", "Crear lista": "Create list", "Crear lista de reproducción": "Create playlist",
"Curva del ecualizador: arrastra los puntos para cambiar frecuencia y ganancia": "Equalizer curve: drag the points to change frequency and gain",
"DEZZNUTS necesita permiso para leer los archivos de audio de este teléfono. No sube nada a internet.":
  "DEZZNUTS needs permission to read the audio files on this phone. It uploads nothing to the internet.",
"Deducir artista y título del nombre del archivo": "Work out artist and title from the file name",
"Desactivar aleatorio": "Turn shuffle off", "Descargas": "Downloads", "Detener": "Stop",
"Detener playlist al finalizar canción": "Stop playlist when the song ends", "Duplicados": "Duplicates",
"Duplicados eliminados": "Duplicates removed", "Duplicados y WhatsApp": "Duplicates and WhatsApp",
"Duplicados y audios de WhatsApp": "Duplicates and WhatsApp audio", "Duración": "Duration", "Dónde buscar": "Where to search",
"Ecualizador": "Equalizer", "Ecualizador…": "Equalizer…", "Editar etiquetas": "Edit tags",
"Editar etiquetas de audio": "Edit audio tags", "Editar foto": "Edit photo", "Editar portada": "Edit cover",
"Editor de etiquetas": "Tag editor", "Elegir carpeta…": "Choose folder…",
"Elige una canción desde su menú ⋮ → Cortador de audio": "Pick a song from its ⋮ menu → Audio cutter",
"Eliminar": "Delete", "Eliminar del dispositivo": "Delete from device", "Eliminar del historial": "Remove from history",
"Eliminar duplicados de la playlist": "Remove duplicates from the playlist", "Eliminar lista": "Delete list",
"Eliminar playlist": "Delete playlist", "Elimnar canciones": "Delete songs",
"En Android 10 o anterior, borra los archivos desde el gestor de archivos.": "On Android 10 or older, delete the files from the file manager.",
"Encontrar copias para liberar espacio": "Find copies to free up space", "Energía": "Energy", "Enfoque": "Focus",
"Enviar comentarios": "Send feedback", "Escanear archivos": "Scan files", "Escanear carpetas": "Scan folders",
"Escaneo terminado": "Scan finished", "Escuchar tramo": "Preview section",
"Ese archivo no es una copia de DEZZNUTS": "That file is not a DEZZNUTS backup", "Estadísticas": "Statistics",
"Exportar M3U": "Export M3U", "Favoritas": "Favorites", "Favoritas, reproducciones y listas en un archivo": "Favorites, plays and playlists in one file",
"Favoritos": "Favorites", "Favoritos ❤": "Favorites ❤", "Fiesta": "Party", "Frecuencia": "Frequency",
"Función no oficial (NewPipeExtractor): puede dejar de funcionar sin aviso y va contra los términos de YouTube. Usa internet y no necesita cuenta":
  "Unofficial feature (NewPipeExtractor): it may stop working without notice and goes against YouTube's terms. Uses the internet and needs no account",
"Función no oficial: puede dejar de funcionar sin aviso y va contra los términos de YouTube.":
  "Unofficial feature: it may stop working without notice and goes against YouTube's terms.",
"Fundido de entrada y salida": "Fade in and out", "Galeria": "Gallery", "Ganancia": "Gain", "Guardar": "Save",
"Guardar +": "Save +", "Guardar copia de seguridad": "Save backup", "Guardar corte": "Save cut",
"Guardar playlist en el dispositivo": "Save playlist to device", "Guardar preset": "Save preset", "Géneros": "Genres",
"Herramientas": "Tools", "INICIAR ESCANEO": "START SCAN", "Ignorar archivos de menos de (KB)": "Ignore files smaller than (KB)",
"Ignorar archivos de menos de (segundos)": "Ignore files shorter than (seconds)",
"Ignorar archivos y carpetas ocultos": "Ignore hidden files and folders", "Importar lista M3U": "Import M3U playlist",
"Información técnica": "Technical info",
"Inicia sesión para ver las canciones que te gustan. La sesión se guarda solo en este teléfono y puedes cerrarla cuando quieras.":
  "Sign in to see the songs you like. The session is stored only on this phone and you can sign out whenever you want.",
"Iniciar sesión": "Sign in", "Iniciar sesión y ver tus canciones con Me gusta": "Sign in and see your liked songs",
"Ir al artista": "Go to artist", "Ir al álbum": "Go to album", "LISTO": "DONE",
"LRCLIB no tiene la letra de esta canción.": "LRCLIB doesn't have the lyrics for this song.",
"La portada se inclina con el teléfono (se desactiva sola en equipos modestos o con ahorro de batería)":
  "The cover tilts with the phone (turns itself off on modest devices or in battery saver)",
"La primera vez puede tardar unos segundos si tienes muchas canciones.": "The first time it can take a few seconds if you have many songs.",
"Las pistas de una hoja CUE no se pueden cortar": "Tracks from a CUE sheet can't be cut", "Letra": "Lyrics",
"Limpiar": "Clear", "Limpiar busqueda": "Clear search", "Lista agregada a la cola": "List added to the queue",
"Lista importada": "Imported list", "Listas de reproducción": "Playlists", "Listo": "Done",
"Lo más escuchado y las horas de música": "Most played and hours of music",
"Los botones y el progreso toman el tono de la portada. Apagado: siempre el morado de DEZZNUTS":
  "Buttons and progress take the cover's color. Off: always DEZZNUTS purple",
"Material You": "Material You", "Melancólico": "Melancholic", "Mi YouTube Music": "My YouTube Music",
"Mueve la curva para guardar el tuyo": "Move the curve to save your own", "Más": "More", "Más opciones": "More options",
"Más reproducidas": "Most played", "Negro AMOLED": "AMOLED black", "No": "No", "No encontramos música": "We couldn't find any music",
"No hay audios de WhatsApp.": "No WhatsApp audio.", "No hay canciones agregadas recientemente": "No recently added songs",
"No hay canciones con artista desconocido que se puedan deducir del nombre del archivo.":
  "There are no songs with an unknown artist that can be worked out from the file name.",
"No hay canciones que coincidan": "No matching songs", "No hay canciones repetidas.": "No duplicate songs.",
"No hay imágenes en caché.": "No cached images.", "No hay letra guardada para esta canción.": "No lyrics saved for this song.",
"No hay listas borradas para restaurar.": "No deleted playlists to restore.",
"No hay navegador para abrir el enlace": "No browser to open the link",
"No hay otras listas de reproducción creadas": "There are no other playlists", "No se encontraron artistas": "No artists found",
"No se encontraron carpetas": "No folders found", "No se encontraron géneros": "No genres found",
"No se encontró en tu biblioteca ninguna canción de esa lista": "None of that list's songs were found in your library",
"No se pudieron guardar los tags": "Couldn't save the tags", "No se pudo abrir la canción de YouTube Music": "Couldn't open the YouTube Music song",
"No se pudo cargar. Revisa la conexión o inicia sesión de nuevo.": "Couldn't load. Check your connection or sign in again.",
"No se pudo consultar YouTube Music. Revisa la conexión o inténtalo más tarde.": "Couldn't reach YouTube Music. Check your connection or try later.",
"No se pudo eliminar": "Couldn't delete", "No se pudo guardar la copia": "Couldn't save the backup", "Nombre": "Name",
"Nombre de la lista nueva:": "New list name:", "Opciones": "Options", "Opciones de pista": "Track options", "Pausar": "Pause",
"Pausar la música pasado un tiempo": "Pause the music after a while", "Permitir acceso": "Allow access",
"Personalizado": "Custom", "Portada": "Cover", "Portada animada (en línea)": "Animated cover (online)",
"Portada con movimiento": "Moving cover", "Portada con movimiento, color de la carátula, funciones en línea": "Moving cover, cover color, online features",
"Portada de la playlist": "Playlist cover", "Quitar": "Remove", "Quitar de favoritos": "Remove from favorites",
"Quitar de la cola": "Remove from queue", "REPRODUCIENDO DESDE": "PLAYING FROM", "REPRODUCIR": "PLAY", "Recientes": "Recent",
"Recién agregadas": "Recently added", "Recortar la canción que suena (tonos, fragmentos)": "Trim the playing song (ringtones, clips)",
"Recuperarlas al reinstalar o en otro teléfono": "Get them back after reinstalling or on another phone", "Relax": "Relax",
"Reordenar pista": "Reorder track", "Repetición desactivada": "Repeat off", "Repetir canción": "Repeat song",
"Repetir cola de reproducción": "Repeat queue", "Reportar un fallo o pedir una función en GitHub": "Report a bug or request a feature on GitHub",
"Reproducción reciente": "Recently played", "Reproducidas recientemente": "Recently played", "Reproduciendo": "Playing",
"Reproducir": "Play", "Reproducir a continuación": "Play next",
"Reproductor de música local pensado para sonar bien y gastar poco, también en teléfonos modestos.":
  "A local music player built to sound good and use little, even on modest phones.",
"Restablecer": "Reset", "Restaurar": "Restore", "Restaurar copia": "Restore backup",
"Restaurar lista de reproducción": "Restore playlist", "Restaurar listas de reproducción": "Restore playlists", "Ruta": "Path",
"Salida float, offload, bit-perfect, ReplayGain y fundidos": "Float output, offload, bit-perfect, ReplayGain and fades",
"Se borrarán las fotos de artistas descargadas y las carátulas extraídas de los archivos. Se volverán a generar cuando hagan falta.":
  "Downloaded artist photos and covers extracted from files will be deleted. They will be regenerated when needed.",
"Se detendrá al terminar la canción": "It will stop when the song ends",
"Se enviará a lrclib.net el título, artista, álbum y duración. Se guarda para usarla sin conexión.":
  "The title, artist, album and duration will be sent to lrclib.net. It is saved to use offline.",
"Se guarda como archivo nuevo en Música/DEZZNUTS; el original no se modifica.": "Saved as a new file in Music/DEZZNUTS; the original is not modified.",
"Selección rápida": "Quick picks", "Siguiente": "Next", "Sin canciones": "No songs",
"Sin conexión. Inténtalo de nuevo con internet.": "No connection. Try again with internet.",
"Sin permiso no se puede guardar el corte": "The cut can't be saved without permission", "Sin resultados.": "No results.",
"Suena a continuación": "Plays next", "Sí": "Yes", "Tarjeta SD": "SD card", "Tema": "Theme", "Temporizador": "Timer",
"Todas": "All", "Traer una lista de otro reproductor": "Bring in a playlist from another player",
"Tu música, sin anuncios ni suscripciones": "Your music, no ads, no subscriptions", "Título": "Title", "VER TODOS": "SEE ALL",
"Ver más": "See more", "Versión, licencia y créditos": "Version, license and credits", "Volver": "Back",
"cerrar búsqueda": "close search", "crear": "create", "no se encontraron álbumes": "no albums found", "opciones": "options",
"volver": "back", "¿Eliminar del dispositivo?": "Delete from device?", "¿Eliminar el preset?": "Delete the preset?",
"Álbum": "Album", "Álbum desconocido": "Unknown album", "Álbumes": "Albums",
"15 minutos": "15 minutes", "30 minutos": "30 minutes", "45 minutos": "45 minutes", "1 hora": "1 hour", "1 hora 30 min": "1 hour 30 min",
"Moods:": "Moods:",
"Exporta la lista como M3U para usarla en otro reproductor, o elimínala (podrás recuperarla con \"Restaurar listas de reproducción\"; las canciones no se borran del dispositivo).":
  "Export the list as M3U to use it in another player, or delete it (you can recover it with \"Restore playlists\"; songs are not deleted from the device).",
"Activar YouTube Music": "Turn on YouTube Music", "Buscar en YouTube Music": "Search YouTube Music",
"Tus canciones con Me gusta": "Your liked songs", "o escribe arriba para buscar": "or type above to search", "YT Music": "YT Music",
"Apariencia": "Appearance", "En línea (opcional)": "Online (optional)", "Audio": "Audio", "Almacenamiento": "Storage", "Información": "About",
"Cuenta de YouTube Music": "YouTube Music account", "Sesión iniciada en este teléfono": "Signed in on this phone",
"Sin sesión: inicia desde la pestaña YT Music": "Not signed in: do it from the YT Music tab",
"1 canción": "1 song", "0 canciones": "0 songs", "1 álbum": "1 album",
}

def key(es): return "s_" + hashlib.sha1(es.encode()).hexdigest()[:10]

def esc(s):
    s = s.replace("\\", "\\\\").replace("&", "&amp;").replace("<", "&lt;").replace("'", "\\'").replace('"', '\\"').replace("\n", "\\n")
    return ("\\" + s) if s[:1] in "@?" else s

def write(folder, mapping):
    d = os.path.join(ROOT, folder); os.makedirs(d, exist_ok=True)
    lines = ['<?xml version="1.0" encoding="utf-8"?>', '<!-- Generado por scripts/gen_strings.py: no editar a mano. -->', "<resources>"]
    if folder == "values": lines.append('    <string name="app_name">DEZZNUTS</string>')
    for es, tr in sorted(mapping.items()): lines.append(f'    <string name="{key(es)}">{esc(tr)}</string>')
    lines.append("</resources>")
    open(os.path.join(d, "strings.xml"), "w").write("\n".join(lines) + "\n")

if __name__ == "__main__":
    write("values", {es: es for es in EN})
    write("values-en", EN)
    os.makedirs(os.path.join(ROOT, "raw"), exist_ok=True)
    open(os.path.join(ROOT, "raw", "keep.xml"), "w").write(
        '<?xml version="1.0" encoding="utf-8"?>\n<!-- Los textos se buscan por nombre (I18n.kt): el recorte de recursos no debe quitarlos. -->\n'
        '<resources xmlns:tools="http://schemas.android.com/tools" tools:keep="@string/s_*" />\n')
    print(len(EN), "textos")
