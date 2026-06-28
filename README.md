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
| **CameraX Integration** | Full camera integration with live preview using CameraX |
| **Photo Capture** | Capture photos using the rear or front camera |
| **Video Recording** | Record videos with start/stop functionality |
| **Camera Switching** | Switch between front and rear cameras seamlessly |
| **Flash Control** | Toggle flash modes (Auto, On, Off) |
| **Timer Support** | Configurable timer (Off, 3 seconds, 10 seconds) |
| **Grid Overlay** | Rule of thirds grid overlay toggle |
| **Camera Settings UI** | Configure front/rear camera, photo/video mode, grid overlay, and timer options |
| **Bluetooth Device Scanning** | Real Bluetooth device discovery and connection management |
| **Paired Devices** | View and connect to previously paired Bluetooth devices |
| **Gallery Integration** | Browse, view, and delete captured photos and videos |
| **Event History** | Real-time display of detected media button events with timestamps |
| **Settings Persistence** | All gesture mappings and camera settings saved to SharedPreferences |
| **Material Design 3** | Modern theming with custom premium crayon-like color palette |
| **Premium UI** | Warm purple/coral/teal colors with gradient headers and rounded corners |
| **Dark Mode** | Automatic dark theme support (follows system setting) |
| **Edge-to-Edge UI** | Transparent status/navigation bars with proper insets handling |

### Planned

- MediaSession API integration (optional enhancement)

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
| CameraX Video | 1.3.1 |
| ConstraintLayout | 2.1.4 |
| Activity KTX | 1.8.2 |
| Fragment KTX | 1.6.2 |
| Media | 1.7.0 |
| CoordinatorLayout | 1.2.0 |

## Architecture

The app follows **MVVM** (Model-View-ViewModel) architecture with Clean Architecture principles:

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                              UI Layer                                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐    │
│  │  Home    │  │  Device  │  │  Gesture │  │  Camera  │  │   Media      │    │
│  │ Fragment │  │ Fragment │  │ Fragment │  │ Fragment │  │   Fragment   │    │
│  └────┬─────┘  └───┬──────┘  └──┬───────┘  └─┬────────┘  └──┬───────────┘    │
└───────┼────────────┼────────────┼────────────┼──────────────┼────────────────┘
        │            │            │            │              │
┌───────┼────────────┼────────────┼────────────┼──────────────┼───────────────┐
│       ▼            ▼            ▼            ▼              ▼               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────┐   │
│  │Bluetooth │  │Bluetooth │  │GestureVM │  │CameraVM  │  │MediaButtonVM │   │
│  │ViewModel │  │ViewModel │  │          │  │          │  │              │   │
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
│       │  └─────────────┘      └────────────┘    │  Manager  │     │         │
│       │                                         └───────────┘     │         │
│       └───────────────────────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Key Components**

- **Fragments**: HomeFragment, DeviceFragment, GestureFragment, CameraFragment, MediaButtonFragment, GalleryFragment
- **ViewModels**: BluetoothViewModel, CameraViewModel, GalleryViewModel, GestureViewModel, MediaButtonViewModel
- **Repositories**: BluetoothRepository, SettingsRepository, GalleryRepository
- **Services**: MediaButtonService (foreground), MediaButtonReceiver (BroadcastReceiver), GestureEngine
- **Bluetooth**: BluetoothManager (singleton coordinator), BluetoothScanner, BluetoothConnectionManager, BluetoothPermissionManager, BluetoothStateMonitor
- **Camera**: CameraXManager (CameraX lifecycle and operations), CameraPhotoOutput (photo file handling)
- **Models**: BluetoothDevice, GestureMapping, CameraSettings, MediaButtonEvent, GestureType, CameraAction, CameraMode, CameraSelection

## Project Structure

