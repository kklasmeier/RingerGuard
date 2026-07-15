# RingerGuard — Android App Implementation Spec

## Overview

RingerGuard is a lightweight Android background service app that ensures your phone ringer is always active and at full volume unless you have explicitly silenced it within a configurable grace period. It runs persistently despite battery saver mode and is controlled via a home screen widget and a full in-app UI.

---

## Core Problem

The Samsung S23 (Android) frequently ends up with the ringer silenced or volume lowered — either by the user during a meeting/call, by Do Not Disturb, or accidentally. The user misses incoming calls as a result. The app solves this by automatically restoring the ringer after a grace period expires, unless the user intentionally chose to silence it for longer.

---

## Platform & Language

- **Language:** Kotlin
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** API 34 (Android 14)
- **Build system:** Gradle (Android Studio)
- **Architecture:** Single-module app, no external dependencies required

---

## Permissions Required

Declare all of the following in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
```

---

## App State Model

All persistent state is stored in `SharedPreferences` (key: `"ringer_guard_prefs"`).

| Key | Type | Description |
|-----|------|-------------|
| `silence_mode` | String | `"none"` / `"timed"` / `"indefinite"` |
| `grace_until_ms` | Long | Epoch ms when grace period expires (0 if none) |
| `grace_duration_min` | Int | User-configured grace period in minutes (default: 60) |
| `restore_volume_level` | Int | Volume level to restore to (default: 15 = max) |
| `check_interval_min` | Int | How often the service checks state (default: 5) |
| `restore_disable_dnd` | Boolean | Whether to disable DND on restore (default: true) |
| `restore_disable_vibrate` | Boolean | Whether to disable vibrate-only on restore (default: true) |
| `grace_on_volume_change` | Boolean | Volume change triggers grace period (default: true) |
| `grace_on_call_dismiss` | Boolean | Call dismiss triggers grace period (default: true) |
| `quiet_hours_enabled` | Boolean | Whether quiet hours are active (default: false) |
| `quiet_start_hour` | Int | Quiet hours start (24h, default: 22) |
| `quiet_start_min` | Int | Default: 0 |
| `quiet_end_hour` | Int | Quiet hours end (24h, default: 7) |
| `quiet_end_min` | Int | Default: 0 |
| `quiet_days` | String | Comma-separated day ints: "2,3,4,5,6" = Mon–Fri |
| `last_restored_ms` | Long | Epoch ms of last auto-restore event |
| `service_start_ms` | Long | Epoch ms when service last started |

---

## Components

### 1. `RingerService.kt` — Foreground Service

The core of the app. Runs 24/7 as a foreground service.

**Startup behavior:**
- Start as `FOREGROUND_SERVICE_TYPE_PHONE_CALL`
- Show a persistent notification (see Notification spec below)
- Schedule the first `AlarmManager` check immediately

**Check logic (runs every N minutes via AlarmManager):**
```
1. Read silence_mode from SharedPreferences
2. If silence_mode == "indefinite" → do nothing, reschedule next check
3. If silence_mode == "timed":
   a. If current time < grace_until_ms → do nothing, reschedule
   b. If current time >= grace_until_ms → call restoreRinger(), set silence_mode = "none"
4. If silence_mode == "none":
   a. Check if currently in quiet hours → if yes, do nothing
   b. Check AudioManager ringer volume
   c. If volume < restore_volume_level OR DND active OR vibrate-only → call restoreRinger()
