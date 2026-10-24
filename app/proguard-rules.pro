# Reglas de ProGuard/R8 para DEZZNUTS.
#
# Nota: la build de release tiene isMinifyEnabled = false por ahora,
# pero este archivo se referencia desde app/build.gradle.kts, así que
# conviene mantenerlo aquí listo para cuando se active R8.

# jaudiotagger (edición de etiquetas ID3) usa reflexión internamente.
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**

# YouTube Music (NewPipeExtractor): mismas reglas que usa NewPipe. Rhino ejecuta el JS de YouTube para descifrar
# direcciones y el extractor carga patrones de fechas por nombre.
-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
