# Snaplet

A Locket-inspired Android app that lets you share real-time photos directly to your friends' **home screen widgets** — with live chat, instant reactions, and push notifications to keep your closest circle always connected.

---

## Demo

> [▶ Watch Demo — Google Drive](https://drive.google.com/file/d/1K_-u9MFn6BWkQ7CjLroAnV3W7iQEHh1P/view?usp=sharing)

---

## Features

| Feature | Description |
|---|---|
| 📸 **Home Screen Widget** | Friends' latest photos appear live on each other's home screens via Glance AppWidget |
| 📷 **Camera Capture** | In-app camera built with CameraX for instant photo/video capture |
| 🏠 **Social Feed** | Browse friends' posts, react with emoji, and see who reacted |
| 💬 **Real-time Chat** | 1-on-1 messaging over Socket.io with offline support (Room + Paging 3) |
| 🤝 **Friend System** | Send, accept, and remove friend requests with real-time socket notifications |
| 🔔 **Push Notifications** | FCM-powered notifications with quick reply directly from the notification shade |
| 🔗 **Deep Links** | Navigate directly to a specific post via deep link (spotlight post flow) |
| 🔐 **Google Sign-In** | OAuth via Android Credential Manager |
| ⚙️ **Two Environments** | `development` and `production` flavors for safe testing and release |

---

## Tech Stack

### Core
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture (data → domain → presentation) |
| Dependency Injection | Hilt |
| Navigation | Compose Navigation (single-activity) |

### Networking & Real-time
| | Technology |
|---|---|
| REST Client | Retrofit + OkHttp + Gson |
| Real-time Events | Socket.io (`socket.io-client`) |
| Auth | Automatic token refresh via `OkHttp TokenAuthenticator` |

### Persistence
| | Technology |
|---|---|
| Local Database | Room |
| Pagination | Paging 3 (`RemoteMediator` — DB as single source of truth for chat) |
| Preferences | DataStore |

### Platform & Services
| | Technology |
|---|---|
| Camera | CameraX |
| Image Loading | Coil |
| Home Screen Widget | Glance AppWidget |
| Background Work | WorkManager |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Crash Reporting | Firebase Crashlytics |
| Sign-In | Google Sign-In (Credential Manager) |

### CI/CD
- **Fastlane** — builds `developmentRelease` APK and distributes via Firebase App Distribution
- **GitHub Actions** — triggers on PRs labeled `build-for-test`

---

## Architecture Overview

Built on **MVVM + Clean Architecture** with a strict unidirectional data flow across three layers:

- **Data** — Retrofit (REST) + Socket.io (real-time) + Room (local cache) + DataStore (preferences)
- **Domain** — pure Kotlin use cases, no Android dependencies
- **Presentation** — Jetpack Compose UI driven by `StateFlow` from ViewModels

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- A connected device or emulator running Android 9 (API 28)+

### Build

```bash
# Debug build
./gradlew assembleDevelopmentDebug

# Release build (development flavor — for testing)
./gradlew assembleDevelopmentRelease

# Production release
./gradlew assembleProductionRelease
```

### Run tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedDevelopmentDebugAndroidTest
```

---

## Project Structure

```
app/src/main/java/com/thinh/snaplet/
├── data/               # Models, DTOs, repositories, Room DB, DataStore
├── domain/             # Use cases, domain models
├── ui/                 # Screens, components, theme, overlays, widget UI
├── platform/           # Camera, socket, notifications, deep links, permissions
├── navigation/         # NavGraph, NavScreen, deep link handling
├── network/            # Token refresh, session lifecycle, OkHttp interceptors
├── di/                 # Hilt modules
└── utils/              # Extensions, validators, logging
```