5. Reschedule next AlarmManager check
```

**`restoreRinger()` function:**
```
1. Set AudioManager STREAM_RING volume to restore_volume_level
2. If restore_disable_dnd: set NotificationManager to INTERRUPTION_FILTER_ALL
3. If restore_disable_vibrate: set AudioManager ringer mode to RINGER_MODE_NORMAL
4. Update last_restored_ms in SharedPreferences
5. Log event to event log (see Event Log spec)
6. Update widget
7. Update foreground notification
```

**Scheduling:**
- Use `AlarmManager.setExactAndAllowWhileIdle()` — this is mandatory for battery saver compatibility
- Use a `PendingIntent` targeting `AlarmReceiver`
- Reschedule immediately after each check completes

**Service restart:**
- Override `onStartCommand` to return `START_STICKY`
- Register `BootReceiver` to restart on device boot

---

### 2. `AlarmReceiver.kt` — BroadcastReceiver

Receives the periodic `AlarmManager` intent.

- Acquires a `WakeLock` for the duration of the check
- Triggers `RingerService` check logic
- Releases `WakeLock` when done
- Must complete in under 10 seconds

---

### 3. `VolumeChangeReceiver.kt` — BroadcastReceiver

Listens for `AudioManager.VOLUME_CHANGED_ACTION`.

**Logic:**
```
1. Check if the changed stream is STREAM_RING
2. Check if new volume < previous volume (user lowered it)
3. If grace_on_volume_change is true → start grace period
4. Log event
```

Register dynamically in `RingerService` (not in manifest) — unregister on service destroy.

---

### 4. `CallStateReceiver.kt` — BroadcastReceiver

Listens for `TelephonyManager.ACTION_PHONE_STATE_CHANGED`.

**Logic:**
```
1. Watch for transition: RINGING → IDLE (call dismissed without answering)
   OR RINGING → OFFHOOK then quickly OFFHOOK → IDLE (call silenced)
2. If grace_on_call_dismiss is true → start grace period
3. Log event with trigger = "call dismissed"
```

Register in manifest with `READ_PHONE_STATE` permission.

**Grace period start helper:**
```kotlin
fun startGracePeriod(context: Context, triggerReason: String) {
    val prefs = context.getSharedPreferences("ringer_guard_prefs", Context.MODE_PRIVATE)
    val durationMin = prefs.getInt("grace_duration_min", 60)
    val expiresAt = System.currentTimeMillis() + (durationMin * 60 * 1000L)
    prefs.edit()
        .putString("silence_mode", "timed")
        .putLong("grace_until_ms", expiresAt)
        .apply()
    EventLogger.log(context, "Silenced — $triggerReason", "grace: ${durationMin}min")
    RingerWidget.update(context)
}
```

---

### 5. `BootReceiver.kt` — BroadcastReceiver

Receives `BOOT_COMPLETED`.

- Starts `RingerService` via `startForegroundService()`
- Logs "Service restarted after boot"

Register in manifest:
```xml
<receiver android:name=".BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

### 6. `RingerWidget.kt` — AppWidgetProvider

Home screen widget. Updates every 60 seconds via `AppWidgetManager`.

**Widget layout (`ringer_widget_layout.xml`):**
- App name label (small, top left)
- Status badge (top right): "🟢 Ringing" / "🔕 Silenced" / "∞ Indefinite"
- Countdown display (center): `47:23` or `∞` or hidden when ringing
- Countdown sublabel: "minutes remaining" or "silence until re-enabled"
- Three buttons (bottom row):
  - `🔕 1 Hour` — triggers `ACTION_SILENCE_HOUR`
  - `∞ Indefinite` — triggers `ACTION_SILENCE_INDEFINITE`
  - `🔔 Ring Now` — triggers `ACTION_RING_NOW`
- Buttons that aren't applicable in current state should appear visually dimmed (alpha 0.4)

**Widget actions (PendingIntents → `WidgetActionReceiver`):**

| Action constant | Behavior |
|-----------------|----------|
| `ACTION_SILENCE_HOUR` | Set silence_mode="timed", grace_until = now + grace_duration_min |
| `ACTION_SILENCE_INDEFINITE` | Set silence_mode="indefinite" |
| `ACTION_RING_NOW` | Set silence_mode="none", call restoreRinger() immediately |

**Widget update logic:**
- Called after every state change and every minute
- Read current state from SharedPreferences
- Compute remaining minutes: `(grace_until_ms - now) / 60000`
- Format as `MM:SS` countdown
- Push update via `AppWidgetManager.updateAppWidget()`

Register in manifest:
```xml
<receiver android:name=".RingerWidget" android:exported="true">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data android:name="android.appwidget.provider"
               android:resource="@xml/ringer_widget_info" />
</receiver>
```

`ringer_widget_info.xml`:
```xml
<appwidget-provider
    android:minWidth="250dp"
    android:minHeight="110dp"
    android:updatePeriodMillis="60000"
    android:initialLayout="@layout/ringer_widget_layout"
    android:resizeMode="horizontal|vertical" />
```

---

### 7. `WidgetActionReceiver.kt` — BroadcastReceiver

Receives button taps from the widget.

