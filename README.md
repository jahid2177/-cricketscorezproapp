# Cricket Scorez Pro (Android)

A native Android app (Java) for cricket scoring, analysis, and tournament management.

## Build requirements

- JDK 17
- Gradle 8.11.1 (via the included `gradlew` wrapper — no manual install needed)
- Android Gradle Plugin 8.9.1 (declared in the root `build.gradle`) — this
  version was chosen because it's the newest AGP 8.x release that still
  supports compileSdk/targetSdk 36, and it works with the Gradle Tooling API
  client bundled in most mobile Android IDEs (AGP 9.x needs Gradle 9.x, which
  those apps often can't talk to yet)
- Android SDK Platform 36 installed (compileSdk / targetSdk = 36)

## Building in Android Code Studio (mobile)

1. Open this project folder in Android Code Studio.
2. Make sure the app's SDK Manager has **Android SDK Platform 36** and a recent
   **Android Build-Tools** version installed — download them from the SDK
   Manager screen if missing.
3. Let it sync Gradle (it will download the Gradle 8.11.1 distribution the
   first time — needs an internet connection).
4. Run **Build → Build APK** (or the equivalent action) to produce a debug APK.

## Building in desktop Android Studio

1. **File → Open** and select this project directory.
2. Let Android Studio sync Gradle and download any missing SDK components it
   prompts for.
3. Run the app on an emulator or a physical device, or **Build → Build Bundle(s)/APK(s)**.

## Firebase setup (required for login / live sync features)

This project ships with a **placeholder** `app/google-services.json` just so
the build doesn't fail. It will compile, but Firebase Auth/Database calls will
not actually work until you replace it with a real one:

1. Create a project in the [Firebase console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.cricketscorez.pro`.
3. Download the real `google-services.json` and replace
   `app/google-services.json` with it.

## Signing

A debug keystore (`debug.keystore`, standard `android`/`androiddebugkey`/`android`
credentials) is included at the project root so debug builds sign correctly
out of the box. For a **release** build, create your own release keystore and
add a matching `signingConfigs.release` block in `app/build.gradle` — do not
ship using the debug keystore.

## Notes

- This project previously included Google AI Studio boilerplate instructions
  (Kotlin DSL / `.env` / `GEMINI_API_KEY`) left over from an unrelated
  template; those don't apply here since this is a plain native Java Android
  app and have been removed.
