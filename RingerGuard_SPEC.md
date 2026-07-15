# RingerGuard — Android App Implementation Spec

> **Distribution:** Designed for personal use first, architected for Google Play Store compliance.
> **Backup:** Original spec preserved at `RingerGuard_SPEC.backup.md`.

---

## Overview

RingerGuard is a lightweight Android background service app that ensures your phone ringer is always active and at full volume unless you have explicitly silenced it within a configurable grace period. It runs persistently despite battery saver mode and is controlled via a home screen widget and a full in-app UI.

**Privacy principle:** All data stays on-device. No network calls, no analytics, no account system. This simplifies Play Store Data Safety declarations and user trust.

---

## Core Problem

The Samsung S23 (Android) frequently ends up with the ringer silenced or volume lowered — either by the user during a meeting/call, by Do Not Disturb, or accidentally. The user misses incoming calls as a result. The app solves this by automatically restoring the ringer after a grace period expires, unless the user intentionally chose to silence it for longer.

**Market context:** Shush! Ringer Restorer (~2.2M Play Store downloads, removed ~2019 when Android permission rules changed) proved demand for this category. No modern replacement exists that handles Samsung battery management, DND, and grace periods together.

---

## Platform & Language

- **Language:** Kotlin
- **Min SDK:** API 26 (Android 8.0)
- **Target SDK:** API 35 (Android 15) — required for Play Store; bump to API 36 when mandated
- **Compile SDK:** API 35
- **Build system:** Gradle (Android Studio)
- **Architecture:** Single-module app, no external dependencies required
- **Package name:** `com.ringerguard.app`
- **Distribution format:** Android App Bundle (`.aab`) for Play Store; debug APK for sideload

---

## Play Store Compliance Summary

| Area | Approach |
|------|----------|
| Foreground service type | `specialUse` — **not** `phoneCall` (reserved for dialer/VoIP apps) |
| Sensitive permissions | Request incrementally with prominent in-app disclosure before each prompt |
| Phone numbers | **Never** read, store, or transmit — call-state only, no `READ_CALL_LOG` |
| Data collection | None — declare "No data collected" in Data Safety form |
| Privacy policy | Required URL (simple static page; GitHub Pages is fine) |
| Closed testing | 12 testers opted-in for 14 consecutive days before production (personal dev accounts) |

---

## Permissions

### Manifest — required

```xml
<!-- Foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- Core ringer control -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

<!-- DND restore (special access — not a runtime permission) -->
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />

<!-- Boot restart + scheduling -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### Manifest — optional (feature-gated)

```xml
<!-- Only declared; requested at runtime when user enables "grace on call dismiss" -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
```

**Do NOT declare:**
- `FOREGROUND_SERVICE_PHONE_CALL` — requires default dialer or `MANAGE_OWN_CALLS`; wrong category for this app
- `USE_EXACT_ALARM` — auto-granted only to alarm-clock apps; use `SCHEDULE_EXACT_ALARM` with runtime request instead
- `READ_CALL_LOG`, `READ_CONTACTS`, `READ_SMS` — not needed in v1; high Play Store rejection risk

### Permission tiers

| Tier | Permission / Access | When to request | If denied |
|------|-------------------|-----------------|-----------|
| **Required** | Battery optimization exemption | First launch (after disclosure) | Show persistent banner; app may not survive Samsung battery saver |
| **Required** | DND policy access | When user enables "Disable DND on restore" (default: on) | Skip DND restore; volume/vibrate restore still works |
| **Required** | Exact alarm (`SCHEDULE_EXACT_ALARM`) | First launch, API 31+ | Fall back to inexact alarms with degraded reliability; warn user |
| **Optional** | `READ_PHONE_STATE` | When user enables "Start grace period on call dismiss" | Disable call-dismiss trigger; volume-change trigger still works |

### Prominent disclosure (Play Store requirement)

Before requesting `READ_PHONE_STATE`, show a non-dismissible dialog:

> **Why RingerGuard needs phone access**
>
> RingerGuard detects when you dismiss an incoming call (without answering) so it can start a grace period before restoring your ringer. RingerGuard does **not** access your phone number, call history, or contacts. This permission is optional — you can use all other features without it.

Log the user's acknowledgment timestamp in SharedPreferences (`phone_disclosure_ack_ms`).

---

## Service Manifest Declaration

```xml
<service
    android:name=".RingerService"
    android:exported="false"
    android:foregroundServiceType="specialUse">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="Monitors ringer volume and Do Not Disturb state on a schedule; restores user-configured ringer settings when unintentionally silenced." />
