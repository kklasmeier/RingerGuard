# RingerGuard — Play Store listing copy

Paste these into Google Play Console. Adjust wording if you like before publishing.

## App name (30 chars max)

```
RingerGuard
```

## Short description (80 chars max)

```
Auto-restore your ringer after meetings. Never miss a call again.
```

## Full description (4000 chars max)

```
Your phone ends up silenced more often than you think — after a meeting, a quick volume button press, Do Not Disturb, or vibrate-only mode. RingerGuard quietly watches your ringer and brings it back unless you explicitly chose to silence it.

HOW IT WORKS
• Runs a lightweight background service that checks your ringer on a schedule you control
• If your phone is silenced unintentionally, RingerGuard restores it after a grace period
• Use the home screen widget for quick silence, indefinite silence, or ring now
• Quiet hours let you pause automatic restores overnight or during meetings you schedule

YOU STAY IN CONTROL
• Timed silence and indefinite silence are always intentional — RingerGuard will not override them
• Optional grace period after volume is lowered or a call is dismissed
• Choose full-volume restore or preserve your previous volume before silence
• Optional restore of vibrate-only mode and Do Not Disturb

BUILT FOR REAL PHONES
• Designed with Samsung battery management in mind
• Setup guidance for battery optimization and exact alarms
• Works without accounts, ads, or cloud sync

PRIVACY
• No data collection, no analytics, no ads
• Settings and logs stay on your device
• Phone state permission is optional and only used for call-dismiss grace period if you enable it

PERMISSIONS EXPLAINED
RingerGuard needs background and audio-related permissions to monitor and restore your ringer. Optional phone state access is only requested if you turn on grace period after dismissing a call. The app does not read your phone number, contacts, or call history.

Note: Results may vary by device manufacturer and Android version. For best results on Samsung phones, follow the in-app battery setup tips.
```

## Category

**Tools** (or **Productivity**)

## Tags / keywords (for your own ASO notes)

```
ringer restore, unmute phone, silence fix, samsung ringer, DND restore, volume restore, missed calls
```

## Contact email

Use an email you check regularly — Play Console requires a public developer contact.

## Privacy policy URL

After enabling GitHub Pages (see PLAY_STORE_GUIDE.md):

```
https://kklasmeier.github.io/RingerGuard/privacy-policy.html
```

## Foreground service (special use) justification

```
RingerGuard runs a user-enabled foreground service that periodically checks whether the phone ringer was unintentionally silenced and restores the user's configured ringer settings after an optional grace period. The service must run in the background between user interactions so the phone does not stay silent after meetings, accidental volume changes, or Do Not Disturb. Users explicitly start protection from the app and can stop it by disabling the service or uninstalling the app.
```

## READ_PHONE_STATE declaration notes

- Declare as **optional** functionality
- Used only when user enables **Grace period on call dismiss**
- App does not read phone numbers, call logs, or contacts

## Data safety (quick answers)

| Question | Answer |
|----------|--------|
| Collects or shares user data? | No |
| Data encrypted in transit | N/A (no transmission) |
| Data deletion request | Uninstall app |
| Independent security review | No |

If asked about on-device app activity / diagnostics:
- **Collected:** Yes, on-device only (event log)
- **Shared:** No
- **Purpose:** App functionality
- **Optional:** Yes (user can ignore/clear log)

## Screenshots to capture (phone)

1. Home — ringer on (green status)
2. Home — timed silence with countdown
3. Home screen widget (1-row and 2-row if possible)
4. Settings — grace period and restore options
5. Event log screen

Minimum: 2 screenshots. Recommended: 4–8.

## Feature graphic

Create a **1024 × 500** banner in any design tool. Suggested text:

```
RingerGuard
Never stay silenced by accident
```

Use dark navy background `#0D1520` and green accent `#34D399`.

## High-res icon

Play Console requires **512 × 512 PNG**. Export from Android Studio:
**File → New → Image Asset**, or use your bell + waves artwork at 512×512.
