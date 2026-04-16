# Crawl4AI Android

> **Non-commercial** • Android 15 (API 35) • Kotlin + Compose + Chaquopy + crawl4ai

A fully-featured Android application that brings the power of **crawl4ai** to
your Android device. The Python runtime is embedded via [Chaquopy](https://chaquo.com/chaquopy/),
so no external server is needed for basic crawling.

---

## Architecture

```
┌─────────────────────────────────────────────────┐
│  Android UI  (Jetpack Compose + Material 3)     │
│  ViewModels  (Hilt + Coroutines + Flow)         │
│  Use Cases   (Domain layer)                     │
│  Repositories (Data layer + Room DB)            │
│                                                  │
│  ┌──────────────┐    ┌────────────────────────┐ │
│  │  PythonBridge│    │ CrawlForegroundService  │ │
│  │  (Chaquopy)  │    │ (WorkManager)           │ │
│  └──────┬───────┘    └────────────────────────┘ │
│         │ JNI (Chaquopy)                         │
│  ┌──────▼──────────────────────────────────────┐│
│  │  Python 3.10 (embedded in APK)              ││
│  │  crawl4ai_bridge.py                         ││
│  │  crawl4ai_engine.py  ← AsyncWebCrawler      ││
│  │  extraction_bridge.py ← CSS/XPath/Regex/LLM ││
│  │  deep_crawl_bridge.py ← BFS/DFS/BFF         ││
│  │  utils_bridge.py     ← URL utils, metadata  ││
│  └─────────────────────────────────────────────┘│
└─────────────────────────────────────────────────┘
```

## Features

| Feature | Status |
|---|---|
| Single URL crawl (HTTP, no browser) | ✅ Phase 1 |
| Multi-URL concurrent crawl | ✅ Phase 1 |
| Deep crawl (BFS / DFS / Best-First) | ✅ Phase 1 |
| Markdown generation | ✅ Phase 1 |
| CSS / XPath / Regex extraction | ✅ Phase 1 |
| LLM extraction (API key required) | ✅ Phase 1 |
| Pruning & BM25 content filters | ✅ Phase 1 |
| Text chunking (Regex / NLP / Fixed) | ✅ Phase 1 |
| URL seeding | ✅ Phase 1 |
| SQLite cache (persistent) | ✅ Phase 1 |
| JS-rendered pages (via Android WebView) | ✅ Phase 1 |
| Foreground service + notifications | ✅ Phase 1 |
| EventBus (SDLC event system) | ✅ Phase 1 |
| History screen | 🔜 Phase 4 |
| Config manager (presets) | 🔜 Phase 4 |
| Export (JSON/CSV/Markdown) | 🔜 Phase 5 |
| Remote Docker server support | 🔜 Phase 5 |
| Extraction template library | 🔜 Phase 5 |

## Prerequisites

- **Android Studio Ladybug (2024.2.1)** or later
- **Android SDK API 35** (Android 15)
- **JDK 17**
- A physical device or emulator running API 28+ (arm64-v8a preferred)
- Internet connection for the first build (Chaquopy downloads Python + packages)

## Setup

```bash
# 1. Clone the repository (or open the android/ directory in Android Studio)
cd android/

# 2. Copy and configure local properties
cp local.properties.example local.properties
# Edit local.properties — set sdk.dir to your Android SDK path

# 3. Build
./gradlew assembleDebug

# 4. Install on device / emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **First build warning:** Chaquopy will download Python 3.10 + all pip
> packages (~150 MB) from `chaquo.com/maven`.  Subsequent builds use the
> Gradle cache and are much faster.

## Android Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | HTTP crawling |
| `ACCESS_NETWORK_STATE` | Detect connectivity |
| `FOREGROUND_SERVICE` | Background crawls |
| `FOREGROUND_SERVICE_DATA_SYNC` | Android 14+ requirement |
| `POST_NOTIFICATIONS` | Crawl progress notifications |
| `RECEIVE_BOOT_COMPLETED` | Resume scheduled crawls on reboot |
| `READ_MEDIA_IMAGES` | Save screenshots |

## Python Bridge API

All Python functions are called via `PythonBridge.kt` which wraps `crawl4ai_bridge.py`.

```kotlin
// Single crawl
val response = pythonBridge.crawlUrl("https://example.com", CrawlConfig())

// Deep crawl (Flow)
pythonBridge.deepCrawlFlow("https://example.com", deepConfig, config).collect { page ->
    println("Crawled: ${page.url}")
}

// Markdown from WebView HTML
val markdown = pythonBridge.generateMarkdown(webViewHtml, CrawlConfig())
```

## Event System (SDLC Triggers)

The `EventBus` emits typed `CrawlEvent` objects that trigger SDLC-level actions:

| Event | Consumers |
|---|---|
| `CrawlStarted` | UI (status), ForegroundService |
| `CrawlCompleted` | UI (navigate to results), WorkManager |
| `CrawlFailed` | UI (error snackbar), Notification |
| `DeepCrawlPageCrawled` | UI (progress), ForegroundService |
| `DeepCrawlCompleted` | UI (summary), Database |
| `PythonEngineReady` | Home screen status indicator |
| `CacheCleared` | UI (confirmation) |
| `NetworkStateChanged` | UI (offline banner) |

## Build Variants

| Variant | Description |
|---|---|
| `debug` | `applicationId = com.crawl4ai.android.debug`; no minification |
| `release` | R8 minification; unsigned (non-commercial, no Play Store) |

## CI

GitHub Actions workflow at `.github/workflows/android-ci.yml`:
- Runs on every push touching `android/**`
- Validates Python bridge syntax + capability checks
- Builds debug APK
- Uploads APK as artifact
- Builds release APK on main/master pushes

## Project Structure

```
android/
├── app/
│   ├── src/main/
│   │   ├── python/               # crawl4ai Python bridge modules
│   │   ├── java/.../
│   │   │   ├── App.kt            # Application (Hilt + Chaquopy init)
│   │   │   ├── MainActivity.kt   # Single Activity (Compose host)
│   │   │   ├── bridge/           # Kotlin ↔ Python bridge + mapper
│   │   │   ├── data/             # Room DB, DAOs, repositories
│   │   │   ├── di/               # Hilt modules
│   │   │   ├── domain/           # Models, use cases, repository interfaces
│   │   │   ├── service/          # ForegroundService, EventBus
│   │   │   └── ui/               # Compose screens + ViewModels
│   │   └── res/
│   └── build.gradle.kts          # Chaquopy + all dependencies
├── gradle/libs.versions.toml     # Version catalog
├── settings.gradle.kts
└── README.md
```

## License

Non-commercial use only. See the repository root [LICENSE](../LICENSE).