</service>
```

Play Console declaration (App content → Foreground service permissions):
- **Type:** `specialUse`
- **Use case:** Ringer volume monitoring and restoration
- **Justification:** App must run periodic background checks to detect and restore silenced ringer state; no standard FGS type covers scheduled audio/ringer monitoring

---

## App State Model

All persistent state is stored in `SharedPreferences` (key: `"ringer_guard_prefs"`).

| Key | Type | Description |
|-----|------|-------------|
| `silence_mode` | String | `"none"` / `"timed"` / `"indefinite"` |
| `grace_until_ms` | Long | Epoch ms when grace period expires (0 if none) |
| `grace_duration_min` | Int | User-configured grace period in minutes (default: 60) |
| `restore_volume_level` | Int | Volume level to restore to (default: max for device) |
| `check_interval_min` | Int | How often the service checks state (default: 5) |
| `restore_disable_dnd` | Boolean | Whether to disable DND on restore (default: true) |
| `restore_disable_vibrate` | Boolean | Whether to disable vibrate-only on restore (default: true) |
| `grace_on_volume_change` | Boolean | Volume change triggers grace period (default: true) |
| `grace_on_call_dismiss` | Boolean | Call dismiss triggers grace period (default: true) |
| `call_dismiss_enabled` | Boolean | Whether call-dismiss feature is active (false until READ_PHONE_STATE granted) |
| `quiet_hours_enabled` | Boolean | Whether quiet hours are active (default: false) |
| `quiet_start_hour` | Int | Quiet hours start (24h, default: 22) |
| `quiet_start_min` | Int | Default: 0 |
| `quiet_end_hour` | Int | Quiet hours end (24h, default: 7) |
| `quiet_end_min` | Int | Default: 0 |
| `quiet_days` | String | Comma-separated day ints: "2,3,4,5,6" = Mon–Fri |
| `last_restored_ms` | Long | Epoch ms of last auto-restore event |
| `service_start_ms` | Long | Epoch ms when service last started |
| `phone_disclosure_ack_ms` | Long | When user acknowledged phone permission disclosure (0 if never) |
| `is_restoring_volume` | Boolean | Transient flag — true while `restoreRinger()` is executing (prevents feedback loop) |

---

## Components

### 1. `RingerService.kt` — Foreground Service

The core of the app. Runs 24/7 as a foreground service.

**Startup behavior:**
- Start as `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`
- Show a persistent notification (see Notification spec below)
- Register `VolumeChangeReceiver` dynamically
- Register `CallStateReceiver` dynamically **only if** `call_dismiss_enabled` is true
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
1. Set is_restoring_volume = true (suppress VolumeChangeReceiver feedback)
2. Set AudioManager STREAM_RING volume to restore_volume_level
3. If restore_disable_dnd AND DND access granted:
     set NotificationManager to INTERRUPTION_FILTER_ALL
4. If restore_disable_vibrate:
     set AudioManager ringer mode to RINGER_MODE_NORMAL
5. Set is_restoring_volume = false
6. Update last_restored_ms in SharedPreferences
7. Log event to event log (see Event Log spec)
8. Update widget
9. Update foreground notification
```

