# FlowOrbit

FlowOrbit is an Android app that helps people spend less time procrastinating and more time on intentional work. It combines focus sessions, app-blocking tools, task tracking, and usage insights in one app.

## Features

- **Focus timer** with session controls and optional Do Not Disturb integration.
- **App blocking** with access and overlay setup screens.
- **Boring Mode** to request system-wide grayscale during focus sessions. Android restricts this setting; see the device setup notes below.
- **Tasks and progress** to plan work and review completed sessions.
- **Usage analytics** to review focus and app-use patterns.
- **Motivation chat** backed by OpenRouter, with chat history stored locally in Room.
- **Firebase integration** for account and cloud features used by the app.

## Tech stack

- Kotlin and Jetpack Compose with Material 3
- MVVM / layered app structure
- Room for local persistence
- Firebase Authentication and Cloud Firestore
- Retrofit and OkHttp for the OpenRouter chat API

## Requirements

- Android Studio with Android SDK Platform 36
- JDK 17 (Android Studio's bundled runtime is recommended)
- A device or emulator running Android 7.0 (API 24) or newer
- A Firebase project and `app/google-services.json`
- An OpenRouter API key to use chat

The repository uses Gradle 8.13 and Android Gradle Plugin 8.13.1 through the Gradle wrapper and version catalog.

## Getting started

1. Clone this repository and open the project folder in Android Studio:

   ```powershell
   git clone https://github.com/XtremeseOISHEE/FlowOrbit.git
   cd FlowOrbit
   ```

2. In Android Studio, allow Gradle to sync and install any requested SDK components.
3. Make sure the Firebase configuration for your Firebase project is available at `app/google-services.json`. Use the matching Android app registration (`com.example.floworbit`).
4. Add your OpenRouter key to your **user-level** Gradle properties file, not to the project:

   Windows: `%USERPROFILE%\.gradle\gradle.properties`

   ```properties
   OPENROUTER_API_KEY=your_new_openrouter_key
   ```

   Create the `.gradle` directory or file if it does not exist, then restart Gradle sync. The app reads this Gradle property or the `OPENROUTER_API_KEY` environment variable. Never commit a real key. A key embedded in an Android app can be extracted from its APK; use a backend proxy before distributing the app publicly.

5. Select the `app` run configuration and run it on a device or emulator.

## Android access setup

Some features need permissions granted in Android Settings after installation:

- **App blocking:** enable FlowOrbit's requested Accessibility access and display-over-other-apps access.
- **Usage analytics:** grant Usage access.
- **Do Not Disturb:** grant Notification Policy access.
- **Notifications:** allow notifications when Android asks.

### Boring Mode on development devices

System-wide grayscale uses Android's secure settings, which ordinary apps cannot change without the privileged `WRITE_SECURE_SETTINGS` grant. For a development/demo device with USB debugging enabled, grant it once for that device using Android Debug Bridge:

```powershell
adb shell pm grant com.example.floworbit android.permission.WRITE_SECURE_SETTINGS
```

This grant is device-specific and is not a normal runtime permission. A consumer installation cannot grant it this way, so system-wide Boring Mode should be treated as a development/demo capability unless the app adopts a supported platform or managed-device approach.

## Project layout

```text
app/src/main/java/com/example/floworbit/
├── data/          # API clients, Room storage, repositories, profile and task data
├── domain/        # Domain models and business rules
├── navigation/    # Compose navigation
├── presentation/  # Compose screens and view models
├── service/       # Focus timer, app tracking/blocking and notifications
├── ui/theme/      # Colors, typography and Material theme
└── util/          # Shared app utilities
```

## Team

**Asma-Ul-Husna Oishee** and **Mursalin Sarkar Nayeem**  
Department of Computer Science and Engineering  
Jahangirnagar University