- Handles `ACTION_SILENCE_HOUR`, `ACTION_SILENCE_INDEFINITE`, `ACTION_RING_NOW`
- Updates SharedPreferences
- Calls `restoreRinger()` if Ring Now
- Calls `RingerWidget.update(context)` after any change
- Logs event

---

### 8. `EventLogger.kt` — Utility class

Simple append-only event log stored as JSON array in SharedPreferences or a flat file.

**Log entry structure:**
```kotlin
data class LogEntry(
    val timestampMs: Long,
    val title: String,       // e.g. "Ringer restored to max volume"
    val detail: String,      // e.g. "Grace period expired"
    val type: String         // "restored" | "silenced" | "manual" | "boot"
)
```

- Store last 200 entries max, drop oldest when full
- Persist as JSON array to `files/event_log.json`
- Expose `getAll(): List<LogEntry>` and `log(title, detail, type)`

---

### 9. `MainActivity.kt` — Main App UI

Four-tab bottom navigation:

#### Tab 1: Home
- **Status card** (full-width, color-coded):
  - Green card: "🔔 Ringing — Your ringer is active"
  - Orange card: "🔕 Silenced — `47:23` remaining"
  - Purple card: "∞ Indefinite — Silence until re-enabled"
- **Quick Actions:**
  - `🔕 1 Hour` button
  - `∞ Indefinite` button
  - `🔔 Ring Now` button (full width, prominent)
  - `⏱️ Custom Duration` button → shows a number picker dialog (minutes)
- **Live Status panel:**
  - Current ringer volume (`X / 15`)
  - Do Not Disturb: On/Off
  - Vibrate only: On/Off
  - Service running: Yes/No with color dot
- Refresh live status every 5 seconds while app is in foreground

#### Tab 2: Log
- Scrollable list of `LogEntry` items grouped by date
- Each item: colored dot (by type) + title + detail + time
- "Clear Log" button (top right, confirm dialog)
- **7-day summary card** at bottom:
  - Times auto-restored
  - Grace periods triggered
  - Estimated missed calls prevented (= times auto-restored)

#### Tab 3: Schedule
- **Quiet Hours card:**
  - Toggle: Enable Quiet Hours
  - Time pickers: Start time / End time
- **Active Days card:**
  - 7 pill buttons for Su/Mo/Tu/We/Th/Fr/Sa (tap to toggle)
- **Exceptions card:**
  - Toggle: Always ring for starred contacts *(note: requires Contacts permission — ask at runtime)*
  - Toggle: Repeat caller override (ring if same number calls twice within 3 min)
- **Current status row:** "Right now: Active hours" + "Next quiet period: 10:00 PM tonight"

#### Tab 4: Settings
- **Grace Period slider:** 15 / 30 / 60 / 120 / 240 minutes
- **Volume Restore Level slider:** 1–15 (max)
- **Check Interval selector:** 5 / 10 / 15 / 30 min (segmented button)
- **Restore Options toggles:**
  - Disable Do Not Disturb on restore
  - Disable Vibrate-only on restore
  - Start grace period on volume change
  - Start grace period on call dismiss
- **System Health card:**
  - Battery optimization exemption: Active / Inactive (tap to fix)
  - Service uptime
  - Last auto-restore time
  - Button: "Re-request Battery Exemption" (opens system dialog)

---

## Notification Spec

The foreground service requires a persistent notification.

**Channel:** `"ringer_guard_channel"` — importance LOW (silent, no sound)

**Notification content (dynamic):**
| State | Title | Text |
|-------|-------|------|
| Ringing | 🔔 RingerGuard Active | Ringer will be protected |
| Silenced (timed) | 🔕 Silenced — 47 min remaining | Tap to open app |
| Indefinite | ∞ Silenced Indefinitely | Tap to re-enable |

**Actions on notification:**
- "Ring Now" quick action button (no need to open app)

---

## Quiet Hours Logic

```
isInQuietHours(): Boolean
  1. If quiet_hours_enabled == false → return false
  2. Check current day of week against quiet_days
  3. If today not in quiet_days → return false
  4. Compare current time to [quiet_start, quiet_end] window
     (handle midnight crossing: e.g. 22:00–07:00)
  5. Return true if within window
```

If `isInQuietHours()` is true, skip the auto-restore check entirely. Do not log a skipped check.