**Scheduling:**
- Use `AlarmManager.setExactAndAllowWhileIdle()` — mandatory for battery saver compatibility
- On API 31+, check `alarmManager.canScheduleExactAlarms()` before scheduling; if false, prompt user via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`
- Use a `PendingIntent` targeting `AlarmReceiver`
- Reschedule immediately after each check completes

**Service restart:**
- Override `onStartCommand` to return `START_STICKY`
- Register `BootReceiver` to restart on device boot (see Boot Receiver notes)

---

### 2. `AlarmReceiver.kt` — BroadcastReceiver

Receives the periodic `AlarmManager` intent.

- Acquires a `WakeLock` for the duration of the check
- Triggers `RingerService` check logic via `startForegroundService()` or internal check method
- Releases `WakeLock` when done
- Must complete in under 10 seconds

---

### 3. `VolumeChangeReceiver.kt` — BroadcastReceiver

Listens for `AudioManager.VOLUME_CHANGED_ACTION`.

**Logic:**
```
1. If is_restoring_volume == true → ignore (app-initiated change)
2. Check if the changed stream is STREAM_RING
3. Check if new volume < previous volume (user lowered it)
4. If grace_on_volume_change is true → start grace period
5. Log event
```

Register dynamically in `RingerService` (not in manifest) — unregister on service destroy.

---

### 4. `CallStateReceiver.kt` — BroadcastReceiver

Listens for `TelephonyManager.ACTION_PHONE_STATE_CHANGED`.

**Only registered when** `call_dismiss_enabled == true` (READ_PHONE_STATE granted + user toggle on).

**Logic:**
```
1. Track call state transitions only — do NOT read phone number from extras
2. Watch for transition: RINGING → IDLE (call dismissed without answering)
3. If grace_on_call_dismiss is true → start grace period
4. Log event with trigger = "call dismissed"
```

Register dynamically in `RingerService` when permission is granted — **not** in manifest.

**Grace period start helper (`GracePeriodHelper.kt`):**
```kotlin
fun startGracePeriod(context: Context, triggerReason: String) {
    val prefs = context.getSharedPreferences("ringer_guard_prefs", Context.MODE_PRIVATE)
    val durationMin = prefs.getInt("grace_duration_min", 60)
    val expiresAt = System.currentTimeMillis() + (durationMin * 60 * 1000L)
    prefs.edit()
        .putString("silence_mode", "timed")
        .putLong("grace_until_ms", expiresAt)
        .apply()
    EventLogger.log(context, "Silenced — $triggerReason", "grace: ${durationMin}min", "silenced")
    RingerWidget.update(context)
    // Notify service to refresh foreground notification
}
```

---

### 5. `BootReceiver.kt` — BroadcastReceiver

Receives `BOOT_COMPLETED` and `LOCKED_BOOT_COMPLETED` (direct-boot aware if needed).

**Android 15+ behavior:** Do not call `startForeground()` directly from the receiver. Instead:
```
1. Receive BOOT_COMPLETED
2. Call startForegroundService(RingerService) — service promotes itself to foreground in onStartCommand
3. Log "Service restarted after boot"
```

Register in manifest:
```xml
<receiver
    android:name=".BootReceiver"
    android:exported="true"
    android:directBootAware="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

---

### 6. `RingerWidget.kt` — AppWidgetProvider

Home screen widget.

**Widget layout (`ringer_widget_layout.xml`):**
- App name label (small, top left)
- Status badge (top right): "🟢 Ringing" / "🔕 Silenced" / "∞ Indefinite"
- Countdown display (center): `47:23` or `∞` or hidden when ringing
- Countdown sublabel: "minutes remaining" or "silence until re-enabled"
- Three buttons (bottom row):
  - `🔕 Silence` — label shows configured duration, e.g. "🔕 60 min" (uses `grace_duration_min`, not hardcoded "1 Hour")
  - `∞ Indefinite` — triggers `ACTION_SILENCE_INDEFINITE`
  - `🔔 Ring Now` — triggers `ACTION_RING_NOW`
- Buttons that aren't applicable in current state should appear visually dimmed (alpha 0.4)

