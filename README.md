# GramaUrja – Smart Energy Monitoring App (Android)

GramaUrja is an Android app that shows village electricity supply status by zone and alerts users when power status changes. It was built during my Android Application Development internship at MindMatrix.io (Jan–May 2026).

## Features
- **Zone selection** – users pick their village/zone to follow
- **Live power status** – shows whether power is ON/OFF for the selected zone, synced in real time from Firebase Firestore
- **Push notifications** – Firebase Cloud Messaging alerts when the power status changes
- **Secure sign-in** – Firebase Authentication
- **Modern UI** – built with Jetpack Compose

## Tech stack
Kotlin · Jetpack Compose · Firebase Authentication · Cloud Firestore · Firebase Cloud Messaging · Gradle (Kotlin DSL)

## Project structure
```
app/src/main/java/com/gramaurja/app/
├── SplashActivity.kt            # Launch screen
├── ZoneSelectionActivity.kt     # Choose zone
├── PowerStatusActivity.kt       # Live power status screen
├── PowerStatus.kt               # Data model
├── MyFirebaseMessagingService.kt# Push notifications (FCM)
├── GramaUrjaApp.kt / MainActivity.kt
└── ui/theme/                    # Compose theme
```

## How to run
1. Clone the repo and open it in **Android Studio** (latest stable).
2. Create a Firebase project, add an Android app with package `com.gramaurja.app`, and download your own `google-services.json` into `app/`.
3. Enable **Authentication**, **Cloud Firestore** and **Cloud Messaging** in the Firebase console.
4. Sync Gradle and run on an emulator or a physical device.

## Security notes
- Use your own Firebase project: place your `google-services.json` in `app/` locally and do not commit it.

## My role
Designed and built the Android client: UI screens in Jetpack Compose, Firebase Authentication flow, real-time Firestore sync and FCM notifications.
