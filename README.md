# TimetableTracker 📅

A native Android app (Kotlin) that monitors the UPES student portal for timetable changes and notifies you before classes get rescheduled, added, or canceled.

### 🔋 Battery & Background Philosophy
**TimetableTracker does not run continuously in the background.**
Instead of using foreground services or holding CPU wake locks, it uses Android's `AlarmManager.setExactAndAllowWhileIdle()` to wake up only at your chosen schedule (e.g. once a day at 8:00 AM).
- **Between alarms:** The app process is 100% dead. Zero CPU usage, zero RAM, zero battery drain.
- **When alarm fires:** Android awakens a `BroadcastReceiver` briefly (~10s) which dispatches a one-shot `WorkManager` job to fetch and compare the timetable.
- **After check:** Snapshots and history are updated, notifications/WhatsApp intents are fired if changes are detected, the next alarm is queued with the OS, and the app process dies immediately.
- **Reboot survival:** A `BOOT_COMPLETED` receiver re-registers your exact alarm after device reboot.

---

## 🛠️ Requirements to Build on Another Machine

To replicate and build this project on any other machine (Linux, macOS, or Windows):

1. **Java Development Kit (JDK 17 or JDK 21):**
   - *Important:* Do **not** use JDK 22+ (like JDK 27), as Android Gradle Plugin's internal bytecode packaging (`jlink`) currently requires JDK 17 or 21.
2. **Android SDK:**
   - Platform: `android-35` (Android 15)
   - Build Tools: `34.0.0` or higher
3. **Android Device or Emulator:**
   - Minimum Android 12 (API 31)

---

## 🚀 Step-by-Step Rebuild Guide

### 1. Clone the Repository
```bash
git clone <repository_url> TimeTableTracker
cd TimeTableTracker
```

### 2. Install JDK 17

- **Arch Linux:**
  ```bash
  sudo pacman -S jdk17-openjdk
  ```
- **Ubuntu / Debian:**
  ```bash
  sudo apt update && sudo apt install -y openjdk-17-jdk
  ```
- **macOS (via Homebrew):**
  ```bash
  brew install openjdk@17
  ```
- **Windows:**
  Download and install OpenJDK 17 (e.g., from Eclipse Temurin or Microsoft).

### 3. Setup Android SDK

#### Option A: Command-Line (CLI Only — No Android Studio Needed)
If building on a headless server or without Android Studio:
1. Ensure the `cmdline-tools` package is installed.
2. Accept the licenses and install platforms & build tools:
   ```bash
   sdkmanager "platforms;android-35" "build-tools;34.0.0" "platform-tools"
   ```
3. Set your `local.properties` file in the project root:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ```
   *(e.g., `sdk.dir=/home/username/Android/Sdk` on Linux or `sdk.dir=C:\\Users\\username\\AppData\\Local\\Android\\Sdk` on Windows)*

#### Option B: Using Android Studio
1. Open Android Studio and choose **Open Project**.
2. Select the `TimeTableTracker` folder.
3. Android Studio will automatically read `settings.gradle.kts` and prompt to install any missing SDK platforms (`platforms;android-35`) or build-tools.

### 4. Build the APK

Run the Gradle wrapper:

- **Linux / macOS:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Windows:**
  ```cmd
  gradlew.bat assembleDebug
  ```

Upon success, your debug APK will be created at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Installing & Testing on Your Phone

### 1. Install via ADB
Enable **Developer Options** and **USB Debugging** on your phone, connect it via USB, and run:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
*(Or transfer `app-debug.apk` directly to your phone and tap to sideload).*

### 2. Essential Permissions on Phone
1. **Alarms & Reminders:**
   Go to `Settings > Apps > TimetableTracker > Alarms & reminders` and toggle **Allow setting alarms** to `ON`.
2. **Notifications:**
   Allow notification permissions when the app launches (Android 13+).
3. **Battery Optimization:**
   Go to `Settings > Apps > TimetableTracker > App battery usage` and select **Unrestricted**. This prevents aggressive OEM task killers (such as Nothing OS / MIUI) from killing the worker before the network request finishes.

### 3. Usage Flow
1. Open the app and head to the **Settings** tab.
2. Enter your UPES student email and password, then tap **Login & Verify**.
3. Select your preferred daily check time (default is 8:00 AM) and tap **Save Settings**.
4. Go to the **Home** tab and tap **Check Now** to run an immediate baseline check.
5. Review the **History** tab for past snapshots and diff logs.

---

## 🧪 Testing Alarms via ADB (Simulate Triggers)

You don't have to wait for the daily scheduled time to verify background execution. You can fire triggers directly with ADB:

- **Trigger the alarm broadcast manually:**
  ```bash
  adb shell am broadcast -a com.timetabletracker.ACTION_CHECK_TIMETABLE -n com.timetabletracker/.scheduler.AlarmReceiver
  ```
- **Simulate device reboot to test alarm re-scheduling:**
  ```bash
  adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -n com.timetabletracker/.scheduler.BootReceiver
  ```
- **Stream live background execution logs:**
  ```bash
  adb logcat -s TimetableFetchWorker:D AlarmReceiver:D AlarmScheduler:D TimetableRepository:D
  ```

---

## 📂 Project Structure

```
TimeTableTracker/
├── app/
│   ├── build.gradle.kts           # App-level dependencies & Android config
│   ├── src/main/
│   │   ├── AndroidManifest.xml    # Permissions, receivers, activities
│   │   ├── java/com/timetabletracker/
│   │   │   ├── MainActivity.kt    # Root single activity with bottom navigation
│   │   │   ├── data/
│   │   │   │   ├── api/           # Retrofit service, client & models
│   │   │   │   ├── local/         # EncryptedSharedPreferences & snapshot storage
│   │   │   │   └── repository/    # 3-step auth & timetable fetch repository
│   │   │   ├── domain/            # Timetable diffing & comparison engine
│   │   │   ├── scheduler/         # AlarmManager & boot receivers
│   │   │   ├── worker/            # WorkManager CoroutineWorker for portal fetch
│   │   │   ├── notification/      # System notifications & WhatsApp intent
│   │   │   └── ui/                # ViewModel, adapters & fragments
│   │   └── res/                   # Layouts, vector drawables, Material 3 themes
├── gradle/
│   ├── libs.versions.toml         # Version catalog
│   └── wrapper/                   # Gradle wrapper configuration
├── build.gradle.kts               # Root build script
├── gradle.properties              # JVM options & compiler flags
└── settings.gradle.kts            # Project repositories & modules
```
