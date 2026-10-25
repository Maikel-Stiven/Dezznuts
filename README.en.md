<div align="center">

<img src="docs/assets/dezznuts-icon.gif" alt="DEZZNUTS: acorn with headphones" width="160" />

# DEZZNUTS 🎧

**A free, open-source local music player for Android.**

[🇪🇸 Español](README.md) · [🇬🇧 English](README.en.md)

![Android](https://img.shields.io/badge/Android-7.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Version](https://img.shields.io/badge/version-1.1.0-brightgreen)
![PRs](https://img.shields.io/badge/PRs-welcome-brightgreen)

</div>

Features other players lock behind a paywall (auto-tagging, audio cutter, equalizer, sleep timer…), offered freely, "just for the love of it".

> ✅ **Version 1.1.0**. Grab the APK from [Releases](../../releases/latest) and read the [CHANGELOG](CHANGELOG.md). What still needs real-phone testing is in [Known issues](#-known-issues).

## 📸 Screenshots

<div align="center">
<img src="docs/screenshots/01-inicio.png" alt="Home" width="15%" />
<img src="docs/screenshots/02-canciones.png" alt="Songs" width="15%" />
<img src="docs/screenshots/04-reproductor.png" alt="Player" width="15%" />
<img src="docs/screenshots/07-letras.png" alt="Lyrics" width="15%" />
<img src="docs/screenshots/06-menu.png" alt="Menu" width="15%" />
<img src="docs/screenshots/05-opciones.png" alt="Options" width="15%" />
</div>

**Pick your theme** (purple is the original): Omarchy, terminal green with a pixel font.

<div align="center">
<img src="docs/screenshots/08-tema-omarchy.png" alt="Omarchy theme" width="22%" />
<img src="docs/screenshots/09-omarchy-reproductor.png" alt="Player in Omarchy" width="22%" />
</div>

## 📦 What is it?

A native Android app written in **Kotlin + Jetpack Compose** that scans the music stored on the device (MediaStore) and plays it in the background with **Media3 / ExoPlayer**. It organizes by album, artist and genre, and supports playlists, favorites, search, ID3 tag editing and automatic cover art (public Deezer and iTunes APIs, no API keys).

## ✨ Features

| Area | Detail |
|------|--------|
| **Navigation** | Top bar ☰ · search · ⋮, swipeable tabs (Quick picks, Songs, Albums, Artists, Genres, Playlists, Folders, YT Music) and a side menu; detail screens stack, so Back returns to the previous one. |
| **Design** | DEZZNUTS identity: **purple** `#BB86FC` and teal on black, top tabs + side menu; **10 themes to choose from**: DEZZNUTS (purple, default), **Omarchy** (pixel green), **Premium** (caramel), Sakura, Ocean, Synthwave, Ember, AMOLED, light and Material You; full-screen Options; Spanish and English. |
| **Player** | Cover, title & favorite, seek bar, controls, synced **lyrics** (LRCLIB), animated cover backdrop. Swipe the mini-player up to open, drag down to close, swipe sideways to skip. |
| **Library** | Songs (filters + **sorting**), Albums, Artists, Playlists, Genres, Folders; Home with recent, most played, favorites and moods. |
| **Queue** | **Play next**, add to queue, drag to reorder, remove. |
| **Playlists** | Covers, restore deleted ones, **M3U import/export**. |
| **Backup** | Favorites, play counts and playlists to a file, matched **by path** so they survive reinstalls and phone changes. |
| **Audio** | Parametric EQ with presets and per-output profiles, ReplayGain, fades, float output, offload, USB bit-perfect, sleep timer. |
| **Online (optional, off by default)** | **YouTube Music** (search, play, sign in, Likes) and **animated cover** (album video loop, Wi-Fi only). |
| **System** | Android Auto browse tree, home-screen **widget**, resume the last queue from Bluetooth, listening stats. |
| **Tools** | Tag editor (**jaudiotagger**), bulk "unknown artist" fix, audio cutter, duplicate & WhatsApp-audio finder. |
| **Low-end friendly** | Animations off automatically on 2-core / low-RAM phones or battery saver. |

For deep details (Hi/Res audio, seek latency, architecture) see [`README.md`](README.md) and [`RESUMEN_PROYECTO.md`](RESUMEN_PROYECTO.md) (Spanish).

## 🧱 Tech stack

Kotlin `2.2.10` · Jetpack Compose (BOM `2026.02.01`) + Material 3 · AndroidX Media3 `1.9.1` · Coil `2.7.0` · jaudiotagger `3.0.1` · AGP `9.4.1` / Gradle `9.8.1` · `minSdk` 24 / `targetSdk` 37 · `SharedPreferences` + text backup · app id `io.github.maikelstiven.dezznuts` · JUnit 4 (105 tests) + GitHub Actions.

## 🔧 Build

Requires a recent Android Studio (or JDK 21) and Android SDK API 37.

```bash
./gradlew assembleDebug        # debug APK in app/build/outputs/apk/debug/
./gradlew installDebug         # install on a connected device/emulator
./gradlew testDebugUnitTest    # unit tests
```

## 🔐 Permissions

| Permission | Why |
|-----------|-----|
| `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Scan local music |
| `INTERNET` | Artist photos (Deezer) and metadata (iTunes) |
| `FOREGROUND_SERVICE` + `…_MEDIA_PLAYBACK` | Background playback |
| `POST_NOTIFICATIONS` | Player notification |

## 🐞 Known issues

- With **shuffle on**, "Play next" lands at a random spot of the shuffled order (Media3 owns that order).
- **Skip silence** and **playback speed** are left out on purpose: the hi-fi audio chain drops those 16-bit-only processors.
- Genre is only read on Android 11+.
- Not yet tested on a real phone: placed widget, Android Auto and YouTube Music sign-in (measurements were on an emulator). Strings with embedded values (`"$n songs"`) are still Spanish-only.

## 🗺️ Roadmap

See [`docs/HOJA_DE_RUTA.md`](docs/HOJA_DE_RUTA.md) (Spanish): what's done and what comes next, by stage.

## 🤝 Contributing

Contributions of any size are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of Conduct](CODE_OF_CONDUCT.md).

## 👤 Credits

Open source (GPL-3.0), built by several people: **@Pedroj-64** (design, themes, player, lyrics, YouTube Music, Android Auto, widget, optimisation), **@Maikel-Stiven** (original author; playlist menus, queue persistence and the **bottom-bar interface, theme/accent picker, Silkscreen typography and YT Music home** that are now the app's foundation) and **Brandon Gil (@AiiwA)** (hi-fi audio, seek performance, covers, tests and CI).

## 📜 License

Distributed under **GPL-3.0**; see [`LICENSE`](LICENSE).
