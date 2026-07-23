# Publish RingerGuard to Google Play

Step-by-step guide for your first Play Store release. Work through the sections in order.

---

## Overview

| Step | What | Time |
|------|------|------|
| 1 | Play Console account | ~30 min + identity verification (1–2 days) |
| 2 | Privacy policy live on the web | ~15 min |
| 3 | Release signing key + AAB build | ~30 min |
| 4 | Store listing + graphics | 1–2 hours |
| 5 | Policy declarations in Console | ~45 min |
| 6 | Closed testing (12 testers × 14 days) | **14 days minimum** |
| 7 | Production release | Review ~1–7 days |

> **Important:** Personal developer accounts created after November 2023 must run a **closed test with at least 12 testers for 14 consecutive days** before applying for production access.

---

## Step 1 — Create a Play Console developer account

1. Go to [Google Play Console](https://play.google.com/console)
2. Sign in with your Google account
3. Pay the **one-time $25** registration fee
4. Complete **identity verification** (government ID — can take 1–2 business days)
5. Enable **2-step verification** on your Google account

---

## Step 2 — Publish the privacy policy

Play Store requires a **public HTTPS URL** before you can publish.

### Option A — GitHub Pages (recommended, free)

1. Push this repo to GitHub (`kklasmeier/RingerGuard`)
2. On GitHub: **Settings → Pages**
3. Source: **Deploy from a branch**
4. Branch: **main** → folder **/docs**
5. Save. After a minute, your policy is live at:

   ```
   https://kklasmeier.github.io/RingerGuard/privacy-policy.html
   ```

6. Open that URL in a browser to confirm it loads

The policy file is already in `docs/privacy-policy.html`.

---

## Step 3 — Create your upload keystore (one time)

Open PowerShell in the `android` folder:

```powershell
cd c:\Projects\andriod-RingGuard\android
& "$env:JAVA_HOME\bin\keytool.exe" -genkey -v `
  -keystore ringerguard-upload.jks `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias upload
```

You'll be prompted for:
- Keystore password (remember this!)
- Key password (can be the same)
- Your name, organization, city, etc.

**Back up `ringerguard-upload.jks` and passwords somewhere safe** (password manager + offline backup). If you lose them, you cannot update the app on Play Store.

### Configure signing

1. Copy the template:
   ```powershell
   copy keystore.properties.template keystore.properties
   ```
2. Edit `android/keystore.properties` with your real passwords
3. Confirm `storeFile=ringerguard-upload.jks` points to the keystore you created

`keystore.properties` and `*.jks` are gitignored — never commit them.

---

## Step 4 — Build the release App Bundle (AAB)

```powershell
cd c:\Projects\andriod-RingGuard\android
$env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat bundleRelease
```

Output file:

```
android\app\build\outputs\bundle\release\app-release.aab
```

Upload **this `.aab` file** to Play Console — not the debug APK.

### Optional: test release build locally first

```powershell
.\gradlew.bat assembleRelease
adb install app\build\outputs\apk\release\app-release.apk
```

---

## Step 5 — Create the app in Play Console

1. Play Console → **Create app**
2. App name: **RingerGuard**
3. Default language: English (United States)
4. App or game: **App**
5. Free or paid: **Free**
6. Accept declarations

### Upload the first build

1. **Testing → Closed testing → Create track** (e.g. "Internal" or "Closed")
2. **Create new release**
3. Upload `app-release.aab`
4. Release name: `1.1.3` (or your current `versionName`)
5. Save (don't roll out yet until store listing is ready)

---

## Step 6 — Complete required Play Console forms

Work through **Policy → App content** and fix every item with a warning.

### Privacy policy
- URL: `https://kklasmeier.github.io/RingerGuard/privacy-policy.html`

### App access
- Select **All functionality is available without special access** (no login)

### Ads
- **No, my app does not contain ads**

### Content rating
- Start questionnaire → likely **Everyone** / low maturity
- No violence, gambling, user-generated content, etc.

### Target audience
- **18 and over** (or 13+ if you prefer; app is not for children)
- Not designed primarily for children

### Data safety
Use answers from `docs/store-listing.md`. Summary:
- **No data collected** for Play's purposes (on-device log only, not transmitted)
- No data shared with third parties
- No account required

### Foreground service permissions
Declare **special use** foreground service. Paste justification from `docs/store-listing.md`.

### Sensitive permissions — Phone
- `READ_PHONE_STATE` is **optional**
- Only used when user enables call-dismiss grace period
- Prominent disclosure is already shown in-app before requesting permission

### Health apps / Financial / Government etc.
- None apply — skip or answer No

---

## Step 7 — Store listing

Use copy from `docs/store-listing.md`.

### Required graphics

| Asset | Size | Notes |
|-------|------|-------|
| App icon | 512 × 512 PNG | Export bell + waves icon |
| Feature graphic | 1024 × 500 | Banner for store page |
| Phone screenshots | Min 2, max 8 | 16:9 or 9:16 phone captures |

Capture screenshots on your S23 from:
- Home (ringer on)
- Home (silenced + countdown)
- Widget on home screen
- Settings
- Event log

---

## Step 8 — Closed testing (required before production)

1. **Testing → Closed testing** → create release with your AAB
2. Add testers:
   - Create an email list, or
   - Use a Google Group, or
   - Share the opt-in link with friends/family
3. You need **at least 12 testers** opted in
4. Testers must stay opted in for **14 consecutive days**
5. Install the app from the Play Store test link (not sideload) to count as a tester

After 14 days:
1. **Publishing overview → Apply for production**
2. Answer the testing questionnaire (describe what you tested)
3. Submit for review

---

## Step 9 — Production release

1. **Production → Create new release**
2. Promote the tested AAB or upload the same / newer version
3. Roll out to **100%** (or staged rollout starting at 20%)
4. Review typically takes **1–7 days**

---

## Version numbers for future updates

In `android/app/build.gradle.kts`:

```kotlin
versionCode = 15   // must increase every upload
versionName = "1.2.0"
```

Every Play upload needs a **higher `versionCode`**. `versionName` is what users see.

---

## Checklist before you submit

- [ ] Privacy policy URL loads in a browser
- [ ] `bundleRelease` succeeds with signing configured
- [ ] Tested release build on your S23
- [ ] Store listing text and graphics uploaded
- [ ] Data safety form complete
- [ ] FGS special use declared
- [ ] Phone permission marked optional with justification
- [ ] 12 testers enrolled in closed testing
- [ ] Waited 14 days of closed testing
- [ ] Upload keystore backed up securely

---

## Common rejection reasons to avoid

1. **Missing privacy policy** — URL must work
2. **FGS not declared** — declare `specialUse` with clear justification
3. **Phone permission** — must be optional; in-app disclosure before request (already implemented)
4. **Misleading description** — don't claim 100% reliability on all devices
5. **Broken backup / test crash** — run release build on device before upload

---

## Need help?

- [Play Console Help](https://support.google.com/googleplay/android-developer)
- [Foreground service requirements](https://developer.android.com/develop/background-work/services/fgs)
- [Data safety form guide](https://support.google.com/googleplay/android-developer/answer/10787469)