```
app/src/main/
├── java/com/earshot/
│   ├── model/
│   │   ├── BluetoothDevice.kt
│   │   ├── CameraSettings.kt
│   │   ├── CameraMode.kt
│   │   ├── CameraSelection.kt
│   │   ├── GestureMapping.kt
│   │   ├── MediaButtonEvent.kt
│   │   └── TimerOption.kt
│   ├── bluetooth/
│   │   ├── BluetoothManager.kt
│   │   ├── BluetoothScanner.kt
│   │   ├── BluetoothConnectionManager.kt
│   │   ├── BluetoothPermissionManager.kt
│   │   ├── BluetoothStateMonitor.kt
│   │   ├── BluetoothRepository.kt
│   │   ├── BluetoothState.kt
│   │   └── BluetoothUiState.kt
│   ├── repository/
│   │   ├── SettingsRepository.kt
│   │   └── GalleryRepository.kt
│   ├── service/
│   │   ├── GestureEngine.kt
│   │   ├── MediaButtonReceiver.kt
│   │   └── MediaButtonService.kt
│   ├── camera/
│   │   ├── CameraXManager.kt
│   │   └── CameraPhotoOutput.kt
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
│   │   ├── gallery/
│   │   │   ├── GalleryAdapter.kt
│   │   │   └── GalleryFragment.kt
│   │   ├── home/
│   │   │   └── HomeFragment.kt
│   │   └── media/
│   │       ├── MediaButtonEventAdapter.kt
│   │       └── MediaButtonFragment.kt
│   └── viewmodel/
│       ├── CameraViewModel.kt
│       ├── BluetoothViewModel.kt
│       ├── GestureViewModel.kt
│       ├── GalleryViewModel.kt
│       └── MediaButtonViewModel.kt
└── res/
    ├── drawable/
    ├── layout/
    ├── menu/
    ├── navigation/
    ├── values/
    └── values-night/
```

## Premium UI Design

The app features a **premium crayon-like interface** with:

- **Warm Color Palette**: Purple primary (#8B5CF6), Coral secondary (#F97316), Teal tertiary (#14B8A6)
- **Gradient Headers**: All screens feature gradient headers with smooth transitions
- **Rounded Corners**: Large rounded corners (16-28dp) for a playful feel
- **Premium Cards**: Elevated cards with subtle shadows and strokes
- **Material 3 Components**: Modern Bottom Navigation, FABs, TextInputLayouts
- **Active Indicators**: Bottom navigation with active state indicators

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
| `BLUETOOTH_ADVERTISE` | Advertise Bluetooth device (API 31+) |
| `ACCESS_FINE_LOCATION` | Location required for Bluetooth scanning (legacy) |
| `NEARBY_WIFI_DEVICES` | BLE scanning on Android 13+ |
| `CAMERA` | Camera preview and capture |
| `RECORD_AUDIO` | Video recording with audio |
| `FOREGROUND_SERVICE` | Run persistent background service |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Media-related foreground service |
| `POST_NOTIFICATIONS` | Show notifications (Android 13+) |
| `MEDIA_CONTENT_CONTROL` | Control media playback |

## Navigation Flow

The app uses **Bottom Navigation** with 5 main screens accessible from the navigation bar:

| Screen | Description |
|--------|-------------|
| **Home** | Welcome screen with Bluetooth status and quick action buttons |
| **Device** | Bluetooth device management - scan, pair, and connect to earbuds |
| **Gallery** | Browse captured photos and videos |
| **Media** | Media button event detection - view detected button press events |
| **Camera** | Full camera preview with capture controls |

The **Gesture Mapping** screen is accessible from the Home screen via the "Map Gestures" quick action.

## Camera Features

The Camera screen includes:

- **Full-screen Camera Preview** with CameraX
- **Capture Button** - Large white button for photo/video capture
- **Camera Switch** - Toggle between front and rear cameras
- **Flash Control** - Auto/On/Off toggle
- **Settings Button** - Opens bottom sheet with camera settings
- **Gallery Shortcut** - Quick access button to open the Gallery screen
- **Recording Indicator** - Red dot and "Recording..." text when video is being recorded
- **Timer Countdown** - Large countdown display when timer is enabled
- **Settings Bottom Sheet** - Slide-up panel for camera configuration

### Camera Settings (Bottom Sheet)

- Camera Selection (Front/Rear)
- Timer (Off, 3s, 10s)
- Camera Mode (Photo/Video)
- Grid Overlay (On/Off)
- Save Settings button

## Current Limitations

- **Limited earbud compatibility** — Media button support varies by device manufacturer
- **API 26+ only** — Minimum Android 8.0 required for media button intercept

## Roadmap

| Phase | Features |
|-------|-----------|
| 1 | Media button detection, gesture engine, gesture mapping UI ✓ |
| 2 | CameraX integration, photo capture, video recording ✓ |
| 3 | Real Bluetooth device scanning, gallery integration ✓ |
| 4 | Beta testing, performance optimization, release |
| 5 | Additional features and improvements |

## Testing

The project includes **Espresso UI tests** for automated testing:

- **HomeScreenTest**: Validates that the Home screen loads correctly and displays key UI elements
- Tests verify: Bluetooth status, Quick Actions, Bottom Navigation, and status indicators

Run tests:
```bash
./gradlew test
./gradlew connectedAndroidTest
```

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
