# EarShot

<p align="left">
  <a href="https://android.com">
    <img src="https://img.shields.io/badge/Platform-Android 8.0+-3DC083?style=flat&logo=android" alt="Platform">
  </a>
  <a href="https://kotlinlang.org">
    <img src="https://img.shields.io/badge/Language-Kotlin 1.9.22-7F52FF?style=flat&logo=kotlin" alt="Language">
  </a>
  <a href="https://gradle.org">
    <img src="https://img.shields.io/badge/Build-Gradle 8.4-02303A?style=flat&logo=gradle" alt="Build">
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-green?style=flat" alt="License">
  </a>
</p>

EarShot is an Android application that enables hands-free photography by letting you control your phone's camera using Bluetooth earbud button presses. Map gestures like single tap, double tap, triple tap, or long press on your wireless earbuds to camera actions — no need to touch your phone.

## Features

### Implemented

| Feature | Description |
|---------|-------------|
| **Media Button Detection** | Foreground service that intercepts hardware media button events from Bluetooth earbuds |
| **Gesture Engine** | Detects single tap, double tap, triple tap, and long press (600ms threshold) with configurable tap window (300ms) |
| **Gesture Mapping UI** | Map each gesture type to a camera action via dropdown selectors |
| **Camera Settings UI** | Configure front/rear camera, photo/video mode, grid overlay, and timer options |
| **Device Management UI** | View paired and connected Bluetooth devices with connection status |
| **Event History** | Real-time display of detected media button events with timestamps |
| **Settings Persistence** | All gesture mappings and camera settings saved to SharedPreferences |
| **Material Design 3** | Modern theming with custom premium color palette |
| **Dark Mode** | Automatic dark theme support (follows system setting) |
| **Edge-to-Edge UI** | Transparent status/navigation bars with proper insets handling |

### In Progress

- **CameraX Integration** — CameraX dependencies added, actual capture not yet implemented
- **Real Bluetooth Device Scanning** — UI and repository structure in place, actual scanning pending

### Planned

- Photo capture via CameraX
- Video recording (start/stop)
- Real Bluetooth device discovery and connection
- MediaSession API integration

## Tech Stack

| Component | Version |
|-----------|---------|
| Kotlin | 1.9.22 |
| Android Gradle Plugin | 8.3.2 |
| Min SDK | 26 (Android 8.0) |
| Target/Compile SDK | 35 |
| Java | 17 |

### Dependencies

| Library | Version |
|---------|---------|
| Material 3 | 1.11.0 |
| Navigation Component | 2.7.7 |
| Lifecycle (ViewModel/LiveData) | 2.7.0 |
| CameraX | 1.3.1 |
| ConstraintLayout | 2.1.4 |
| Activity KTX | 1.8.2 |
| Fragment KTX | 1.6.2 |
| Media | 1.7.0 |

## Architecture

The app follows **MVVM** (Model-View-ViewModel) architecture with Clean Architecture principles:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              UI Layer                                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │  Home    │  │  Device  │  │  Gesture │  │  Camera  │  │   Media      │    │
│  │ Fragment │  │ Fragment │  │ Fragment │  │ Fragment │  │   Fragment   │    │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘    │
└───────┼────────────┼────────────┼────────────┼──────────────┼────────────────┘
        │            │            │            │              │
┌───────┼────────────┼────────────┼────────────┼──────────────┼───────────────┐
│       ▼            ▼            ▼            ▼              ▼               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │HomeVM    │  │DeviceVM  │  │GestureVM │  │CameraVM  │  │MediaButtonVM │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘  └──────┬───────┘   │
│       │             │             │             │               │           │
│       └─────────────┴─────────────┴─────────────┴───────────────┘           │
│                              ViewModel Layer                                │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────┼───────────────────────────────────────┐
│                             Data Layer                                      │
│       ┌─────────────────────────────┴──────────────────────────────┐        │
│       │                   Repository Layer                         │        │
│       │  ┌─────────────┐    ┌─────────────────┐                    │        │
│       │  │  Device     │    │    Settings     │                    │        │
│       │  │ Repository  │    │   Repository    │                    │        │
│       │  └─────────────┘    └─────────────────┘                    │        │
│       └────────────────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
┌─────────────────────────────────────┼───────────────────────────────────────┐
│                            Service Layer                                    │
│       ┌─────────────────────────────┼─────────────────────────────┐         │
│       │  ┌─────────────┐      ┌─────┴──────┐    ┌───────────┐     │         │
│       │  │ MediaButton │      │  Gesture   │    │   Camera  │     │         │
│       │  │  Service    │      │   Engine   │    │     X     │     │         │
│       │  └─────────────┘      └────────────┘    └───────────┘     │         │
│       └───────────────────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Key Components**

