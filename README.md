# FP (Checklist Restocker, native Android)

Native Kotlin + Jetpack Compose port of the Orencio Mendoza order checklist PWA.
Autosaves the current list, sends it through the Android share sheet, keeps order history,
and lets you edit items and types inside the app.

## Building and installing

1. Push this folder to your GitHub repo's `main` branch (the workflow is `.github/workflows/build.yml`).
2. Every push builds a signed APK and publishes it on the repo's **Releases** page (v1.0.1, v1.0.2, ...).
   The same file is also under the run's Artifacts.
3. Download the newest APK on your phone and install it. It updates the existing app in place and keeps
   your saved lists.

No secrets or setup are needed. The signing key is `app/restocker.jks`, committed on purpose, so every
build is signed identically. Do not delete or regenerate it after you have installed the app.

If Android ever says the app "conflicts with an existing package", the installed copy was signed with a
different key. Uninstall that copy once (this erases its data) and install the new one; after that all
updates work.

Tip: keep the repo private. Anyone who can read the repo can read the key.

## Building locally

Open the folder in Android Studio and press Run. (No Gradle wrapper is included; Android Studio
will offer to set one up, or run `gradle wrapper` once.) Debug builds use the same key, so they update
release builds and the other way round.

## Where things are

- `app/src/main/assets/catalog.json`: the starting item list (121 items, stable ids, original order)
- `data/`: Room database, catalog JSON, repository
- `domain/OrderText.kt`: how the sent list is written; `domain/Codes.kt`: share codes
- `ui/`: Compose screens
- Old PWA share codes (`0001...`) can still be pasted into Import code.

## If you change the database tables

Bump `version` in `AppDatabase` and add a Room Migration. Never use destructive migration, or saved
orders get wiped on update.