**Widget actions (PendingIntents → `WidgetActionReceiver`):**

| Action constant | Behavior |
|-----------------|----------|
| `ACTION_SILENCE_TIMED` | Set silence_mode="timed", grace_until = now + grace_duration_min |
| `ACTION_SILENCE_INDEFINITE` | Set silence_mode="indefinite" |
| `ACTION_RING_NOW` | Set silence_mode="none", call restoreRinger() immediately |

**Widget update logic:**
- Called after every state change
- Separate `AlarmManager` tick every 60 seconds for countdown refresh — do **not** rely on `updatePeriodMillis`
- Read current state from SharedPreferences
- Compute remaining: `(grace_until_ms - now)`
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
    android:updatePeriodMillis="0"
    android:initialLayout="@layout/ringer_widget_layout"
    android:resizeMode="horizontal|vertical"
    android:description="@string/widget_description" />
```

---

### 7. `WidgetActionReceiver.kt` — BroadcastReceiver

Receives button taps from the widget.

- Handles `ACTION_SILENCE_TIMED`, `ACTION_SILENCE_INDEFINITE`, `ACTION_RING_NOW`
- Updates SharedPreferences
- Calls `restoreRinger()` if Ring Now
- Calls `RingerWidget.update(context)` after any change
- Logs event

---

### 8. `EventLogger.kt` — Utility class

Simple append-only event log stored locally.

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
- Persist as JSON array to `files/event_log.json` (app-private storage)
- Expose `getAll(): List<LogEntry>` and `log(context, title, detail, type)`
- **Never** log phone numbers or contact names

---

### 9. `MainActivity.kt` — Main App UI

Four-tab bottom navigation:

#### Tab 1: Home
- **Status card** (full-width, color-coded):
  - Green card: "🔔 Ringing — Your ringer is active"
  - Orange card: "🔕 Silenced — `47:23` remaining"
  - Purple card: "∞ Indefinite — Silence until re-enabled"
- **Quick Actions:**
  - `🔕 Silence` button — label reflects `grace_duration_min` (e.g. "60 min")
  - `∞ Indefinite` button
  - `🔔 Ring Now` button (full width, prominent)
  - `⏱️ Custom Duration` button → number picker dialog (minutes)
- **Live Status panel:**
  - Current ringer volume (`X / max`)
  - Do Not Disturb: On/Off
  - Vibrate only: On/Off
  - Service running: Yes/No with color dot
- Refresh live status every 5 seconds while app is in foreground
- **Setup banner** (shown until all required permissions granted): links to fix missing battery exemption, DND access, exact alarm

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
- **Current status row:** "Right now: Active hours" + "Next quiet period: 10:00 PM tonight"

> **v2 (deferred — Play Store complexity):** Starred contacts exception and repeat-caller override moved to Out of Scope. Both require Contacts/Call Log permissions with strict Play policy.

#### Tab 4: Settings
- **Grace Period slider:** 15 / 30 / 60 / 120 / 240 minutes
- **Volume Restore Level slider:** 1–max (device-dependent)
- **Check Interval selector:** 5 / 10 / 15 / 30 min (segmented button)
- **Restore Options toggles:**
  - Disable Do Not Disturb on restore → prompts DND access if not granted
  - Disable Vibrate-only on restore
  - Start grace period on volume change
  - Start grace period on call dismiss → shows disclosure dialog, then requests READ_PHONE_STATE
- **System Health card:**
  - Battery optimization exemption: Active / Inactive (tap to fix)
  - DND access: Granted / Not granted (tap to fix)
  - Exact alarm permission: Granted / Not granted (tap to fix, API 31+)
  - Phone state permission: Granted / Not granted / Not requested
  - Service uptime
  - Last auto-restore time
  - Samsung tip: link/card explaining Settings → Battery → Unrestricted
- **About section:**
  - Privacy policy link
  - App version
  - Open-source licenses (if any)

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

Tap on notification body opens `MainActivity`.

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

**Interaction with timed silence:** If user is in a grace period (`silence_mode == "timed"`) during quiet hours, restore still fires when grace expires — the user explicitly chose to silence. Quiet hours only suppresses proactive restore when `silence_mode == "none"`.

---

## Battery Saver Survival Strategy

1. **Foreground service** declared as `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`
2. **`AlarmManager.setExactAndAllowWhileIdle()`** for all scheduled checks — regular `Handler` and `WorkManager` are throttled in battery saver
3. **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** — prompt user on first launch via `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` intent
4. **`START_STICKY`** — service restarts itself if killed
5. **`BOOT_COMPLETED`** — restarts after reboot (service self-promotes to foreground)
6. **WakeLock** held only for the duration of the check (~50ms), released immediately after

**On first launch**, if battery optimization exemption is not granted:
- Show a prominent banner on the Home tab
- Prompt system dialog automatically
- Re-check exemption status every time the app is foregrounded

**Samsung-specific:** Warn in Settings that user may also need Settings → Battery → Background usage limits → Unrestricted.

**Check in Settings tab:** `powerManager.isIgnoringBatteryOptimizations(packageName)`

---

## Privacy & Data Safety (Play Store)

| Data type | Collected? | Shared? | Notes |
|-----------|-----------|---------|-------|
| Personal info | No | No | |
| Financial info | No | No | |
| Location | No | No | |
| Photos/videos | No | No | |
| Audio | No | No | Does not record |
| Contacts | No | No | v1 does not access contacts |
| Call logs | No | No | |
| Phone number | No | No | Call-state transitions only; no number read |
| App activity (event log) | Yes, on-device only | No | Stored in app-private file |
| Device IDs | No | No | |

**Privacy policy must state:**
- What the app does (monitors/restores ringer)
- What permissions are used and why
- That no data leaves the device
- How to contact the developer
- That users can uninstall to delete all data

Host at a stable URL before Play Store submission (e.g. GitHub Pages: `https://<username>.github.io/ringerguard/privacy.html`).

