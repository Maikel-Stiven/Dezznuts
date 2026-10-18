# Guía de contribución / Contributing

🇪🇸 [Español](#español) · 🇬🇧 [English](#english)

## Español

¡Gracias por querer aportar a DEZZNUTS! Al participar aceptas el [Código de conducta](CODE_OF_CONDUCT.md).

### Cómo ayudar
- 🐛 **Reportar errores:** abre un *issue* con pasos para reproducir, dispositivo y versión de Android, y logs (`adb logcat`) si puedes.
- 💡 **Proponer ideas:** abre un *issue* antes de empezar algo grande, para alinear el enfoque.
- 🔧 **Código:** las funciones pendientes están en la [hoja de ruta](README.md#️-hoja-de-ruta-sugerida) y en `RESUMEN_PROYECTO.md` (hay *buenos primeros issues*).

### Preparar el entorno
1. Android Studio reciente (AGP 9.x), JDK 17+ y Android SDK API 37.
2. `git clone https://github.com/Maikel-Stiven/Dezznuts.git`
3. `./gradlew assembleDebug` y `./gradlew testDebugUnitTest`

### Flujo de trabajo
1. Haz un *fork* y crea una rama desde `main`: `feature/mi-mejora` o `fix/mi-arreglo`.
2. Cambios pequeños y enfocados; un tema por PR.
3. Sigue el estilo existente: Kotlin idiomático, Compose, nombres en español donde ya se usan.
4. Añade pruebas unitarias para la lógica pura (`app/src/test`) y comprueba que `./gradlew testDebugUnitTest` pasa. El CI lo verifica en cada PR.
5. Commits claros, preferiblemente estilo *Conventional Commits* (`feat(player): …`, `fix(tags): …`, `perf: …`, `docs: …`).
6. Abre el *Pull Request* explicando **qué** cambia y **por qué**; adjunta capturas si toca la interfaz.

### Reglas
- No añadas claves de API ni datos personales al repositorio.
- No agregues dependencias nuevas si la librería estándar o las ya incluidas resuelven el problema.
- Respeta la privacidad: la app funciona sin cuentas ni telemetría.

## English

Thanks for helping DEZZNUTS! By participating you agree to the [Code of Conduct](CODE_OF_CONDUCT.md).

### Ways to help
- 🐛 **Bug reports:** open an issue with repro steps, device and Android version, and logs (`adb logcat`) if possible.
- 💡 **Ideas:** open an issue before starting something big so we can align.
- 🔧 **Code:** pending features are listed in the [roadmap](README.en.md#️-roadmap) and `RESUMEN_PROYECTO.md` (look for *good first issues*).

### Setup
1. Recent Android Studio (AGP 9.x), JDK 17+, Android SDK API 37.
2. `git clone https://github.com/Maikel-Stiven/Dezznuts.git`
3. `./gradlew assembleDebug` and `./gradlew testDebugUnitTest`

### Workflow
1. Fork and branch from `main`: `feature/my-change` or `fix/my-fix`.
2. Small, focused changes; one topic per PR.
3. Match the existing style: idiomatic Kotlin and Compose; Spanish names where already used.
4. Add unit tests for pure logic (`app/src/test`) and make sure `./gradlew testDebugUnitTest` passes. CI checks every PR.
5. Clear commits, preferably *Conventional Commits* (`feat(player): …`, `fix(tags): …`).
6. Open the PR explaining **what** and **why**; attach screenshots for UI changes.

### Rules
- Never commit API keys or personal data.
- Don't add a dependency if the standard library or an existing one covers it.
- Respect privacy: the app works with no accounts and no telemetry.
