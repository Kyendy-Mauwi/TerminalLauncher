# T3rm!n@l — Terminal Android Launcher

*Your phone. Your rules.*

> **Two builds live here.**
> * `app/` — the production **Kotlin + Jetpack Compose** launcher (default home screen, app
>   discovery, notifications, real system control).
> * `tty-expo/` — a **React Native / Expo SDK 58 test build** for trying the terminal UI and
>   command engine on a phone through Expo Go. Expo cannot be a home-screen launcher, so it is a
>   test harness, not a replacement. See [`tty-expo/README.md`](tty-expo/README.md).

T3rm!n@l turns an Android phone into a terminal-driven operating environment. It is a fully native
Android application (Kotlin + Jetpack Compose) that can be selected as the device's default home
screen and exposes real device state and real application launching through a command line.

No web wrapper, no mock data, no simulated system information.

---

## Features

| Area | What is implemented |
| --- | --- |
| Launcher | Registered for `MAIN`/`HOME`/`DEFAULT` and `MAIN`/`LAUNCHER`, launcher onboarding through the Android `ROLE_HOME` flow, application discovery, launching, favourites, recents (own launch log), search, aliases |
| Terminal engine | Quote-aware parser, 50+ commands with metadata, validation and usage errors, Levenshtein "did you mean" suggestions, tab completion, command history with up/down, bounded output buffer |
| System information | Battery (level, status, source, health, temperature), storage, memory, network transport/validation, device model, Android version, uptime, live monitor screen |
| Everyday functions | Store search, dialer, messages, contacts, calendar, clock, camera, document picker, wallpaper picker, volume control, brightness (with `WRITE_SETTINGS`), Wi-Fi/Bluetooth status |
| Notifications | Optional `NotificationListenerService`; shows app, title and age of active notifications, in memory only |
| Personalisation | 5 themes, 2 fonts, font size, cursor style, opacity, animation toggle, boot sequence, startup message, date/time formats, history limit, haptics, default screen, triple-tap quick controls, reset to defaults |
| Privacy | Offline-first, no account, no analytics, no cloud sync, no notification persistence, no shell execution, no root |
| Activation | Default home screen, app launcher shortcut, triple-tap overlay, Quick Settings tile |

---

## Build

### Requirements