---

## Play Store Publishing Checklist

### One-time setup
- [ ] Create Google Play Console developer account ($25 one-time)
- [ ] Complete identity verification
- [ ] Enable 2-step verification on Google account

### App content declarations (Play Console)
- [ ] **Foreground service types:** declare `specialUse` with justification
- [ ] **Sensitive permissions:** declare `READ_PHONE_STATE` as optional feature
- [ ] **Data Safety form:** "No data collected" / on-device logs only
- [ ] **Privacy policy URL**
- [ ] **Content rating** questionnaire (likely "Everyone")
- [ ] **Target audience:** not designed for children

### Store listing
- [ ] App title: "RingerGuard — Ringer Restore"
- [ ] Short description (~80 chars): e.g. "Auto-restore your ringer after meetings. Never miss a call again."
- [ ] Full description explaining the problem, grace period concept, and Samsung setup tips
- [ ] Screenshots: Home (ringing), Home (silenced), Widget, Settings, Log
- [ ] Feature graphic (1024×500)
- [ ] App icon (512×512)

### Testing (personal developer accounts created after Nov 2023)
- [ ] Upload to **Closed testing** track
- [ ] Recruit **12 testers** (friends/family) opted-in for **14 consecutive days**
- [ ] Apply for **Production access** with testing questionnaire
- [ ] Address any policy review feedback

### Build requirements
- [ ] Release build signed with upload key (Play App Signing enabled)
- [ ] `.aab` format
- [ ] ProGuard/R8 minification enabled for release
- [ ] No debug logging in release builds

---

## File Structure

