# TermuxIDE - Native Android APK Build Guide

This directory contains the 100% native Kotlin Android project for **TermuxIDE** (targeting Android SDK 34).

## How to Build and Install the APK

### Method 1: Build directly on Android device inside Termux (Offline)
Termux can compile and package Android APKs directly on your phone or tablet:
```bash
# 1. Install Java and Gradle in Termux:
pkg update && pkg install openjdk-17 gradle git -y

# 2. Navigate to this android directory:
cd TermuxIDE/android

# 3. Assemble the debug APK:
gradle assembleDebug

# 4. Install the generated APK on your device:
termux-open app/build/outputs/apk/debug/app-debug.apk
```

### Method 2: Open in Android Studio
1. Launch **Android Studio**.
2. Select **Open** and choose this `android/` directory.
3. Wait for Gradle sync to complete.
4. Go to **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**.
5. Once built, click **locate** or find it at:
   `app/build/outputs/apk/debug/app-debug.apk`
6. Transfer to your phone or tablet and tap to install!

### Method 3: Command Line (Linux, macOS, Windows)
```bash
./gradlew assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Required Termux Configuration
To allow TermuxIDE to communicate with Termux offline, enable the `RUN_COMMAND` permission in Termux:
1. Open Termux on your device.
2. In `~/.termux/termux.properties`, ensure:
   ```properties
   allow-external-apps = true
   ```
3. Grant TermuxIDE the `com.termux.permission.RUN_COMMAND` permission when prompted.