* Android Studio (Ladybug or newer) **or** command line tools
* JDK 17
* Android SDK Platform 36 and Build-Tools 34+ (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 29`)

### Command line

```bash
# Debug APK
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Install on a connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Release APK (unsigned; configure your own signingConfig to publish)
./gradlew :app:assembleRelease
```

`local.properties` must contain your SDK path, e.g. `sdk.dir=C\:\\Android\\Sdk`. Android Studio
creates it automatically when you open the project.

### Android Studio

1. **File → Open** and select this directory.
2. Let Gradle sync (all dependencies are public: AndroidX, Hilt, Room, DataStore, WorkManager).
3. Run the `app` configuration on a device or emulator running Android 10+.

---

## Using TTY as the home screen

1. Launch TTY once.
2. Run `settings home`, or triple-tap the header and choose **SET TTY AS DEFAULT HOME**.
3. Android shows its own home-app picker (Role Manager). Approve it.

If you do not make TTY the default launcher, the app still works as a normal terminal app and the
header shows a "not the default home" banner. To return to your previous launcher, open Android
Settings → Apps → Default apps → Home app and pick the other launcher.

TTY never tries to bypass Android's launcher selection, and it never leaves the user without a home
screen: the back button does nothing while TTY is the default home.

---

## Commands

Type `help` for the live list, or `help <command>` for usage details. Aliases are accepted
(`launch` → `open`, `cls` → `clear`, `calc` → `calculator`, `?` → `help`, …).

**Navigation** `help` `clear` `home` `exit` `back` `about` `settings [panel]`

**Applications** `apps [filter]` `open [application]` `search [query]` `recent [n]`
`favorites [add|remove] [application]` `install [application]` `alias set [alias] [application]`
`alias remove [alias]`

**System** `sysinfo` `battery` `storage` `memory` `device` `uptime` `network` `wifi [settings]`
`bluetooth [settings]` `brightness [0-100]` `volume [0-100]` `notifications [clear]` `date` `time`

**Productivity** `notes [add|remove|clear]` `calculator [expression]` `calendar` `clock` `camera`
`files` `contacts` `messages` `phone` `wallpaper`

**Appearance** `theme [green|amber|cyan|matrix|mono]` `font [jetbrains|mono]` `fontsize [9-22]`
`opacity [40-100]` `cursor [block|bar|underline]` `animations [on|off]`

**Utility** `whoami` `hostname [name]` `history [n|clear]` `echo [text]` `version` `ping [host]`
`reboot`

Examples:

```
KYENDY@ANDROID:~$ open chrome
LAUNCHING CHROME...
APPLICATION STARTED SUCCESSFULLY.

KYENDY@ANDROID:~$ opn chrome
COMMAND NOT FOUND: opn

Did you mean:
  open chrome

KYENDY@ANDROID:~$ open
ERROR: Missing application name.

Usage: open [application]
```

### Deliberate limitations

* `reboot` explains that privilege escalation is required and does not attempt a restart.
* `install` opens the user's app store; Android forbids silent installation.
* Wi-Fi and Bluetooth cannot be toggled by ordinary apps; TTY opens the system panels instead.
* Recents are TTY's own launch log, because Android does not expose the system recents list.
* No shell execution, no root requests, no privileged system writes.

---

## Architecture

MVVM with a unidirectional data flow and a strict separation between the interpreter, the data
layer and Compose UI.

```
app/src/main/java/com/tty/launcher/
├── MainActivity.kt                  # single activity, edge-to-edge, permission routing
├── TTYApplication.kt                # Hilt app + daily maintenance worker scheduling
├── data/
│   ├── database/                    # Room entities, DAOs, database + migration
│   ├── datastore/SettingsRepository # DataStore preferences with safe defaults
│   └── repositories/                # history, favourites, aliases, notes, recents, apps
├── domain/
│   ├── models/                      # terminal, device and settings models
│   └── commands/                    # parser, catalog, suggestions, evaluator, dispatcher
├── presentation/
│   ├── terminal/                    # terminal screen, view model, quick controls
│   ├── settings/                    # settings screen + view model
│   ├── applications/                # app picker screen + view model
│   ├── system/                      # live system monitor
│   ├── calculator/                  # native calculator screen
│   ├── components/                  # reusable terminal widgets
│   └── theme/                       # palettes, typography, Compose theme
├── services/
│   ├── system/                      # device info, monitor, intents, volume, brightness, worker
│   ├── notifications/               # NotificationListenerService + in-memory store
│   └── quicksettings/               # Quick Settings tile
├── navigation/                      # NavHost and bottom bar
├── di/                              # Hilt modules
└── utils/                           # formatting helpers
```

The command flow is:

`TerminalScreen` → `TerminalViewModel.submit()` → `CommandParser` → `CommandDispatcher` (validates,
calls repositories/Android APIs) → `CommandOutcome` → rendered lines and optional `TerminalAction`
handled by the navigation host.

Persistence:

* **DataStore** — identity, theme, font, cursor, opacity, history limit, formats, animation and
  haptic preferences.
* **Room** — `command_history`, `favorites`, `app_aliases`, `notes`, `app_usage` (schema v2 with an
  explicit `1 → 2` migration, exported schemas in `app/schemas`).

Performance and lifecycle:

* `LazyColumn` output capped at 600 lines; history capped by the user's limit (25–1000).
* System state is event-driven (`ACTION_BATTERY_CHANGED`, `NetworkCallback`) with a slow heartbeat,
  exposed through a `StateFlow` that stops when no screen is subscribed.
* Logcat spam avoided; every Android call is wrapped so a restricted OEM build degrades to an
  "UNKNOWN"/explanation instead of a crash.

---

## Permissions

| Permission | Why | Runtime? |
| --- | --- | --- |
| `INTERNET` | `ping` TCP probe and app-store links only | No |
| `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Network status | No |
| `VIBRATE` | Optional haptics | No |
| `WRITE_SETTINGS` | `brightness <0-100>` only | Yes — user must approve in system settings |
| `BLUETOOTH_CONNECT` | Read Bluetooth on/off state (Android 12+) | Yes |
| Notification listener access | Show recent notification titles | Yes — system settings only |
| Default home role | Become the launcher | Yes — Android role picker |

Package visibility uses a `queries` block for launcher apps and the specific intents TTY opens;
`QUERY_ALL_PACKAGES` is deliberately not requested.

---

## Testing

```bash
# Unit tests: parser, suggestions, evaluator, app resolution, catalog, formatting, models
./gradlew :app:testDebugUnitTest

# Instrumentation tests (connected device/emulator): Room persistence + Compose terminal UI
./gradlew :app:connectedDebugAndroidTest
```

Instrumentation coverage includes database persistence/migration-facing DAO behaviour and terminal
rendering, input handling and boot skipping. Manual verification is still required for
device-specific behaviour (launcher role, notification access, brightness writes).

Verified locally on this machine (Windows, JDK 17, AGP 8.13.2, Gradle 8.14.3, compileSdk 36):

* `./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL**, APK at
  `app/build/outputs/apk/debug/app-debug.apk` (~33 MB debug build, JetBrains Mono included).
* `./gradlew :app:testDebugUnitTest` — **55 tests, 0 failures**.
* `./gradlew :app:assembleDebugAndroidTest` — instrumentation sources compile and the test APK
  builds (`app/build/outputs/apk/androidTest/debug/`).
* `./gradlew :app:lintDebug` — **0 errors** (46 warnings, dominated by “newer dependency version
  available” hints; no correctness or security errors).

Not verified locally: no physical device or emulator AVD was available, so the instrumentation
suite was **not executed**, and the following were not exercised on real hardware: default-home
selection, notification listener binding, `WRITE_SETTINGS` brightness writes, Bluetooth state
reads, keyboard/IME behaviour, rotation, and OEM-specific restrictions. Those require a device and
the user's explicit permission grants.

---

## Privacy statement

TTY stores everything locally. There is no account, no telemetry and no network sync. Command
history, notes, favourites and aliases live in the app's private storage. Notification content is
kept in memory only while notification access is granted, and is never logged or transmitted. The
only outbound requests are the user-triggered `ping` probe and store/wallpaper intents handled by
other apps.

## Fonts

JetBrains Mono is bundled under the SIL Open Font License 1.1
(https://github.com/JetBrains/JetBrainsMono).
