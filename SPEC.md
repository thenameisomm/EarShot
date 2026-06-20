# EarShot - Specification Document

## 1. Project Overview

- **Project Name**: EarShot
- **Project Type**: Android Application
- **Core Functionality**: An Android app that allows users to control their phone camera using Bluetooth earbud button presses. The app detects media button events from connected Bluetooth devices and maps them to camera actions.

## 2. Technology Stack & Choices

- **Language**: Kotlin
- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: Latest stable Android SDK (35)
- **Compile SDK**: 35
- **Build System**: Gradle with Kotlin DSL

### Key Libraries/Dependencies
- **UI**: Material 3 (Material Design Components)
- **Architecture Components**:
  - ViewModel
  - LiveData
  - Navigation Component
- **Dependency Injection**: Manual DI (ViewModel Factory pattern)
- **Local Storage**: SharedPreferences
- **Camera**: CameraX (dependency only, not implemented yet)
- **Media**: MediaSession API (dependency only, not implemented yet)

### Architecture Pattern
- **MVVM (Model-View-ViewModel)**
- Clean Architecture with separation into:
  - **UI Layer**: Activities, Fragments, Composables
  - **ViewModel Layer**: ViewModels for each screen
  - **Data Layer**: Repository pattern with SharedPreferences

## 3. Feature List

### Screen 1: Home Screen
- App title and logo/icon
- Brief description of app functionality
- Quick access buttons to navigate to other screens
- Status indicator showing Bluetooth connection state (placeholder)

### Screen 2: Connected Device Screen
- List of paired/connected Bluetooth devices (placeholder data)
- Device connection status indicators
- "Scan for Devices" button (UI only)
- Device selection functionality

### Screen 3: Gesture Mapping Screen
- List of available gesture/button actions
- Mapping interface to assign gestures to camera actions
- Save/Reset gesture mappings
- Supported gestures: Single Tap, Double Tap, Triple Tap, Long Press

### Screen 4: Camera Settings Screen
- Camera selection (front/rear) - UI only
- Photo/Video mode toggle - UI only
- Grid overlay toggle - UI only
- Timer settings - UI only
- Save settings preference

## 4. UI/UX Design Direction

- **Visual Style**: Material Design 3 with dynamic theming
- **Color Scheme**:
  - Primary: Deep Blue (#1565C0)
  - Secondary: Teal Accent (#00897B)
  - Surface: Light/Dark based on system theme
  - Support both Light and Dark themes
- **Layout Approach**:
  - Bottom Navigation for main screens
  - Card-based layouts for list items
  - Material 3 components throughout
- **Navigation**: Bottom Navigation Bar with 4 destinations

## 5. Project Structure

```
app/
├── src/main/
│   ├── java/com/earshot/
│   │   ├── ui/
│   │   │   ├── home/
│   │   │   ├── device/
│   │   │   ├── gesture/
│   │   │   └── camera/
│   │   ├── viewmodel/
│   │   ├── repository/
│   │   ├── model/
│   │   └── util/
│   └── res/
│       ├── layout/
│       ├── values/
│       ├── navigation/
│       └── drawable/
├── build.gradle.kts
└── proguard-rules.pro
```

## 6. Notes

- **Camera functionality**: Not implemented - placeholder UI only
- **Bluetooth functionality**: Not implemented - placeholder data and UI only
- The app provides the complete UI structure and architecture for future implementation