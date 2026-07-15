# RingerGuard

Automatically restores your Android ringer after meetings, accidental volume changes, or Do Not Disturb — unless you explicitly chose to silence it.

**Developer:** Kevin Klasmeier  
**Repository:** [github.com/kklasmeier/RingerGuard](https://github.com/kklasmeier/RingerGuard)

## Problem

Phones (especially Samsung devices) frequently end up silenced or quiet after meetings, calls, or accidental button presses. RingerGuard runs a lightweight background service that restores your ringer after a configurable grace period.

## Features (v1)

- Persistent foreground service with battery-saver-compatible scheduling
- Home screen widget (silence / ring now / countdown)
- Grace period after volume lowered or call dismissed (optional)
- Quiet hours schedule
- Event log with 7-day summary
- Do Not Disturb and vibrate-only restore (with permission)
- Play Store–ready architecture (`specialUse` foreground service, optional phone permission)

## Project structure

```
RingerGuard/
├── android/                 # Android Studio / Gradle project
├── RingerGuard_SPEC.md      # Implementation spec (Play Store edition)
├── RingerGuard_SPEC.backup.md
└── ringer_ui_mockup.html
```

## Build & install

1. Open the `android/` folder in Android Studio
2. Sync Gradle and connect your device (or use an emulator)
3. Run the **app** configuration, or from terminal:

```bash
cd android
gradlew.bat assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## First-run setup

On first launch, RingerGuard will prompt for:

1. **Battery optimization exemption** — required for reliable background operation on Samsung devices
2. **Exact alarm permission** (Android 12+) — required for scheduled checks in battery saver
3. **Do Not Disturb access** — only if "Disable DND on restore" is enabled (default: on)
4. **Phone state permission** — optional, only if you enable "grace period on call dismiss"

Also set **Settings → Battery → Unrestricted** on Samsung devices.

## Privacy

RingerGuard collects no data. All settings and logs stay on your device. No network access.

## License

Copyright © 2026 Kevin Klasmeier. All rights reserved.
