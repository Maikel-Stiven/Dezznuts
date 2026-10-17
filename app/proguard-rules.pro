# Reglas de ProGuard/R8 para DEZZNUTS.
#
# Nota: la build de release tiene isMinifyEnabled = false por ahora,
# pero este archivo se referencia desde app/build.gradle.kts, así que
# conviene mantenerlo aquí listo para cuando se active R8.

# jaudiotagger (edición de etiquetas ID3) usa reflexión internamente.
-keep class org.jaudiotagger.** { *; }
-dontwarn org.jaudiotagger.**
