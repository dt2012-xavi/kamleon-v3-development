# Conservative Release Design

## Goal

Prepare a low-variability Android release for Google Play deadline readiness by adding Firebase Crashlytics, making only low-risk dependency updates, and auditing security plus QR scanning behavior.

## Scope

In scope:

- Add Firebase Crashlytics using the official Android Gradle plugin and Firebase BoM.
- Keep `targetSdk 36` and `compileSdk 36`; the project already satisfies the current Play update target requirement.
- Increment release version from `versionCode 2025` / `versionName 1.0.14` to `versionCode 2026` / `versionName 1.0.15`.
- Update only dependencies with low expected code impact or required by Crashlytics documentation.
- Run a static audit for risky manifest settings, sensitive logs, force unwraps, outdated dependencies, generated artifacts, and Firebase/Room/Camerax/QR hotspots.
- Audit QR scanning from two paths:
  - external app link: `https://<host>/scan?unitId=...&sessionId=...`
  - in-app CameraX + ML Kit scanner.

Out of scope for this release:

- Large QR scanner rewrite.
- Replacing CameraX/ML Kit with Google Code Scanner.
- Migrating XML screens to Compose.
- Room schema redesign or destructive migrations.
- Broad dependency modernization that forces source changes.

## Design

Crashlytics will be added at Gradle level only: root plugin declaration, app plugin application, and `firebase-crashlytics` dependency under the existing Firebase BoM. No custom runtime crash logging will be added in this pass, to avoid behavior changes.

Dependency updates will be conservative. The project already has recent AGP, Kotlin, compile SDK, target SDK, CameraX, Hilt, Room, Navigation, Firebase BoM, and AndroidX versions. The only planned update is Google Services plugin `4.4.3` to `4.4.4`, matching Firebase Crashlytics documentation. Any additional suggested updates will be documented as follow-up rather than applied unless build tooling reports a hard requirement.

Security audit output will be written as documentation, not mixed into unrelated fixes. Critical, tiny fixes may be applied only if they are directly release-blocking and low risk.

QR audit will inspect lifecycle, analyzer resource handling, parsing, duplicate upload risk, deep link validation, camera compatibility settings, model availability, threading, and known ML Kit guidance. Findings will be ranked by severity with recommended release action.

## Verification

Minimum verification for this release:

- `./gradlew :app:assembleDevelopmentDebug`
- `./gradlew :app:lintDevelopmentDebug`
- `./gradlew :app:testDevelopmentDebugUnitTest`

If an emulator/device is available, manual QR verification should be run on at least one physical Android 13+ device and one Android 15/16 emulator or device.
