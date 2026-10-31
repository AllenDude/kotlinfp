# Checklist Restocker (native Android)

Native Kotlin + Jetpack Compose port of the Orencio Mendoza order checklist PWA.
Autosaves the current list, sends it through the Android share sheet, keeps order history,
and lets you edit items and types inside the app.

## First-time setup (about 10 minutes)

1. Create a NEW empty GitHub repo (keep your PWA repo untouched) and push this folder to its `main` branch.

2. Create your signing key ONCE and keep the file somewhere safe (also back it up):

       keytool -genkeypair -v -keystore release.jks -alias restocker -keyalg RSA -keysize 2048 -validity 10000

3. Turn the key into text:

   - Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) | Set-Clipboard`
   - macOS / Linux: `base64 -w0 release.jks`

4. In the repo: Settings > Secrets and variables > Actions > New repository secret. Add four:

   | Name | Value |
   |---|---|
   | `KEYSTORE_BASE64` | the text from step 3 |
   | `KEYSTORE_PASSWORD` | the keystore password you chose |
   | `KEY_ALIAS` | `restocker` |
   | `KEY_PASSWORD` | the key password you chose |

5. Do this BEFORE the first build you plan to keep. Without the secrets the workflow signs with a
   throwaway debug key, and an APK signed that way can't be updated in place later.

6. Push, or open the Actions tab and run "Build APK". Download the `ChecklistRestocker-apk` artifact,
   unzip it, and install `app-release.apk` on your phone.

Every later build uses the same key and a higher version number, so it installs over the old app
and keeps all saved data.

## Building locally

Open the folder in Android Studio and press Run. (No Gradle wrapper is included; Android Studio
will offer to set one up, or run `gradle wrapper` once.)

## Where things are

- `app/src/main/assets/catalog.json`: the starting item list (121 items, stable ids, original order)
- `data/`: Room database, catalog JSON, repository
- `domain/OrderText.kt`: how the sent list is written; `domain/Codes.kt`: share codes
- `ui/`: Compose screens
- Old PWA share codes (`0001...`) can still be pasted into Import code.

## If you change the database tables

Bump `version` in `AppDatabase` and add a Room Migration. Never use destructive migration, or saved
orders get wiped on update.