- **Fragments**: HomeFragment, DeviceFragment, GestureFragment, CameraFragment, MediaButtonFragment
- **ViewModels**: One per screen with Factory pattern for dependency injection
- **Repositories**: DeviceRepository (Bluetooth devices), SettingsRepository (SharedPreferences)
- **Services**: MediaButtonService (foreground), MediaButtonReceiver (BroadcastReceiver)
- **Models**: BluetoothDevice, GestureMapping, CameraSettings, MediaButtonEvent, GestureType, CameraAction

## Project Structure

```
app/src/main/
├── java/com/earshot/
│   ├── model/
│   │   ├── BluetoothDevice.kt
│   │   ├── CameraSettings.kt
│   │   ├── GestureMapping.kt
│   │   └── MediaButtonEvent.kt
│   ├── repository/
│   │   ├── DeviceRepository.kt
│   │   └── SettingsRepository.kt
│   ├── service/
│   │   ├── GestureEngine.kt
│   │   ├── MediaButtonReceiver.kt
│   │   └── MediaButtonService.kt
│   ├── ui/
│   │   ├── MainActivity.kt
│   │   ├── base/
│   │   │   └── BaseFragment.kt
│   │   ├── camera/
│   │   │   └── CameraFragment.kt
│   │   ├── device/
│   │   │   ├── DeviceAdapter.kt
│   │   │   └── DeviceFragment.kt
│   │   ├── gesture/
│   │   │   ├── GestureAdapter.kt
│   │   │   └── GestureFragment.kt
│   │   ├── home/
│   │   │   └── HomeFragment.kt
│   │   └── media/
│   │       ├── MediaButtonEventAdapter.kt
│   │       └── MediaButtonFragment.kt
│   └── viewmodel/
│       ├── CameraViewModel.kt
│       ├── DeviceViewModel.kt
│       ├── GestureViewModel.kt
│       ├── HomeViewModel.kt
│       └── MediaButtonViewModel.kt
└── res/
    ├── drawable/
    ├── layout/
    ├── menu/
    ├── navigation/
    ├── values/
    └── values-night/
```

## Installation

### Prerequisites

- Android Studio (Arctic Fox or newer)
- JDK 17
- Android SDK 35

### Build

```bash
# Clone the repository
git clone https://github.com/ombadgujar/EarShot.git
cd EarShot

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

| Permission | Purpose |
|------------|---------|
| `BLUETOOTH` | Classic Bluetooth access (API < 31) |
| `BLUETOOTH_ADMIN` | Device discovery (API < 31) |
| `BLUETOOTH_CONNECT` | Connect to Bluetooth devices (API 31+) |
| `BLUETOOTH_SCAN` | Scan for Bluetooth devices (API 31+) |
| `CAMERA` | Camera capture (placeholder) |
| `FOREGROUND_SERVICE` | Run persistent background service |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Media-related foreground service |
| `POST_NOTIFICATIONS` | Show notifications (Android 13+) |
| `MEDIA_CONTENT_CONTROL` | Control media playback |

## Current Limitations

- **No actual camera capture** — Gesture engine logs actions but CameraX capture not implemented
- **Placeholder device data** — DeviceRepository uses mock data; real Bluetooth scanning pending
- **Limited earbud compatibility** — Media button support varies by device manufacturer
- **API 26+ only** — Minimum Android 8.0 required for media button intercept

## Roadmap

| Phase | Features |
|-------|-----------|
| 1 | Media button detection, gesture engine, gesture mapping UI |
| 2 | CameraX integration, photo capture |
| 3 | Video recording, real Bluetooth device scanning |
| 4 | Settings polish, dark mode, beta testing |

## Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Make changes and commit (`git commit -m "feat: add feature"`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Open a Pull Request

### Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful names for variables and functions
- Add KDoc comments for public APIs
- Keep functions focused and concise
- Test your changes on a physical device before submitting

## Author

**Om Badgujar**

- GitHub: [@ombadgujar](https://github.com/thenameisomm)
- Project: [EarShot](https://github.com/thenameisomm/EarShot)

## Acknowledgments

- [Android Jetpack](https://developer.android.com/jetpack) — Modern Android development platform
- [Material Design 3](https://m3.material.io/) — Design system and components
- [CameraX](https://developer.android.com/camera) — Camera API made simple

---

**Screenshots and demo coming soon.**