---

## Battery Saver Survival Strategy

1. **Foreground service** declared as `FOREGROUND_SERVICE_TYPE_PHONE_CALL`
2. **`AlarmManager.setExactAndAllowWhileIdle()`** for all scheduled checks — regular `Handler` and `WorkManager` are throttled in battery saver
3. **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** — prompt user on first launch via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent
4. **`START_STICKY`** — service restarts itself if killed
5. **`BOOT_COMPLETED`** — restarts after reboot
6. **WakeLock** held only for the duration of the check (~50ms), released immediately after

**On first launch**, if battery optimization exemption is not granted:
- Show a prominent banner on the Home tab
- Prompt system dialog automatically
- Re-check exemption status every time the app is foregrounded

**Check in Settings tab:** `powerManager.isIgnoringBatteryOptimizations(packageName)`

---

## File Structure

```
app/src/main/
├── java/com/yourname/ringerguard/
│   ├── MainActivity.kt
│   ├── RingerService.kt
│   ├── AlarmReceiver.kt
│   ├── BootReceiver.kt
│   ├── VolumeChangeReceiver.kt
│   ├── CallStateReceiver.kt
│   ├── WidgetActionReceiver.kt
│   ├── RingerWidget.kt
│   ├── EventLogger.kt
│   ├── GracePeriodHelper.kt
│   ├── QuietHoursHelper.kt
│   └── ui/
│       ├── HomeFragment.kt
│       ├── LogFragment.kt
│       ├── ScheduleFragment.kt
│       └── SettingsFragment.kt
├── res/
│   ├── layout/
│   │   ├── activity_main.xml
│   │   ├── fragment_home.xml
│   │   ├── fragment_log.xml
│   │   ├── fragment_schedule.xml
│   │   ├── fragment_settings.xml
│   │   └── ringer_widget_layout.xml
│   ├── xml/
│   │   └── ringer_widget_info.xml
│   └── values/
│       ├── strings.xml
│       └── colors.xml
└── AndroidManifest.xml
```

---

## Key Implementation Notes for AI Coding Session

1. **Do not use `WorkManager`** — it is throttled in battery saver. Use `AlarmManager.setExactAndAllowWhileIdle()` exclusively for scheduling.

2. **`NotificationManager.setInterruptionFilter()`** requires the app to be granted Do Not Disturb access. Add a check on startup and prompt the user to grant it via `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` if not granted.

3. **Volume listener** must be registered as a `ContentObserver` on `Settings.System.VOLUME_SETTINGS` OR via `AudioManager` volume change broadcast — use the broadcast approach for reliability.

4. **Widget countdown** must use a `PendingIntent` with `AlarmManager` for the 1-minute tick — `updatePeriodMillis` in the widget provider XML is throttled by the system and unreliable.

5. **Samsung-specific:** Samsung adds an extra layer of battery management ("Adaptive Battery" / "Sleeping apps"). The battery optimization exemption dialog handles this, but warn the user in the UI that they may also need to go to Settings → Battery → Background usage limits and set the app to "Unrestricted."

6. **SharedPreferences** should use `apply()` (async) not `commit()` (sync on main thread).

7. **All UI updates from service** must be posted to the main thread via `Handler(Looper.getMainLooper()).post {}`.

8. **Repeat caller detection** (Schedule tab exception): store last incoming number + timestamp in SharedPreferences; compare on each `RINGING` state.

9. **Target color scheme** (dark theme): green `#34d399`, orange `#fb923c`, purple `#a78bfa`, blue `#60a5fa`, background `#0d1520`, card background `rgba(255,255,255,0.04)`.

10. **Package name suggestion:** `com.ringerguard.app`

---

## First Launch Flow

```
1. Show MainActivity
2. Check MODIFY_AUDIO_SETTINGS permission → request if missing
3. Check READ_PHONE_STATE permission → request if missing
4. Check DND access (NotificationManager.isNotificationPolicyAccessGranted()) → prompt if missing
5. Check battery optimization exemption → show system dialog if missing
6. Start RingerService via startForegroundService()
7. Show Home tab
```

---

## Out of Scope (for now)

- Cloud sync or backup of settings
- Multiple profiles
- Tasker/automation integration
- Lock screen widget
- Wear OS companion
