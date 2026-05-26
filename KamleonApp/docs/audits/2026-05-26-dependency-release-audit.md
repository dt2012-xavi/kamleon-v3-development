# Conservative Release Dependency Audit - 2026-05-26

## Goal

Prepare a low-variability Android release for the Google Play target API deadline while adding crash monitoring.

## Applied Release Changes

- App version bumped from `versionCode 2025` / `versionName 1.0.14` to `versionCode 2026` / `versionName 1.0.15`.
- Added Firebase Crashlytics Gradle plugin `com.google.firebase.crashlytics:3.0.6`.
- Added Firebase Crashlytics runtime dependency through the existing Firebase BOM.
- Updated Google Services Gradle plugin from `4.4.3` to `4.4.4`.
- Removed a duplicate Firebase Analytics dependency declaration, leaving the BOM-managed dependency in place.

## Deadline Fit

- Project already uses `compileSdkVersion 36` and `targetSdk 36`.
- The Google Play requirement for app updates is API 35 or higher for the current deadline window, so no target SDK bump was needed.
- Source: https://developer.android.com/google/play/requirements/target-sdk

## Current Important Resolved Versions

From `:app:developmentDebugRuntimeClasspath`:

- Firebase BOM: `34.2.0`
- Firebase Analytics: `23.0.0`
- Firebase Crashlytics: `20.0.1`
- Firebase Auth: `24.0.1`
- Firebase Firestore: `26.0.0`
- Firebase Storage: `22.0.0`
- Firebase Database: `22.0.0`
- Firebase Functions: `22.0.0`
- Firebase Messaging: `25.0.0`
- CameraX: `1.5.0`
- ML Kit barcode scanning: `17.3.0`
- Play Services ML Kit barcode scanning transitive dependency: `18.3.1`
- ZXing core: `3.5.3`
- Kotlin stdlib resolved to `2.2.10`
- OkHttp resolved to `4.12.0`

## Conservative Update Recommendation

For this release, keep the dependency surface stable:

- Ship Crashlytics and Google Services plugin update only.
- Do not upgrade broad UI/runtime dependencies in this release unless Google Play, Play Console pre-launch, or vulnerability tooling flags a concrete blocker.
- Defer larger cleanups such as Compose BOM, CameraX API migration, Accompanist replacement, Room/KSP modernization, and minification enablement to a separate hardening release.

## Follow-Up Candidates

- Remove duplicated direct dependency declarations, especially CameraX core and Material Components.
- Replace deprecated `setTargetResolution` with current CameraX resolution selector APIs.
- Replace Accompanist system UI controller usage with supported edge-to-edge/system bar APIs.
- Decide whether release builds should enable `minifyEnabled true` after a dedicated regression pass and ProGuard/R8 rule review.

## Verification Notes

- `./gradlew :app:assembleDevelopmentDebug :app:testDevelopmentDebugUnitTest :app:lintDevelopmentDebug` passed after adding Crashlytics and the QR analyzer close-path fix.
- Lint report generated at `app/build/reports/lint-results-developmentDebug.html`.