```
app/src/main/
├── java/com/ringerguard/app/
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
│   ├── PermissionHelper.kt
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
│   │   ├── dialog_permission_disclosure.xml
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

1. **Do not use `WorkManager`** — throttled in battery saver. Use `AlarmManager.setExactAndAllowWhileIdle()` exclusively for scheduling.

2. **Do not use `FOREGROUND_SERVICE_TYPE_PHONE_CALL`** — wrong category; use `specialUse` with manifest property and Play Console declaration.

3. **`NotificationManager.setInterruptionFilter()`** requires DND policy access. Check `NotificationManager.isNotificationPolicyAccessGranted()` before calling; prompt via `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` if not granted.

4. **Volume listener feedback loop:** Set `is_restoring_volume = true` before programmatic volume changes in `restoreRinger()`; `VolumeChangeReceiver` must ignore events while flag is set.

5. **Widget countdown:** Use dedicated `AlarmManager` tick every 60 seconds — `updatePeriodMillis` is throttled/unreliable; set to `0` in widget provider XML.

6. **Samsung-specific:** Battery optimization exemption + Settings → Battery → Unrestricted. Document in Settings UI.

7. **SharedPreferences** should use `apply()` (async) not `commit()` (sync on main thread).

8. **All UI updates from service** must be posted to the main thread via `Handler(Looper.getMainLooper()).post {}`.

9. **CallStateReceiver:** Register dynamically only when permission granted. Never read `EXTRA_INCOMING_NUMBER` or store phone numbers.

10. **Target color scheme** (dark theme): green `#34d399`, orange `#fb923c`, purple `#a78bfa`, blue `#60a5fa`, background `#0d1520`, card background `rgba(255,255,255,0.04)`.

11. **Release builds:** Strip Log.d/Log.v calls or gate behind `BuildConfig.DEBUG`.

12. **Exact alarms on API 31+:** Check `canScheduleExactAlarms()`; if false, show Settings link via `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.

---

## First Launch Flow

Staggered onboarding — do not bombard with all permission dialogs at once.

```
1. Show MainActivity with Home tab
2. Show welcome/setup card explaining what the app does (1-2 sentences)
3. Start RingerService via startForegroundService()
   (service starts with volume monitoring; DND/call features degrade gracefully until permissions granted)
4. Prompt battery optimization exemption (required for reliability)
   → If denied: show persistent banner, app still runs with degraded reliability
5. On API 31+: check exact alarm permission
   → If denied: show banner with link to Settings
6. When user enables "Disable DND on restore" (default ON):
   → Check NotificationManager.isNotificationPolicyAccessGranted()
   → If not granted: explain, then open ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
7. When user enables "Start grace period on call dismiss":
   → Show prominent disclosure dialog (see Permissions section)
   → Request READ_PHONE_STATE at runtime
   → If granted: set call_dismiss_enabled = true, register CallStateReceiver
   → If denied: disable toggle, explain feature unavailable
8. Prompt user to add home screen widget (optional tip card)
```

---

## Development Phases

### Phase 1 — MVP (personal sideload)
- RingerService with specialUse FGS
- AlarmManager scheduling + restore logic
- Volume change → grace period
- SharedPreferences state model
- Home screen widget (silence / ring now)
- Home tab + Settings tab
- Event log (basic)
- Battery exemption + exact alarm prompts

### Phase 2 — Full app
- Log tab with 7-day summary
- Schedule tab (quiet hours)
- Notification "Ring Now" action
- Call dismiss → grace period (optional permission)
- DND restore
- Samsung setup guide in Settings

### Phase 3 — Play Store
- Privacy policy page
- Store listing assets (screenshots, descriptions)
- Closed testing (12 testers × 14 days)
- Production submission
- Respond to policy review feedback

---

## Out of Scope

### v1 (not building now)
- Cloud sync or backup of settings
- Multiple profiles
- Tasker/automation integration
- Lock screen widget
- Wear OS companion
- Analytics or crash reporting SDKs (keeps privacy story clean; reconsider only if needed)

### v2 (future — high Play Store friction)
- **Starred contacts exception** — requires READ_CONTACTS; strict Play policy
- **Repeat caller override** — requires READ_PHONE_STATE + potentially READ_CALL_LOG; store last caller number on-device only with clear disclosure
- Paid tier / in-app purchases
- Localization beyond English
