# Kavita for Android — Native Client

A native Android client for [Kavita](https://github.com/Kareadita/Kavita), your self-hosted manga, comic and book server.

<p align="center">
  <img src="screenshot/screenshot.png" alt="Kavita for Android" width="80%">
</p>

<p align="center">
  <a href="https://github.com/u1145h/kavita-android/releases">
    <img src="https://img.shields.io/github/v/release/u1145h/kavita-android?style=for-the-badge&color=2563EB&logo=github&label=Download%20APK" alt="Download Latest Release">
  </a>
</p>

## Features

- **Native UI** — fully native Kotlin / Jetpack Compose interface (no embedded WebView)
- **Library browsing** — adaptive series grid per library with infinite scroll
- **Home dashboard** — On Deck, In Progress, and Recently Added carousels
- **Series detail** — cover art, summary, genre chips, volume/chapter list, progress bar
- **Native comic reader** — HorizontalPager with pinch-to-zoom, double-tap zoom, tap-zone navigation, RTL toggle, and webtoon scroll mode
- **Reading progress sync** — bidirectional sync with the Kavita server (debounced, auto on page turn)
- **Live search** — 300 ms debounced search across series and collections
- **Authentication** — JWT login with automatic token refresh; or use a Kavita API key
- **Offline-first caching** — Room + DataStore persist session and metadata across restarts
- **Material 3 / Dynamic Color** — supports Material You on Android 12+
- **Dark / light theme** — respects system theme or manual override

## Requirements

- **JDK 17+**
- **Android Studio** (latest stable, SDK 37)
- A reachable [Kavita](https://github.com/Kareadita/Kavita) server (Docker or bare-metal)

## Setup

On first launch, enter your **server URL** (e.g. `http://192.168.1.100:5000`) and your Kavita **username + password**. The app verifies the server, logs in, and persists the JWT for future sessions. To connect to a different server, clear app data and reopen.

> **HTTP servers** — if your Kavita instance runs on plain HTTP (common for LAN Docker setups), the app has `usesCleartextTraffic="true"` in the manifest so it works out of the box.

## Build

```bash
# Windows
.\gradlew.bat :app:assembleDebug

# macOS / Linux
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

> **Windows note:** if the project and the Gradle cache live on different drives, KSP must run non-incrementally (`ksp.incremental=false` is already set in `gradle.properties`).

## Project structure

```
app/src/main/java/com/u1145h/kavitaandroid/
├── core/
│   ├── config/          # ServerConfig (timeouts, API path)
│   └── util/            # CoverUrlBuilder
├── data/
│   ├── local/
│   │   ├── datastore/   # SettingsDataStore, SessionDataStore
│   │   └── db/          # Room database (offline library, reading sessions)
│   ├── remote/
│   │   ├── api/         # KavitaApiService (full REST surface)
│   │   ├── auth/        # AuthInterceptor, DynamicBaseUrlInterceptor, SessionManager
│   │   ├── dto/         # All Kavita DTOs
│   │   └── DtoMapper    # DTO → domain mappers
│   └── repository/      # KavitaRepository, LibraryRepository, SearchRepository,
│                        #   SettingsRepository, BookRepository
├── di/                  # Hilt modules (AppModule, NetworkModule, RepositoryModule)
├── domain/model/        # Library, Series, Volume, Chapter, SearchResult, Book …
├── feature/
│   ├── setup/           # Login / server setup screen
│   ├── home/            # Home dashboard (On Deck, In Progress, Recently Added)
│   ├── library/         # Paginated series grid for a single library
│   ├── series/          # Series detail (volumes, chapters, metadata)
│   ├── reader/          # Native comic reader (pager + webtoon mode)
│   └── search/          # Live search
└── ui/                  # KavitaApp (NavHost), RootViewModel, theme
```

## Tech stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Networking | Retrofit + OkHttp + kotlinx.serialization |
| Auth | JWT in DataStore |
| Local DB | Room |
| Preferences | DataStore |
| Images | Coil 3 |
| Background sync | WorkManager (future) |
| Testing | JUnit 4 + Robolectric + Truth |

## Kavita API

Kavita exposes a full REST API documented via Swagger / OpenAPI at `/api/swagger`. The app authenticates via `POST /api/Account/login` and attaches the returned JWT as `Authorization: Bearer <token>` on all subsequent requests. The token is auto-refreshed via `POST /api/Account/refresh-token`.

Full API spec: [openapi.json](https://raw.githubusercontent.com/Kareadita/Kavita/develop/openapi.json)

## License

This project is a third-party, unofficial client and is not affiliated with the Kavita project. Kavita is distributed under its own license. This project is released under the [MIT License](LICENSE).
