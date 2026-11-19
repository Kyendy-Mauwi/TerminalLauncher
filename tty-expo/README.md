# T3rm!n@l — Expo test build

*Your phone. Your rules.*

A React Native (Expo) port of the TTY terminal launcher, built so the interface and command engine
can be tested quickly on a phone through **Expo Go** before shipping the native Kotlin app.

This is a **test build, not a launcher**. Android only allows an app to become the home screen
through a native `HOME` intent filter and the Role Manager flow; Expo Go cannot register those.
The production launcher lives in the repository root (`app/`, Kotlin + Jetpack Compose).

---

## Run it

The project targets **Expo SDK 58**, whose CLI needs Node 22+. This machine has Node 20, so a
project-local Node 22 toolchain is used (`.tools/node22`, gitignored).

```bash
# from the repository root
export PATH="$PWD/.tools/node22:$PATH"

cd tty-expo
npm install          # already installed
npx expo start
```

Then scan the QR code with **Expo Go** on the phone (Android or iOS) — both must be on the same
network. Useful flags:

```bash
npx expo start --tunnel    # different networks
npx expo start --web       # browser preview
npx tsc --noEmit           # typecheck
npx expo export --platform android   # verify the release bundle builds
```

If you use your own Node 22+ installation, delete nothing — just make sure `node -v` is 22.13 or
newer before running Expo.

---

## Full app list needs the development build

Expo Go blocks `PackageManager`, so it cannot enumerate installed apps. The **APPS** screen detects the
local native module `modules/app-list` and shows **every launchable app on the device** when it is
present; inside Expo Go it falls back to the curated catalogue.

Build and install the development build once (phone connected over USB with debugging enabled):

```bash
export PATH="$PWD/../.tools/node22:$PATH"
npx expo run:android          # prebuild + build + install, starts Metro
```

After that, launch **T3rm!n@l** from the phone instead of scanning the QR. If you only want the APK:

```bash
cd android && ./gradlew assembleDebug
# android/app/build/outputs/apk/debug/app-debug.apk
```

A debug build loads JavaScript from Metro, so keep `npx expo start` running and forward the port:
`adb reverse tcp:8081 tcp:8081`.

## What works in this build

| Area | Status |
| --- | --- |
| Terminal engine | Quote-aware parser, 50 commands with usage/suggestions, tab completion, ↑/↓ history, bounded output |
| Themes | Green / amber / cyan / matrix / mono palettes, cursor styles, font size, opacity, animations toggle |
| Persistence | Settings, command history, notes, favourites and aliases in `AsyncStorage` (offline) |
| Device info | Battery (`expo-battery`), network + IP (`expo-network`), model/OS (`expo-device`), app version (`expo-application`), disk (`expo-file-system`), uptime (`expo-device`) |
| Applications | Full installed-app list from the native `AppList` module (development build) with live filter, favourites and direct launch; curated catalogue with deep-link fallbacks inside Expo Go |
| System panels | Wi-Fi, Bluetooth, display, sound, battery, storage, apps, location, notification access, home settings |
| Productivity | Notes, calculator (safe evaluator), calendar, clock, camera, document picker, contacts, messages, dialer, wallpaper |
| Extras | Triple-tap quick controls, calculator screen, apps screen with favourites, live system screen, `ping` HTTPS latency probe |

## What Expo Go cannot do (deliberately not faked)

| Native ability | Why | Where it works |
| --- | --- | --- |
| Default home screen / launcher role | Needs a native `HOME` intent filter + Role Manager | Kotlin app |
| List every installed app | No `PackageManager` access in Expo Go | Kotlin app |
| Read notifications | Needs `NotificationListenerService` | Kotlin app |
| Toggle Wi-Fi / Bluetooth | Android forbids app-level toggling | Opens system panels |
| Change system volume / ringer | No Expo audio-policy module in Go | Opens sound settings |
| System brightness | Expo only changes the app window brightness | `brightness` command is window-scoped |
| Reboot / shell / root | Privileged access | `reboot` explains why |

Commands that hit these limits print a clear explanation instead of pretending to succeed.

---

## Commands

Same vocabulary as the native build: `help`, `apps`, `open chrome`, `search`, `recent`,
`favorites`, `alias`, `install`, `sysinfo`, `battery`, `storage`, `memory`, `device`, `uptime`,
`network`, `wifi`, `bluetooth`, `brightness`, `volume`, `notifications`, `date`, `time`, `notes`,
`calculator`, `calendar`, `clock`, `camera`, `files`, `contacts`, `messages`, `phone`, `wallpaper`,
`theme`, `font`, `fontsize`, `opacity`, `cursor`, `animations`, `whoami`, `hostname`, `history`,
`echo`, `version`, `ping`, `reboot`.

```text
KYENDY@ANDROID:~$ open chrome
LAUNCHING CHROME...
APPLICATION STARTED.
Chrome opened through its deep link.

KYENDY@ANDROID:~$ opn chrome
COMMAND NOT FOUND: opn

Did you mean:
  open chrome
```

---

## Structure

```
tty-expo/
├── App.tsx                  # app shell, tabs, persisted settings
├── index.ts                 # Expo entry
├── app.json                 # name, scheme, package, dark theme
└── src/
    ├── types.ts             # settings, themes, line styles
    ├── parser.ts            # quote-aware command parser
    ├── catalog.ts           # command metadata + help grouping
    ├── suggestions.ts       # Levenshtein suggestions
    ├── evaluator.ts         # safe arithmetic evaluator
    ├── commands.ts          # Expo command interpreter
    ├── storage.ts           # AsyncStorage persistence
    ├── device.ts            # battery/network/device/brightness/intents
    ├── apps.ts              # app catalogue + deep-link launching
    ├── format.ts            # bytes, uptime, date patterns
    ├── useTerminal.ts       # boot sequence, history, submission
    ├── useStatus.ts         # lifecycle-aware device state polling
    ├── ui.tsx               # terminal-styled primitives
    ├── QuickControls.tsx    # triple-tap panel
    └── screens/             # terminal, apps, system, settings, calculator
```

---

## Verification

* `npx tsc --noEmit` — **passes**.
* `npx expo export --platform android` — bundles for Android without errors (see `dist/`).
* Not verified: a live run inside Expo Go on a physical phone, deep-link availability for each
  third-party app, and OEM-specific intent behaviour. Scan the QR code to check these.

## Privacy

Everything is local: no account, no analytics, no telemetry. `AsyncStorage` holds only terminal
preferences and user content. Network access happens only for `ping` and store/wallpaper intents.
